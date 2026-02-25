package com.fermetal.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "valorizacion")
public class Valorizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idValorizacion;

    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede ser mayor a 100")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    // Se calcula automáticamente según la Orden
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoValorizado = BigDecimal.ZERO;

    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate fechaValorizacion = LocalDate.now();

    @Column(nullable = false, length = 30)
    private String estado = "REGISTRADA";
    // REGISTRADA / FACTURADA / ANULADA (por ahora usaremos REGISTRADA)

    // Relación: muchas valorizaciones pertenecen a una orden
    @ManyToOne
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;
}