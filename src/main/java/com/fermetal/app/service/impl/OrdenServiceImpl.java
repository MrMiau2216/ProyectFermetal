package com.fermetal.app.service.impl;

import com.fermetal.app.entity.Orden;
import com.fermetal.app.repository.OrdenRepository;
import com.fermetal.app.service.OrdenService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrdenServiceImpl implements OrdenService {

    private final OrdenRepository repo;

    public OrdenServiceImpl(OrdenRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Orden> listar() {
        return repo.findAll();
    }

    @Override
    public List<Orden> listarPorObra(Long idObra) {
        return repo.findByObra_IdObra(idObra);
    }

    @Override
    public Orden obtenerPorId(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Orden guardar(Orden orden) {
        return repo.save(orden);
    }

    @Override
    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}