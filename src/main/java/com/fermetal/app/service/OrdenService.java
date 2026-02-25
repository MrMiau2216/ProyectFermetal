package com.fermetal.app.service;

import com.fermetal.app.entity.Orden;
import java.util.List;

public interface OrdenService {
    List<Orden> listar();
    List<Orden> listarPorObra(Long idObra);
    Orden obtenerPorId(Long id);
    Orden guardar(Orden orden);
    void eliminar(Long id);
}