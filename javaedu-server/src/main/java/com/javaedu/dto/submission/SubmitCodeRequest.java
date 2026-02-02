package com.javaedu.dto.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitCodeRequest {

    @NotNull(message = "Exercise ID is required")
    private Long exerciseId;

    @NotBlank(message = "Code is required")
    private String code;
}
