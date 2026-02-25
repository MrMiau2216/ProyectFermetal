package com.fermetal.app.controller;

import com.fermetal.app.entity.Orden;
import com.fermetal.app.entity.Valorizacion;
import com.fermetal.app.repository.ValorizacionRepository;
import com.fermetal.app.service.EmpresaService;
import com.fermetal.app.service.ObraService;
import com.fermetal.app.service.OrdenService;
import com.fermetal.app.service.ValorizacionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/valorizacion")
public class ValorizacionController {

    private final ValorizacionService valorizacionService;
    private final OrdenService ordenService;
    private final ObraService obraService;
    private final EmpresaService empresaService; // ✅ NUEVO
    private final ValorizacionRepository valorizacionRepository;

    public ValorizacionController(ValorizacionService valorizacionService,
                                  OrdenService ordenService,
                                  ObraService obraService,
                                  EmpresaService empresaService,               // ✅ NUEVO
                                  ValorizacionRepository valorizacionRepository) {
        this.valorizacionService = valorizacionService;
        this.ordenService = ordenService;
        this.obraService = obraService;
        this.empresaService = empresaService;     // ✅ NUEVO
        this.valorizacionRepository = valorizacionRepository;
    }

    // ✅ Listado con filtros (igual que lo tienes)
    @GetMapping
    public String listar(@RequestParam(value = "idObra", required = false) Long idObra,
                         @RequestParam(value = "qOrden", required = false) String qOrden,
                         @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         Model model) {

        String tipoOrden = null;
        String numeroOrden = null;

        if (qOrden != null && !qOrden.isBlank()) {
            String limpio = qOrden.toUpperCase().replace("-", "").replace(" ", "");
            if (limpio.length() >= 2 && (limpio.startsWith("OS") || limpio.startsWith("OC"))) {
                tipoOrden = limpio.substring(0, 2);
                numeroOrden = limpio.length() > 2 ? limpio.substring(2) : "";
            } else {
                numeroOrden = limpio;
            }
        }

        List<Valorizacion> lista = valorizacionRepository.buscarFiltrado(idObra, tipoOrden, numeroOrden, desde, hasta);

        model.addAttribute("lista", lista);
        model.addAttribute("obras", obraService.listar());

        model.addAttribute("idObra", idObra);
        model.addAttribute("qOrden", qOrden == null ? "" : qOrden);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        // Resumen % usado/pendiente (como lo tienes)
        if (lista != null && !lista.isEmpty()) {
            Orden ordenRef = lista.get(0).getOrden();
            boolean unaSolaOrden = lista.stream().allMatch(v -> v.getOrden() != null
                    && v.getOrden().getIdOrden().equals(ordenRef.getIdOrden()));

            if (unaSolaOrden) {
                BigDecimal usado = lista.stream()
                        .map(Valorizacion::getPorcentaje)
                        .filter(p -> p != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal pendiente = BigDecimal.valueOf(100).subtract(usado);
                if (pendiente.compareTo(BigDecimal.ZERO) < 0) pendiente = BigDecimal.ZERO;

                model.addAttribute("ordenSeleccionada", ordenRef);
                model.addAttribute("porcentajeUsado", usado);
                model.addAttribute("porcentajePendiente", pendiente.setScale(2, RoundingMode.HALF_UP));
            }
        }

        return "valorizacion/index";
    }

    // ✅ NEW (corregido): Empresa + Obra y luego Orden por autocomplete
    @GetMapping("/new")
    public String nuevo(@RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                        @RequestParam(value = "idObra", required = false) Long idObra,
                        Model model) {

        Valorizacion v = new Valorizacion();
        if (v.getFechaValorizacion() == null) v.setFechaValorizacion(LocalDate.now());

        model.addAttribute("valorizacion", v);

        // combos
        model.addAttribute("empresas", empresaService.listar());
        model.addAttribute("obras", obraService.listar()); // (si luego quieres filtrar por empresa, lo hacemos opcional)
        model.addAttribute("idEmpresa", idEmpresa);
        model.addAttribute("idObra", idObra);

        return "valorizacion/create";
    }

    @PostMapping("/save")
    public String guardar(@Valid @ModelAttribute("valorizacion") Valorizacion valorizacion,
                          BindingResult br,
                          @RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                          @RequestParam(value = "idObra", required = false) Long idObra,
                          Model model) {

        Orden orden = obtenerOrdenSeguro(valorizacion, br);

        if (orden != null) {
            validarDuplicadosYPorcentaje(valorizacion, orden, br);
        }

        if (br.hasErrors()) {
            model.addAttribute("empresas", empresaService.listar());
            model.addAttribute("obras", obraService.listar());
            model.addAttribute("idEmpresa", idEmpresa);
            model.addAttribute("idObra", idObra);
            return "valorizacion/create";
        }

        valorizacion.setOrden(orden);
        calcularMontoValorizado(valorizacion, orden);

        valorizacionService.guardar(valorizacion);
        return "redirect:/valorizacion";
    }

    // (lo demás igual que tu código)
    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model) {
        Valorizacion v = valorizacionService.obtenerPorId(id);
        if (v == null) return "redirect:/valorizacion";
        model.addAttribute("valorizacion", v);
        return "valorizacion/ver";
    }

    @GetMapping("/edit/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Valorizacion v = valorizacionService.obtenerPorId(id);
        if (v == null) return "redirect:/valorizacion";

        model.addAttribute("valorizacion", v);
        return "valorizacion/edit";
    }

    @PostMapping("/update")
    public String actualizar(@Valid @ModelAttribute("valorizacion") Valorizacion valorizacion,
                             BindingResult br,
                             Model model) {

        Orden orden = obtenerOrdenSeguro(valorizacion, br);

        if (orden != null) {
            validarDuplicadosYPorcentaje(valorizacion, orden, br);
        }

        if (br.hasErrors()) {
            return "valorizacion/edit";
        }

        valorizacion.setOrden(orden);
        calcularMontoValorizado(valorizacion, orden);

        valorizacionService.guardar(valorizacion);
        return "redirect:/valorizacion";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id) {
        valorizacionService.eliminar(id);
        return "redirect:/valorizacion";
    }

    // ---------------- helpers ----------------

    private Orden obtenerOrdenSeguro(Valorizacion valorizacion, BindingResult br) {
        if (valorizacion.getOrden() == null || valorizacion.getOrden().getIdOrden() == null) {
            br.rejectValue("orden", "orden", "Debe seleccionar una orden");
            return null;
        }

        Orden orden = ordenService.obtenerPorId(valorizacion.getOrden().getIdOrden());
        if (orden == null) {
            br.rejectValue("orden", "orden", "Orden no válida");
        }
        return orden;
    }

    private void validarDuplicadosYPorcentaje(Valorizacion valorizacion, Orden orden, BindingResult br) {
        // TU MISMA LÓGICA (sin tocar)
        if (valorizacion.getPorcentaje() == null) {
            br.rejectValue("porcentaje", "porcentaje", "El porcentaje es obligatorio");
            return;
        }

        BigDecimal porcentajeNuevo = valorizacion.getPorcentaje().setScale(2, RoundingMode.HALF_UP);

        if (porcentajeNuevo.compareTo(BigDecimal.ZERO) <= 0 || porcentajeNuevo.compareTo(BigDecimal.valueOf(100)) > 0) {
            br.rejectValue("porcentaje", "porcentaje", "El porcentaje debe estar entre 0.01 y 100");
            return;
        }

        Long idVal = valorizacion.getIdValorizacion();

        boolean existeMismoPorcentaje = (idVal == null)
                ? valorizacionRepository.existsByOrden_IdOrdenAndPorcentaje(orden.getIdOrden(), porcentajeNuevo)
                : valorizacionRepository.existsByOrden_IdOrdenAndPorcentajeAndIdValorizacionNot(orden.getIdOrden(), porcentajeNuevo, idVal);

        if (existeMismoPorcentaje) {
            br.rejectValue("porcentaje", "porcentaje", "Ya existe una valorización con ese porcentaje para esta orden");
            return;
        }

        BigDecimal sumaActual = (idVal == null)
                ? valorizacionRepository.sumaPorcentajePorOrden(orden.getIdOrden())
                : valorizacionRepository.sumaPorcentajePorOrdenExcluyendo(orden.getIdOrden(), idVal);

        if (sumaActual == null) sumaActual = BigDecimal.ZERO;

        if (sumaActual.add(porcentajeNuevo).compareTo(BigDecimal.valueOf(100)) > 0) {
            br.rejectValue("porcentaje", "porcentaje",
                    "La suma de valorizaciones para esta orden excede el 100% (actual: " + sumaActual + "%)");
        }
    }

    private void calcularMontoValorizado(Valorizacion valorizacion, Orden orden) {
        if (orden.getMontoTotal() == null) {
            valorizacion.setMontoValorizado(BigDecimal.ZERO);
            return;
        }

        BigDecimal porcentaje = valorizacion.getPorcentaje().setScale(2, RoundingMode.HALF_UP);

        BigDecimal monto = orden.getMontoTotal()
                .multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        valorizacion.setMontoValorizado(monto);
    }
}