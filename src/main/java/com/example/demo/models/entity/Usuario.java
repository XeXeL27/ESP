package com.example.demo.models.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Setter
@Getter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;
    
    @Column(name = "user_name", unique = true, nullable = false)
    private String user_name;
    
    @Column(nullable = false)
    private String clave;
    
    private String estado;
    
    @Column(unique = true)  // ✅ Token debe ser único
    private String token;
    
    private LocalDate fechaRegistro;
    private LocalDate fechaModificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = true)
    private Persona persona;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = true)
    private Rol rol;
}