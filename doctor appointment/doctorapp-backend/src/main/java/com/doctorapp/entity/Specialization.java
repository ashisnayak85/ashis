package com.doctorapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specializations", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // e.g. "Dentist", "Cardiologist"
}
