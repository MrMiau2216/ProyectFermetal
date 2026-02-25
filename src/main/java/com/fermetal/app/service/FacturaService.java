package com.fermetal.app.service;

import com.fermetal.app.entity.Factura;

import java.time.LocalDate;
import java.util.List;

public interface FacturaService {
    List<Factura> listar();
    List<Factura> listarPorOrden(Long idOrden);
    List<Factura> listarPorEstado(String estadoFactura);
    Factura obtenerPorId(Long id);
    Factura guardar(Factura factura);
    void eliminar(Long id);
    List<String> listarSeriesFactura();
    
    List<Factura> listarFiltrado(Long idEmpresa, Long idObra, String qOrden, LocalDate desde, LocalDate hasta);
}