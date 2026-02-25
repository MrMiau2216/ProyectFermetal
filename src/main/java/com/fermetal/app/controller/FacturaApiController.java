package com.fermetal.app.controller;

import com.fermetal.app.entity.Orden;
import com.fermetal.app.entity.Valorizacion;
import com.fermetal.app.service.OrdenService;
import com.fermetal.app.service.ValorizacionService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller REST (JSON) solo para ayudas de UI:
 * - Autocompletar órdenes por obra
 * - Cargar valorizaciones por orden
 *
 * IMPORTANTE:
 * Esto NO reemplaza tus vistas.
 * Solo se usa desde el HTML con fetch() para "mientras escribo".
 */
@RestController
@RequestMapping("/api/factura")
public class FacturaApiController {

    private final OrdenService ordenService;
    private final ValorizacionService valorizacionService;

    public FacturaApiController(OrdenService ordenService, ValorizacionService valorizacionService) {
        this.ordenService = ordenService;
        this.valorizacionService = valorizacionService;
    }

    /**
     * Devuelve sugerencias de órdenes filtradas por obra + texto.
     * q puede ser: "OS", "OS12", "OC-123", "123", etc.
     */
    @GetMapping("/ordenes")
    public List<OrdenItem> listarOrdenes(@RequestParam Long idObra,
                                        @RequestParam(required = false, defaultValue = "") String q) {

        String texto = q.trim().toUpperCase().replace("-", "").replace(" ", "");
        String tipo = null;
        String numero = null;

        // Si el usuario empieza con OS/OC, separamos tipo y número
        if (texto.startsWith("OS") || texto.startsWith("OC")) {
            tipo = texto.substring(0, 2);
            numero = texto.length() > 2 ? texto.substring(2) : "";
        } else {
            // si no empieza con OS/OC, lo tomamos como parte del número
            numero = texto;
        }

        // MVP (simple): filtramos en memoria.
        List<Orden> todas = ordenService.listar();

        List<OrdenItem> salida = new ArrayList<>();
        for (Orden o : todas) {

            if (o.getObra() == null || o.getObra().getIdObra() == null) continue;
            if (!idObra.equals(o.getObra().getIdObra())) continue;

            if (tipo != null && !tipo.equalsIgnoreCase(o.getTipoOrden())) continue;
            if (numero != null && !numero.isEmpty() && !o.getNumeroOrden().toUpperCase().contains(numero)) continue;

            String label = o.getTipoOrden() + o.getNumeroOrden()
                    + " | PEN " + o.getMontoTotal()
                    + " | Val=" + (Boolean.TRUE.equals(o.getUsaValorizacion()) ? "SI" : "NO");

            salida.add(new OrdenItem(o.getIdOrden(), label, Boolean.TRUE.equals(o.getUsaValorizacion())));

            if (salida.size() >= 20) break; // límite
        }

        return salida;
    }

    /**
     * Devuelve valorizaciones SOLO de una orden.
     * Evita mezclar valorizaciones de otras órdenes.
     */
    @GetMapping("/valorizaciones")
    public List<ValorizacionItem> listarValorizaciones(@RequestParam Long idOrden) {

        // Requiere: valorizacionService.listarPorOrden(idOrden)
        List<Valorizacion> lista = valorizacionService.listarPorOrden(idOrden);

        List<ValorizacionItem> salida = new ArrayList<>();
        for (Valorizacion v : lista) {
            String label = v.getPorcentaje() + "% | PEN " + v.getMontoValorizado() + " | " + v.getFechaValorizacion();
            salida.add(new ValorizacionItem(v.getIdValorizacion(), label));
        }
        return salida;
    }

    // DTOs simples (para evitar ciclos JSON)
    public record OrdenItem(Long idOrden, String label, boolean usaValorizacion) {}
    public record ValorizacionItem(Long idValorizacion, String label) {}
}