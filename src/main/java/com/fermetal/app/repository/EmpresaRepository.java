package com.fermetal.app.repository;

import com.fermetal.app.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    boolean existsByRuc(String ruc);
}