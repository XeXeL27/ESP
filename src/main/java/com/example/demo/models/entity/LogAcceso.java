package com.example.demo.models.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "log_acceso")
@Setter
@Getter
public class LogAcceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLog;
    
    @Column(name = "user_name", nullable = false)
    private String userName;
    
    @Column(nullable = false)
    private String accion;
    
    @Column(nullable = false)
    private String resultado;
    
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
    
    @Column(name = "direccion_ip")
    private String direccionIp;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String detalles;
    
    // Constructor por defecto
    public LogAcceso() {
        this.fechaHora = LocalDateTime.now();
    }
    
    // Constructor con parámetros básicos
    public LogAcceso(String userName, String accion, String resultado) {
        this();
        this.userName = userName;
        this.accion = accion;
        this.resultado = resultado;
    }
    
    // Constructor completo
    public LogAcceso(String userName, String accion, String resultado, 
                     String direccionIp, String userAgent, String detalles) {
        this(userName, accion, resultado);
        this.direccionIp = direccionIp;
        this.userAgent = userAgent;
        this.detalles = detalles;
    }
}