package com.promptguard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate db;

    public DatabaseInitializer(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== PromptGuard DB Init ===");

        // ── MIGRATION: add browser_name column if missing ──────────
        // This runs as Java code so it is NEVER skipped by Spring caching.
        try {
            Integer exists = db.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_name='audit_logs' AND column_name='browser_name'",
                    Integer.class);
            if (exists == null || exists == 0) {
                db.execute("ALTER TABLE audit_logs ADD COLUMN browser_name VARCHAR(50) DEFAULT 'Unknown'");
                log.info("✅ Migration: added browser_name column to audit_logs");
            } else {
                log.info("⏭️  browser_name column already exists");
            }
        } catch (Exception e) {
            log.warn("⚠️  browser_name migration check failed: {}", e.getMessage());
        }

        // ── Seed default users ──────────────────────────────────────
        insertIfNotExists("admin-user", "Admin", "ADMIN");
        insertIfNotExists("rohan-user", "Rohan", "USER");
        insertIfNotExists("kushal-user", "Kushal", "USER");

        seedPolicies();

        Long userCount = db.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        Long logCount = db.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);
        log.info("✅ Tables ready — users: {}, audit_logs: {}", userCount, logCount);
        log.info("=== DB Init Complete ===");
    }

    private void seedPolicies() {
        log.info("── Seeding User Keyword Policies ──");
        // Rohan User (user1 + user2)
        upsertPolicy("rohan-user", "user1", "*", false, false, false, true, "R-user1 All detector working");
        upsertPolicy("rohan-user", "user2", "*", false, true, false, false, "R-user2 All detector working");
        // Kushal User (user1)
        upsertPolicy("kushal-user", "user1", "*", false, false, true, false, "K-user1 All detector working");
        log.info("✅ Policies seeded");
    }

    private void upsertPolicy(String userId, String subUser, String words, boolean allow, boolean redact, boolean crit,
            boolean block, String prompt) {
        Integer exists = db.queryForObject(
                "SELECT COUNT(*) FROM user_keyword_policies WHERE user_id = ? AND sub_user = ?",
                Integer.class, userId, subUser);

        if (exists == null || exists == 0) {
            db.update(
                    "INSERT INTO user_keyword_policies (user_id, sub_user, keyword_list, allow_col, redacted_col, critial_col, block_col, prompt_col) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    userId, subUser, words, allow, redact, crit, block, prompt);
            log.info("✅ Created policy for {}:{}", userId, subUser);
        }
    }

    private void insertIfNotExists(String userId, String displayName, String role) {
        Integer exists = db.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id = ?", Integer.class, userId);
        if (exists == null || exists == 0) {
            db.update("INSERT INTO users (user_id, display_name, role) VALUES (?, ?, ?)",
                    userId, displayName, role);
            log.info("✅ Seeded user: {} ({})", userId, role);
        } else {
            log.info("⏭️  User already exists: {}", userId);
        }
    }
}
