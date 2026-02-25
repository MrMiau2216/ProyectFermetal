package com.fermetal.app.service.impl;

import com.fermetal.app.entity.Empresa;
import com.fermetal.app.repository.EmpresaRepository;
import com.fermetal.app.service.EmpresaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository repo;

    public EmpresaServiceImpl(EmpresaRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Empresa> listar() {
        return repo.findAll();
    }

    @Override
    public Empresa obtenerPorId(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Empresa guardar(Empresa empresa) {
        return repo.save(empresa);
    }

    @Override
    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existePorRuc(String ruc) {
        return repo.existsByRuc(ruc);
    }
}