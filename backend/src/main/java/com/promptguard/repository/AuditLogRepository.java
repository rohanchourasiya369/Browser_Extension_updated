package com.promptguard.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate db;

    public AuditLogRepository(JdbcTemplate db) { this.db = db; }

    public long countTotal() {
        Long n = db.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);
        return n != null ? n : 0L;
    }

    public long countByAction(String action) {
        Long n = db.queryForObject(
            "SELECT COUNT(*) FROM audit_logs WHERE action = ?", Long.class, action);
        return n != null ? n : 0L;
    }

    public List<Map<String, Object>> countByTool() {
        return db.queryForList(
            "SELECT tool, COUNT(*) AS count FROM audit_logs GROUP BY tool ORDER BY count DESC");
    }

    public List<Map<String, Object>> countByRiskType() {
        return db.queryForList(
            "SELECT highest_risk_type AS \"riskType\", COUNT(*) AS count " +
            "FROM audit_logs GROUP BY highest_risk_type ORDER BY count DESC");
    }

    public List<Map<String, Object>> topUsers() {
        return db.queryForList(
            "SELECT user_id, COUNT(*) AS total, " +
            "SUM(CASE WHEN action='BLOCK' THEN 1 ELSE 0 END) AS blocked " +
            "FROM audit_logs GROUP BY user_id ORDER BY total DESC LIMIT 10");
    }

    public List<Map<String, Object>> findRecent(int limit) {
        return db.queryForList(
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
            "FROM audit_logs ORDER BY created_at DESC LIMIT ?",
            limit);
    }
}
