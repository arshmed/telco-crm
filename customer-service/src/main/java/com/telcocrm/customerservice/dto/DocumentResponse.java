package com.telcocrm.customerservice.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse implements Serializable {

    private UUID id;
    private String type;
    private String fileRef;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}
