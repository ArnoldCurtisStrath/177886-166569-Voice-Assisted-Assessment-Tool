package com.voiceassess.backend.model;

import jakarta.persistence.*;

/**
 * Teacher — uploads audio assessments, reviews staging records, resolves appeals.
 * No extra fields beyond the User base; the discriminator column identifies the type.
 */
@Entity
@DiscriminatorValue("TEACHER")
public class Teacher extends User {

    public Teacher() {
        super();
    }
}
