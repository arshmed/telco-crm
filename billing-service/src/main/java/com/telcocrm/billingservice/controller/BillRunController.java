package com.telcocrm.billingservice.controller;

import com.telcocrm.billingservice.service.BillRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillRunController {

    private final BillRunService billRunService;

    @PostMapping("/runs")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Map<String, Object>> triggerBillRun(
            @RequestParam(required = false) String asOf) {
        LocalDate date = asOf != null ? LocalDate.parse(asOf) : LocalDate.now();
        int generated = billRunService.runBillCycle(date);
        return ResponseEntity.ok(Map.of(
                "generated", generated,
                "asOf", date.toString(),
                "status", "completed"
        ));
    }
}
