package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A school operates in academic terms. Each term scopes classrooms.
 * Status is typically "ACTIVE", "ARCHIVED", or "UPCOMING".
 */
@Entity
@Table(name = "academic_terms")
public class AcademicTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "term_id")
    private UUID termId;

    @Column(name = "term_name", nullable = false)
    private String termName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    private String status;

    // Each term belongs to one school
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    public AcademicTerm() {}

    public UUID getTermId() { return termId; }
    public void setTermId(UUID termId) { this.termId = termId; }

    public String getTermName() { return termName; }
    public void setTermName(String termName) { this.termName = termName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
}
