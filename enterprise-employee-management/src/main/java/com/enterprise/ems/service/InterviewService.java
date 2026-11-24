package com.enterprise.ems.service;

import com.enterprise.ems.constant.InterviewStatus;
import com.enterprise.ems.dto.HiringRequests.FeedbackRequest;
import com.enterprise.ems.dto.InterviewDTO;
import com.enterprise.ems.service.hiring.HiringActor;

import java.util.List;

public interface InterviewService {

    InterviewDTO schedule(Long applicationId, InterviewDTO dto, HiringActor actor);

    // Reschedule / change panel - only while still SCHEDULED.
    InterviewDTO update(Long id, InterviewDTO dto, HiringActor actor);

    InterviewDTO cancel(Long id, String reason, HiringActor actor);

    // Manual COMPLETED / NO_SHOW (COMPLETED also happens automatically once every panelist has submitted).
    InterviewDTO markStatus(Long id, InterviewStatus status, HiringActor actor);

    InterviewDTO submitFeedback(Long id, FeedbackRequest request, HiringActor actor);

    InterviewDTO getById(Long id, HiringActor actor);

    List<InterviewDTO> listByApplication(Long applicationId, HiringActor actor);

    // The signed-in person's own interview inbox.
    List<InterviewDTO> myInterviews(HiringActor actor);
}
