package com.equipo.proyecto_io.services;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SimplexService {

    // Resultado de la resolución del método Simplex.
    public static class SimplexResult {
        public double[][] tablaFinal;   // Tabla final del método Simplex.
        public double utilidadOptima;   // Valor óptimo de la función objetivo.
        public boolean esFactible;      // Indica si la solución es factible.
        public String mensaje;          // Mensaje explicativo para el usuario.

        public SimplexResult(double[][] tabla, double utilidad, boolean factible, String msg) {
            this.tablaFinal = tabla;
            this.utilidadOptima = utilidad;
            this.esFactible = factible;
            this.mensaje = msg;
        }
    }

    /**
     * Resuelve un problema de programación lineal usando el método de las dos fases.
     * @param matriz Coeficientes de las restricciones y el lado derecho en la última columna.
     * @param utilidades Coeficientes de la función objetivo.
     * @param operadores Lista de operadores para cada restricción (<=, >=, =).
     * @param nombres Nombres de las variables originales.
     */
    public SimplexResult resolver(double[][] matriz, double[] utilidades, List<String> operadores, List<String> nombres) {
        int numRestricciones = matriz.length;
        int numVarsOriginales = utilidades.length;
        
        int holguras = 0, excesos = 0, artificiales = 0;
        for (String op : operadores) {
            if (op.equals("<=")) holguras++;
            else if (op.equals(">=")) { excesos++; artificiales++; }
            else if (op.equals("=")) artificiales++;
        }

        int totalCols = numVarsOriginales + holguras + excesos + artificiales + 1;
        // El tablero contiene las restricciones y la fila objetivo final.
        // Última columna = Lado Derecho (LD).
        double[][] tablero = new double[numRestricciones + 1][totalCols];

        int colActualAdicional = numVarsOriginales;
        int colArtificialInicio = numVarsOriginales + holguras + excesos;
        int currentArt = 0;

        for (int i = 0; i < numRestricciones; i++) {
            // Copiar los coeficientes de las variables originales.
            for (int j = 0; j < numVarsOriginales; j++) {
                tablero[i][j] = matriz[i][j];
            }
            
            // Guardar el valor del lado derecho de la restricción.
            tablero[i][totalCols - 1] = matriz[i][matriz[i].length - 1];

            // Añadir la variable de holgura, exceso o artificial según el operador.
            String op = operadores.get(i);
            if (op.equals("<=")) {
                tablero[i][colActualAdicional++] = 1.0; // Holgura
            } else if (op.equals(">=")) {
                tablero[i][colActualAdicional++] = -1.0; // Exceso
                tablero[i][colArtificialInicio + currentArt++] = 1.0; // Artificial
            } else if (op.equals("=")) {
                tablero[i][colArtificialInicio + currentArt++] = 1.0; // Artificial
            }
        }

        // --- FASE 1: eliminar artificiales ---
        if (artificiales > 0) {
            double[] filaFase1 = new double[totalCols];
            // La fila objetivo de fase 1 busca minimizar la suma de artificiales.
            // Para eso se resta cada fila con variables artificiales de la fila objetivo.
            for (int i = 0; i < numRestricciones; i++) {
                if (operadores.get(i).equals(">=") || operadores.get(i).equals("=")) {
                    for (int j = 0; j < totalCols; j++) {
                        filaFase1[j] -= tablero[i][j];
                    }
                }
            }
            
            ejecutarAlgoritmo(tablero, filaFase1, numRestricciones);
            
            // Si el valor óptimo de fase 1 no es 0, no existe solución factible.
            if (Math.abs(filaFase1[totalCols - 1]) > 0.0001) {
                return new SimplexResult(tablero, 0, false, "El problema no tiene solución factible (restricciones contradictorias)");
            }
        }

        // --- FASE 2: optimizar la utilidad original ---
        double[] filaZ = new double[totalCols];
        for (int j = 0; j < numVarsOriginales; j++) {
            filaZ[j] = -utilidades[j];
        }
        
        // Ajustar Z para que las variables básicas actuales tengan coeficiente cero.
        for (int i = 0; i < numRestricciones; i++) {
            int colBasica = encontrarColumnaBasicaEnFila(tablero, i, totalCols);
            if (colBasica != -1 && colBasica < totalCols - 1) {
                double factor = filaZ[colBasica];
                for (int j = 0; j < totalCols; j++) {
                    filaZ[j] -= factor * tablero[i][j];
                }
            }
        }

        ejecutarAlgoritmo(tablero, filaZ, numRestricciones);

        return construirResultado(tablero, filaZ, numRestricciones, numVarsOriginales, totalCols, nombres);
    }

    /**
     * Ejecuta las iteraciones del Simplex con la tabla y la fila objetivo dadas.
     */
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

            // Si no hay coeficientes negativos, la solución es óptima.
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

            // Si no existe fila pivote, el problema no está acotado.
            if (filaPivote == -1) break;

            // Normalizar la fila pivote.
            double pivotVal = tablero[filaPivote][colPivote];
            for (int j = 0; j < totalCols; j++) tablero[filaPivote][j] /= pivotVal;

            // Eliminar la variable pivote de las demás filas.
            for (int i = 0; i < numRest; i++) {
                if (i != filaPivote) {
                    double factor = tablero[i][colPivote];
                    for (int j = 0; j < totalCols; j++) tablero[i][j] -= factor * tablero[filaPivote][j];
                }
            }

            // Actualizar la fila objetivo.
            double factorZ = filaObjetivo[colPivote];
            for (int j = 0; j < totalCols; j++) filaObjetivo[j] -= factorZ * tablero[filaPivote][j];
        }
    }

    /**
     * Identifica si una fila de la tabla corresponde a una variable básica.
     * Retorna la columna de la variable básica o -1 si no existe.
     */
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

    /**
     * Construye el resultado final con la utilidad óptima y las variables básicas.
     */
    private SimplexResult construirResultado(double[][] tablero, double[] filaZ, int numRest, int numVars, int totalCols, List<String> nombres) {
        double utilidad = filaZ[totalCols - 1];
        StringBuilder detalle = new StringBuilder("Para obtener este beneficio, debes producir: ");
        
        for (int j = 0; j < numVars; j++) {
            double valor = 0;
            for (int i = 0; i < numRest; i++) {
                if (Math.abs(tablero[i][j] - 1.0) < 0.00001) {
                    boolean unica = true;
                    for (int k = 0; k < numRest; k++) {
                        if (k != i && Math.abs(tablero[k][j]) > 0.00001) { unica = false; break; }
                    }
                    if (unica) valor = tablero[i][totalCols - 1];
                }
            }
            detalle.append(String.format("%s: %.2f unidades. ", nombres.get(j), Math.max(0, valor)));
        }

        return new SimplexResult(tablero, utilidad, true, detalle.toString());
    }
}