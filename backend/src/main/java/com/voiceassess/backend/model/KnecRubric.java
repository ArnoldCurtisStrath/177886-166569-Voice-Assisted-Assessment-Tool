package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * KNEC-defined rubric for CBC assessment.
 * Maps a competency description to a strand/sub-strand with a rating scale.
 * Each rubric belongs to a subject (e.g. "Numbers" strand under Mathematics).
 */
@Entity
@Table(name = "knec_rubrics")
public class KnecRubric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rubric_id")
    private UUID rubricId;

    @Column(name = "competency_desc", columnDefinition = "TEXT", nullable = false)
    private String competencyDesc;

    @Column(name = "strand", nullable = false)
    private String strand;

    @Column(name = "sub_strand")
    private String subStrand;

    @Column(name = "rating_scale", nullable = false)
    private String ratingScale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    public KnecRubric() {}

    public UUID getRubricId() { return rubricId; }
    public void setRubricId(UUID rubricId) { this.rubricId = rubricId; }

    public String getCompetencyDesc() { return competencyDesc; }
    public void setCompetencyDesc(String competencyDesc) { this.competencyDesc = competencyDesc; }

    public String getStrand() { return strand; }
    public void setStrand(String strand) { this.strand = strand; }

    public String getSubStrand() { return subStrand; }
    public void setSubStrand(String subStrand) { this.subStrand = subStrand; }

    public String getRatingScale() { return ratingScale; }
    public void setRatingScale(String ratingScale) { this.ratingScale = ratingScale; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
}
