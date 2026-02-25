package com.fermetal.app.repository;

import com.fermetal.app.entity.Valorizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ValorizacionRepository extends JpaRepository<Valorizacion, Long> {

    List<Valorizacion> findByOrden_IdOrden(Long idOrden);

    // ✅ No repetir mismo % dentro de una misma orden
    boolean existsByOrden_IdOrdenAndPorcentaje(Long idOrden, BigDecimal porcentaje);
    boolean existsByOrden_IdOrdenAndPorcentajeAndIdValorizacionNot(Long idOrden, BigDecimal porcentaje, Long idValorizacion);

    // ✅ SUMA de porcentajes por orden (BigDecimal)
    @Query("select coalesce(sum(v.porcentaje), 0) from Valorizacion v where v.orden.idOrden = :idOrden")
    BigDecimal sumaPorcentajePorOrden(@Param("idOrden") Long idOrden);

    @Query("select coalesce(sum(v.porcentaje), 0) from Valorizacion v where v.orden.idOrden = :idOrden and v.idValorizacion <> :idValorizacion")
    BigDecimal sumaPorcentajePorOrdenExcluyendo(@Param("idOrden") Long idOrden,
                                                @Param("idValorizacion") Long idValorizacion);

    // ✅ Listado filtrado por Obra + Orden (tipo/numero) + fechas
    @Query("""
        select v
        from Valorizacion v
        join v.orden o
        join o.obra ob
        where (:idObra is null or ob.idObra = :idObra)
          and (:tipoOrden is null or o.tipoOrden = :tipoOrden)
          and (:numeroOrden is null or o.numeroOrden like concat('%', :numeroOrden, '%'))
          and (:desde is null or v.fechaValorizacion >= :desde)
          and (:hasta is null or v.fechaValorizacion <= :hasta)
        order by v.fechaValorizacion desc, v.idValorizacion desc
    """)
    List<Valorizacion> buscarFiltrado(@Param("idObra") Long idObra,
                                      @Param("tipoOrden") String tipoOrden,
                                      @Param("numeroOrden") String numeroOrden,
                                      @Param("desde") LocalDate desde,
                                      @Param("hasta") LocalDate hasta);
}