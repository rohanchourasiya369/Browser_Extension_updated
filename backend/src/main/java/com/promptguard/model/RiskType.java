package com.promptguard.model;

public enum RiskType {
    SECRET,       // Passwords, API keys, tokens
    PII,          // Email, phone, Aadhaar, SSN
    SOURCE_CODE,  // Java, Python, SQL code
    KEYWORD,      // Blocked keywords
    NONE          // No risk
}
