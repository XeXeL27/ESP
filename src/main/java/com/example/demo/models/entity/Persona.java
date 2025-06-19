package com.example.demo.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "persona")
public class Persona {
    private Long idPersona;
    private String nombre;
    private String paterno;
    private String materno;
    private String ci;
    private String estado;
}