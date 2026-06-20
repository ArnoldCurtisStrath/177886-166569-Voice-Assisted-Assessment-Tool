package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log for system-level errors.
 * Stores error details for admin investigation.
 */
@Entity
@Table(name = "system_error_logs")
public class SystemErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "error_type", nullable = false)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Reference to the entity that triggered the error, e.g. 'AudioAssessment:abc123' */
    @Column(name = "related_entity")
    private String relatedEntity;

    // Optional direct link back to the audio assessment if the error was assessment-related
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id")
    private AudioAssessment audioAssessment;

    public SystemErrorLog() {}

    public UUID getLogId() { return logId; }
    public void setLogId(UUID logId) { this.logId = logId; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getRelatedEntity() { return relatedEntity; }
    public void setRelatedEntity(String relatedEntity) { this.relatedEntity = relatedEntity; }

    public AudioAssessment getAudioAssessment() { return audioAssessment; }
    public void setAudioAssessment(AudioAssessment audioAssessment) { this.audioAssessment = audioAssessment; }
}
