package com.fermetal.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "obra")
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idObra;

    @NotBlank(message = "El nombre de la obra es obligatorio")
    @Size(max = 150, message = "Máximo 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    @Size(max = 200, message = "Máximo 200 caracteres")
    @Column(length = 200)
    private String direccion;

    @Column(nullable = false, length = 10)
    private String estado = "ACTIVO";

    // Relación: muchas obras pertenecen a una empresa
    @ManyToOne
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;
    
    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Orden> ordenes = new ArrayList<>();
}