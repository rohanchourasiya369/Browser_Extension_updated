package com.promptguard.service;

import com.promptguard.model.*;
import org.springframework.stereotype.Service;

@Service
public class PolicyEngine {

    /**
     * Decision priority (ORDER MATTERS):
     *
     * 1. SECRET → BLOCK (credentials must never reach AI)
     * 2. KEYWORD → BLOCK (admin-defined forbidden words)
     * 3. PII → REDACT (remove sensitive data, allow the rest)
     * 4. SOURCE_CODE → ALERT (warn, still allow — code review needed)
     * 5. score >= 80 → BLOCK (any other critical-score detection)
     * 6. score >= 60 → REDACT (high-score unknown)
     * 7. score >= 40 → ALERT (medium risk)
     * 8. else → ALLOW
     *
     * ROOT CAUSE FIX:
     * PII (SSN=90, CC=90) was hitting "CRITICAL score → BLOCK" before
     * reaching the PII REDACT branch. Now PII is checked FIRST.
     */
    public PolicyDecision decide(RiskScore riskScore) {
        int score = riskScore.getTotalScore();
        RiskType type = riskScore.getRiskType();

        // 1. SECRET → BLOCK
        if (type == RiskType.SECRET) {
            return new PolicyDecision(
                    Action.BLOCK,
                    "Secret/credential detected (score: " + score + "/100). " +
                            "Sending credentials to AI tools is a critical security risk.");
        }

        // 2. KEYWORD → BLOCK
        if (type == RiskType.KEYWORD) {
            return new PolicyDecision(
                    Action.BLOCK,
                    "Blocked keyword detected. This content cannot be sent to AI tools.");
        }

        // 3. PII → REDACT (SSN=90, CC=90, AADHAAR=80, PAN=80, PHONE=70, EMAIL=60)
        if (type == RiskType.PII) {
            return new PolicyDecision(
                    Action.REDACT,
                    "PII detected and automatically removed (score: " + score + "/100). " +
                            "Sensitive personal data has been redacted before sending.");
        }

        // 4. SOURCE_CODE → ALERT (SQL=60, Python=50, Java class=70)
        if (type == RiskType.SOURCE_CODE) {
            return new PolicyDecision(
                    Action.ALERT,
                    "Source code / SQL detected (score: " + score + "/100). " +
                            "Sharing proprietary code with AI tools may expose intellectual property.");
        }

        // 5-8. Score-based fallthrough
        if (score >= 80) {
            return new PolicyDecision(
                    Action.BLOCK,
                    "Critical risk detected. Score: " + score + "/100.");
        }
        if (score >= 60) {
            return new PolicyDecision(
                    Action.REDACT,
                    "High-risk content detected and redacted. Score: " + score + "/100.");
        }
        if (score >= 40) {
            return new PolicyDecision(
                    Action.ALERT,
                    "Medium-risk content detected. Score: " + score + "/100. Please review before sharing.");
        }

        return new PolicyDecision(
                Action.ALLOW,
                "No significant risk detected.");
    }
}
