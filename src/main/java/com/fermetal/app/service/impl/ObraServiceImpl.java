package com.fermetal.app.service.impl;

import com.fermetal.app.entity.Obra;
import com.fermetal.app.repository.ObraRepository;
import com.fermetal.app.service.ObraService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ObraServiceImpl implements ObraService {

    private final ObraRepository repo;

    public ObraServiceImpl(ObraRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Obra> listar() {
        return repo.findAll();
    }

    @Override
    public List<Obra> listarPorEmpresa(Long idEmpresa) {
        return repo.findByEmpresa_IdEmpresa(idEmpresa);
    }

    @Override
    public Obra obtenerPorId(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Obra guardar(Obra obra) {
        return repo.save(obra);
    }

    @Override
    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}