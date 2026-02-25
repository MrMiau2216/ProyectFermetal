package com.fermetal.app.controller;

import com.fermetal.app.entity.Orden;
import com.fermetal.app.service.ObraService;
import com.fermetal.app.service.OrdenService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/orden")
public class OrdenController {

    private final OrdenService ordenService;
    private final ObraService obraService;

    public OrdenController(OrdenService ordenService, ObraService obraService) {
        this.ordenService = ordenService;
        this.obraService = obraService;
    }

    /**
     * Lista de órdenes con filtros:
     * - idObra (select)
     * - qOrden (texto tipo "OS123" / "OC456")
     *
     * Esta forma es simple (estilo guía) y evita bugs.
     */
    @GetMapping
    public String listar(@RequestParam(value = "idObra", required = false) Long idObra,
                         @RequestParam(value = "qOrden", required = false) String qOrden,
                         Model model) {

        // 1) Traemos todo (MVP). Si luego crece mucho, lo movemos a query en repository.
        List<Orden> base = ordenService.listar();

        // 2) Filtro por obra (si viene)
        if (idObra != null) {
            base = base.stream()
                    .filter(o -> o.getObra() != null && idObra.equals(o.getObra().getIdObra()))
                    .toList();
        }

        // 3) Filtro por orden (si viene)
        // qOrden ejemplo: "OS123", "OC-456", "OS 789"
        if (qOrden != null && !qOrden.isBlank()) {
            String limpio = qOrden.toUpperCase().replace("-", "").replace(" ", "");
            String tipo = null;
            String numero = null;

            if (limpio.length() >= 2 && (limpio.startsWith("OS") || limpio.startsWith("OC"))) {
                tipo = limpio.substring(0, 2);         // OS / OC
                numero = limpio.length() > 2 ? limpio.substring(2) : "";
            } else {
                // Si no puso OS/OC, lo tomamos como parte del número
                numero = limpio;
            }

            final String tipoFinal = tipo;
            final String numeroFinal = numero;

            base = base.stream()
                    .filter(o -> {
                        boolean okTipo = true;
                        if (tipoFinal != null) {
                            okTipo = tipoFinal.equalsIgnoreCase(o.getTipoOrden());
                        }

                        boolean okNumero = true;
                        if (numeroFinal != null && !numeroFinal.isEmpty()) {
                            okNumero = o.getNumeroOrden() != null
                                    && o.getNumeroOrden().toUpperCase().contains(numeroFinal);
                        }
                        return okTipo && okNumero;
                    })
                    .toList();
        }

        // 4) Mandamos datos a la vista
        model.addAttribute("lista", base);
        model.addAttribute("obras", obraService.listar());

        // Mantener valores en pantalla
        model.addAttribute("idObraSeleccionada", idObra);
        model.addAttribute("qOrden", qOrden == null ? "" : qOrden);

        return "orden/index";
    }

    /**
     * Ver detalle de una Orden.
     */
    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model) {
        Orden orden = ordenService.obtenerPorId(id);
        if (orden == null) return "redirect:/orden";

        model.addAttribute("orden", orden);
        return "orden/ver";
    }

    /**
     * Formulario crear.
     */
    @GetMapping("/new")
    public String nuevo(Model model) {
        model.addAttribute("orden", new Orden());
        model.addAttribute("obras", obraService.listar());
        return "orden/create";
    }

    /**
     * Guardar nueva orden.
     * (Aquí solo guardamos; si ya tienes validaciones extra, las mantienes.)
     */
    @PostMapping("/save")
    public String guardar(@Valid @ModelAttribute("orden") Orden orden,
                          BindingResult br,
                          Model model) {

        if (orden.getObra() == null || orden.getObra().getIdObra() == null) {
            br.rejectValue("obra", "obra", "Debe seleccionar una obra");
        }

        if (br.hasErrors()) {
            model.addAttribute("obras", obraService.listar());
            return "orden/create";
        }

        ordenService.guardar(orden);
        return "redirect:/orden";
    }

    /**
     * Formulario editar.
     */
    @GetMapping("/edit/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Orden orden = ordenService.obtenerPorId(id);
        if (orden == null) return "redirect:/orden";

        model.addAttribute("orden", orden);
        model.addAttribute("obras", obraService.listar());
        return "orden/edit";
    }

    /**
     * Actualizar orden.
     */
    @PostMapping("/update")
    public String actualizar(@Valid @ModelAttribute("orden") Orden orden,
                             BindingResult br,
                             Model model) {

        if (orden.getObra() == null || orden.getObra().getIdObra() == null) {
            br.rejectValue("obra", "obra", "Debe seleccionar una obra");
        }

        if (br.hasErrors()) {
            model.addAttribute("obras", obraService.listar());
            return "orden/edit";
        }

        ordenService.guardar(orden);
        return "redirect:/orden";
    }

    /**
     * Eliminar orden.
     */
    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id) {
        ordenService.eliminar(id);
        return "redirect:/orden";
    }
}