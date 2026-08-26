package com.doctorapp.service;

import com.doctorapp.dto.DoctorRatingSummaryDTO;
import com.doctorapp.dto.RatingDTO;
import com.doctorapp.entity.Appointment;
import com.doctorapp.entity.Doctor;
import com.doctorapp.entity.Patient;
import com.doctorapp.entity.Rating;
import com.doctorapp.exception.BusinessException;
import com.doctorapp.exception.ResourceNotFoundException;
import com.doctorapp.repository.AppointmentRepository;
import com.doctorapp.repository.DoctorRepository;
import com.doctorapp.repository.PatientRepository;
import com.doctorapp.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * A patient can rate a specific appointment only once, only after the doctor
     * has marked it COMPLETED, and only if it's their own appointment. Ratings
     * are keyed to the appointment (not doctor+patient), so the same patient can
     * rate the same doctor again after a later, separate visit.
     *
     * Recalculates the doctor's denormalized avgRating/ratingCount in the same
     * transaction as the insert, so a reader can never see a rating that isn't
     * yet reflected in the doctor's average.
     */
    @Transactional
    public RatingDTO submitRating(Long patientUserId, Long appointmentId, Integer stars, String reviewText) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("You can only rate your own appointments");
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.COMPLETED) {
            throw new BusinessException("You can only rate a visit after it has been completed");
        }
        if (ratingRepository.existsByAppointmentId(appointmentId)) {
            throw new BusinessException("You've already rated this appointment");
        }

        Rating rating = ratingRepository.save(Rating.builder()
                .appointment(appointment)
                .patient(patient)
                .doctor(appointment.getDoctor())
                .rating(stars)
                .reviewText(reviewText)
                .status(Rating.RatingStatus.VISIBLE)
                .build());

        recalculateDoctorRating(appointment.getDoctor().getId());

        return toDTO(rating);
    }

    @Transactional(readOnly = true)
    public DoctorRatingSummaryDTO getDoctorRatingSummary(Long doctorId, int page, int size) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        Double avg = ratingRepository.avgRatingForDoctor(doctorId);
        long count = ratingRepository.countVisibleForDoctor(doctorId);

        // Pre-fill all 5 stars with 0 so the UI can always render a full 5-row
        // bar chart, then overlay the actual counts on top.
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(star, 0L);
        }
        for (Object[] row : ratingRepository.ratingDistributionRaw(doctorId)) {
            distribution.put((Integer) row[0], (Long) row[1]);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Rating> reviewPage = ratingRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(
                doctorId, Rating.RatingStatus.VISIBLE, pageable);

        return DoctorRatingSummaryDTO.builder()
                .avgRating(round1(avg))
                .ratingCount(count)
                .distribution(distribution)
                .reviews(reviewPage.getContent().stream().map(this::toDTO).collect(Collectors.toList()))
                .page(page)
                .totalPages(reviewPage.getTotalPages())
                .build();
    }

    private void recalculateDoctorRating(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        Double avg = ratingRepository.avgRatingForDoctor(doctorId);
        long count = ratingRepository.countVisibleForDoctor(doctorId);
        doctor.setAvgRating(round1(avg));
        doctor.setRatingCount((int) count);
        doctorRepository.save(doctor);
    }

    // Rounds to 1 decimal place (e.g. 4.6), matching how Flipkart/Amazon-style
    // ratings display - never rounded to a whole star.
    private Double round1(Double value) {
        if (value == null) return 0.0;
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private RatingDTO toDTO(Rating r) {
        return RatingDTO.builder()
                .id(r.getId())
                .appointmentId(r.getAppointment().getId())
                .patientName(r.getPatient().getName())
                .rating(r.getRating())
                .reviewText(r.getReviewText())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
