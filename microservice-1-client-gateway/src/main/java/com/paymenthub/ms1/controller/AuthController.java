package com.paymenthub.ms1.controller;

import com.paymenthub.ms1.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    @Autowired
    private SessionService sessionService;

    /**
     * Client calls this FIRST to get a sessionId
     *
     * POST /api/v1/auth/token
     * Body: { "clientId": "BANK001", "clientSecret": "secret123" }
     *
     * Response: { "sessionId": "uuid", "expiresIn": 86400 }
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken(
            @RequestBody Map<String, String> body) {

        String clientId     = body.get("clientId");
        String clientSecret = body.get("clientSecret");
        
        System.out.println("ClientID" + clientId);

        if (!sessionService.validateCredentials(clientId, clientSecret)) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid clientId or clientSecret"
            ));
        }

        String sessionId = sessionService.createSession(clientId);
        log.info("Token issued | clientId={}", clientId);

        return ResponseEntity.ok(Map.of(
            "sessionId",  sessionId,
            "expiresIn",  86400,
            "tokenType",  "Bearer",
            "howToUse",   "Add header → Authorization: Bearer " + sessionId
        ));
    }

    /**
     * Client calls this to logout
     *
     * DELETE /api/v1/auth/token
     * Header: Authorization: Bearer <sessionId>
     */
    @DeleteMapping("/deletetoken")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authorization) {
        sessionService.deleteSession(authorization);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}