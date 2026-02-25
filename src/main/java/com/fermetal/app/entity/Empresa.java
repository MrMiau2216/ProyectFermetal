package com.fermetal.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEmpresa;

    @NotBlank(message = "El RUC es obligatorio")
    @Size(min = 11, max = 11, message = "El RUC debe tener 11 dígitos")
    @Column(nullable = false, length = 11, unique = true)
    private String ruc;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "Máximo 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nombre;

    @Size(max = 120, message = "Máximo 120 caracteres")
    @Column(length = 120)
    private String contactoNombre;

    @Size(max = 30, message = "Máximo 30 caracteres")
    @Column(length = 30)
    private String contactoTelefono;

    @Email(message = "Correo no válido")
    @Size(max = 120, message = "Máximo 120 caracteres")
    @Column(length = 120)
    private String contactoCorreo;

    @Size(max = 200, message = "Máximo 200 caracteres")
    @Column(length = 200)
    private String direccion;

    @Column(nullable = false, length = 10)
    private String estado = "ACTIVO";
    
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Obra> obras = new ArrayList<>();
}