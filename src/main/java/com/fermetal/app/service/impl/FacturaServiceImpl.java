package com.fermetal.app.service.impl;

import com.fermetal.app.entity.Factura;
import com.fermetal.app.repository.FacturaRepository;
import com.fermetal.app.service.FacturaService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository repo;

    public FacturaServiceImpl(FacturaRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Factura> listar() {
        return repo.findAll();
    }

    @Override
    public List<Factura> listarPorOrden(Long idOrden) {
        return repo.findByOrden_IdOrden(idOrden);
    }

    @Override
    public List<Factura> listarPorEstado(String estadoFactura) {
        return repo.findByEstadoFactura(estadoFactura);
    }

    @Override
    public Factura obtenerPorId(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Factura guardar(Factura factura) {
        return repo.save(factura);
    }

    @Override
    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    @Override
    public List<String> listarSeriesFactura() {
        return repo.listarSeriesFactura();
    }

	@Override
	public List<Factura> listarFiltrado(Long idEmpresa, Long idObra, String qOrden, LocalDate desde, LocalDate hasta) {
		String tipoOrden = null;
        String numeroOrden = null;

        if (qOrden != null && !qOrden.isBlank()) {
            String limpio = qOrden.toUpperCase().replace("-", "").replace(" ", "");
            if (limpio.length() >= 3) {
                tipoOrden = limpio.substring(0, 2);  // OS / OC
                numeroOrden = limpio.substring(2);   // 123...
            }
        }

        return repo.buscarFiltrado(idEmpresa, idObra, tipoOrden, numeroOrden, desde, hasta);
    }
}