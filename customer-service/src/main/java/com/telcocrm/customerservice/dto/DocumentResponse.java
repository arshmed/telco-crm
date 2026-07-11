package com.telcocrm.customerservice.dto;

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
    private String type;
    private String fileRef;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}
