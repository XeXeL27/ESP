package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.models.dao.PersonaDao;
import com.example.demo.models.dao.RolDao;
import com.example.demo.models.dao.UsuarioDao;
import com.example.demo.models.entity.Rol;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.servicio.PasswordService;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolDao rolDao;

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private PersonaDao personaDao;

    @Autowired
    private PasswordService passwordService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Inicializando datos del sistema...");

        // Crear roles por defecto si no existen
        Rol rolUsuario = crearRolSiNoExiste("USUARIO");
        Rol rolAdmin = crearRolSiNoExiste("ADMIN");

        // Crear usuario administrador por defecto
        crearUsuarioAdminPorDefecto(rolAdmin);

        System.out.println("🎯 Inicialización de datos completada");
    }

    /**
     * Crea un rol si no existe
     */
    private Rol crearRolSiNoExiste(String nombreRol) {
        return rolDao.findByNombre(nombreRol).orElseGet(() -> {
            Rol rol = new Rol();
            rol.setNombre(nombreRol);
            rol.setEstado("ACTIVO");
            rolDao.save(rol);
            System.out.println("✅ Rol " + nombreRol + " creado");
            return rol;
        });
    }

    /**
     * Crea usuario administrador por defecto si no existe
     */
    private void crearUsuarioAdminPorDefecto(Rol rolAdmin) {
        // Verificar si ya existe un admin
        String usuarioDefecto = "admin";

        if (usuarioDao.findByUserName(usuarioDefecto).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUser_name(usuarioDefecto);

            // Encriptar contraseña por defecto
            String claveDefecto = "Admin123!";
            admin.setClave(passwordService.encriptarClave(claveDefecto));

            admin.setEstado("ACTIVO");
            admin.setRol(rolAdmin);

            usuarioDao.save(admin);

            System.out.println("👤 =======================================");
            System.out.println("✅ Usuario administrador creado:");
            System.out.println("   👉 Usuario: " + usuarioDefecto);
            System.out.println("   👉 Contraseña: " + claveDefecto);
            System.out.println("   ⚠️  ¡CAMBIA ESTA CONTRASEÑA INMEDIATAMENTE!");
            System.out.println("==========================================");
        } else {
            System.out.println("ℹ️  Usuario administrador ya existe");
        }
    }
}