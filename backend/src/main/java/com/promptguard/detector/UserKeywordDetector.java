package com.promptguard.detector;

import com.promptguard.model.DetectionResult;
import com.promptguard.model.RiskType;
import com.promptguard.model.UserKeywordPolicy;
import com.promptguard.repository.UserPolicyRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserKeywordDetector {

    private final UserPolicyRepository repository;

    public UserKeywordDetector(UserPolicyRepository repository) {
        this.repository = repository;
    }

    public List<DetectionResult> detect(String userId, String subUser, String prompt) {
        List<DetectionResult> results = new ArrayList<>();
        if (prompt == null || prompt.isBlank()) return results;
        if (userId == null || subUser == null) return results;

        List<UserKeywordPolicy> policies = repository.findPolicies(userId, subUser);
        String lowerPrompt = prompt.toLowerCase();

        for (UserKeywordPolicy policy : policies) {
            String[] keywords = policy.getKeywordList().split(",");
            for (String kw : keywords) {
                String cleanKw = kw.trim();
                if (cleanKw.isEmpty()) continue;

                if (lowerPrompt.contains(cleanKw.toLowerCase())) {
                    // Map the DB flags to appropriate RiskScore and messages
                    // The highest risk hit for this keyword wins
                    int score = 0;
                    String actionStr = "NONE";

                    if (policy.isBlockCol()) {
                        score = 100; // Force BLOCK
                        actionStr = "BLOCK";
                    } else if (policy.isCritialCol()) {
                        score = 85;  // Force ALERT/CRITICAL
                        actionStr = "CRITICAL";
                    } else if (policy.isRedactedCol()) {
                        score = 75;  // Force REDACT
                        actionStr = "REDACT";
                    } else if (policy.isAllowCol()) {
                        score = 0;   // ALLOW
                        actionStr = "ALLOW";
                    }

                    results.add(new DetectionResult(
                        RiskType.KEYWORD,
                        score,
                        "User-specific keyword hit (" + actionStr + "): \"" + cleanKw + "\"",
                        cleanKw
                    ));
                    break; // Move to next policy
                }
            }
        }
        return results;
    }
}
