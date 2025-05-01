package com.server.dto;

/**
 * Data Transfer Object for CRDT operations
 */
public record CrdtOperation(
    String type,     // "insert" or "delete"
    String userId,   // ID of the user performing the operation
    String clock,    // Logical clock timestamp
    String nodeId,   // ID of the node (for delete operations)
    String parentId, // ID of the parent node (for insert operations)
    Character value  // Character value (for insert operations)
) {}
