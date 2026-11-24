package com.enterprise.ems.service;

import com.enterprise.ems.constant.CandidateSource;
import com.enterprise.ems.dto.CandidateDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.service.hiring.HiringActor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CandidateService {

    // What the download endpoint needs to stream a resume back.
    record ResumeDownload(Resource resource, String filename, String contentType) {}

    CandidateDTO create(CandidateDTO dto, HiringActor actor);

    CandidateDTO update(Long id, CandidateDTO dto, HiringActor actor);

    CandidateDTO getById(Long id, HiringActor actor);

    PageResponse<CandidateDTO> search(String keyword, CandidateSource source, Pageable pageable, HiringActor actor);

    CandidateDTO uploadResume(Long id, MultipartFile file, HiringActor actor);

    ResumeDownload downloadResume(Long id, HiringActor actor);

    // Retention / deletion request: wipes personal data and the resume file, keeps the row so history stays intact.
    CandidateDTO anonymise(Long id, HiringActor actor);
}
