package com.server.controller;

import com.server.dto.CreateDocumentRequest;
import com.server.model.Document;
import com.server.model.User;
import com.server.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller that handles document creation
 */
@RestController
@RequestMapping("/document")
public class Controller {

    private final DocumentService documentService;

    public Controller(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Creates a new document with the provided name and content.
     * 
     * @param file The request containing document name and content
     * @return ResponseEntity containing the created document information including ID, name, access codes, and initial CRDT state or error message if document creation fails
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createDocument(@RequestBody @Valid CreateDocumentRequest file) {
        Document document = documentService.createDocument(file.name());
        User user = document.createNewUser(true);
        document.importContent(user.getId(), file.content());

        Map<String, Object> response = new HashMap<>();
        response.put("documentId", document.getId());
        response.put("documentName", document.getName());
        response.put("editorCode", document.getEditorCode());
        response.put("viewerCode", document.getViewerCode());
        response.put("userId", user.getId());
        response.put("userColor", user.getColor());
        response.put("crdt", document.getCrdt().serialize());

        return ResponseEntity.ok(response);
    }
}