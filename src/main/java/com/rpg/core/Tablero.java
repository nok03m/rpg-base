package com.rpg.core;

/**
 * Terreno de combate 5x5 modelado con una matriz bidimensional de caracteres.
 * La coordenada x es la columna y la coordenada y es la fila.
 */
public class Tablero {

    public static final int TAMANO = 5;
    public static final char CELDA_VACIA = '.';

    private final char[][] celdas = new char[TAMANO][TAMANO];

    public Tablero() {
        for (int i = 0; i < TAMANO; i++) {
            for (int j = 0; j < TAMANO; j++) {
                celdas[i][j] = CELDA_VACIA;
            }
        }
    }

    /** @return true si la coordenada cae dentro de los limites de la matriz */
    public boolean esPosicionValida(int x, int y) {
        return x >= 0 && x < TAMANO && y >= 0 && y < TAMANO;
    }

    public char obtener(int x, int y) {
        verificarPosicion(x, y);
        return celdas[y][x];
    }

    public void colocar(int x, int y, char simbolo) {
        verificarPosicion(x, y);
        celdas[y][x] = simbolo;
    }

    /** Deja la celda vacia de nuevo. */
    public void limpiar(int x, int y) {
        colocar(x, y, CELDA_VACIA);
    }

    private void verificarPosicion(int x, int y) {
        if (!esPosicionValida(x, y)) {
            throw new IllegalArgumentException("Posicion fuera del tablero: (" + x + ", " + y + ")");
        }
    }

    /** Renderiza cualquier matriz con bucles anidados, una fila por linea. */
    public void mostrarTablero(char[][] matriz) {
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                System.out.print("[" + matriz[i][j] + "]");
            }
            System.out.println();
        }
    }

    public void mostrar() {
        mostrarTablero(celdas);
    }
}
