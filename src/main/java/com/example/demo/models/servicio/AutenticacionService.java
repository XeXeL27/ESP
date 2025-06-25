package com.example.demo.models.servicio;

import java.util.Optional;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.entity.Persona;
import com.example.demo.models.entity.Rol;

public interface AutenticacionService {
    // Autenticación
    Optional<Usuario> autenticar(String userName, String clave);
    
    // Registro de nuevos usuarios
    Usuario registrarUsuario(String userName, String clave);
    Usuario registrarUsuarioCompleto(String userName, String clave, 
                                   String nombre, String paterno, String materno, String ci);
    
    // Token management
    String generarTokenParaUsuario(Usuario usuario);
    Optional<Usuario> validarToken(String token);
    void cerrarSesion(String userName);
    
    // Validaciones
    boolean existeUsuario(String userName);
    boolean existePersonaPorCi(String ci);
    
    // Gestión de roles y personas
    Rol obtenerRolPorDefecto();
    Optional<Rol> obtenerRolPorNombre(String nombre);
}