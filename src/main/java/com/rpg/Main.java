package com.rpg;

import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Consola consola = new Consola(scanner);
            MotorJuego motor = new MotorJuego(consola);
            ejecutarMenu(consola, motor);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        } catch (IllegalStateException e) {
            // Scanner cerrado antes de tiempo, o torneo interrumpido: mensaje limpio en
            // vez de traza, y el juego termina de forma ordenada (el daemon es demonio).
            System.out.println("\nEl juego se cerro: " + e.getMessage());
        }
    }

    private static void ejecutarMenu(Consola consola, MotorJuego motor) {
        int opcion;
        do {
            mostrarMenu();
            // TALLER ARCHIVOS (secciones 3 a 5): el menu ahora llega hasta la opcion 7 y
            // delega en el motor las operaciones de archivos (ver log, cargar y borrar).
            opcion = consola.leerEntero("Opcion: ", 0, 7);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.batallaEnTablero();
                case 5 -> {
                    motor.informacionArchivos();
                    motor.verHistorial();
                }
                case 6 -> motor.cargarProgreso();
                case 7 -> borrarPartidaConConfirmacion(consola, motor);
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != 0);
    }

    /** Seccion 4: la partida se borra solo con confirmacion explicita del jugador. */
    private static void borrarPartidaConConfirmacion(Consola consola, MotorJuego motor) {
        if (!motor.hayPartidaGuardada()) {
            System.out.println("No hay ninguna partida guardada.");
            return;
        }
        String respuesta = consola.leerLinea("Seguro que quieres borrar la partida? (escribe SI): ");
        if (respuesta.equalsIgnoreCase("SI")) {
            if (motor.borrarPartidaGuardada()) {
                System.out.println("Partida borrada.");
            } else {
                System.out.println("No se pudo borrar la partida.");
            }
        } else {
            System.out.println("Borrado cancelado.");
        }
    }

    private static void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Batalla en tablero");
        System.out.println("5. Ver historial de combate (archivo)");
        System.out.println("6. Cargar partida guardada");
        System.out.println("7. Borrar partida guardada");
        System.out.println("0. Salir");
    }
}
