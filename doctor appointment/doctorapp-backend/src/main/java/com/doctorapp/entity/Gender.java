package com.doctorapp.entity;

/**
 * Shared across every entity that collects gender (Patient, Doctor, ...).
 * Previously Patient and Doctor each declared their own private nested
 * Gender enum with identical constants - structurally the same but distinct
 * types to the compiler, which caused Doctor's `import Patient.Gender` to be
 * silently shadowed by its own nested enum of the same name. One shared type
 * avoids that trap and the duplicated parsing logic.
 */
public enum Gender {
    MALE, FEMALE, OTHER
}
