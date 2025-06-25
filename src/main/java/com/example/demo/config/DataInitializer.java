package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.demo.models.dao.RolDao;
import com.example.demo.models.entity.Rol;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private RolDao rolDao;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Inicializando datos del sistema...");
        
        // Crear roles por defecto si no existen
        if (rolDao.findByNombre("USUARIO").isEmpty()) {
            Rol rolUsuario = new Rol();
            rolUsuario.setNombre("USUARIO");
            rolUsuario.setEstado("ACTIVO");
            rolDao.save(rolUsuario);
            System.out.println("✅ Rol USUARIO creado");
        }
        
        if (rolDao.findByNombre("ADMIN").isEmpty()) {
            Rol rolAdmin = new Rol();
            rolAdmin.setNombre("ADMIN");
            rolAdmin.setEstado("ACTIVO");
            rolDao.save(rolAdmin);
            System.out.println("✅ Rol ADMIN creado");
        }
        
        System.out.println("🎯 Inicialización de datos completada");
    }
}