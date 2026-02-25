package com.fermetal.app.controller;

import com.fermetal.app.entity.Obra;
import com.fermetal.app.service.EmpresaService;
import com.fermetal.app.service.ObraService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/obra")
public class ObraController {

    private final ObraService obraService;
    private final EmpresaService empresaService;

    public ObraController(ObraService obraService, EmpresaService empresaService) {
        this.obraService = obraService;
        this.empresaService = empresaService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "idEmpresa", required = false) Long idEmpresa,
                         Model model) {

        model.addAttribute("empresas", empresaService.listar());

        if (idEmpresa != null) {
            model.addAttribute("lista", obraService.listarPorEmpresa(idEmpresa));
            model.addAttribute("idEmpresaSeleccionada", idEmpresa);
        } else {
            model.addAttribute("lista", obraService.listar());
            model.addAttribute("idEmpresaSeleccionada", null);
        }

        return "obra/index";
    }

    @GetMapping("/new")
    public String nuevo(Model model) {
        model.addAttribute("obra", new Obra());
        model.addAttribute("empresas", empresaService.listar());
        return "obra/create";
    }

    @PostMapping("/save")
    public String guardar(@Valid @ModelAttribute("obra") Obra obra,
                          BindingResult br,
                          Model model) {

        if (br.hasErrors()) {
            model.addAttribute("empresas", empresaService.listar());
            return "obra/create";
        }

        obraService.guardar(obra);
        return "redirect:/obra";
    }

    @GetMapping("/edit/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Obra obra = obraService.obtenerPorId(id);
        if (obra == null) return "redirect:/obra";

        model.addAttribute("obra", obra);
        model.addAttribute("empresas", empresaService.listar());
        return "obra/edit";
    }

    @PostMapping("/update")
    public String actualizar(@Valid @ModelAttribute("obra") Obra obra,
                             BindingResult br,
                             Model model) {

        if (br.hasErrors()) {
            model.addAttribute("empresas", empresaService.listar());
            return "obra/edit";
        }

        obraService.guardar(obra);
        return "redirect:/obra";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id) {
        obraService.eliminar(id);
        return "redirect:/obra";
    }
}