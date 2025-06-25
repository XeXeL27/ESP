package com.example.demo.models.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.demo.models.entity.Persona;
import java.util.Optional;

@Repository
public interface PersonaDao extends JpaRepository<Persona, Long> {
    
    @Query("SELECT p FROM Persona p WHERE p.ci = :ci")
    Optional<Persona> findByCi(@Param("ci") String ci);
    
    @Query("SELECT p FROM Persona p WHERE p.estado = :estado")
    java.util.List<Persona> findByEstado(@Param("estado") String estado);
}