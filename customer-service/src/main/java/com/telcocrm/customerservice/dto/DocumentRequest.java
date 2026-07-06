package com.telcocrm.customerservice.dto;

import com.telcocrm.customerservice.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 255)
    private String fileRef;
}
