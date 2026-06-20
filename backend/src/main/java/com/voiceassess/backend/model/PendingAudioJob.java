package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks an async job spawned by an audio upload.
 * Job types: "TRANSCRIPTION", "LLM_PROMPT", etc.
 * The SystemMonitor service polls this table to update the UI.
 */
@Entity
@Table(name = "pending_audio_jobs")
public class PendingAudioJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id")
    private UUID jobId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false, unique = true)
    private AudioAssessment audioAssessment;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    // "PENDING", "PROCESSING", "COMPLETED", "FAILED"
    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public PendingAudioJob() {}

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public AudioAssessment getAudioAssessment() { return audioAssessment; }
    public void setAudioAssessment(AudioAssessment audioAssessment) { this.audioAssessment = audioAssessment; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
