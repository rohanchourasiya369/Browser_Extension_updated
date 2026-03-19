package com.promptguard.service;

import com.promptguard.model.DetectionResult;
import com.promptguard.model.RiskType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedactionService {

    public String redact(String prompt, List<DetectionResult> detections) {
        String redacted = prompt;
        for (DetectionResult d : detections) {
            if (d.getMatchedValue() != null && !d.getMatchedValue().isBlank()) {
                String placeholder = getPlaceholder(d.getRiskType());
                redacted = redacted.replace(d.getMatchedValue(), placeholder);
            }
        }
        return redacted;
    }

    private String getPlaceholder(RiskType type) {
        return switch (type) {
            case SECRET      -> "[REDACTED-SECRET]";
            case PII         -> "[REDACTED-PII]";
            case SOURCE_CODE -> "[REDACTED-CODE]";
            case KEYWORD     -> "[REDACTED]";
            default          -> "[REDACTED]";
        };
    }
}
