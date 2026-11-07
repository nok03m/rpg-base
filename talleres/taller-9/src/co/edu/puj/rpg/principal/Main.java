package co.edu.puj.rpg.principal;

import co.edu.puj.rpg.engine.Bestiario;
import co.edu.puj.rpg.exception.CapacidadExcedidaException;
import co.edu.puj.rpg.exception.SinManaException;
import co.edu.puj.rpg.modelo.PerfilJugador;
import co.edu.puj.rpg.modelo.Personaje;
import co.edu.puj.rpg.modelo.enums.TipoPersonaje;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Punto de entrada de la Arena RPG. Arquitectura por capas: la capa de
 * dominio (Personaje, Bestiario) evalua las reglas de negocio y arroja
 * las excepciones correspondientes; esta capa de interfaz las captura con
 * bloques catch especificos y muestra e.getMessage() sin interrumpir el
 * bucle principal del menu. La lectura numerica esta blindada con
 * try-catch-finally frente a InputMismatchException.
 */
public class Main {

    private static final int OPCION_SALIR = 6;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Personaje> personajes = new ArrayList<>();
        PerfilJugador perfil = new PerfilJugador();
        Bestiario bestiario = new Bestiario();
        int opcion;

        do {
            System.out.println("\n=== ARENA DE HEROES RPG ===");
            System.out.println("1. Crear Personaje");
            System.out.println("2. Asignar Habilidad Unica");
            System.out.println("3. Registrar Victoria en Bestiario");
            System.out.println("4. Ejecutar Ataque Especial");
            System.out.println("5. Consultar Bestiario y Logros");
            System.out.println("6. Salir");
            opcion = leerEntero(scanner, "Seleccione una opcion: ");

            switch (opcion) {
                case 1:
                    crearPersonaje(scanner, personajes);
                    break;
                case 2:
                    asignarHabilidad(scanner, personajes);
                    break;
                case 3:
                    registrarVictoria(scanner, bestiario, perfil);
                    break;
                case 4:
                    ejecutarAtaqueEspecial(scanner, personajes);
                    break;
                case 5:
                    consultarBestiarioYLogros(bestiario, perfil, personajes);
                    break;
                case 6:
                    System.out.println("Cerrando la Arena de Heroes...");
                    break;
                default:
                    System.out.println("Opcion invalida.");
            }
        } while (opcion != OPCION_SALIR);

        scanner.close();
    }

    // ---- Opcion 1: crear Personaje (nombre + TipoPersonaje del enum)

    private static void crearPersonaje(Scanner scanner, List<Personaje> personajes) {
        String nombre = leerTexto(scanner, "Nombre del personaje: ");
        TipoPersonaje tipo = seleccionarTipo(scanner);
        if (tipo == null) {
            return;
        }
        try {
            Personaje nuevo = new Personaje(nombre, tipo);
            personajes.add(nuevo);
            System.out.printf("Personaje '%s' (%s) creado con estado inicial VIVO: "
                            + "vida %d, mana %d, ataque total %.1f, defensa total %.1f%n",
                    nombre, tipo, nuevo.getVida(), nuevo.getMana(),
                    nuevo.getAtaqueTotal(), nuevo.getDefensaTotal());
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo crear el personaje: " + e.getMessage());
        }
    }

    private static TipoPersonaje seleccionarTipo(Scanner scanner) {
        TipoPersonaje[] tipos = TipoPersonaje.values();
        System.out.println("Tipos disponibles:");
        for (int i = 0; i < tipos.length; i++) {
            System.out.printf("%d. %s (ataque +%.1f, defensa +%.1f)%n",
                    i + 1, tipos[i],
                    tipos[i].getBonificadorAtaqueBase(), tipos[i].getBonificadorDefensaBase());
        }
        int indice = leerEntero(scanner, "Seleccione el tipo por numero: ");
        if (indice < 1 || indice > tipos.length) {
            System.out.println("Seleccion fuera de rango.");
            return null;
        }
        return tipos[indice - 1];
    }

    // ---- Opcion 2: asignar Habilidad Unica (HashSet, rechazo de duplicado)

    private static void asignarHabilidad(Scanner scanner, List<Personaje> personajes) {
        Personaje personaje = seleccionarPersonaje(scanner, personajes);
        if (personaje == null) {
            return;
        }
        String habilidad = leerTexto(scanner, "Nombre de la habilidad: ");
        try {
            if (personaje.aprenderHabilidad(habilidad)) {
                System.out.println("Habilidad aprendida: " + habilidad);
            } else {
                System.out.println("La habilidad '" + habilidad
                        + "' ya estaba aprendida (duplicado rechazado).");
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo aprender la habilidad: " + e.getMessage());
        }
    }

    // ---- Opcion 3: registrar Victoria en Bestiario (HashMap + logro automatico)

    private static void registrarVictoria(Scanner scanner, Bestiario bestiario, PerfilJugador perfil) {
        String enemigo = leerTexto(scanner, "Nombre del enemigo derrotado: ");
        boolean primeraVictoria = bestiario.registrarVictoria(enemigo);
        System.out.printf("Victoria registrada: %s acumula %d derrota(s).%n",
                enemigo, bestiario.consultarBajas(enemigo));
        if (primeraVictoria) {
            perfil.registrarLogro("Cazador de " + enemigo);
            System.out.println("Logro desbloqueado: Cazador de " + enemigo);
        }
    }

    // ---- Opcion 4: ejecutar Ataque Especial (manejo de SinManaException)

    private static void ejecutarAtaqueEspecial(Scanner scanner, List<Personaje> personajes) {
        Personaje personaje = seleccionarPersonaje(scanner, personajes);
        if (personaje == null) {
            return;
        }
        int costoMana = leerEntero(scanner, "Costo de mana del ataque especial: ");
        try {
            System.out.println(personaje.lanzarHechizo(costoMana));
        } catch (SinManaException e) {
            System.out.println("El ataque fallo: " + e.getMessage());
        }
    }

    // ---- Opcion 5: consultar Bestiario (entrySet) y Logros (TreeSet)

    private static void consultarBestiarioYLogros(Bestiario bestiario, PerfilJugador perfil,
                                                   List<Personaje> personajes) {
        System.out.println("\n--- Bestiario (enemigos derrotados) ---");
        System.out.print(bestiario.generarReporte());

        System.out.println("--- Logros desbloqueados (orden alfabetico) ---");
        if (perfil.getCantidadLogros() == 0) {
            System.out.println("(aun no hay logros desbloqueados)");
        } else {
            for (String logro : perfil.listarLogrosOrdenados()) {
                System.out.println("- " + logro);
            }
        }

        System.out.println("--- Personajes y habilidades ---");
        System.out.printf("%-12s %-10s %-11s %-6s %-6s %s%n",
                "Nombre", "Tipo", "Estado", "Vida", "Mana", "Habilidades");
        if (personajes.isEmpty()) {
            System.out.println("(aun no hay personajes en la arena)");
        }
        for (Personaje p : personajes) {
            System.out.printf("%-12s %-10s %-11s %-6d %-6d %s%n",
                    p.getNombre(), p.getTipoPersonaje(), p.getEstado(),
                    p.getVida(), p.getMana(),
                    p.obtenerHabilidades().isEmpty()
                            ? "ninguna" : String.join(", ", p.obtenerHabilidades()));
        }
    }

    // ---- utilidades de seleccion

    private static Personaje seleccionarPersonaje(Scanner scanner, List<Personaje> personajes) {
        if (personajes.isEmpty()) {
            System.out.println("Primero cree un personaje (opcion 1).");
            return null;
        }
        System.out.println("Personajes disponibles:");
        for (int i = 0; i < personajes.size(); i++) {
            System.out.printf("%d. %s (%s)%n", i + 1,
                    personajes.get(i).getNombre(), personajes.get(i).getTipoPersonaje());
        }
        int indice = leerEntero(scanner, "Seleccione un personaje por numero: ");
        if (indice < 1 || indice > personajes.size()) {
            System.out.println("Seleccion fuera de rango.");
            return null;
        }
        return personajes.get(indice - 1);
    }

    // ---- lectura blindada con try-catch-finally

    /**
     * Lee un entero reintentando ante entradas no numericas. El bloque
     * finally limpia el buffer del Scanner tanto en el exito (salto de linea
     * pendiente) como en el fallo (token invalido), confirmando el ciclo de
     * lectura sin interrumpir el programa.
     */
    private static int leerEntero(Scanner scanner, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            boolean leido = false;
            int valor = 0;
            try {
                valor = scanner.nextInt();
                leido = true;
            } catch (InputMismatchException e) {
                System.err.println("Entrada invalida: se esperaba un numero entero. Intente de nuevo.");
            } finally {
                // Limpieza del buffer: descarta el token invalido o el salto
                // de linea pendiente en ambos casos.
                if (scanner.hasNextLine()) {
                    scanner.nextLine();
                }
            }
            if (leido) {
                return valor;
            }
        }
    }

    private static String leerTexto(Scanner scanner, String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }
}
