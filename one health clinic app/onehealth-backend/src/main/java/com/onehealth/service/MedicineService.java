package com.onehealth.service;

import com.onehealth.dto.*;
import com.onehealth.entity.*;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Owns the medicine catalog (org-level), per-clinic stock (batches + margin),
 * and the two-step prescribe -> bill workflow. Legal note baked into the
 * logic below: a "scheduled" medicine (DPCO/NLEM) has its retailer margin
 * capped at 16% by law - see Medicine.scheduledDrug javadoc - so margin
 * updates for those are rejected above that cap rather than silently allowed.
 *
 * Pricing authority is deliberately centralized at the OWNER: only the owner
 * sets a medicine's margin (updateMargin) and only the owner ever sees cost
 * price or profit. Clinic admins and doctors only ever see selling price -
 * see ClinicStockViewDTO / BillReceiptDTO and MedicineService's *View / *Receipt
 * builder methods below, as opposed to the owner-only ClinicMedicineStockDTO /
 * PrescriptionDTO ones.
 */
@Service
@RequiredArgsConstructor
public class MedicineService {

    private static final BigDecimal SCHEDULED_DRUG_MAX_MARGIN_PERCENT = new BigDecimal("16.0");

    private final MedicineRepository medicineRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ClinicMedicineStockRepository stockRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PrescribedItemRepository prescribedItemRepository;
    private final AppointmentRepository appointmentRepository;

    // --- Catalog (owner) ---

    @Transactional
    public MedicineDTO createMedicine(Long organizationId, MedicineRequest req) {
        if (req.getPackSize() != null && (req.getPackLabel() == null || req.getPackLabel().isBlank())) {
            throw new BusinessException("Give the pack a label (e.g. \"strip\") when setting a pack size.");
        }
        if (req.getPackSize() == null && req.getPackLabel() != null && !req.getPackLabel().isBlank()) {
            throw new BusinessException("A pack label needs a pack size (how many units per pack).");
        }
        Medicine medicine = medicineRepository.save(Medicine.builder()
                .organizationId(organizationId)
                .name(req.getName())
                .genericName(req.getGenericName())
                .manufacturer(req.getManufacturer())
                .unit(req.getUnit())
                .category(req.getCategory())
                .scheduledDrug(req.isScheduledDrug())
                .packSize(req.getPackSize())
                .packLabel(req.getPackLabel())
                .active(true)
                .build());
        return toDTO(medicine);
    }

    @Transactional(readOnly = true)
    public List<MedicineDTO> listMedicines(Long organizationId) {
        return medicineRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicineDTO setMedicineActive(Long organizationId, Long medicineId, boolean active) {
        Medicine medicine = getOwnedMedicine(organizationId, medicineId);
        medicine.setActive(active);
        return toDTO(medicineRepository.save(medicine));
    }

    // --- Stock dispatch (owner sends stock to a clinic) ---

    /** Full pricing view (cost, margin, profit potential) - OWNER-ONLY. */
    @Transactional(readOnly = true)
    public List<ClinicMedicineStockDTO> getClinicStock(Long clinicId) {
        List<ClinicMedicineStock> rows = stockRepository.findByClinicId(clinicId);
        return rows.stream().map(this::toStockDTO).collect(Collectors.toList());
    }

    /** Selling-price-only view, no cost/margin - for a clinic admin or doctor. */
    @Transactional(readOnly = true)
    public List<ClinicStockViewDTO> getClinicStockPublicView(Long clinicId) {
        List<ClinicMedicineStock> rows = stockRepository.findByClinicId(clinicId);
        return rows.stream().map(this::toStockViewDTO).collect(Collectors.toList());
    }

    /**
     * Owner sends (dispatches) a quantity of medicine to one clinic. Creates
     * the ClinicMedicineStock row (default 0% margin - the OWNER sets the
     * real margin afterwards via updateMargin, never the clinic) the first
     * time this clinic receives this medicine, then adds the batch and rolls
     * the quantity into quantityOnHand. See StockBatch's class javadoc for
     * why this is an owner action, not a clinic-admin one.
     *
     * Quantity is entered as either a number of packs (converted to base
     * units via the medicine's packSize) or directly in base units - see
     * DispatchStockRequest javadoc.
     */
    @Transactional
    public ClinicMedicineStockDTO dispatchStock(Long organizationId, Long ownerUserId, DispatchStockRequest req) {
        Medicine medicine = getOwnedMedicine(organizationId, req.getMedicineId());
        if (!medicine.isActive()) {
            throw new BusinessException("This medicine is inactive in the catalog and can't be dispatched.");
        }

        int totalBaseUnits;
        Integer packsReceived = null;
        if (req.getPacksReceived() != null) {
            if (medicine.getPackSize() == null) {
                throw new BusinessException(
                        "\"" + medicine.getName() + "\" has no pack size configured - dispatch it in base units ("
                        + medicine.getUnit() + ") instead.");
            }
            packsReceived = req.getPacksReceived();
            totalBaseUnits = packsReceived * medicine.getPackSize();
        } else if (req.getQuantityReceived() != null) {
            totalBaseUnits = req.getQuantityReceived();
        } else {
            throw new BusinessException(medicine.getPackSize() != null
                    ? "Enter the quantity as either packs (" + medicine.getPackLabel() + "s) or loose " + medicine.getUnit() + "s."
                    : "Enter the quantity dispatched.");
        }

        Long clinicId = req.getClinicId();
        ClinicMedicineStock stock = stockRepository.findByClinicIdAndMedicineId(clinicId, medicine.getId())
                .orElseGet(() -> ClinicMedicineStock.builder()
                        .clinicId(clinicId)
                        .medicine(medicine)
                        .marginPercent(BigDecimal.ZERO)
                        .quantityOnHand(0)
                        .reorderThreshold(10)
                        .build());

        stockBatchRepository.save(StockBatch.builder()
                .clinicId(clinicId)
                .medicine(medicine)
                .batchNumber(req.getBatchNumber())
                .costPrice(req.getCostPrice())
                .mrp(req.getMrp())
                .packsReceived(packsReceived)
                .quantityReceived(totalBaseUnits)
                .quantityRemaining(totalBaseUnits)
                .expiryDate(req.getExpiryDate())
                .receivedDate(req.getReceivedDate() != null ? req.getReceivedDate() : LocalDate.now())
                .dispatchedByOwnerId(ownerUserId)
                .build());

        stock.setQuantityOnHand(stock.getQuantityOnHand() + totalBaseUnits);
        return toStockDTO(stockRepository.save(stock));
    }

    /**
     * The shrinkage-detection report: for every (clinic, medicine) combo that
     * has ever had stock dispatched, shows how much went out, how much the
     * system has recorded as sold, and what should be left. Compare
     * `systemRemaining` against each clinic's physical shelf count to catch
     * gaps - see StockReconciliationDTO's javadoc.
     */
    @Transactional(readOnly = true)
    public List<StockReconciliationDTO> getReconciliation(Long organizationId, Map<Long, String> clinicNamesById) {
        List<StockBatch> allBatches = stockBatchRepository.findAll().stream()
                .filter(b -> clinicNamesById.containsKey(b.getClinicId()))
                .collect(Collectors.toList());

        Map<String, List<StockBatch>> byClinicMedicine = allBatches.stream()
                .collect(Collectors.groupingBy(b -> b.getClinicId() + ":" + b.getMedicine().getId()));

        List<StockReconciliationDTO> result = new java.util.ArrayList<>();
        for (List<StockBatch> batches : byClinicMedicine.values()) {
            StockBatch first = batches.get(0);
            Long clinicId = first.getClinicId();
            Medicine medicine = first.getMedicine();

            long dispatched = batches.stream().mapToLong(StockBatch::getQuantityReceived).sum();
            long remaining = batches.stream().mapToLong(StockBatch::getQuantityRemaining).sum();
            long sold = dispatched - remaining;

            result.add(StockReconciliationDTO.builder()
                    .clinicId(clinicId)
                    .clinicName(clinicNamesById.get(clinicId))
                    .medicineId(medicine.getId())
                    .medicineName(medicine.getName())
                    .unit(medicine.getUnit())
                    .totalDispatched(dispatched)
                    .totalSold(sold)
                    .systemRemaining(remaining)
                    .build());
        }
        result.sort((a, b) -> {
            int byClinic = a.getClinicName().compareTo(b.getClinicName());
            return byClinic != 0 ? byClinic : a.getMedicineName().compareTo(b.getMedicineName());
        });
        return result;
    }

    /**
     * OWNER-ONLY: set the chargeable margin for one medicine at one branch.
     * See ClinicMedicineStock's class javadoc for why this is not a
     * clinic-admin power.
     */
    @Transactional
    public ClinicMedicineStockDTO updateMargin(Long organizationId, Long clinicId, Long medicineId,
                                                UpdateMarginRequest req) {
        Medicine medicine = getOwnedMedicine(organizationId, medicineId);
        ClinicMedicineStock stock = stockRepository.findByClinicIdAndMedicineId(clinicId, medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("This clinic hasn't stocked this medicine yet."));

        if (medicine.isScheduledDrug() && req.getMarginPercent().compareTo(SCHEDULED_DRUG_MAX_MARGIN_PERCENT) > 0) {
            throw new BusinessException(
                    "This is a price-controlled (scheduled) medicine under DPCO - the retailer margin " +
                    "is legally capped at " + SCHEDULED_DRUG_MAX_MARGIN_PERCENT + "%.");
        }

        stock.setMarginPercent(req.getMarginPercent());
        return toStockDTO(stockRepository.save(stock));
    }

    /** Clinic-admin-facing: purely operational, not a pricing decision - see UpdateReorderThresholdRequest. */
    @Transactional
    public ClinicStockViewDTO updateReorderThreshold(Long clinicId, Long medicineId, UpdateReorderThresholdRequest req) {
        ClinicMedicineStock stock = stockRepository.findByClinicIdAndMedicineId(clinicId, medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("This clinic hasn't stocked this medicine yet."));
        stock.setReorderThreshold(req.getReorderThreshold());
        return toStockViewDTO(stockRepository.save(stock));
    }

    // --- Step 1: Prescribing (doctor) - no pricing, no stock deduction ---

    /**
     * Doctor writes a prescription: medicine + quantity + dosage per line,
     * nothing else. No stock is touched and no price is computed - that only
     * happens when the clinic admin bills it (see billPrescription). Each
     * medicine must at least be stocked at this clinic (so the doctor isn't
     * prescribing something this branch has never carried); exact
     * availability is re-checked (and enforced) for real at billing time,
     * since stock can move between prescribing and billing.
     */
    @Transactional
    public PrescriptionSummaryDTO createPrescription(Long organizationId, Long doctorId, Long appointmentId,
                                                       CreatePrescriptionRequest req) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        if (!appointment.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This appointment does not belong to your organization.");
        }
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedBusinessException("You can only prescribe for your own appointments.");
        }
        if (prescriptionRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new BusinessException("A prescription already exists for this appointment.");
        }

        Long clinicId = appointment.getClinic().getId();

        Prescription prescription = prescriptionRepository.save(Prescription.builder()
                .organizationId(organizationId)
                .appointment(appointment)
                .clinicId(clinicId)
                .doctorId(doctorId)
                .patientId(appointment.getPatient().getId())
                .status(Prescription.PrescriptionStatus.PRESCRIBED)
                .build());

        for (PrescriptionItemRequest itemReq : req.getItems()) {
            Medicine medicine = getOwnedMedicine(organizationId, itemReq.getMedicineId());
            if (stockRepository.findByClinicIdAndMedicineId(clinicId, medicine.getId()).isEmpty()) {
                throw new BusinessException("This clinic doesn't stock \"" + medicine.getName() + "\".");
            }
            prescribedItemRepository.save(PrescribedItem.builder()
                    .prescription(prescription)
                    .medicine(medicine)
                    .prescribedQuantity(itemReq.getQuantity())
                    .dosageInstructions(itemReq.getDosageInstructions())
                    .build());
        }

        return toSummaryDTO(prescription, appointment);
    }

    @Transactional(readOnly = true)
    public PrescriptionSummaryDTO getPrescriptionForAppointment(Long appointmentId) {
        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("No prescription for this appointment yet."));
        return toSummaryDTO(prescription, prescription.getAppointment());
    }

    // --- Step 2: Billing (clinic admin) - this is where price is computed and stock is deducted ---

    /** The clinic admin's billing queue: prescriptions written at this branch that haven't been billed yet. */
    @Transactional(readOnly = true)
    public List<PrescriptionSummaryDTO> getPendingPrescriptionsForClinic(Long clinicId) {
        return prescriptionRepository.findByClinicIdAndStatusOrderByCreatedAtAsc(clinicId, Prescription.PrescriptionStatus.PRESCRIBED)
                .stream()
                .map(p -> toSummaryDTO(p, p.getAppointment()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrescriptionSummaryDTO getPrescriptionSummaryForClinic(Long clinicId, Long prescriptionId) {
        Prescription prescription = getPrescriptionForClinic(clinicId, prescriptionId);
        return toSummaryDTO(prescription, prescription.getAppointment());
    }

    /**
     * Clinic admin creates the bill: for each line, dispenses up to (but never
     * more than) what was prescribed, using FEFO across batches exactly like
     * before, snapshotting cost/selling/profit per line. The admin only ever
     * gets back selling-price data (BillReceiptDTO) - cost and profit are
     * computed and stored for the owner's reporting but never returned here.
     */
    @Transactional
    public BillReceiptDTO billPrescription(Long organizationId, Long clinicId, Long adminUserId,
                                            Long prescriptionId, BillPrescriptionRequest req) {
        Prescription prescription = getPrescriptionForClinic(clinicId, prescriptionId);
        if (!prescription.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This prescription does not belong to your organization.");
        }
        if (prescription.getStatus() == Prescription.PrescriptionStatus.BILLED) {
            throw new BusinessException("This prescription has already been billed.");
        }

        Map<Long, PrescribedItem> prescribedByMedicineId = prescribedItemRepository.findByPrescriptionId(prescription.getId())
                .stream()
                .collect(Collectors.toMap(pi -> pi.getMedicine().getId(), pi -> pi));

        List<PrescriptionItem> billedItems = new java.util.ArrayList<>();
        for (BillLineRequest line : req.getItems()) {
            PrescribedItem prescribed = prescribedByMedicineId.get(line.getMedicineId());
            if (prescribed == null) {
                throw new BusinessException("This medicine wasn't prescribed for this patient.");
            }
            if (line.getQuantity() > prescribed.getPrescribedQuantity()) {
                throw new BusinessException("Can't bill more than prescribed for \"" + prescribed.getMedicine().getName()
                        + "\" (prescribed " + prescribed.getPrescribedQuantity() + ").");
            }
            billedItems.add(dispenseOneItem(clinicId, prescription, prescribed.getMedicine(), line.getQuantity(),
                    prescribed.getDosageInstructions()));
        }

        prescription.setStatus(Prescription.PrescriptionStatus.BILLED);
        prescription.setBilledByAdminId(adminUserId);
        prescription.setBilledAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        return toBillReceiptDTO(prescription, billedItems);
    }

    @Transactional(readOnly = true)
    public BillReceiptDTO getBillReceipt(Long clinicId, Long prescriptionId) {
        Prescription prescription = getPrescriptionForClinic(clinicId, prescriptionId);
        if (prescription.getStatus() != Prescription.PrescriptionStatus.BILLED) {
            throw new BusinessException("This prescription hasn't been billed yet.");
        }
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(prescription.getId());
        return toBillReceiptDTO(prescription, items);
    }

    /** OWNER-ONLY: full cost/profit detail for one prescription, once billed. */
    @Transactional(readOnly = true)
    public PrescriptionDTO getPrescriptionFullDetail(Long organizationId, Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));
        if (!prescription.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This prescription does not belong to your organization.");
        }
        return toFullDTO(prescription);
    }

    private Prescription getPrescriptionForClinic(Long clinicId, Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));
        if (!prescription.getClinicId().equals(clinicId)) {
            throw new AccessDeniedBusinessException("This prescription does not belong to your branch.");
        }
        return prescription;
    }

    /** FEFO dispense of one medicine line, snapshotting cost/selling/profit. Used only at billing time. */
    private PrescriptionItem dispenseOneItem(Long clinicId, Prescription prescription, Medicine medicine,
                                              int quantityToBill, String dosageInstructions) {
        ClinicMedicineStock stock = stockRepository.findByClinicIdAndMedicineId(clinicId, medicine.getId())
                .orElseThrow(() -> new BusinessException(
                        "This clinic doesn't stock \"" + medicine.getName() + "\"."));

        int remainingToDispense = quantityToBill;
        if (stock.getQuantityOnHand() < remainingToDispense) {
            throw new BusinessException("Not enough stock of \"" + medicine.getName() + "\" - only "
                    + stock.getQuantityOnHand() + " " + medicine.getUnit() + "(s) available.");
        }

        List<StockBatch> batches = stockBatchRepository
                .findByClinicIdAndMedicineIdAndQuantityRemainingGreaterThanOrderByExpiryDateAsc(
                        clinicId, medicine.getId(), 0);

        BigDecimal margin = stock.getMarginPercent();
        BigDecimal sellingMultiplier = BigDecimal.ONE.add(margin.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));

        PrescriptionItem lastItem = null;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalSelling = BigDecimal.ZERO;
        int totalQty = 0;

        for (StockBatch batch : batches) {
            if (remainingToDispense <= 0) break;
            int takeFromThisBatch = Math.min(remainingToDispense, batch.getQuantityRemaining());
            if (takeFromThisBatch <= 0) continue;

            BigDecimal costPrice = batch.getCostPrice();
            BigDecimal sellingPrice = costPrice.multiply(sellingMultiplier).setScale(2, RoundingMode.HALF_UP);
            // Never exceed the printed MRP, even if the margin math would push past it.
            if (batch.getMrp() != null && sellingPrice.compareTo(batch.getMrp()) > 0) {
                sellingPrice = batch.getMrp();
            }
            BigDecimal profit = sellingPrice.subtract(costPrice)
                    .multiply(BigDecimal.valueOf(takeFromThisBatch))
                    .setScale(2, RoundingMode.HALF_UP);

            PrescriptionItem item = prescriptionItemRepository.save(PrescriptionItem.builder()
                    .prescription(prescription)
                    .medicine(medicine)
                    .quantity(takeFromThisBatch)
                    .costPriceAtSale(costPrice)
                    .sellingPriceAtSale(sellingPrice)
                    .profitAmount(profit)
                    .dosageInstructions(dosageInstructions)
                    .build());

            batch.setQuantityRemaining(batch.getQuantityRemaining() - takeFromThisBatch);
            stockBatchRepository.save(batch);

            remainingToDispense -= takeFromThisBatch;
            totalCost = totalCost.add(costPrice.multiply(BigDecimal.valueOf(takeFromThisBatch)));
            totalSelling = totalSelling.add(sellingPrice.multiply(BigDecimal.valueOf(takeFromThisBatch)));
            totalQty += takeFromThisBatch;
            lastItem = item;
        }

        stock.setQuantityOnHand(stock.getQuantityOnHand() - quantityToBill);
        stockRepository.save(stock);

        // If FEFO spread this line across multiple batches (rare, at the tail
        // end of one batch running out), collapse to a single representative
        // line for the receipt with the blended per-unit selling price - the
        // individual PrescriptionItem rows underneath still carry the exact
        // per-batch cost/profit for the owner's reporting.
        if (lastItem == null) {
            throw new BusinessException("Couldn't dispense \"" + medicine.getName() + "\" - no stock batches available.");
        }
        BigDecimal blendedSelling = totalQty == 0 ? BigDecimal.ZERO
                : totalSelling.divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_UP);
        return PrescriptionItem.builder()
                .prescription(prescription)
                .medicine(medicine)
                .quantity(totalQty)
                .costPriceAtSale(totalQty == 0 ? BigDecimal.ZERO : totalCost.divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_UP))
                .sellingPriceAtSale(blendedSelling)
                .profitAmount(totalSelling.subtract(totalCost).setScale(2, RoundingMode.HALF_UP))
                .dosageInstructions(dosageInstructions)
                .build();
    }

    // --- Dashboard aggregation helpers (used by DashboardService) ---

    /** Per-medicine revenue/profit rollup over a set of already-fetched BILLED line items. */
    public List<MedicineStatsDTO> buildMedicineStats(List<PrescriptionItem> items) {
        Map<Long, List<PrescriptionItem>> byMedicine = items.stream()
                .collect(Collectors.groupingBy(i -> i.getMedicine().getId()));

        return byMedicine.values().stream().map(list -> {
            Medicine medicine = list.get(0).getMedicine();
            long qty = list.stream().mapToLong(PrescriptionItem::getQuantity).sum();
            BigDecimal revenue = list.stream()
                    .map(i -> i.getSellingPriceAtSale().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal profit = list.stream()
                    .map(PrescriptionItem::getProfitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double marginPct = revenue.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : profit.multiply(new BigDecimal("100")).divide(revenue, 1, RoundingMode.HALF_UP).doubleValue();

            return MedicineStatsDTO.builder()
                    .medicineId(medicine.getId())
                    .medicineName(medicine.getName())
                    .quantitySold(qty)
                    .revenue(revenue)
                    .profit(profit)
                    .profitMarginPercent(marginPct)
                    .build();
        })
        .sorted((a, b) -> b.getProfit().compareTo(a.getProfit()))
        .collect(Collectors.toList());
    }

    // --- DTO builders ---

    private ClinicMedicineStockDTO toStockDTO(ClinicMedicineStock stock) {
        Medicine medicine = stock.getMedicine();
        StockBatch earliest = earliestBatch(stock.getClinicId(), medicine.getId());
        BigDecimal costPrice = earliest != null ? earliest.getCostPrice() : null;
        BigDecimal sellingPrice = computeSellingPrice(stock.getMarginPercent(), earliest);

        return ClinicMedicineStockDTO.builder()
                .medicineId(medicine.getId())
                .medicineName(medicine.getName())
                .unit(medicine.getUnit())
                .packSize(medicine.getPackSize())
                .packLabel(medicine.getPackLabel())
                .scheduledDrug(medicine.isScheduledDrug())
                .marginPercent(stock.getMarginPercent())
                .quantityOnHand(stock.getQuantityOnHand())
                .reorderThreshold(stock.getReorderThreshold())
                .lowStock(stock.getQuantityOnHand() <= stock.getReorderThreshold())
                .currentCostPrice(costPrice)
                .currentMrp(earliest != null ? earliest.getMrp() : null)
                .currentSellingPrice(sellingPrice)
                .nearestExpiryDate(earliest != null ? earliest.getExpiryDate() : null)
                .build();
    }

    private ClinicStockViewDTO toStockViewDTO(ClinicMedicineStock stock) {
        Medicine medicine = stock.getMedicine();
        StockBatch earliest = earliestBatch(stock.getClinicId(), medicine.getId());
        BigDecimal sellingPrice = computeSellingPrice(stock.getMarginPercent(), earliest);

        return ClinicStockViewDTO.builder()
                .medicineId(medicine.getId())
                .medicineName(medicine.getName())
                .unit(medicine.getUnit())
                .packSize(medicine.getPackSize())
                .packLabel(medicine.getPackLabel())
                .scheduledDrug(medicine.isScheduledDrug())
                .quantityOnHand(stock.getQuantityOnHand())
                .reorderThreshold(stock.getReorderThreshold())
                .lowStock(stock.getQuantityOnHand() <= stock.getReorderThreshold())
                .sellingPrice(sellingPrice)
                .nearestExpiryDate(earliest != null ? earliest.getExpiryDate() : null)
                .build();
    }

    private StockBatch earliestBatch(Long clinicId, Long medicineId) {
        List<StockBatch> batches = stockBatchRepository
                .findByClinicIdAndMedicineIdAndQuantityRemainingGreaterThanOrderByExpiryDateAsc(
                        clinicId, medicineId, 0);
        return batches.isEmpty() ? null : batches.get(0);
    }

    private BigDecimal computeSellingPrice(BigDecimal marginPercent, StockBatch earliest) {
        if (earliest == null) return null;
        BigDecimal multiplier = BigDecimal.ONE.add(
                marginPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal sellingPrice = earliest.getCostPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        if (earliest.getMrp() != null && sellingPrice.compareTo(earliest.getMrp()) > 0) {
            sellingPrice = earliest.getMrp();
        }
        return sellingPrice;
    }

    private PrescriptionSummaryDTO toSummaryDTO(Prescription prescription, Appointment appointment) {
        List<PrescribedItem> items = prescribedItemRepository.findByPrescriptionId(prescription.getId());
        return PrescriptionSummaryDTO.builder()
                .id(prescription.getId())
                .appointmentId(appointment.getId())
                .clinicId(prescription.getClinicId())
                .patientName(appointment.getPatient() != null ? appointment.getPatient().getName() : null)
                .doctorName(appointment.getDoctor() != null ? appointment.getDoctor().getName() : null)
                .status(prescription.getStatus().name())
                .createdAt(prescription.getCreatedAt())
                .items(items.stream().map(i -> PrescribedItemDTO.builder()
                        .medicineId(i.getMedicine().getId())
                        .medicineName(i.getMedicine().getName())
                        .unit(i.getMedicine().getUnit())
                        .packLabel(i.getMedicine().getPackLabel())
                        .prescribedQuantity(i.getPrescribedQuantity())
                        .dosageInstructions(i.getDosageInstructions())
                        .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private BillReceiptDTO toBillReceiptDTO(Prescription prescription, List<PrescriptionItem> items) {
        List<BillReceiptItemDTO> lines = items.stream().map(i -> BillReceiptItemDTO.builder()
                        .medicineId(i.getMedicine().getId())
                        .medicineName(i.getMedicine().getName())
                        .unit(i.getMedicine().getUnit())
                        .quantityBilled(i.getQuantity())
                        .sellingPricePerUnit(i.getSellingPriceAtSale())
                        .lineTotal(i.getSellingPriceAtSale().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .collect(Collectors.toList());
        BigDecimal total = lines.stream().map(BillReceiptItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BillReceiptDTO.builder()
                .prescriptionId(prescription.getId())
                .appointmentId(prescription.getAppointment().getId())
                .billedAt(prescription.getBilledAt())
                .items(lines)
                .totalAmount(total)
                .build();
    }

    /** OWNER-ONLY: full cost/profit view, built from whatever's been billed so far. */
    private PrescriptionDTO toFullDTO(Prescription prescription) {
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(prescription.getId());
        BigDecimal total = items.stream()
                .map(i -> i.getSellingPriceAtSale().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = items.stream()
                .map(PrescriptionItem::getProfitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PrescriptionDTO.builder()
                .id(prescription.getId())
                .appointmentId(prescription.getAppointment().getId())
                .status(prescription.getStatus().name())
                .createdAt(prescription.getCreatedAt())
                .items(items.stream().map(i -> PrescriptionItemDTO.builder()
                        .medicineId(i.getMedicine().getId())
                        .medicineName(i.getMedicine().getName())
                        .quantity(i.getQuantity())
                        .costPriceAtSale(i.getCostPriceAtSale())
                        .sellingPriceAtSale(i.getSellingPriceAtSale())
                        .profitAmount(i.getProfitAmount())
                        .dosageInstructions(i.getDosageInstructions())
                        .build())
                        .collect(Collectors.toList()))
                .totalAmount(total)
                .totalProfit(totalProfit)
                .build();
    }

    private MedicineDTO toDTO(Medicine medicine) {
        return MedicineDTO.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .genericName(medicine.getGenericName())
                .manufacturer(medicine.getManufacturer())
                .unit(medicine.getUnit())
                .category(medicine.getCategory())
                .scheduledDrug(medicine.isScheduledDrug())
                .packSize(medicine.getPackSize())
                .packLabel(medicine.getPackLabel())
                .active(medicine.isActive())
                .build();
    }

    private Medicine getOwnedMedicine(Long organizationId, Long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + medicineId));
        if (!medicine.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This medicine does not belong to your organization.");
        }
        return medicine;
    }
}
