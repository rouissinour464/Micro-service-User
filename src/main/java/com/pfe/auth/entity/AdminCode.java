package com.pfe.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class AdminCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private boolean used = false;
}