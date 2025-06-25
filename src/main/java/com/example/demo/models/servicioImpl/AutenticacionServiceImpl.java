package com.example.demo.models.servicioImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.models.dao.UsuarioDao;
import com.example.demo.models.dao.PersonaDao;
import com.example.demo.models.dao.RolDao;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.entity.Persona;
import com.example.demo.models.entity.Rol;
import com.example.demo.models.servicio.AutenticacionService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {
    
    @Autowired
    private UsuarioDao usuarioDao;
    
    @Autowired
    private PersonaDao personaDao;
    
    @Autowired
    private RolDao rolDao;
    
    @Override
    public Optional<Usuario> autenticar(String userName, String clave) {
        System.out.println("🔍 Intentando autenticar usuario: " + userName);
        return usuarioDao.findByUserNameAndClave(userName, clave);
    }
    
    @Override
    public Usuario registrarUsuario(String userName, String clave) {
        System.out.println("📝 Iniciando registro básico de usuario: " + userName);
        
        if (existeUsuario(userName)) {
            System.out.println("❌ Usuario ya existe: " + userName);
            throw new RuntimeException("El usuario ya existe");
        }
        
        // Crear nuevo usuario SIN persona (registro rápido)
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUser_name(userName);
        nuevoUsuario.setClave(clave);
        nuevoUsuario.setEstado("ACTIVO");
        nuevoUsuario.setFechaRegistro(LocalDate.now());
        
        // Asignar rol por defecto
        Rol rolDefecto = obtenerRolPorDefecto();
        nuevoUsuario.setRol(rolDefecto);
        
        // Persona queda como null (opcional)
        nuevoUsuario.setPersona(null);
        
        // Generar token único inicial
        String tokenInicial = UUID.randomUUID().toString();
        nuevoUsuario.setToken(tokenInicial);
        
        System.out.println("💾 Guardando usuario en base de datos...");
        
        try {
            Usuario usuarioGuardado = usuarioDao.save(nuevoUsuario);
            System.out.println("✅ Usuario guardado exitosamente con ID: " + usuarioGuardado.getIdUsuario());
            return usuarioGuardado;
        } catch (Exception e) {
            System.out.println("❌ Error al guardar usuario: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage());
        }
    }
    
    @Override
    public Usuario registrarUsuarioCompleto(String userName, String clave, 
                                          String nombre, String paterno, String materno, String ci) {
        System.out.println("📝 Iniciando registro completo de usuario: " + userName);
        
        if (existeUsuario(userName)) {
            throw new RuntimeException("El usuario ya existe");
        }
        
        if (existePersonaPorCi(ci)) {
            throw new RuntimeException("Ya existe una persona con este CI");
        }
        
        try {
            // 1. Crear y guardar persona
            Persona nuevaPersona = new Persona();
            nuevaPersona.setNombre(nombre);
            nuevaPersona.setPaterno(paterno);
            nuevaPersona.setMaterno(materno);
            nuevaPersona.setCi(ci);
            nuevaPersona.setEstado("ACTIVO");
            
            Persona personaGuardada = personaDao.save(nuevaPersona);
            System.out.println("✅ Persona guardada con ID: " + personaGuardada.getIdPersona());
            
            // 2. Crear usuario con la persona
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setUser_name(userName);
            nuevoUsuario.setClave(clave);
            nuevoUsuario.setEstado("ACTIVO");
            nuevoUsuario.setFechaRegistro(LocalDate.now());
            nuevoUsuario.setPersona(personaGuardada);
            
            // Asignar rol por defecto
            Rol rolDefecto = obtenerRolPorDefecto();
            nuevoUsuario.setRol(rolDefecto);
            
            // Generar token único
            String tokenInicial = UUID.randomUUID().toString();
            nuevoUsuario.setToken(tokenInicial);
            
            Usuario usuarioGuardado = usuarioDao.save(nuevoUsuario);
            System.out.println("✅ Usuario completo guardado con ID: " + usuarioGuardado.getIdUsuario());
            
            return usuarioGuardado;
            
        } catch (Exception e) {
            System.out.println("❌ Error al guardar usuario completo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al registrar usuario completo: " + e.getMessage());
        }
    }
    
    @Override
    public Rol obtenerRolPorDefecto() {
    try {
        // Buscar rol "USUARIO" o crear uno por defecto
        Optional<Rol> rolOpt = rolDao.findByNombre("USUARIO");
        
        if (rolOpt.isPresent()) {
            System.out.println("✅ Rol USUARIO encontrado con ID: " + rolOpt.get().getIdRol());
            return rolOpt.get();
        } else {
            // Crear rol por defecto si no existe
            System.out.println("📝 Creando rol USUARIO por defecto...");
            Rol rolDefecto = new Rol();
            rolDefecto.setNombre("USUARIO");
            rolDefecto.setEstado("ACTIVO");
            Rol rolGuardado = rolDao.save(rolDefecto);
            System.out.println("✅ Rol USUARIO creado con ID: " + rolGuardado.getIdRol());
            return rolGuardado;
        }
    } catch (Exception e) {
        System.out.println("❌ Error al obtener/crear rol por defecto: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Error al gestionar rol por defecto: " + e.getMessage());
    }
}
    
    @Override
    public Optional<Rol> obtenerRolPorNombre(String nombre) {
        return rolDao.findByNombre(nombre);
    }
    
    @Override
    public boolean existePersonaPorCi(String ci) {
        return personaDao.findByCi(ci).isPresent();
    }
    
    @Override
    public String generarTokenParaUsuario(Usuario usuario) {
        System.out.println("🔑 Generando nuevo token para usuario: " + usuario.getUser_name());
        
        usuarioDao.clearTokenByUserName(usuario.getUser_name());
        
        String token = UUID.randomUUID().toString();
        usuario.setToken(token);
        usuario.setFechaModificacion(LocalDate.now());
        
        usuarioDao.save(usuario);
        System.out.println("✅ Token generado: " + token.substring(0, 8) + "...");
        return token;
    }
    
    @Override
    public Optional<Usuario> validarToken(String token) {
        System.out.println("🔍 Validando token: " + token.substring(0, 8) + "...");
        return usuarioDao.findByToken(token);
    }
    
    @Override
    public void cerrarSesion(String userName) {
        System.out.println("🚪 Cerrando sesión para usuario: " + userName);
        usuarioDao.clearTokenByUserName(userName);
    }
    
    @Override
    public boolean existeUsuario(String userName) {
        boolean existe = usuarioDao.findByUserName(userName).isPresent();
        System.out.println("🔍 ¿Usuario " + userName + " existe? " + existe);
        return existe;
    }
}