package com.fermetal.app.service.impl;

import com.fermetal.app.entity.Valorizacion;
import com.fermetal.app.repository.ValorizacionRepository;
import com.fermetal.app.service.ValorizacionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ValorizacionServiceImpl implements ValorizacionService {

    private final ValorizacionRepository repo;

    public ValorizacionServiceImpl(ValorizacionRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Valorizacion> listar() {
        return repo.findAll();
    }

    @Override
    public List<Valorizacion> listarPorOrden(Long idOrden) {
        return repo.findByOrden_IdOrden(idOrden);
    }

    @Override
    public Valorizacion obtenerPorId(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Valorizacion guardar(Valorizacion valorizacion) {
        return repo.save(valorizacion);
    }

    @Override
    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    @Override
    public BigDecimal sumarPorcentajeUsado(Long idOrden, Long idValorizacionExcluir) {
        List<Valorizacion> lista = repo.findByOrden_IdOrden(idOrden);
        BigDecimal suma = BigDecimal.ZERO;

        for (Valorizacion v : lista) {
            if (idValorizacionExcluir != null && v.getIdValorizacion().equals(idValorizacionExcluir)) {
                continue;
            }
            if (v.getPorcentaje() != null) {
                suma = suma.add(v.getPorcentaje());
            }
        }
        return suma;
    }
}