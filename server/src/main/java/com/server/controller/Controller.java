package com.server.controller;

import com.server.dto.CreateDocumentRequest;
import com.server.model.Document;
import com.server.model.User;
import com.server.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        for (Map<String,Object> x : document.getCrdt().serialize()){
            System.out.println(x);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a document by its unique identifier and returns its CRDT (Conflict-free Replicated Data Type) data.
     * 
     * @param documentId The unique identifier of the document to retrieve
     * @return ResponseEntity containing the document's CRDT serialized data if found, 
     *         or a 404 Not Found response if the document doesn't exist
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<List<Map<String, Object>>> getDocument(@PathVariable String documentId) {
        Optional<Document> documentOpt = documentService.getDocumentById(documentId);
        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();
            return ResponseEntity.ok(document.getCrdt().serialize());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}