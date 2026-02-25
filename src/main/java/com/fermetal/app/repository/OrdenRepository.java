package com.fermetal.app.repository;

import com.fermetal.app.entity.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    List<Orden> findByObra_IdObra(Long idObra);
    boolean existsByObra_IdObraAndTipoOrdenAndNumeroOrden(Long idObra, String tipoOrden, String numeroOrden);

    boolean existsByObra_IdObraAndTipoOrdenAndNumeroOrdenAndIdOrdenNot(
            Long idObra, String tipoOrden, String numeroOrden, Long idOrden
    );
}