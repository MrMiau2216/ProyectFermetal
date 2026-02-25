package com.fermetal.app.service;

import com.fermetal.app.entity.Empresa;
import java.util.List;

public interface EmpresaService {
    List<Empresa> listar();
    Empresa obtenerPorId(Long id);
    Empresa guardar(Empresa empresa);
    void eliminar(Long id);
    boolean existePorRuc(String ruc);
    
}