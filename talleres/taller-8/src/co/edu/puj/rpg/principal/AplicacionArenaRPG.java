package co.edu.puj.rpg.principal;

import co.edu.puj.rpg.modelo.Arma;
import co.edu.puj.rpg.modelo.Escuadron;
import co.edu.puj.rpg.modelo.Gremio;
import co.edu.puj.rpg.modelo.Personaje;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Reto integrador de la Arena de Heroes RPG: interfaz textual de consola
 * que gestiona personajes, gremios, escuadrones y armas aplicando las
 * relaciones entre clases (asociacion, agregacion y composicion).
 * Control de flujo con do-while + switch, reportes con System.out.printf
 * y construccion de textos con StringBuilder.
 */
public class AplicacionArenaRPG {

    private static final int OPCION_SALIR = 6;
    private static final int OPCION_INVALIDA = -1;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcion = 0;

        // Colecciones globales de la Arena.
        List<Personaje> personajes = new ArrayList<>();
        List<Gremio> gremios = new ArrayList<>();
        List<Escuadron> escuadrones = new ArrayList<>();

        do {
            System.out.println("\n=== MENÚ ARENA DE HÉROES RPG ===");
            System.out.println("1. Crear Personaje");
            System.out.println("2. Crear Gremio");
            System.out.println("3. Reclutar Personaje a Escuadrón");
            System.out.println("4. Equipar Arma a Personaje");
            System.out.println("5. Mostrar Estado Detallado (Personaje y Corazón RPG)");
            System.out.println("6. Salir");
            System.out.print("Seleccione una opción: ");
            opcion = leerOpcion(scanner);

            switch (opcion) {
                case 1:
                    crearPersonaje(scanner, personajes);
                    break;
                case 2:
                    crearGremio(scanner, gremios);
                    break;
                case 3:
                    reclutarAEscuadron(scanner, personajes, escuadrones);
                    break;
                case 4:
                    equiparArma(scanner, personajes);
                    break;
                case 5:
                    mostrarEstadoDetallado(personajes, gremios, escuadrones);
                    break;
                case 6:
                    System.out.println("Cerrando la Arena de Héroes...");
                    break;
                default:
                    System.out.println("Opción inválida.");
            }
        } while (opcion != OPCION_SALIR);

        scanner.close();
    }

    // ---- Opcion 1: crear Personaje

    private static void crearPersonaje(Scanner scanner, List<Personaje> personajes) {
        String nombre = leerTexto(scanner, "Nombre del personaje: ");
        int nivel = leerEntero(scanner, "Nivel del personaje: ");
        try {
            Personaje nuevo = new Personaje(nombre, nivel);
            personajes.add(nuevo);
            System.out.printf("Personaje '%s' (nivel %d) listo para la arena.%n", nombre, nivel);
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo crear el personaje: " + e.getMessage());
        }
    }

    // ---- Opcion 2: crear Gremio

    private static void crearGremio(Scanner scanner, List<Gremio> gremios) {
        String nombre = leerTexto(scanner, "Nombre del gremio: ");
        try {
            Gremio nuevo = new Gremio(nombre);
            gremios.add(nuevo);
            System.out.printf("Gremio '%s' fundado.%n", nombre);
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo crear el gremio: " + e.getMessage());
        }
    }

    // ---- Opcion 3: reclutar Personaje a Escuadron (Agregacion)

    private static void reclutarAEscuadron(Scanner scanner, List<Personaje> personajes,
                                           List<Escuadron> escuadrones) {
        Personaje reclutado = seleccionarPersonaje(scanner, personajes);
        if (reclutado == null) {
            return;
        }
        Escuadron escuadron = seleccionarOcrearEscuadron(scanner, escuadrones);
        if (escuadron == null) {
            return;
        }
        escuadron.reclutarPersonaje(reclutado);
    }

    // ---- Opcion 4: equipar Arma a Personaje (Asociacion Unidireccional)

    private static void equiparArma(Scanner scanner, List<Personaje> personajes) {
        Personaje portador = seleccionarPersonaje(scanner, personajes);
        if (portador == null) {
            return;
        }
        String nombreArma = leerTexto(scanner, "Nombre del arma: ");
        int danioBase = leerEntero(scanner, "Daño base del arma: ");
        try {
            Arma arma = new Arma(nombreArma, danioBase);
            portador.equiparArma(arma);
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo equipar el arma: " + e.getMessage());
        }
    }

    // ---- Opcion 5: estado detallado con printf y StringBuilder

    private static void mostrarEstadoDetallado(List<Personaje> personajes,
                                               List<Gremio> gremios,
                                               List<Escuadron> escuadrones) {
        System.out.println("\n=== ESTADO DETALLADO DE LA ARENA ===");

        System.out.println("--- Personajes y Corazón RPG ---");
        System.out.printf("%-15s %-6s %-8s %-18s %-15s %-11s %-10s%n",
                "Nombre", "Nivel", "Ataque", "Arma", "Gremio", "SaludBase", "ManaBase");
        if (personajes.isEmpty()) {
            System.out.println("(aún no hay personajes en la arena)");
        }
        for (Personaje p : personajes) {
            System.out.printf("%-15s %-6d %-8d %-18s %-15s %-11d %-10d%n",
                    p.getNombre(),
                    p.getNivel(),
                    p.calcularAtaqueTotal(),
                    p.getArmaEquipada() == null ? "desarmado" : p.getArmaEquipada().getNombre(),
                    p.getGremio() == null ? "ninguno" : p.getGremio().getNombre(),
                    p.getSaludBase(),
                    p.getManaBase());
        }

        System.out.println("--- Escuadrones (Agregación) ---");
        if (escuadrones.isEmpty()) {
            System.out.println("(aún no hay escuadrones formados)");
        }
        for (Escuadron e : escuadrones) {
            System.out.printf("%-18s %-10d %s%n", e.getNombre(), e.getCantidadMiembros(),
                    concatenarNombres(e.getMiembros()));
        }

        System.out.println("--- Gremios (Asociación Bidireccional) ---");
        if (gremios.isEmpty()) {
            System.out.println("(aún no hay gremios fundados)");
        }
        for (Gremio g : gremios) {
            System.out.printf("%-18s %-10d %s%n", g.getNombre(), g.getCantidadMiembros(),
                    concatenarNombres(g.getMiembros()));
        }
    }

    /** Concatena nombres simples con StringBuilder para los reportes de grupo. */
    private static String concatenarNombres(List<Personaje> miembros) {
        StringBuilder sb = new StringBuilder();
        if (miembros.isEmpty()) {
            sb.append("sin integrantes");
        } else {
            for (int i = 0; i < miembros.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(miembros.get(i).getNombre());
            }
        }
        return sb.toString();
    }

    // ---- Utilidades de seleccion y lectura

    private static Personaje seleccionarPersonaje(Scanner scanner, List<Personaje> personajes) {
        if (personajes.isEmpty()) {
            System.out.println("Primero cree un personaje (opción 1).");
            return null;
        }
        System.out.println("Personajes disponibles:");
        for (int i = 0; i < personajes.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, personajes.get(i).getNombre());
        }
        int indice = leerEntero(scanner, "Seleccione un personaje por número: ");
        if (indice < 1 || indice > personajes.size()) {
            System.out.println("Selección fuera de rango.");
            return null;
        }
        return personajes.get(indice - 1);
    }

    private static Escuadron seleccionarOcrearEscuadron(Scanner scanner, List<Escuadron> escuadrones) {
        System.out.println("Escuadrones disponibles:");
        System.out.println("0. Crear un escuadrón nuevo");
        for (int i = 0; i < escuadrones.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, escuadrones.get(i).getNombre());
        }
        int indice = leerEntero(scanner, "Seleccione un escuadrón por número: ");
        if (indice == 0) {
            String nombre = leerTexto(scanner, "Nombre del nuevo escuadrón: ");
            try {
                Escuadron nuevo = new Escuadron(nombre);
                escuadrones.add(nuevo);
                System.out.printf("Escuadrón '%s' formado.%n", nombre);
                return nuevo;
            } catch (IllegalArgumentException e) {
                System.out.println("No se pudo crear el escuadrón: " + e.getMessage());
                return null;
            }
        }
        if (indice < 1 || indice > escuadrones.size()) {
            System.out.println("Selección fuera de rango.");
            return null;
        }
        return escuadrones.get(indice - 1);
    }

    private static String leerTexto(Scanner scanner, String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    private static int leerEntero(Scanner scanner, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            if (scanner.hasNextInt()) {
                int valor = scanner.nextInt();
                scanner.nextLine();
                return valor;
            }
            System.out.println("Entrada inválida: ingrese un número entero.");
            scanner.nextLine();
        }
    }

    private static int leerOpcion(Scanner scanner) {
        try {
            int opcion = scanner.nextInt();
            scanner.nextLine();
            return opcion;
        } catch (InputMismatchException e) {
            scanner.nextLine();
            return OPCION_INVALIDA;
        }
    }
}
