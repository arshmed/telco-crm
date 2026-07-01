package com.telcocrm.customerservice.dto;

import com.telcocrm.customerservice.enums.DocumentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private UUID id;
    private DocumentType type;
    private String fileRef;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}
