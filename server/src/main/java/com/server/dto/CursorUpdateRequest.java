package com.server.dto;

public record CursorUpdateRequest(
        String userId,
        Integer position
) {}