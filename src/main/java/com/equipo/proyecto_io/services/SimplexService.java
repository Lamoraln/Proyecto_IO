package com.equipo.proyecto_io.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SimplexService {

    // --- CLASES INTERNAS DE RESULTADOS ---

    public static class SimplexResult {
        public double[][] tablaFinal;
        public double utilidadOptima;
        public boolean esFactible;
        public String mensaje;
        public List<AnalisisRecurso> analisisRecursos;
        public List<AnalisisVariable> analisisVariables;

        public SimplexResult(double[][] tabla, double utilidad, boolean factible, String msg) {
            this.tablaFinal = tabla;
            this.utilidadOptima = utilidad;
            this.esFactible = factible;
            this.mensaje = msg;
            this.analisisRecursos = new ArrayList<>();
            this.analisisVariables = new ArrayList<>();
        }
    }

    public static class AnalisisRecurso {
        public String nombre;
        public double precioSombra;
        public double limiteInferior;
        public double limiteSuperior;
        
        public AnalisisRecurso(String nombre, double precioSombra, double inf, double sup) {
            this.nombre = nombre;
            this.precioSombra = precioSombra;
            this.limiteInferior = inf;
            this.limiteSuperior = sup;
        }
    }

    public static class AnalisisVariable {
        public String nombre;
        public double costoReducido;
        public AnalisisVariable(String nombre, double costoReducido) {
            this.nombre = nombre;
            this.costoReducido = costoReducido;
        }
    }

    // --- MÉTODOS PRINCIPALES DEL ALGORITMO ---

    public SimplexResult resolver(double[][] matriz, double[] utilidades, List<String> operadores, List<String> nombres, List<String> nombresRecursos) {
        int numRestricciones = matriz.length;
        int numVarsOriginales = utilidades.length;
        
        int holguras = 0, excesos = 0, artificiales = 0;
        for (String op : operadores) {
            if (op.equals("<=")) holguras++;
            else if (op.equals(">=")) { excesos++; artificiales++; }
            else if (op.equals("=")) artificiales++;
        }

        int totalCols = numVarsOriginales + holguras + excesos + artificiales + 1;
        double[][] tablero = new double[numRestricciones + 1][totalCols];

        int colActualAdicional = numVarsOriginales;
        int colArtificialInicio = numVarsOriginales + holguras + excesos;
        int currentArt = 0;

        for (int i = 0; i < numRestricciones; i++) {
            for (int j = 0; j < numVarsOriginales; j++) tablero[i][j] = matriz[i][j];
            tablero[i][totalCols - 1] = matriz[i][matriz[i].length - 1];

            String op = operadores.get(i);
            if (op.equals("<=")) {
                tablero[i][colActualAdicional++] = 1.0;
            } else if (op.equals(">=")) {
                tablero[i][colActualAdicional++] = -1.0;
                tablero[i][colArtificialInicio + currentArt++] = 1.0;
            } else if (op.equals("=")) {
                tablero[i][colArtificialInicio + currentArt++] = 1.0;
            }
        }

        // Fase 1 si hay variables artificiales
        if (artificiales > 0) {
            double[] filaFase1 = new double[totalCols];
            for (int i = 0; i < numRestricciones; i++) {
                if (operadores.get(i).equals(">=") || operadores.get(i).equals("=")) {
                    for (int j = 0; j < totalCols; j++) filaFase1[j] -= tablero[i][j];
                }
            }
            ejecutarAlgoritmo(tablero, filaFase1, numRestricciones);
            if (Math.abs(filaFase1[totalCols - 1]) > 0.0001) {
                return new SimplexResult(tablero, 0, false, "El problema no tiene solución factible.");
            }
        }

        // Fase 2 (Optimización)
        double[] filaZ = new double[totalCols];
        for (int j = 0; j < numVarsOriginales; j++) filaZ[j] = -utilidades[j];
        
        for (int i = 0; i < numRestricciones; i++) {
            int colBasica = encontrarColumnaBasicaEnFila(tablero, i, totalCols);
            if (colBasica != -1 && colBasica < totalCols - 1) {
                double factor = filaZ[colBasica];
                for (int j = 0; j < totalCols; j++) filaZ[j] -= factor * tablero[i][j];
            }
        }

        ejecutarAlgoritmo(tablero, filaZ, numRestricciones);

        // Se pasa la matriz original para extraer los b_i y calcular rangos
        return construirResultado(tablero, filaZ, numRestricciones, numVarsOriginales, totalCols, nombres, nombresRecursos, matriz);
    }

    private void ejecutarAlgoritmo(double[][] tablero, double[] filaObjetivo, int numRest) {
        int totalCols = filaObjetivo.length;
        while (true) {
            int colPivote = -1;
            double min = -0.00001;
            for (int j = 0; j < totalCols - 1; j++) {
                if (filaObjetivo[j] < min) {
                    min = filaObjetivo[j];
                    colPivote = j;
                }
            }
            if (colPivote == -1) break;

            int filaPivote = -1;
            double razonMinima = Double.MAX_VALUE;
            for (int i = 0; i < numRest; i++) {
                if (tablero[i][colPivote] > 0.00001) {
                    double razon = tablero[i][totalCols - 1] / tablero[i][colPivote];
                    if (razon < razonMinima) {
                        razonMinima = razon;
                        filaPivote = i;
                    }
                }
            }
            if (filaPivote == -1) break;

            double pivotVal = tablero[filaPivote][colPivote];
            for (int j = 0; j < totalCols; j++) tablero[filaPivote][j] /= pivotVal;

            for (int i = 0; i < numRest; i++) {
                if (i != filaPivote) {
                    double factor = tablero[i][colPivote];
                    for (int j = 0; j < totalCols; j++) tablero[i][j] -= factor * tablero[filaPivote][j];
                }
            }
            double factorZ = filaObjetivo[colPivote];
            for (int j = 0; j < totalCols; j++) filaObjetivo[j] -= factorZ * tablero[filaPivote][j];
        }
    }

    private int encontrarColumnaBasicaEnFila(double[][] tablero, int fila, int totalCols) {
        for (int j = 0; j < totalCols - 1; j++) {
            if (Math.abs(tablero[fila][j] - 1.0) < 0.00001) {
                boolean esBasica = true;
                for (int i = 0; i < tablero.length - 1; i++) {
                    if (i != fila && Math.abs(tablero[i][j]) > 0.00001) {
                        esBasica = false;
                        break;
                    }
                }
                if (esBasica) return j;
            }
        }
        return -1;
    }

    // --- CONSTRUCCIÓN DEL ANÁLISIS POST-OPTIMAL ---

    private SimplexResult construirResultado(double[][] tablero, double[] filaZ, int numRest, int numVars, int totalCols, List<String> nombres, List<String> nombresRecursos, double[][] matrizOriginal) {
        double utilidad = filaZ[totalCols - 1];
        SimplexResult res = new SimplexResult(tablero, utilidad, true, "");
        
        StringBuilder detalle = new StringBuilder("Para obtener este beneficio, debes producir: ");

        // ANÁLISIS DE RECURSOS (Disponibilidad y Rangos)
        for (int j = 0; j < numRest; j++) {
            double valorActualRecurso = matrizOriginal[j][matrizOriginal[j].length - 1]; 
            double precioSombra = filaZ[numVars + j];
            
            double maxDecremento = Double.MAX_VALUE;
            double maxIncremento = Double.MAX_VALUE;

            // Uso de la matriz inversa B^-1 (ubicada en las columnas de las holguras)
            for (int i = 0; i < numRest; i++) {
                double bInvElemento = tablero[i][numVars + j]; 
                double ladoDerechoFinal = tablero[i][totalCols - 1]; 

                if (bInvElemento > 0.00001) {
                    maxDecremento = Math.min(maxDecremento, ladoDerechoFinal / bInvElemento);
                } else if (bInvElemento < -0.00001) {
                    maxIncremento = Math.min(maxIncremento, -ladoDerechoFinal / bInvElemento);
                }
            }

            double limiteInf = valorActualRecurso - (maxDecremento == Double.MAX_VALUE ? valorActualRecurso : maxDecremento);
            double limiteSup = (maxIncremento == Double.MAX_VALUE) ? Double.POSITIVE_INFINITY : valorActualRecurso + maxIncremento;

            res.analisisRecursos.add(new AnalisisRecurso(
                nombresRecursos.get(j), 
                Math.abs(precioSombra), 
                Math.max(0, limiteInf), 
                limiteSup
            ));
        }

        // ANÁLISIS DE VARIABLES (Producción y Costos Reducidos)
        for (int j = 0; j < numVars; j++) {
            res.analisisVariables.add(new AnalisisVariable(nombres.get(j), filaZ[j]));
            
            double valorProduccion = 0;
            // Identificar si la variable es básica en el tablero final
            for (int i = 0; i < numRest; i++) {
                if (Math.abs(tablero[i][j] - 1.0) < 0.00001) {
                    boolean unica = true;
                    for (int k = 0; k < numRest; k++) {
                        if (k != i && Math.abs(tablero[k][j]) > 0.00001) { unica = false; break; }
                    }
                    if (unica) valorProduccion = tablero[i][totalCols - 1];
                }
            }
            detalle.append(String.format("%s: %.2f unidades. ", nombres.get(j), Math.max(0, valorProduccion)));
        }

        res.mensaje = detalle.toString();
        return res;
    }
}