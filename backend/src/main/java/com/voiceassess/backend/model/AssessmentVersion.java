package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable version history for an assessment record.
 * Every time a teacher overrides an assessment, we create a new version
 * and keep the original. This satisfies the audit requirement.
 */
@Entity
@Table(name = "assessment_versions")
public class AssessmentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id")
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private AssessmentRecord assessmentRecord;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "json_data", columnDefinition = "TEXT", nullable = false)
    private String jsonData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by", nullable = false)
    private Teacher editedBy;

    @Column(name = "edit_timestamp", nullable = false)
    private LocalDateTime editTimestamp = LocalDateTime.now();

    @Column(name = "is_original", nullable = false)
    private boolean isOriginal = false;

    public AssessmentVersion() {}

    public UUID getVersionId() { return versionId; }
    public void setVersionId(UUID versionId) { this.versionId = versionId; }

    public AssessmentRecord getAssessmentRecord() { return assessmentRecord; }
    public void setAssessmentRecord(AssessmentRecord assessmentRecord) { this.assessmentRecord = assessmentRecord; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getJsonData() { return jsonData; }
    public void setJsonData(String jsonData) { this.jsonData = jsonData; }

    public Teacher getEditedBy() { return editedBy; }
    public void setEditedBy(Teacher editedBy) { this.editedBy = editedBy; }

    public LocalDateTime getEditTimestamp() { return editTimestamp; }
    public void setEditTimestamp(LocalDateTime editTimestamp) { this.editTimestamp = editTimestamp; }

    public boolean isOriginal() { return isOriginal; }
    public void setOriginal(boolean original) { isOriginal = original; }
}
