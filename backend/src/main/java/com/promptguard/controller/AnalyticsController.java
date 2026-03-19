package com.promptguard.controller;

import com.promptguard.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AuditLogRepository repository;
    private final JdbcTemplate       db;

    public AnalyticsController(AuditLogRepository repository, JdbcTemplate db) {
        this.repository = repository;
        this.db         = db;
    }

    @GetMapping("/risk-summary")
    public ResponseEntity<Map<String, Object>> getRiskSummary() {
        long total    = repository.countTotal();
        long blocked  = repository.countByAction("BLOCK");
        long redacted = repository.countByAction("REDACT");
        long alerted  = repository.countByAction("ALERT");
        long allowed  = repository.countByAction("ALLOW");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalPrompts",    total);
        m.put("blockedPrompts",  blocked);
        m.put("redactedPrompts", redacted);
        m.put("alertedPrompts",  alerted);
        m.put("allowedPrompts",  allowed);
        m.put("blockRate",       total > 0 ? Math.round((double) blocked / total * 100) + "%" : "0%");
        return ResponseEntity.ok(m);
    }

    @GetMapping("/tool-usage")
    public ResponseEntity<?> getToolUsage() {
        return ResponseEntity.ok(repository.countByTool());
    }

    @GetMapping("/risk-breakdown")
    public ResponseEntity<?> getRiskBreakdown() {
        return ResponseEntity.ok(repository.countByRiskType());
    }

    @GetMapping("/top-users")
    public ResponseEntity<?> getTopUsers() {
        return ResponseEntity.ok(repository.topUsers());
    }

    @GetMapping("/recent-logs")
    public ResponseEntity<?> getRecentLogs(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(repository.findRecent(limit));
    }

    @GetMapping("/my-prompts")
    public ResponseEntity<?> myPrompts(
            @RequestParam String userId,
            @RequestParam(defaultValue = "50") int limit) {
        if (userId == null || userId.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));

        List<Map<String, Object>> rows = db.queryForList(
            "SELECT id, " +
            "  user_id           AS \"userId\", " +
            "  tool, " +
            "  browser_name      AS \"browserName\", " +
            "  highest_risk_type AS \"highestRiskType\", " +
            "  risk_score        AS \"riskScore\", " +
            "  risk_level        AS \"riskLevel\", " +
            "  action, " +
            "  action_reason     AS \"actionReason\", " +
            "  created_at        AS \"timestamp\" " +
            "FROM audit_logs WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            userId, limit);
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUserList() {
        List<Map<String, Object>> rows = db.queryForList(
            "SELECT user_id, display_name, role FROM users ORDER BY role DESC, user_id ASC");
        return ResponseEntity.ok(rows);
    }
}
