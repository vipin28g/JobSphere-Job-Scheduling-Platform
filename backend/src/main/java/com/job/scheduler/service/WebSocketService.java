package com.job.scheduler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketService {

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void broadcastDashboardUpdate() {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/metrics", Map.of("event", "REFRESH_DASHBOARD", "timestamp", System.currentTimeMillis()));
        }
    }

    public void broadcastJobProgress(String jobId, int progress) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/job-progress/" + jobId, Map.of("jobId", jobId, "progress", progress));
        }
    }
}
