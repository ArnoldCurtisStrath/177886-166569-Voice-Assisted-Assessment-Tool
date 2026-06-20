package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores audio assessment metadata and file path.
 */
@Entity
@Table(name = "audio_assessments")
public class AudioAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audio_id")
    private UUID audioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** Teacher's curated prompt context — JSON with selected words/phrases for the AI */
    @Column(name = "curated_context", columnDefinition = "TEXT")
    private String curatedContext;

    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    private String status = "UPLOADED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private KnecRubric rubric;

    public AudioAssessment() {}

    public UUID getAudioId() { return audioId; }
    public void setAudioId(UUID audioId) { this.audioId = audioId; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public ClassRoom getClassRoom() { return classRoom; }
    public void setClassRoom(ClassRoom classRoom) { this.classRoom = classRoom; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getCuratedContext() { return curatedContext; }
    public void setCuratedContext(String curatedContext) { this.curatedContext = curatedContext; }

    public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public KnecRubric getRubric() { return rubric; }
    public void setRubric(KnecRubric rubric) { this.rubric = rubric; }
}
