package com.server.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequest(
        @NotBlank String name,
        String content
) {}