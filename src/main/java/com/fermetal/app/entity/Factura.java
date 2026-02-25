package com.fermetal.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFactura;

    @ManyToOne
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;

    // Opcional: solo si la orden usa valorización
    @ManyToOne
    @JoinColumn(name = "id_valorizacion")
    private Valorizacion valorizacion;

    // Serie opcional, número obligatorio
    @Size(max = 10, message = "Máximo 10 caracteres")
    @Column(length = 10)
    private String serieFactura;

    @NotBlank(message = "El número de factura es obligatorio")
    @Size(max = 20, message = "Máximo 20 caracteres")
    @Column(nullable = false, length = 20)
    private String numeroFactura;

    @NotNull(message = "La fecha de emisión es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate fechaEmision = LocalDate.now();

    @NotNull(message = "Los días de pago son obligatorios")
    @Column(nullable = false)
    private Integer diasPago = 7;

    @Column(nullable = false)
    private LocalDate fechaVencimiento = LocalDate.now();

    // ✅ En tu negocio: Orden/Valorización ya vienen con IGV incluido.
    // Dejamos estos campos por compatibilidad, pero NO se usan en formulario.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Detracción (4%) - opción
    @Column(nullable = false)
    private Boolean aplicaDetraccion = true;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDetraccion = BigDecimal.ZERO;

    @Column(nullable = false, length = 15)
    private String estadoDetraccion = "PENDIENTE"; // PENDIENTE / PAGADA / NO_APLICA

    // Fondo de garantía - opción
    @Column(nullable = false)
    private Boolean aplicaFondoGarantia = false;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeFondoGarantia = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoFondoRetenido = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoFondoPagado = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoFondoUsado = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String estadoFondoGarantia = "NO_APLICA";
    // NO_APLICA / PENDIENTE / PAGADO_TOTAL / PARCIAL_PAGADO / USO_PARCIAL

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netoCobrado = BigDecimal.ZERO;

    @Column(nullable = false, length = 15)
    private String estadoFactura = "POR_COBRAR";
    // POR_COBRAR / PAGADA / EN_RECLAMO / ANULADA

    // Si está ANULADA: Nota de crédito o Baja
    @Column(length = 15)
    private String tipoAnulacion; // NOTA_CREDITO / BAJA

    @Column(length = 10)
    private String serieAnulacion;

    @Column(length = 20)
    private String numeroAnulacion;

    private LocalDate fechaAnulacion;

    @Column(length = 200)
    private String motivoAnulacion;
    
    // =========================================================
    // Conciliación (dato real del estado de cuenta)
    // =========================================================
    @Column(precision = 12, scale = 2)
    private BigDecimal montoDepositado;

    private LocalDate fechaDeposito;

    @Column(length = 20)
    private String modoConciliacion; // CON_DETRACCION / SIN_DETRACCION
}