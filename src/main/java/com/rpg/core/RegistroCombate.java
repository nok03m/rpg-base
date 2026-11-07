package com.rpg.core;

/**
 * Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida.
 * TALLER CONCURRENCIA: desde que las semifinales del torneo y los efectos DoT/HoT corren
 * en hilos propios, VARIOS hilos agregan lineas a la vez. StringBuilder no es seguro entre
 * hilos (podria corromper el texto o perder lineas), por eso todos los metodos van
 * sincronizados: dos hilos pueden escribir, pero nunca al mismo tiempo.
 */
public class RegistroCombate {

    private final StringBuilder texto = new StringBuilder();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        texto.append(linea).append(System.lineSeparator());
    }

    public synchronized void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    // TALLER ARCHIVOS (secciones 2 y 3): el volcado del registro a .txt vive en
    // com.rpg.persistencia.GestorArchivos (escribirLog con BufferedWriter + FileWriter y
    // leerLog con BufferedReader + FileReader). El registro solo expone su texto.
    public synchronized String getTexto() {
        return texto.toString();
    }
}
