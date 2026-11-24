package com.onehealth.service;

import com.onehealth.dto.*;
import com.onehealth.entity.Appointment;
import com.onehealth.entity.Clinic;
import com.onehealth.entity.DoctorAvailability;
import com.onehealth.entity.PrescriptionItem;
import com.onehealth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Backs the owner's (and clinic admin's) analytics dashboard. Deliberately does
 * a live aggregation over the Appointment table for the requested range rather
 * than a pre-computed rollup table - for a single clinic-chain's data volume
 * this is fast and always-correct. Revisit with a nightly summary table only if
 * (once there are many orgs + years of history) these queries start to show up
 * as slow in practice; don't build that complexity pre-emptively.
 *
 * IMPORTANT (fixed after real-world testing): every public method here runs
 * inside a single read-only transaction. Appointment/Clinic/Doctor relations
 * are lazy-loaded, and with spring.jpa.open-in-view=false the Hibernate
 * session closes the moment a repository call returns - so touching a lazy
 * relation (e.g. appointment.getClinic().getId()) AFTER that point throws
 * LazyInitializationException. That's exactly what caused the dashboard to
 * fail once real clinics/doctors/appointments existed (it worked on an empty
 * DB because there was nothing lazy to touch yet). Wrapping getStats in one
 * transaction keeps the session open for the whole computation.
 *
 * Also fixed: each section (branch stats, doctor utilization, trend) is now
 * computed independently and defensively - if one section's calculation
 * throws for any reason, that section comes back empty/zeroed with a note in
 * sectionWarnings, but the rest of the dashboard still renders. A single bad
 * branch's data should never take down the whole screen for the owner.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentSlotRepository slotRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final ClinicMedicineStockRepository clinicMedicineStockRepository;
    private final MedicineService medicineService;

    /**
     * @param organizationId tenant scope (always enforced)
     * @param filter         from/to default to "today" if either is missing, per
     *                       the requirement that the dashboard loads current-date
     *                       data by default; clinicId narrows to one branch (used
     *                       for a clinic admin's own view, or an owner drilling in)
     * @param includeFinancials whether cost/profit data may appear in the result -
     *                       true for the OWNER only. A clinic admin gets revenue
     *                       (money collected) but never cost price or profit, so
     *                       margin can't be reconstructed from the dashboard - see
     *                       ClinicMedicineStock's class javadoc for why.
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats(Long organizationId, DashboardFilter filter, boolean includeFinancials) {
        LocalDate today = LocalDate.now();
        LocalDate from = filter.getFrom() != null ? filter.getFrom() : today;
        LocalDate to = filter.getTo() != null ? filter.getTo() : today;
        if (from.isAfter(to)) {
            LocalDate tmp = from; from = to; to = tmp;
        }
        final LocalDate rangeFrom = from;
        final LocalDate rangeTo = to;

        List<String> warnings = new ArrayList<>();

        List<Clinic> clinics = safely(() -> filter.getClinicId() != null
                        ? clinicRepository.findById(filter.getClinicId()).map(List::of).orElse(List.<Clinic>of())
                        : clinicRepository.findByOrganizationId(organizationId),
                List.of(), warnings, "branch list");

        List<Appointment> appointments = safely(() -> filter.getClinicId() != null
                        ? appointmentRepository.findByOrganizationIdAndClinicIdAndAppointmentDateBetween(
                                organizationId, filter.getClinicId(), rangeFrom, rangeTo)
                        : appointmentRepository.findByOrganizationIdAndAppointmentDateBetween(organizationId, rangeFrom, rangeTo),
                List.of(), warnings, "appointment totals");

        Map<Long, List<Appointment>> byClinicId = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getClinic().getId()));

        List<PrescriptionItem> medicineItems = safely(() -> filter.getClinicId() != null
                        ? prescriptionItemRepository.findForClinicInRange(organizationId, filter.getClinicId(), rangeFrom, rangeTo)
                        : prescriptionItemRepository.findForOrganizationInRange(organizationId, rangeFrom, rangeTo),
                List.of(), warnings, "medicine sales");

        Map<Long, List<PrescriptionItem>> medicineItemsByClinicId = medicineItems.stream()
                .collect(Collectors.groupingBy(i -> i.getPrescription().getClinicId()));

        List<ClinicStatsDTO> byClinic = safely(
                () -> clinics.stream()
                        .map(c -> buildClinicStats(c, byClinicId.getOrDefault(c.getId(), List.of()),
                                medicineItemsByClinicId.getOrDefault(c.getId(), List.of())))
                        .collect(Collectors.toList()),
                List.of(), warnings, "branch breakdown");

        List<MedicineStatsDTO> medicineStats = safely(
                () -> medicineService.buildMedicineStats(medicineItems),
                List.of(), warnings, "medicine profit breakdown");

        BigDecimal totalMedicineRevenue = safely(() -> medicineItems.stream()
                        .map(i -> i.getSellingPriceAtSale().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.ZERO, warnings, "medicine revenue total");

        BigDecimal totalMedicineProfit = safely(() -> medicineItems.stream()
                        .map(PrescriptionItem::getProfitAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.ZERO, warnings, "medicine profit total");

        List<ClinicMedicineStockDTO> lowStockAlerts = safely(
                () -> buildLowStockAlerts(clinics),
                List.of(), warnings, "low stock alerts");

        List<DoctorUtilizationDTO> utilization = safely(
                () -> buildDoctorUtilization(organizationId, filter.getClinicId(), rangeFrom, rangeTo),
                List.of(), warnings, "doctor utilization");

        List<TrendPointDTO> trend = safely(
                () -> buildTrend(appointments, rangeFrom, rangeTo),
                List.of(), warnings, "appointment trend");

        long total = appointments.size();
        long online = count(appointments, a -> a.getSource() == Appointment.BookingSource.ONLINE);
        long walkIn = count(appointments, a -> a.getSource() == Appointment.BookingSource.WALK_IN);
        long completed = count(appointments, a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED);
        long noShow = count(appointments, a -> a.getStatus() == Appointment.AppointmentStatus.NO_SHOW);
        long cancelled = count(appointments, a -> a.getStatus() == Appointment.AppointmentStatus.CANCELLED);
        long expectedVisits = total - cancelled;
        double completionRate = expectedVisits == 0 ? 0.0 : round1((completed * 100.0) / expectedVisits);

        BigDecimal revenue = safely(() -> appointments.stream()
                        .filter(a -> a.getPaymentStatus() == Appointment.PaymentStatus.PAID && a.getConsultationFee() != null)
                        .map(Appointment::getConsultationFee)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.ZERO, warnings, "revenue total");

        long uniquePatients = safely(
                () -> appointments.stream().map(a -> a.getPatient().getId()).distinct().count(),
                0L, warnings, "unique patient count");

        int activeClinicCount = (int) clinics.stream().filter(Clinic::isActive).count();
        int activeDoctorCount = safely(
                () -> doctorRepository.findByOrganizationIdAndActiveTrue(organizationId).size(),
                0, warnings, "active doctor count");

        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .from(from)
                .to(to)
                .totalAppointments(total)
                .totalOnlineBookings(online)
                .totalWalkIns(walkIn)
                .totalCompleted(completed)
                .totalNoShow(noShow)
                .totalCancelled(cancelled)
                .overallCompletionRatePercent(completionRate)
                .totalRevenue(revenue)
                .totalUniquePatients(uniquePatients)
                .activeClinicCount(activeClinicCount)
                .activeDoctorCount(activeDoctorCount)
                .byClinic(byClinic)
                .doctorUtilization(utilization)
                .trend(trend)
                .totalMedicineRevenue(totalMedicineRevenue)
                .totalMedicineProfit(totalMedicineProfit)
                .medicineStats(medicineStats)
                .lowStockAlerts(lowStockAlerts)
                .sectionWarnings(warnings)
                .build();

        return includeFinancials ? stats : stripFinancials(stats);
    }

    /**
     * Removes cost/profit data for a clinic-admin caller - they may see
     * revenue (money collected, which they need to reconcile the cash
     * drawer) but never cost price, margin, or profit. See
     * ClinicMedicineStock's class javadoc: letting a branch see its own
     * margin would let it back-calculate cost from selling price + margin.
     */
    private DashboardStatsDTO stripFinancials(DashboardStatsDTO stats) {
        List<ClinicStatsDTO> strippedByClinic = stats.getByClinic().stream()
                .map(c -> c.toBuilder().medicineProfit(null).build())
                .collect(Collectors.toList());

        List<MedicineStatsDTO> strippedMedicineStats = stats.getMedicineStats().stream()
                .map(m -> m.toBuilder().profit(null).profitMarginPercent(0.0).build())
                .collect(Collectors.toList());

        List<ClinicMedicineStockDTO> strippedAlerts = stats.getLowStockAlerts().stream()
                .map(a -> a.toBuilder().currentCostPrice(null).marginPercent(null).build())
                .collect(Collectors.toList());

        return stats.toBuilder()
                .totalMedicineProfit(null)
                .byClinic(strippedByClinic)
                .medicineStats(strippedMedicineStats)
                .lowStockAlerts(strippedAlerts)
                .build();
    }

    /** Runs a section's computation; on any exception, logs it, records a short warning, and returns the fallback. */
    private <T> T safely(Supplier<T> computation, T fallback, List<String> warnings, String sectionName) {
        try {
            return computation.get();
        } catch (Exception e) {
            log.error("Dashboard section '{}' failed to compute - returning empty/zero for it instead of failing the whole dashboard.", sectionName, e);
            warnings.add("Couldn't load " + sectionName + " for this range.");
            return fallback;
        }
    }

    /** Every clinic's stock rows below their own reorder threshold, across the branches in scope. */
    private List<ClinicMedicineStockDTO> buildLowStockAlerts(List<Clinic> clinics) {
        List<ClinicMedicineStockDTO> alerts = new ArrayList<>();
        for (Clinic c : clinics) {
            for (ClinicMedicineStockDTO stock : medicineService.getClinicStock(c.getId())) {
                if (stock.isLowStock()) {
                    alerts.add(ClinicMedicineStockDTO.builder()
                            .clinicId(c.getId())
                            .clinicName(c.getClinicName())
                            .medicineId(stock.getMedicineId())
                            .medicineName(stock.getMedicineName())
                            .unit(stock.getUnit())
                            .scheduledDrug(stock.isScheduledDrug())
                            .currentMrp(stock.getCurrentMrp())
                            .marginPercent(stock.getMarginPercent())
                            .quantityOnHand(stock.getQuantityOnHand())
                            .reorderThreshold(stock.getReorderThreshold())
                            .lowStock(true)
                            .currentCostPrice(stock.getCurrentCostPrice())
                            .currentSellingPrice(stock.getCurrentSellingPrice())
                            .nearestExpiryDate(stock.getNearestExpiryDate())
                            .build());
                }
            }
        }
        return alerts;
    }

    private ClinicStatsDTO buildClinicStats(Clinic clinic, List<Appointment> appts, List<PrescriptionItem> medicineItems) {
        long total = appts.size();
        long online = count(appts, a -> a.getSource() == Appointment.BookingSource.ONLINE);
        long walkIn = count(appts, a -> a.getSource() == Appointment.BookingSource.WALK_IN);
        long completed = count(appts, a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED);
        long noShow = count(appts, a -> a.getStatus() == Appointment.AppointmentStatus.NO_SHOW);
        long cancelled = count(appts, a -> a.getStatus() == Appointment.AppointmentStatus.CANCELLED);
        long stillBooked = count(appts, a -> a.getStatus() == Appointment.AppointmentStatus.BOOKED);
        long expected = total - cancelled;
        double rate = expected == 0 ? 0.0 : round1((completed * 100.0) / expected);

        BigDecimal revenue = appts.stream()
                .filter(a -> a.getPaymentStatus() == Appointment.PaymentStatus.PAID && a.getConsultationFee() != null)
                .map(Appointment::getConsultationFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long uniquePatients = appts.stream().map(a -> a.getPatient().getId()).distinct().count();

        BigDecimal medicineRevenue = medicineItems.stream()
                .map(i -> i.getSellingPriceAtSale().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal medicineProfit = medicineItems.stream()
                .map(PrescriptionItem::getProfitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ClinicStatsDTO.builder()
                .clinicId(clinic.getId())
                .clinicName(clinic.getClinicName())
                .city(clinic.getCity())
                .totalAppointments(total)
                .onlineBookings(online)
                .walkIns(walkIn)
                .completed(completed)
                .noShow(noShow)
                .cancelled(cancelled)
                .stillBooked(stillBooked)
                .completionRatePercent(rate)
                .revenue(revenue)
                .uniquePatients(uniquePatients)
                .medicineRevenue(medicineRevenue)
                .medicineProfit(medicineProfit)
                .build();
    }

    /**
     * Doctor utilization = booked slots / total generated slots, per doctor per
     * branch, within the date range. Note this only counts slots that have
     * actually been generated (i.e. a date someone tried to view/book) - a future
     * date nobody has opened yet has no slot rows and won't appear here, which is
     * the correct behavior for "how busy is this doctor" rather than a theoretical
     * capacity number.
     */
    private List<DoctorUtilizationDTO> buildDoctorUtilization(Long organizationId, Long clinicId, LocalDate from, LocalDate to) {
        List<DoctorAvailability> templates = clinicId != null
                ? availabilityRepository.findByClinicIdOrderByDayOfWeekAscStartTimeAsc(clinicId)
                : doctorRepository.findByOrganizationId(organizationId).stream()
                        .flatMap(d -> availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(d.getId()).stream())
                        .collect(Collectors.toList());

        record Key(Long doctorId, Long clinicId) {}
        Map<Key, String[]> names = new LinkedHashMap<>();
        for (DoctorAvailability t : templates) {
            names.putIfAbsent(new Key(t.getDoctor().getId(), t.getClinic().getId()),
                    new String[]{t.getDoctor().getName(), t.getClinic().getClinicName()});
        }

        List<DoctorUtilizationDTO> result = new ArrayList<>();
        for (var entry : names.entrySet()) {
            Long doctorId = entry.getKey().doctorId();
            Long clinicIdKey = entry.getKey().clinicId();
            // NOTE: iterates day-by-day rather than a single ranged query, since
            // AppointmentSlotRepository doesn't yet expose a date-range finder.
            // Fine for dashboard-sized ranges (days/weeks); if owners start
            // pulling multi-month utilization reports, add a proper
            // findByDoctorIdAndClinicIdAndDateBetween query instead of N day-queries.
            long slotsTotal = 0, slotsBooked = 0;
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                List<com.onehealth.entity.AppointmentSlot> daySlots = slotRepository
                        .findByDoctorIdAndClinicIdAndDateOrderByStartTimeAsc(doctorId, clinicIdKey, cursor);
                slotsTotal += daySlots.size();
                slotsBooked += daySlots.stream()
                        .filter(s -> s.getStatus() == com.onehealth.entity.AppointmentSlot.SlotStatus.BOOKED)
                        .count();
                cursor = cursor.plusDays(1);
            }

            double rate = slotsTotal == 0 ? 0.0 : round1((slotsBooked * 100.0) / slotsTotal);
            result.add(DoctorUtilizationDTO.builder()
                    .doctorId(doctorId)
                    .doctorName(entry.getValue()[0])
                    .clinicId(clinicIdKey)
                    .clinicName(entry.getValue()[1])
                    .slotsTotal(slotsTotal)
                    .slotsBooked(slotsBooked)
                    .utilizationRatePercent(rate)
                    .build());
        }
        return result;
    }

    private List<TrendPointDTO> buildTrend(List<Appointment> appointments, LocalDate from, LocalDate to) {
        Map<LocalDate, List<Appointment>> byDate = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate));

        List<TrendPointDTO> trend = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            List<Appointment> dayAppts = byDate.getOrDefault(cursor, List.of());
            trend.add(TrendPointDTO.builder()
                    .date(cursor)
                    .total(dayAppts.size())
                    .completed(count(dayAppts, a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED))
                    .noShow(count(dayAppts, a -> a.getStatus() == Appointment.AppointmentStatus.NO_SHOW))
                    .walkIns(count(dayAppts, a -> a.getSource() == Appointment.BookingSource.WALK_IN))
                    .onlineBookings(count(dayAppts, a -> a.getSource() == Appointment.BookingSource.ONLINE))
                    .build());
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    private long count(List<Appointment> list, java.util.function.Predicate<Appointment> p) {
        return list.stream().filter(p).count();
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
