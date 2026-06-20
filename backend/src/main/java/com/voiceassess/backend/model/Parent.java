package com.voiceassess.backend.model;

import jakarta.persistence.*;

/**
 * Parent / guardian — can view linked children's progress and assessments.
 * Has a phone_number field for contact purposes.
 */
@Entity
@DiscriminatorValue("PARENT")
public class Parent extends User {

    @Column(name = "phone_number")
    private String phoneNumber;

    public Parent() {
        super();
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
