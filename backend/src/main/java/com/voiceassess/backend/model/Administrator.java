package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * School admin — the super-user who sets up the workspace.
 * Owns a set of Capabilities for fine-grained RBAC.
 */
@Entity
@DiscriminatorValue("ADMINISTRATOR")
public class Administrator extends User {

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    // An admin manages exactly one school (simplified — one admin = one school for now)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    // RBAC: admin holds zero or more capability flags
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "administrator_capabilities",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "capability_id")
    )
    private Set<Capability> capabilities = new HashSet<>();

    public Administrator() {
        super();
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    public Set<Capability> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<Capability> capabilities) { this.capabilities = capabilities; }
}
