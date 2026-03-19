package com.promptguard.service;

import com.promptguard.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final JdbcTemplate db;

    public AuditService(JdbcTemplate db) { this.db = db; }

    @Async
    public void log(PromptRequest request,
                    RiskScore riskScore,
                    PolicyDecision decision,
                    String finalPrompt,
                    long processingTimeMs) {

        String userId = (request.getUserId() != null && !request.getUserId().isBlank())
                ? request.getUserId().trim()
                : "anonymous-user";

        try {
            String riskType = "NONE";
            if (riskScore != null && riskScore.getRiskType() != null)
                riskType = riskScore.getRiskType().name();

            String riskLevel = "NONE";
            if (riskScore != null && riskScore.getRiskLevel() != null)
                riskLevel = riskScore.getRiskLevel().name();

            int score  = (riskScore != null) ? riskScore.getTotalScore() : 0;

            String action = "ALLOW";
            if (decision != null && decision.getAction() != null)
                action = decision.getAction().name();

            String reason = (decision != null) ? decision.getReason() : "";

            // FIX: Do NOT pass id — let PostgreSQL generate it via DEFAULT gen_random_uuid()
            // Passing UUID.toString() into a UUID column causes "bad SQL grammar" in PostgreSQL
            db.update(
                "INSERT INTO audit_logs " +
                "(user_id, tool, browser_name, original_prompt, redacted_prompt, " +
                " highest_risk_type, risk_score, risk_level, action, action_reason, " +
                " processing_time_ms) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                request.getTool()        != null ? request.getTool()        : "Unknown",
                request.getBrowserName() != null ? request.getBrowserName() : "Unknown",
                request.getPrompt(),
                finalPrompt,
                riskType,
                score,
                riskLevel,
                action,
                reason,
                processingTimeMs
            );

            System.out.println("[AuditService] ✅ Saved — userId=" + userId
                + ", tool=" + request.getTool()
                + ", browser=" + request.getBrowserName()
                + ", action=" + action);

        } catch (Exception e) {
            System.err.println("[AuditService] ❌ Failed to save log: " + e.getMessage());
            System.err.println("[AuditService] userId=" + userId + ", tool=" + request.getTool());
        }
    }
}
