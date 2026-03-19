package com.promptguard.service;

import com.promptguard.detector.*;
import com.promptguard.model.DetectionResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PromptValidationService {

    private final SecretDetector     secretDetector;
    private final PiiDetector        piiDetector;
    private final SourceCodeDetector sourceCodeDetector;
    private final KeywordDetector    keywordDetector;
    private final UserKeywordDetector userKeywordDetector;

    public PromptValidationService(SecretDetector secretDetector,
                                   PiiDetector piiDetector,
                                   SourceCodeDetector sourceCodeDetector,
                                   KeywordDetector keywordDetector,
                                   UserKeywordDetector userKeywordDetector) {
        this.secretDetector     = secretDetector;
        this.piiDetector        = piiDetector;
        this.sourceCodeDetector = sourceCodeDetector;
        this.keywordDetector    = keywordDetector;
        this.userKeywordDetector = userKeywordDetector;
    }

    public List<DetectionResult> validate(String prompt, String userId, String subUser) {
        List<DetectionResult> all = new ArrayList<>();
        all.addAll(secretDetector.detect(prompt));
        all.addAll(piiDetector.detect(prompt));
        all.addAll(sourceCodeDetector.detect(prompt));
        all.addAll(keywordDetector.detect(prompt));
        
        // Add the user-specific keywords last as the final check layer
        all.addAll(userKeywordDetector.detect(userId, subUser, prompt));
        return all;
    }
}
