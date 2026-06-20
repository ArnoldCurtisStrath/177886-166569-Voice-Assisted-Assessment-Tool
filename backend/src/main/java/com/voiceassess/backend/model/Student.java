package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Student — the learner being assessed.
 * Has date of birth and age (denormalized for quick access).
 * Linked to one parent/guardian.
 */
@Entity
@DiscriminatorValue("STUDENT")
public class Student extends User {

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Denormalized from dateOfBirth — avoids joins on dashboard queries */
    @Column(name = "age")
    private Integer age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    // M:N — a student can be enrolled in multiple classrooms
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "student_class_enrollments",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    private Set<ClassRoom> enrolledClasses = new HashSet<>();

    public Student() {
        super();
    }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Parent getParent() { return parent; }
    public void setParent(Parent parent) { this.parent = parent; }

    public Set<ClassRoom> getEnrolledClasses() { return enrolledClasses; }
    public void setEnrolledClasses(Set<ClassRoom> enrolledClasses) { this.enrolledClasses = enrolledClasses; }
}
