package com.fermetal.app.controller;

import com.fermetal.app.entity.Factura;
import com.fermetal.app.entity.Orden;
import com.fermetal.app.entity.Valorizacion;
import com.fermetal.app.repository.FacturaRepository;
import com.fermetal.app.service.EmpresaService;
import com.fermetal.app.service.FacturaService;
import com.fermetal.app.service.ObraService;
import com.fermetal.app.service.OrdenService;
import com.fermetal.app.service.ValorizacionService;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;




@Controller
@RequestMapping("/factura")
public class FacturaController {

	private final FacturaService facturaService;
    private final OrdenService ordenService;
    private final ValorizacionService valorizacionService;
    private final FacturaRepository facturaRepository;

    private final EmpresaService empresaService;
    private final ObraService obraService;

    public FacturaController(FacturaService facturaService,
                             OrdenService ordenService,
                             ValorizacionService valorizacionService,
                             FacturaRepository facturaRepository,
                             EmpresaService empresaService,
                             ObraService obraService) {
        this.facturaService = facturaService;
        this.ordenService = ordenService;
        this.valorizacionService = valorizacionService;
        this.facturaRepository = facturaRepository;
        this.empresaService = empresaService;
        this.obraService = obraService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "estado", required = false) String estado,
                         @RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                         @RequestParam(value = "idObra", required = false) Long idObra,
                         @RequestParam(value = "qOrden", required = false) String qOrden,
                         @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         Model model) {

        // ✅ Base filtrada (sin estado)
        List<Factura> base = facturaService.listarFiltrado(idEmpresa, idObra, qOrden, desde, hasta);

        long cntPorCobrar = base.stream().filter(f -> "POR_COBRAR".equals(f.getEstadoFactura())).count();
        long cntPagadas   = base.stream().filter(f -> "PAGADA".equals(f.getEstadoFactura())).count();
        long cntReclamo   = base.stream().filter(f -> "EN_RECLAMO".equals(f.getEstadoFactura())).count();
        long cntAnuladas  = base.stream().filter(f -> "ANULADA".equals(f.getEstadoFactura())).count();

        // ✅ Aplicar filtro por estado (si vino)
        List<Factura> lista;
        if (estado != null && !estado.isBlank()) {
            lista = base.stream().filter(f -> estado.equals(f.getEstadoFactura())).toList();
            model.addAttribute("estadoSeleccionado", estado);
        } else {
            lista = base;
            model.addAttribute("estadoSeleccionado", "");
        }

        model.addAttribute("lista", lista);

        // ✅ Mantener filtros en pantalla
        model.addAttribute("idEmpresa", idEmpresa);
        model.addAttribute("idObra", idObra);
        model.addAttribute("qOrden", qOrden == null ? "" : qOrden);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        // ✅ Contadores para los cuadritos
        model.addAttribute("cntPorCobrar", cntPorCobrar);
        model.addAttribute("cntPagadas", cntPagadas);
        model.addAttribute("cntReclamo", cntReclamo);
        model.addAttribute("cntAnuladas", cntAnuladas);

        // ✅ Combos
        model.addAttribute("empresas", empresaService.listar());
        model.addAttribute("obras", obraService.listar());

        return "factura/index";
    }

    @GetMapping("/new")
    public String nuevo(Model model) {
        Factura f = new Factura();
        if (f.getFechaEmision() == null) f.setFechaEmision(LocalDate.now());

        model.addAttribute("factura", f);
        model.addAttribute("seriesFactura", facturaService.listarSeriesFactura());
        model.addAttribute("obras", obraService.listar());
        return "factura/create";
    }

    @PostMapping("/save")
    public String guardar(@Valid @ModelAttribute("factura") Factura factura,
                          BindingResult br,
                          Model model) {

        Orden orden = obtenerOrdenSeguro(factura, br);
        Valorizacion valorizacion = obtenerValorizacionSeguro(factura);

        validarRelacionOrdenValorizacion(orden, valorizacion, br);

        if (orden != null) {
            validarDuplicadosFactura(factura, orden, valorizacion, br);
        }

        if (br.hasErrors()) {
            model.addAttribute("seriesFactura", facturaService.listarSeriesFactura());
            model.addAttribute("obras", obraService.listar());
            return "factura/create";
        }
        

        factura.setOrden(orden);
        factura.setValorizacion(valorizacion);

        recalcularFactura(factura, orden, valorizacion);
        facturaService.guardar(factura);

        return "redirect:/factura";
    }

    @GetMapping("/edit/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Factura factura = facturaService.obtenerPorId(id);
        if (factura == null) return "redirect:/factura";

        model.addAttribute("factura", factura);
        model.addAttribute("seriesFactura", facturaService.listarSeriesFactura());
        model.addAttribute("obras", obraService.listar()); // ✅ necesario
        return "factura/edit";
    }

    @PostMapping("/update")
    public String actualizar(@Valid @ModelAttribute("factura") Factura form,
                             BindingResult br,
                             Model model) {

        Factura factura = facturaService.obtenerPorId(form.getIdFactura());
        if (factura == null) return "redirect:/factura";

        Orden orden = obtenerOrdenSeguro(form, br);
        Valorizacion valorizacion = obtenerValorizacionSeguro(form);

        validarRelacionOrdenValorizacion(orden, valorizacion, br);

        if (orden != null) {
            validarDuplicadosFactura(form, orden, valorizacion, br);
        }

        // ✅ Copiar campos editables del form -> entidad real
        factura.setOrden(orden);
        factura.setValorizacion(valorizacion);

        factura.setSerieFactura(form.getSerieFactura());
        factura.setNumeroFactura(form.getNumeroFactura());
        factura.setDiasPago(form.getDiasPago());

        factura.setAplicaDetraccion(form.getAplicaDetraccion());
        factura.setAplicaFondoGarantia(form.getAplicaFondoGarantia());
        factura.setPorcentajeFondoGarantia(form.getPorcentajeFondoGarantia());
        factura.setMontoFondoPagado(form.getMontoFondoPagado());
        factura.setMontoFondoUsado(form.getMontoFondoUsado());
        factura.setEstadoFondoGarantia(form.getEstadoFondoGarantia());
        factura.setEstadoDetraccion(form.getEstadoDetraccion());

        // Estado factura
        factura.setEstadoFactura(form.getEstadoFactura());

        // ✅ ANULACIÓN
        if ("ANULADA".equals(form.getEstadoFactura())) {
            if (form.getTipoAnulacion() == null || form.getTipoAnulacion().isBlank()) {
                br.rejectValue("tipoAnulacion", "tipoAnulacion", "Debe indicar NOTA_CREDITO o BAJA");
            }
            if (form.getNumeroAnulacion() == null || form.getNumeroAnulacion().isBlank()) {
                br.rejectValue("numeroAnulacion", "numeroAnulacion", "Debe indicar el número");
            }

            factura.setTipoAnulacion(form.getTipoAnulacion());
            factura.setSerieAnulacion(form.getSerieAnulacion());
            factura.setNumeroAnulacion(form.getNumeroAnulacion());
            factura.setMotivoAnulacion(form.getMotivoAnulacion());

            if (factura.getFechaAnulacion() == null) {
                factura.setFechaAnulacion(LocalDate.now());
            }
        } else {
            factura.setTipoAnulacion(null);
            factura.setSerieAnulacion(null);
            factura.setNumeroAnulacion(null);
            factura.setFechaAnulacion(null);
            factura.setMotivoAnulacion(null);
        }

        if (br.hasErrors()) {
            model.addAttribute("factura", factura);
            model.addAttribute("seriesFactura", facturaService.listarSeriesFactura());
            model.addAttribute("obras", obraService.listar());
            return "factura/edit";
        }

        recalcularFactura(factura, orden, valorizacion);
        facturaService.guardar(factura);

        return "redirect:/factura";
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model) {
        Factura factura = facturaService.obtenerPorId(id);
        if (factura == null) return "redirect:/factura";
        model.addAttribute("factura", factura);
        return "factura/ver";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return "redirect:/factura";
    }
    
    

    // ---------------- helpers ----------------

    private Orden obtenerOrdenSeguro(Factura factura, BindingResult br) {
        if (factura.getOrden() == null || factura.getOrden().getIdOrden() == null) {
            br.rejectValue("orden", "orden", "Debe seleccionar una orden");
            return null;
        }
        Orden orden = ordenService.obtenerPorId(factura.getOrden().getIdOrden());
        if (orden == null) {
            br.rejectValue("orden", "orden", "Orden no válida");
        }
        return orden;
    }

    private Valorizacion obtenerValorizacionSeguro(Factura factura) {
        if (factura.getValorizacion() == null || factura.getValorizacion().getIdValorizacion() == null) {
            return null;
        }
        return valorizacionService.obtenerPorId(factura.getValorizacion().getIdValorizacion());
    }

    private void validarRelacionOrdenValorizacion(Orden orden, Valorizacion valorizacion, BindingResult br) {
        if (orden == null) return;

        if (Boolean.FALSE.equals(orden.getUsaValorizacion())) {
            if (valorizacion != null) {
                br.rejectValue("valorizacion", "valorizacion", "Esta orden es facturación directa, no debe elegir valorización");
            }
            return;
        }

        if (Boolean.TRUE.equals(orden.getUsaValorizacion())) {
            if (valorizacion == null) {
                br.rejectValue("valorizacion", "valorizacion", "Debe seleccionar una valorización para esta orden");
                return;
            }
            if (valorizacion.getOrden() == null || !orden.getIdOrden().equals(valorizacion.getOrden().getIdOrden())) {
                br.rejectValue("valorizacion", "valorizacion", "La valorización seleccionada no pertenece a la orden");
            }
        }
    }

    private void validarDuplicadosFactura(Factura factura, Orden orden, Valorizacion valorizacion, BindingResult br) {

        if (factura.getSerieFactura() != null && factura.getSerieFactura().isBlank()) {
            factura.setSerieFactura(null);
        }

        Long idFactura = factura.getIdFactura();

        // 1) serie+número o solo número
        if (factura.getSerieFactura() == null) {
            boolean existe = (idFactura == null)
                    ? facturaRepository.existsByNumeroFactura(factura.getNumeroFactura())
                    : facturaRepository.existsByNumeroFacturaAndIdFacturaNot(factura.getNumeroFactura(), idFactura);

            if (existe) br.rejectValue("numeroFactura", "numeroFactura", "Ya existe una factura con ese número");
        } else {
            boolean existe = (idFactura == null)
                    ? facturaRepository.existsBySerieFacturaAndNumeroFactura(factura.getSerieFactura(), factura.getNumeroFactura())
                    : facturaRepository.existsBySerieFacturaAndNumeroFacturaAndIdFacturaNot(factura.getSerieFactura(), factura.getNumeroFactura(), idFactura);

            if (existe) br.rejectValue("numeroFactura", "numeroFactura", "Ya existe una factura con esa serie y número");
        }

        // 2) orden+valorización
        if (valorizacion != null) {
            boolean existe = (idFactura == null)
                    ? facturaRepository.existsByOrden_IdOrdenAndValorizacion_IdValorizacion(orden.getIdOrden(), valorizacion.getIdValorizacion())
                    : facturaRepository.existsByOrden_IdOrdenAndValorizacion_IdValorizacionAndIdFacturaNot(orden.getIdOrden(), valorizacion.getIdValorizacion(), idFactura);

            if (existe) br.rejectValue("valorizacion", "valorizacion", "Ya existe una factura para esta valorización");
        } else {
            // 3) factura directa sin valorización (1 por orden)
            boolean existe = (idFactura == null)
                    ? facturaRepository.existsByOrden_IdOrdenAndValorizacionIsNull(orden.getIdOrden())
                    : facturaRepository.existsByOrden_IdOrdenAndValorizacionIsNullAndIdFacturaNot(orden.getIdOrden(), idFactura);

            if (existe) br.rejectValue("orden", "orden", "Esta orden ya tiene una factura directa (sin valorización)");
        }
    }

    private void recalcularFactura(Factura factura, Orden orden, Valorizacion valorizacion) {

    	// ✅ Total final (con IGV incluido) = Orden o Valorización
    	BigDecimal totalOperacion = (valorizacion != null)
    	        ? valorizacion.getMontoValorizado()
    	        : orden.getMontoTotal();

    	if (totalOperacion == null) totalOperacion = BigDecimal.ZERO;
    	totalOperacion = totalOperacion.setScale(2, RoundingMode.HALF_UP);

    	// ✅ NO recalcular IGV
    	factura.setSubtotal(totalOperacion);      // solo compatibilidad
    	factura.setIgv(BigDecimal.ZERO);          // siempre 0
    	factura.setTotal(totalOperacion);         // ESTE es el que verás en pantalla

        if (factura.getFechaEmision() == null) factura.setFechaEmision(LocalDate.now());
        if (factura.getDiasPago() == null) factura.setDiasPago(7);

        factura.setFechaVencimiento(factura.getFechaEmision().plusDays(factura.getDiasPago()));

        // ✅ Detracción 4% si aplica y >= 700
        BigDecimal montoDetraccion = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(factura.getAplicaDetraccion()) && totalOperacion.compareTo(BigDecimal.valueOf(700)) >= 0) {
            montoDetraccion = totalOperacion.multiply(BigDecimal.valueOf(0.04)).setScale(2, RoundingMode.HALF_UP);
            if (factura.getEstadoDetraccion() == null || "NO_APLICA".equals(factura.getEstadoDetraccion())) {
                factura.setEstadoDetraccion("PENDIENTE");
            }
        } else {
            factura.setEstadoDetraccion("NO_APLICA");
        }
        factura.setMontoDetraccion(montoDetraccion);

        // ✅ Fondo garantía (sobre total)
        BigDecimal montoFondo = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(factura.getAplicaFondoGarantia())) {
            if (factura.getPorcentajeFondoGarantia() == null) factura.setPorcentajeFondoGarantia(BigDecimal.ZERO);

            montoFondo = totalOperacion
                    .multiply(factura.getPorcentajeFondoGarantia())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (factura.getEstadoFondoGarantia() == null || "NO_APLICA".equals(factura.getEstadoFondoGarantia())) {
                factura.setEstadoFondoGarantia("PENDIENTE");
            }
        } else {
            factura.setPorcentajeFondoGarantia(BigDecimal.ZERO);
            factura.setEstadoFondoGarantia("NO_APLICA");
            factura.setMontoFondoPagado(BigDecimal.ZERO);
            factura.setMontoFondoUsado(BigDecimal.ZERO);
        }
        factura.setMontoFondoRetenido(montoFondo);

        // ✅ Neto a depositar
        BigDecimal neto = totalOperacion.subtract(montoDetraccion).subtract(montoFondo).setScale(2, RoundingMode.HALF_UP);
        factura.setNetoCobrado(neto);
    }
    
    
    
}