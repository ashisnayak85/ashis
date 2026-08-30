package com.orgsite.controller;

import com.orgsite.dto.ContentBlockDTO;
import com.orgsite.security.UserPrincipal;
import com.orgsite.service.ContentBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/content-blocks")
@RequiredArgsConstructor
public class AdminContentBlockController {

    private final ContentBlockService contentBlockService;

    @GetMapping
    public ResponseEntity<List<ContentBlockDTO>> list(@AuthenticationPrincipal UserPrincipal principal) {
        var blocks = contentBlockService.listForOrg(principal.getOrganizationId());
        return ResponseEntity.ok(contentBlockService.toDTOList(blocks));
    }

    @PostMapping
    public ResponseEntity<ContentBlockDTO> create(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody ContentBlockDTO dto) {
        var block = contentBlockService.create(principal.getOrganizationId(), dto);
        return ResponseEntity.ok(contentBlockService.toDTO(block));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContentBlockDTO> update(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long id,
                                                    @RequestBody ContentBlockDTO dto) {
        var block = contentBlockService.update(principal.getOrganizationId(), id, dto);
        return ResponseEntity.ok(contentBlockService.toDTO(block));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        contentBlockService.delete(principal.getOrganizationId(), id);
        return ResponseEntity.noContent().build();
    }
}
