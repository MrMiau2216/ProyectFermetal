package com.fermetal.app.service;

import com.fermetal.app.entity.Obra;
import java.util.List;

public interface ObraService {
    List<Obra> listar();
    List<Obra> listarPorEmpresa(Long idEmpresa);
    Obra obtenerPorId(Long id);
    Obra guardar(Obra obra);
    void eliminar(Long id);
}