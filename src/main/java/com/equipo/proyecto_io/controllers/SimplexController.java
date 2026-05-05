package com.equipo.proyecto_io.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.equipo.proyecto_io.models.SimplexRequest;
import com.equipo.proyecto_io.services.SimplexService;

@RestController
@RequestMapping("/api/simplex")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SimplexController {

    @Autowired
    private SimplexService simplexService;

    @PostMapping("/optimizar")
    public SimplexService.SimplexResult optimizar(@RequestBody SimplexRequest request) {
        // Llamamos al método resolver que ya programamos
        return simplexService.resolver(request.getMatriz(), request.getUtilidades(), request.getOperadores(), request.getNombres());
    }
}