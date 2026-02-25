package com.fermetal.app.controller;

import com.fermetal.app.entity.Orden;
import com.fermetal.app.service.OrdenService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * API interna (JSON) solo para AUTOCOMPLETE.
 * No reemplaza vistas. Sirve para que la orden se busque "tipo navegador".
 */
@RestController
@RequestMapping("/api/valorizacion")
public class ValorizacionApiController {

    private final OrdenService ordenService;

    public ValorizacionApiController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    /**
     * Autocomplete de órdenes por OBRA + texto.
     * q puede ser: "OS", "OS12", "OC-123", "123", etc.
     * Retorna máximo 20 resultados para no saturar.
     */
    @GetMapping("/ordenes")
    public List<OrdenItem> sugerirOrdenes(@RequestParam Long idObra,
                                         @RequestParam(required = false, defaultValue = "") String q) {

        String texto = q.trim().toUpperCase().replace("-", "").replace(" ", "");

        String tipo = null;   // "OS" o "OC"
        String numero = "";   // parte numérica o texto

        // Si empieza con OS u OC, separamos
        if (texto.startsWith("OS") || texto.startsWith("OC")) {
            tipo = texto.substring(0, 2);
            numero = (texto.length() > 2) ? texto.substring(2) : "";
        } else {
            numero = texto; // si no puso OS/OC, filtramos por número
        }

        // MVP: usamos el listado por obra y filtramos en memoria (simple y entendible).
        // Si después tienes miles de órdenes, esto se puede optimizar con query en Repository.
        List<Orden> ordenesObra = ordenService.listarPorObra(idObra);

        List<OrdenItem> salida = new ArrayList<>();
        for (Orden o : ordenesObra) {

            if (tipo != null && !tipo.equalsIgnoreCase(o.getTipoOrden())) continue;

            if (numero != null && !numero.isBlank()) {
                if (o.getNumeroOrden() == null) continue;
                if (!o.getNumeroOrden().toUpperCase().contains(numero)) continue;
            }

            // Texto que verá el usuario en la lista
            String label = o.getTipoOrden() + "-" + o.getNumeroOrden()
                    + " | PEN " + o.getMontoTotal()
                    + " | Val=" + (Boolean.TRUE.equals(o.getUsaValorizacion()) ? "SI" : "NO");

            salida.add(new OrdenItem(o.getIdOrden(), label));

            if (salida.size() >= 20) break;
        }

        return salida;
    }

    // DTO simple (evita ciclos con JPA)
    public record OrdenItem(Long idOrden, String label) {}
}