package com.fermetal.app.service;

import com.fermetal.app.entity.Valorizacion;

import java.math.BigDecimal;
import java.util.List;

public interface ValorizacionService {
    List<Valorizacion> listar();
    List<Valorizacion> listarPorOrden(Long idOrden);
    Valorizacion obtenerPorId(Long id);
    Valorizacion guardar(Valorizacion valorizacion);
    void eliminar(Long id);

    BigDecimal sumarPorcentajeUsado(Long idOrden, Long idValorizacionExcluir);
}