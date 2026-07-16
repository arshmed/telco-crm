package com.telcocrm.customerservice.controller;

import com.telcocrm.customerservice.dto.DocumentTypeResponse;
import com.telcocrm.customerservice.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {

    private final DocumentTypeRepository documentTypeRepository;

    @GetMapping
    public ResponseEntity<List<DocumentTypeResponse>> listDocumentTypes() {
        var types = documentTypeRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(dt -> DocumentTypeResponse.builder()
                        .code(dt.getCode())
                        .label(dt.getLabel())
                        .build())
                .toList();
        return ResponseEntity.ok(types);
    }
}
