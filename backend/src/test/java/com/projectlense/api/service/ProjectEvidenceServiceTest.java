package com.projectlense.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectEvidenceServiceTest {
    @Test
    void redactsQuotedCredentialsAndAuthorizationHeadersBeforeSendingEvidence() {
        String source = "api_key = \"secret value with spaces\"\n"
                + "password: 'another hidden value'\n"
                + "client_secret=shortsecret\n"
                + "Authorization: Bearer bearer-secret-value\n"
                + "ordinary setting = visible\n";

        String redacted = ProjectEvidenceService.redactSecrets(source);

        assertFalse(redacted.contains("secret value with spaces"));
        assertFalse(redacted.contains("another hidden value"));
        assertFalse(redacted.contains("shortsecret"));
        assertFalse(redacted.contains("bearer-secret-value"));
        assertTrue(redacted.contains("ordinary setting = visible"));
        assertTrue(redacted.contains("[REDACTED]"));
    }
}
