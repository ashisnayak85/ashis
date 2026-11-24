package com.enterprise.ems.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationAssetDTO {

    private Long id;

    @NotBlank(message = "Asset description is required")
    private String assetDescription;

    private Boolean returned;
    private String remarks;
    private LocalDateTime returnedAt;
}
