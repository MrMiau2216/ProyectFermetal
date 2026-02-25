package com.fermetal.app.controller;

import com.fermetal.app.entity.Empresa;
import com.fermetal.app.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/empresa")
public class EmpresaController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", service.listar());
        return "empresa/index";
    }

    @GetMapping("/new")
    public String nuevo(Model model) {
        model.addAttribute("empresa", new Empresa());
        return "empresa/create";
    }

    @PostMapping("/save")
    public String guardar(@Valid @ModelAttribute("empresa") Empresa empresa,
                          BindingResult br) {

        if (br.hasErrors()) return "empresa/create";

        if (service.existePorRuc(empresa.getRuc())) {
            br.rejectValue("ruc", "error.ruc", "El RUC ya está registrado");
            return "empresa/create";
        }

        service.guardar(empresa);
        return "redirect:/empresa";
    }

    @GetMapping("/edit/{id}")
    public String editar(@PathVariable("id") Long id, Model model) {
        Empresa empresa = service.obtenerPorId(id);
        if (empresa == null) return "redirect:/empresa";

        model.addAttribute("empresa", empresa);
        return "empresa/edit";
    }

    @PostMapping("/update")
    public String actualizar(@Valid @ModelAttribute("empresa") Empresa empresa,
                             BindingResult br) {

        if (br.hasErrors()) return "empresa/edit";

        Empresa actual = service.obtenerPorId(empresa.getIdEmpresa());
        if (actual != null && !actual.getRuc().equals(empresa.getRuc())) {
            if (service.existePorRuc(empresa.getRuc())) {
                br.rejectValue("ruc", "error.ruc", "El RUC ya está registrado");
                return "empresa/edit";
            }
        }

        service.guardar(empresa);
        return "redirect:/empresa";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id) {
        service.eliminar(id);
        return "redirect:/empresa";
    }
}