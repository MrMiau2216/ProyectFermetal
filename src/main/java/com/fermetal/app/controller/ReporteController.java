package com.fermetal.app.controller;

import com.fermetal.app.entity.Factura;
import com.fermetal.app.service.EmpresaService;
import com.fermetal.app.service.FacturaService;
import com.fermetal.app.service.ObraService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Controller
@RequestMapping("/reporte")
public class ReporteController {

    private final FacturaService facturaService;
    private final EmpresaService empresaService;
    private final ObraService obraService;

    public ReporteController(FacturaService facturaService,
                             EmpresaService empresaService,
                             ObraService obraService) {
        this.facturaService = facturaService;
        this.empresaService = empresaService;
        this.obraService = obraService;
    }

    // =========================================================
    // ✅ REPORTE FACTURAS (VISTA)
    // =========================================================
    @GetMapping("/facturas")
    public String reporteFacturas(@RequestParam(value = "estado", required = false) String estado,
                                  @RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                                  @RequestParam(value = "idObra", required = false) Long idObra,
                                  @RequestParam(value = "qOrden", required = false) String qOrden,
                                  @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                  Model model) {

        List<Factura> base = facturaService.listarFiltrado(idEmpresa, idObra, qOrden, desde, hasta);

        long cntPorCobrar = base.stream().filter(f -> "POR_COBRAR".equals(f.getEstadoFactura())).count();
        long cntPagadas   = base.stream().filter(f -> "PAGADA".equals(f.getEstadoFactura())).count();
        long cntReclamo   = base.stream().filter(f -> "EN_RECLAMO".equals(f.getEstadoFactura())).count();
        long cntAnuladas  = base.stream().filter(f -> "ANULADA".equals(f.getEstadoFactura())).count();

        String estadoSeleccionado = (estado == null) ? "" : estado.trim();

        List<Factura> lista = estadoSeleccionado.isBlank()
                ? base
                : base.stream().filter(f -> estadoSeleccionado.equals(f.getEstadoFactura())).toList();

        BigDecimal totalFacturado = lista.stream()
                .map(f -> nz(f.getTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNeto = lista.stream()
                .map(f -> nz(f.getNetoCobrado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal detraccionPendiente = lista.stream()
                .filter(f -> "PENDIENTE".equals(f.getEstadoDetraccion()))
                .map(f -> nz(f.getMontoDetraccion()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fondoPorLiberar = lista.stream()
                .filter(f -> Boolean.TRUE.equals(f.getAplicaFondoGarantia()))
                .map(f -> {
                    BigDecimal retenido = nz(f.getMontoFondoRetenido());
                    BigDecimal pagado   = nz(f.getMontoFondoPagado());
                    BigDecimal usado    = nz(f.getMontoFondoUsado());
                    BigDecimal saldo = retenido.subtract(pagado).subtract(usado);
                    return saldo.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : saldo;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("lista", lista);

        // filtros
        model.addAttribute("estadoSeleccionado", estadoSeleccionado);
        model.addAttribute("idEmpresa", idEmpresa);
        model.addAttribute("idObra", idObra);
        model.addAttribute("qOrden", qOrden == null ? "" : qOrden);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        // contadores
        model.addAttribute("cntPorCobrar", cntPorCobrar);
        model.addAttribute("cntPagadas", cntPagadas);
        model.addAttribute("cntReclamo", cntReclamo);
        model.addAttribute("cntAnuladas", cntAnuladas);

        // KPIs
        model.addAttribute("totalFacturado", totalFacturado);
        model.addAttribute("totalNeto", totalNeto);
        model.addAttribute("detraccionPendiente", detraccionPendiente);
        model.addAttribute("fondoPorLiberar", fondoPorLiberar);

        model.addAttribute("empresas", empresaService.listar());
        model.addAttribute("obras", obraService.listar());

        return "reporte/facturas";
    }

    // =========================================================
    // ✅ REPORTE FACTURAS (EXCEL)
    //    Exporta exactamente lo que estás filtrando en pantalla.
    // =========================================================
    @GetMapping("/facturas/excel")
    public void exportarFacturasExcel(
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
            @RequestParam(value = "idObra", required = false) Long idObra,
            @RequestParam(value = "qOrden", required = false) String qOrden,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletResponse response
    ) throws Exception {

        // ✅ Si deseas obligar a elegir Empresa y Obra, deja esto activo.
        if (idEmpresa == null || idObra == null) {
            response.sendError(400, "Debe seleccionar Empresa y Obra para exportar.");
            return;
        }

        List<Factura> base = facturaService.listarFiltrado(idEmpresa, idObra, qOrden, desde, hasta);

        String estadoSeleccionado = (estado == null) ? "" : estado.trim();
        List<Factura> lista = estadoSeleccionado.isBlank()
                ? base
                : base.stream().filter(f -> estadoSeleccionado.equals(f.getEstadoFactura())).toList();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Facturas");

            int rowIdx = 0;

            String[] cols = {
                    "Empresa", "Obra", "Orden",
                    "Factura", "Fecha Emision", "Fecha Vencimiento",
                    "Total", "Fondo Retenido", "Detraccion",
                    "Neto Cobrado", "Estado Factura", "Estado Detraccion", "Estado Fondo"
            };

            var header = sheet.createRow(rowIdx++);
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            for (Factura f : lista) {
                var row = sheet.createRow(rowIdx++);
                int c = 0;

                String empresa = (f.getOrden() != null && f.getOrden().getObra() != null && f.getOrden().getObra().getEmpresa() != null)
                        ? f.getOrden().getObra().getEmpresa().getNombre()
                        : "";

                String obra = (f.getOrden() != null && f.getOrden().getObra() != null)
                        ? f.getOrden().getObra().getNombre()
                        : "";

                String orden = (f.getOrden() != null)
                        ? (nvl(f.getOrden().getTipoOrden()) + nvl(f.getOrden().getNumeroOrden()))
                        : "";

                String facturaTxt = (f.getSerieFactura() != null && !f.getSerieFactura().isBlank())
                        ? (f.getSerieFactura() + "-" + f.getNumeroFactura())
                        : nvl(f.getNumeroFactura());

                row.createCell(c++).setCellValue(empresa);
                row.createCell(c++).setCellValue(obra);
                row.createCell(c++).setCellValue(orden);

                row.createCell(c++).setCellValue(facturaTxt);

                row.createCell(c++).setCellValue(f.getFechaEmision() == null ? "" : f.getFechaEmision().toString());
                row.createCell(c++).setCellValue(f.getFechaVencimiento() == null ? "" : f.getFechaVencimiento().toString());

                row.createCell(c++).setCellValue(nz(f.getTotal()).doubleValue());
                row.createCell(c++).setCellValue(nz(f.getMontoFondoRetenido()).doubleValue());
                row.createCell(c++).setCellValue(nz(f.getMontoDetraccion()).doubleValue());

                row.createCell(c++).setCellValue(nz(f.getNetoCobrado()).doubleValue());

                row.createCell(c++).setCellValue(nvl(f.getEstadoFactura()));
                row.createCell(c++).setCellValue(nvl(f.getEstadoDetraccion()));
                row.createCell(c++).setCellValue(nvl(f.getEstadoFondoGarantia()));
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=facturas_filtradas.xlsx");
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    // =========================================================
    // ✅ CONCILIACIÓN
    // =========================================================
    @GetMapping("/conciliacion")
    public String conciliacion(@RequestParam(value = "estado", required = false) String estado,
                               @RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                               @RequestParam(value = "idObra", required = false) Long idObra,
                               @RequestParam(value = "qOrden", required = false) String qOrden,
                               @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                               Model model) {

        List<Factura> base = facturaService.listarFiltrado(idEmpresa, idObra, qOrden, desde, hasta);
        String estadoSeleccionado = (estado == null) ? "" : estado.trim();

        List<Factura> lista = estadoSeleccionado.isBlank()
                ? base
                : base.stream().filter(f -> estadoSeleccionado.equals(f.getEstadoFactura())).toList();

        List<FilaConciliacion> filas = new ArrayList<>();
        for (Factura f : lista) {
            BigDecimal total = nz(f.getTotal());

            BigDecimal fondo = Boolean.TRUE.equals(f.getAplicaFondoGarantia())
                    ? nz(f.getMontoFondoRetenido())
                    : BigDecimal.ZERO;

            BigDecimal detraccion = (Boolean.TRUE.equals(f.getAplicaDetraccion()) && nz(f.getMontoDetraccion()).compareTo(BigDecimal.ZERO) > 0)
                    ? nz(f.getMontoDetraccion())
                    : BigDecimal.ZERO;

            BigDecimal esperadoSinDetraccion = total.subtract(fondo);
            BigDecimal esperadoConDetraccion = esperadoSinDetraccion.subtract(detraccion);

            filas.add(new FilaConciliacion(f, esperadoSinDetraccion, esperadoConDetraccion));
        }

        model.addAttribute("filas", filas);

        model.addAttribute("estadoSeleccionado", estadoSeleccionado);
        model.addAttribute("idEmpresa", idEmpresa);
        model.addAttribute("idObra", idObra);
        model.addAttribute("qOrden", qOrden == null ? "" : qOrden);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        model.addAttribute("empresas", empresaService.listar());
        model.addAttribute("obras", obraService.listar());

        return "reporte/conciliacion";
    }

    @PostMapping("/conciliacion/marcar")
    public String marcarConciliacion(@RequestParam("idFactura") Long idFactura,
                                     @RequestParam("modo") String modo) {

        Factura factura = facturaService.obtenerPorId(idFactura);
        if (factura == null) return "redirect:/reporte/conciliacion";

        // Si está anulada, no debería conciliarse
        if ("ANULADA".equalsIgnoreCase(factura.getEstadoFactura())) {
            return "redirect:/reporte/conciliacion";
        }

        // Recalcular montos esperados (misma lógica que tu tabla)
        BigDecimal total = nz(factura.getTotal());

        BigDecimal fondo = (Boolean.TRUE.equals(factura.getAplicaFondoGarantia()))
                ? nz(factura.getMontoFondoRetenido())
                : BigDecimal.ZERO;

        BigDecimal detraccion = (Boolean.TRUE.equals(factura.getAplicaDetraccion())
                && nz(factura.getMontoDetraccion()).compareTo(BigDecimal.ZERO) > 0)
                ? nz(factura.getMontoDetraccion())
                : BigDecimal.ZERO;

        BigDecimal esperadoSinDetraccion = total.subtract(fondo);
        BigDecimal esperadoConDetraccion = esperadoSinDetraccion.subtract(detraccion);

        // ✅ Guardar modo y monto depositado AUTOMÁTICO
        if ("CON_DETRACCION".equalsIgnoreCase(modo)) {
            factura.setModoConciliacion("CON_DETRACCION");
            factura.setMontoDepositado(esperadoConDetraccion);

            // detracción pagada
            if (detraccion.compareTo(BigDecimal.ZERO) > 0) {
                factura.setEstadoDetraccion("PAGADA");
            } else {
                factura.setEstadoDetraccion("NO_APLICA");
            }

        } else { // SIN_DETRACCION
            factura.setModoConciliacion("SIN_DETRACCION");
            factura.setMontoDepositado(esperadoSinDetraccion);

            // detracción quedó pendiente (si aplica)
            if (detraccion.compareTo(BigDecimal.ZERO) > 0) {
                factura.setEstadoDetraccion("PENDIENTE");
            } else {
                factura.setEstadoDetraccion("NO_APLICA");
            }
        }

        // ✅ Siempre que concilias: PAGADA
        factura.setEstadoFactura("PAGADA");

        facturaService.guardar(factura);
        return "redirect:/reporte/conciliacion";
    }

    // =========================================================
    // ✅ RESUMEN POR EMPRESA (VISTA)
    // =========================================================
    @GetMapping("/resumen-empresas")
    public String resumenEmpresas(@RequestParam(value = "estado", required = false) String estado,
                                  @RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                                  @RequestParam(value = "idObra", required = false) Long idObra,
                                  @RequestParam(value = "qOrden", required = false) String qOrden,
                                  @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                  Model model) {

        List<Factura> base = facturaService.listarFiltrado(idEmpresa, idObra, qOrden, desde, hasta);
        String estadoSeleccionado = (estado == null) ? "" : estado.trim();

        List<Factura> lista = estadoSeleccionado.isBlank()
                ? base
                : base.stream().filter(f -> estadoSeleccionado.equals(f.getEstadoFactura())).toList();

        LinkedHashMap<Long, ResumenEmpresa> mapa = new LinkedHashMap<>();

        for (Factura f : lista) {
            if (f.getOrden() == null || f.getOrden().getObra() == null || f.getOrden().getObra().getEmpresa() == null) continue;

            Long idEmp = f.getOrden().getObra().getEmpresa().getIdEmpresa();
            String nombreEmp = f.getOrden().getObra().getEmpresa().getNombre();

            ResumenEmpresa r = mapa.computeIfAbsent(idEmp, k -> new ResumenEmpresa(idEmp, nombreEmp));

            r.cantidadFacturas++;
            if ("POR_COBRAR".equals(f.getEstadoFactura())) r.cntPorCobrar++;
            if ("PAGADA".equals(f.getEstadoFactura())) r.cntPagadas++;
            if ("EN_RECLAMO".equals(f.getEstadoFactura())) r.cntReclamo++;
            if ("ANULADA".equals(f.getEstadoFactura())) r.cntAnuladas++;

            r.totalFacturado = r.totalFacturado.add(nz(f.getTotal()));
            r.totalNeto      = r.totalNeto.add(nz(f.getNetoCobrado()));

            if (Boolean.TRUE.equals(f.getAplicaDetraccion()) && "PENDIENTE".equals(f.getEstadoDetraccion())) {
                r.detraccionPendiente = r.detraccionPendiente.add(nz(f.getMontoDetraccion()));
            }

            if (Boolean.TRUE.equals(f.getAplicaFondoGarantia())) {
                BigDecimal saldo = nz(f.getMontoFondoRetenido())
                        .subtract(nz(f.getMontoFondoPagado()))
                        .subtract(nz(f.getMontoFondoUsado()));
                if (saldo.compareTo(BigDecimal.ZERO) < 0) saldo = BigDecimal.ZERO;
                r.fondoPorLiberar = r.fondoPorLiberar.add(saldo);
            }
        }

        List<ResumenEmpresa> resumen = new ArrayList<>(mapa.values());
        resumen.sort((a, b) -> b.totalFacturado.compareTo(a.totalFacturado));

        model.addAttribute("resumen", resumen);

        model.addAttribute("estadoSeleccionado", estadoSeleccionado);
        model.addAttribute("idEmpresa", idEmpresa);
        model.addAttribute("idObra", idObra);
        model.addAttribute("qOrden", qOrden == null ? "" : qOrden);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        model.addAttribute("empresas", empresaService.listar());
        model.addAttribute("obras", obraService.listar());

        return "reporte/resumen_empresas";
    }

    // =========================================================
    // ✅ RESUMEN POR EMPRESA (EXCEL)
    // =========================================================
    @GetMapping("/resumen-empresas/excel")
    public void resumenEmpresasExcel(@RequestParam(value = "estado", required = false) String estado,
                                     @RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                                     @RequestParam(value = "idObra", required = false) Long idObra,
                                     @RequestParam(value = "qOrden", required = false) String qOrden,
                                     @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     HttpServletResponse response) throws Exception {

        List<Factura> base = facturaService.listarFiltrado(idEmpresa, idObra, qOrden, desde, hasta);
        String estadoSeleccionado = (estado == null) ? "" : estado.trim();

        List<Factura> lista = estadoSeleccionado.isBlank()
                ? base
                : base.stream().filter(f -> estadoSeleccionado.equals(f.getEstadoFactura())).toList();

        LinkedHashMap<Long, ResumenEmpresa> mapa = new LinkedHashMap<>();

        for (Factura f : lista) {
            if (f.getOrden() == null || f.getOrden().getObra() == null || f.getOrden().getObra().getEmpresa() == null) continue;

            Long idEmp = f.getOrden().getObra().getEmpresa().getIdEmpresa();
            String nombreEmp = f.getOrden().getObra().getEmpresa().getNombre();

            ResumenEmpresa r = mapa.computeIfAbsent(idEmp, k -> new ResumenEmpresa(idEmp, nombreEmp));

            r.cantidadFacturas++;
            if ("POR_COBRAR".equals(f.getEstadoFactura())) r.cntPorCobrar++;
            if ("PAGADA".equals(f.getEstadoFactura())) r.cntPagadas++;
            if ("EN_RECLAMO".equals(f.getEstadoFactura())) r.cntReclamo++;
            if ("ANULADA".equals(f.getEstadoFactura())) r.cntAnuladas++;

            r.totalFacturado = r.totalFacturado.add(nz(f.getTotal()));
            r.totalNeto      = r.totalNeto.add(nz(f.getNetoCobrado()));

            if (Boolean.TRUE.equals(f.getAplicaDetraccion()) && "PENDIENTE".equals(f.getEstadoDetraccion())) {
                r.detraccionPendiente = r.detraccionPendiente.add(nz(f.getMontoDetraccion()));
            }

            if (Boolean.TRUE.equals(f.getAplicaFondoGarantia())) {
                BigDecimal saldo = nz(f.getMontoFondoRetenido())
                        .subtract(nz(f.getMontoFondoPagado()))
                        .subtract(nz(f.getMontoFondoUsado()));
                if (saldo.compareTo(BigDecimal.ZERO) < 0) saldo = BigDecimal.ZERO;
                r.fondoPorLiberar = r.fondoPorLiberar.add(saldo);
            }
        }

        List<ResumenEmpresa> resumen = new ArrayList<>(mapa.values());
        resumen.sort((a, b) -> b.totalFacturado.compareTo(a.totalFacturado));

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Resumen_Empresas");

            int rowIdx = 0;
            var header = sheet.createRow(rowIdx++);
            String[] cols = {
                    "ID Empresa", "Empresa", "Facturas",
                    "Por cobrar", "Pagadas", "En reclamo", "Anuladas",
                    "Total facturado", "Total neto",
                    "Detracción pendiente", "Fondo por liberar"
            };
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            for (ResumenEmpresa r : resumen) {
                var row = sheet.createRow(rowIdx++);
                int c = 0;

                // ✅ evitar problemas con Long
                row.createCell(c++).setCellValue(r.idEmpresa == null ? 0 : r.idEmpresa.longValue());
                row.createCell(c++).setCellValue(nvl(r.nombreEmpresa));
                row.createCell(c++).setCellValue(r.cantidadFacturas);
                row.createCell(c++).setCellValue(r.cntPorCobrar);
                row.createCell(c++).setCellValue(r.cntPagadas);
                row.createCell(c++).setCellValue(r.cntReclamo);
                row.createCell(c++).setCellValue(r.cntAnuladas);

                row.createCell(c++).setCellValue(r.totalFacturado.doubleValue());
                row.createCell(c++).setCellValue(r.totalNeto.doubleValue());
                row.createCell(c++).setCellValue(r.detraccionPendiente.doubleValue());
                row.createCell(c++).setCellValue(r.fondoPorLiberar.doubleValue());
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=resumen_empresas.xlsx");
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    // =========================================================
    // helpers
    // =========================================================
    private BigDecimal nz(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }

    // DTO conciliación
    public static class FilaConciliacion {
        private final Factura factura;
        private final BigDecimal esperadoSinDetraccion;
        private final BigDecimal esperadoConDetraccion;

        public FilaConciliacion(Factura factura, BigDecimal esperadoSinDetraccion, BigDecimal esperadoConDetraccion) {
            this.factura = factura;
            this.esperadoSinDetraccion = esperadoSinDetraccion;
            this.esperadoConDetraccion = esperadoConDetraccion;
        }

        public Factura getFactura() { return factura; }
        public BigDecimal getEsperadoSinDetraccion() { return esperadoSinDetraccion; }
        public BigDecimal getEsperadoConDetraccion() { return esperadoConDetraccion; }
    }

    // DTO resumen empresa
    public static class ResumenEmpresa {
        public Long idEmpresa;
        public String nombreEmpresa;

        public long cantidadFacturas = 0;
        public long cntPorCobrar = 0;
        public long cntPagadas = 0;
        public long cntReclamo = 0;
        public long cntAnuladas = 0;

        public BigDecimal totalFacturado = BigDecimal.ZERO;
        public BigDecimal totalNeto = BigDecimal.ZERO;
        public BigDecimal detraccionPendiente = BigDecimal.ZERO;
        public BigDecimal fondoPorLiberar = BigDecimal.ZERO;

        public ResumenEmpresa(Long idEmpresa, String nombreEmpresa) {
            this.idEmpresa = idEmpresa;
            this.nombreEmpresa = nombreEmpresa;
        }

        public Long getIdEmpresa() { return idEmpresa; }
        public String getNombreEmpresa() { return nombreEmpresa; }
        public long getCantidadFacturas() { return cantidadFacturas; }
        public long getCntPorCobrar() { return cntPorCobrar; }
        public long getCntPagadas() { return cntPagadas; }
        public long getCntReclamo() { return cntReclamo; }
        public long getCntAnuladas() { return cntAnuladas; }
        public BigDecimal getTotalFacturado() { return totalFacturado; }
        public BigDecimal getTotalNeto() { return totalNeto; }
        public BigDecimal getDetraccionPendiente() { return detraccionPendiente; }
        public BigDecimal getFondoPorLiberar() { return fondoPorLiberar; }
    }
}