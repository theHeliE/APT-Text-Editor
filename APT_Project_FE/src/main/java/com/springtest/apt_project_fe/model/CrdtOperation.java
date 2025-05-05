package com.springtest.apt_project_fe.model;

public record CrdtOperation(
        String type,     // "insert" or "delete" or "undoDelete"
        String userId,   // ID of the user performing the operation
        String clock,    // Logical clock timestamp
        String[] nodeId,   // ID of the node (for delete operations)
        String parentId, // ID of the parent node (for insert operations)
        String value  // Character value (for insert operations and paste operations)
) {}