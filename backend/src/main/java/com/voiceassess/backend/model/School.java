package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Root tenant — every other entity links back to a school.
 * KNEC code is the Kenya National Examinations Council identifier.
 */
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "knec_code", nullable = false, unique = true)
    private String knecCode;

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    public School() {}

    public School(String knecCode, String schoolName) {
        this.knecCode = knecCode;
        this.schoolName = schoolName;
    }

    public UUID getSchoolId() { return schoolId; }
    public void setSchoolId(UUID schoolId) { this.schoolId = schoolId; }

    public String getKnecCode() { return knecCode; }
    public void setKnecCode(String knecCode) { this.knecCode = knecCode; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
}
