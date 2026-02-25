package com.fermetal.app.repository;

import com.fermetal.app.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByOrden_IdOrden(Long idOrden);
    List<Factura> findByEstadoFactura(String estadoFactura);

    // --- duplicados por número / serie+número
    boolean existsByNumeroFactura(String numeroFactura);
    boolean existsByNumeroFacturaAndIdFacturaNot(String numeroFactura, Long idFactura);

    boolean existsBySerieFacturaAndNumeroFactura(String serieFactura, String numeroFactura);
    boolean existsBySerieFacturaAndNumeroFacturaAndIdFacturaNot(String serieFactura, String numeroFactura, Long idFactura);

    // --- duplicados por orden + valorización (o factura directa por orden)
    boolean existsByOrden_IdOrdenAndValorizacion_IdValorizacion(Long idOrden, Long idValorizacion);
    boolean existsByOrden_IdOrdenAndValorizacion_IdValorizacionAndIdFacturaNot(Long idOrden, Long idValorizacion, Long idFactura);

    boolean existsByOrden_IdOrdenAndValorizacionIsNull(Long idOrden);
    boolean existsByOrden_IdOrdenAndValorizacionIsNullAndIdFacturaNot(Long idOrden, Long idFactura);

    // --- series (para tu “autocomplete”)
    @Query("""
        select distinct f.serieFactura
        from Factura f
        where f.serieFactura is not null and trim(f.serieFactura) <> ''
        order by f.serieFactura
    """)
    List<String> listarSeriesFactura();
    
    // ✅ Listado filtrado: Empresa + Obra + Orden (OS123) + fechas + estado
    @Query("""
            select f
            from Factura f
            join f.orden o
            join o.obra ob
            join ob.empresa e
            where (:idEmpresa is null or e.idEmpresa = :idEmpresa)
              and (:idObra is null or ob.idObra = :idObra)
              and (:tipoOrden is null or o.tipoOrden = :tipoOrden)
              and (:numeroOrden is null or o.numeroOrden like concat('%', :numeroOrden, '%'))
              and (:estadoFactura is null or f.estadoFactura = :estadoFactura)
              and (:desde is null or f.fechaEmision >= :desde)
              and (:hasta is null or f.fechaEmision <= :hasta)
            order by f.fechaEmision desc
        """)
        List<Factura> buscarFiltrado(@Param("idEmpresa") Long idEmpresa,
                                     @Param("idObra") Long idObra,
                                     @Param("tipoOrden") String tipoOrden,
                                     @Param("numeroOrden") String numeroOrden,
                                     @Param("estadoFactura") String estadoFactura,
                                     @Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);

        // ✅ Contadores por estado (respetando filtros)
        @Query("""
            select f.estadoFactura, count(f)
            from Factura f
            join f.orden o
            join o.obra ob
            join ob.empresa e
            where (:idEmpresa is null or e.idEmpresa = :idEmpresa)
              and (:idObra is null or ob.idObra = :idObra)
              and (:tipoOrden is null or o.tipoOrden = :tipoOrden)
              and (:numeroOrden is null or o.numeroOrden like concat('%', :numeroOrden, '%'))
              and (:desde is null or f.fechaEmision >= :desde)
              and (:hasta is null or f.fechaEmision <= :hasta)
            group by f.estadoFactura
        """)
        List<Object[]> contarPorEstado(@Param("idEmpresa") Long idEmpresa,
                                       @Param("idObra") Long idObra,
                                       @Param("tipoOrden") String tipoOrden,
                                       @Param("numeroOrden") String numeroOrden,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);
    
        @Query("""
                select f
                from Factura f
                join f.orden o
                join o.obra ob
                join ob.empresa e
                where (:idEmpresa is null or e.idEmpresa = :idEmpresa)
                  and (:idObra is null or ob.idObra = :idObra)
                  and (:tipoOrden is null or o.tipoOrden = :tipoOrden)
                  and (:numeroOrden is null or o.numeroOrden like concat('%', :numeroOrden, '%'))
                  and (:desde is null or f.fechaEmision >= :desde)
                  and (:hasta is null or f.fechaEmision <= :hasta)
                order by f.fechaEmision desc, f.idFactura desc
            """)
            List<Factura> buscarFiltrado(@Param("idEmpresa") Long idEmpresa,
                                         @Param("idObra") Long idObra,
                                         @Param("tipoOrden") String tipoOrden,
                                         @Param("numeroOrden") String numeroOrden,
                                         @Param("desde") LocalDate desde,
                                         @Param("hasta") LocalDate hasta);
        
    
}