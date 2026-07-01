package com.telcocrm.customerservice.dto;

import com.telcocrm.customerservice.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequest {

    @NotNull
    private DocumentType type;

    @NotBlank
    private String fileRef;
}
