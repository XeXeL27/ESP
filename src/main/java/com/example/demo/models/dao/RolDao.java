package com.example.demo.models.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.demo.models.entity.Rol;
import java.util.Optional;

@Repository
public interface RolDao extends JpaRepository<Rol, Long> {
    
    @Query("SELECT r FROM Rol r WHERE r.nombre = :nombre")
    Optional<Rol> findByNombre(@Param("nombre") String nombre);
    
    @Query("SELECT r FROM Rol r WHERE r.estado = :estado")
    java.util.List<Rol> findByEstado(@Param("estado") String estado);
}