package com.equipo.proyecto_io.models;

import java.util.List;

/**
 * DTO para recibir los datos de la fábrica de ropa desde el frontend.
 */
public class SimplexRequest {
    private double[][] matriz;
    private double[] utilidades;
    private List<String> nombres; 
    private List<String> operadores;

    // Getters y Setters
    public List<String> getOperadores() { 
        return operadores; 
    }

    public void setOperadores(List<String> operadores) { 
        this.operadores = operadores; 
    }


    public List<String> getNombres() { 
        return nombres; 
    }

    public void setNombres(List<String> nombres) {
         this.nombres = nombres; 
    }
    
    
    public double[][] getMatriz() {
        return matriz;
    }

    public void setMatriz(double[][] matriz) {
        this.matriz = matriz;
    }

    public double[] getUtilidades() {
        return utilidades;
    }

    public void setUtilidades(double[] utilidades) {
        this.utilidades = utilidades;
    }
}