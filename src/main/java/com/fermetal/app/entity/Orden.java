package com.fermetal.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orden")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrden;

    @NotBlank(message = "El tipo de orden es obligatorio")
    @Column(nullable = false, length = 2)
    private String tipoOrden; // "OC" o "OS"
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "El número de orden es obligatorio")
    @Size(max = 30, message = "Máximo 30 caracteres")
    @Column(nullable = false, length = 30)
    private String numeroOrden;

    @Size(max = 30, message = "Máximo 30 caracteres")
    @Column(length = 30)
    private String numeroCotizacionRef; // referencial (no PDF)

 

    @NotNull(message = "La fecha de orden es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // ✅ importante para input type="date"
    @Column(nullable = false)
    private LocalDate fechaOrden = LocalDate.now();

    @NotNull(message = "El monto total es obligatorio")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(nullable = false)
    private Boolean usaValorizacion = true; // true = por valorización, false = facturación directa

    @Column(nullable = false, length = 30)
    private String estadoProceso = "REGISTRADA";
    // opciones sugeridas: REGISTRADA, EN_ESPERA_AUTORIZACION, POR_VALORIZAR, POR_FACTURAR, CERRADA

    // Relación: muchas órdenes pertenecen a una obra
    @ManyToOne
    @JoinColumn(name = "id_obra", nullable = false)
    private Obra obra;
    
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Valorizacion> valorizaciones = new ArrayList<>();
    
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Factura> facturas = new ArrayList<>();
    
  
}