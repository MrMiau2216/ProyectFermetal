package com.fermetal.app.repository;

import com.fermetal.app.entity.Obra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObraRepository extends JpaRepository<Obra, Long> {
    List<Obra> findByEmpresa_IdEmpresa(Long idEmpresa);
    
}