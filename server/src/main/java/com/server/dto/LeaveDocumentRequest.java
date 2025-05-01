package com.server.dto;

public record LeaveDocumentRequest(
        String documentId,
        String userId
) {}