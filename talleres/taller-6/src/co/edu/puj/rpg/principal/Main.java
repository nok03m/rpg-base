package co.edu.puj.rpg.principal;

import co.edu.puj.rpg.modelo.Personaje;
import co.edu.puj.rpg.modelo.enemigo.Enemigo;

import java.util.Scanner;

/** Interfaz por consola: bucle de combate, menu switch, formateo printf e historial con StringBuilder. */
public class Main {

    private static final int OPCION_ATACAR = 1;
    private static final int OPCION_POCION = 2;
    private static final int OPCION_ESTADO = 3;
    private static final int OPCION_HUIR = 4;

    private static final int CURACION_POCION = 30;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Personaje heroe = new Personaje("Aragorn", 100, 15, 50.0);
            Enemigo enemigo = new Enemigo("Goblin", 80, 12, 25.0);
            System.out.println("=== SIMULADOR DE BATALLA / ARENA DE HEROES ===");
            combatir(scanner, heroe, enemigo);
        }
    }

    private static void combatir(Scanner scanner, Personaje heroe, Enemigo enemigo) {
        StringBuilder historial = new StringBuilder();
        int ronda = 1;
        boolean huir = false;
        do {
            mostrarEstadoCombate(heroe, enemigo, ronda);
            int opcion = leerOpcion(scanner);
            boolean turnoConsumido = false;
            switch (opcion) {
                case OPCION_ATACAR: {
                    int vidaAntes = enemigo.getPuntosVida();
                    heroe.atacar(enemigo);
                    int danoCausado = vidaAntes - enemigo.getPuntosVida();
                    historial.append(String.format("Ronda %d: %s ataca a %s y causa %d de dano%n",
                            ronda, heroe.getNombre(), enemigo.getNombre(), danoCausado));
                    if (!enemigo.isEstaVivo()) {
                        historial.append(String.format("Ronda %d: %s cae derrotado%n",
                                ronda, enemigo.getNombre()));
                    }
                    turnoConsumido = true;
                    break;
                }
                case OPCION_POCION: {
                    int vidaAntes = heroe.getPuntosVida();
                    heroe.usarPocion(CURACION_POCION);
                    historial.append(String.format("Ronda %d: %s usa una pocion (%d -> %d de vida)%n",
                            ronda, heroe.getNombre(), vidaAntes, heroe.getPuntosVida()));
                    turnoConsumido = true;
                    break;
                }
                case OPCION_ESTADO:
                    mostrarEstadoDetalle(heroe, enemigo);
                    break;
                case OPCION_HUIR:
                    huir = true;
                    break;
                default:
                    System.out.println("Opcion invalida: elige entre 1 y 4.");
            }
            if (huir) {
                break;
            }
            if (turnoConsumido && heroe.isEstaVivo() && enemigo.isEstaVivo()) {
                int vidaAntes = heroe.getPuntosVida();
                enemigo.atacar(heroe);
                int danoCausado = vidaAntes - heroe.getPuntosVida();
                historial.append(String.format("Ronda %d: %s contraataca a %s y causa %d de dano%n",
                        ronda, enemigo.getNombre(), heroe.getNombre(), danoCausado));
            }
            if (turnoConsumido) {
                System.out.println("--- Historial de la batalla ---");
                System.out.print(historial);
                ronda++;
            }
        } while (heroe.isEstaVivo() && enemigo.isEstaVivo());

        System.out.println("\n=== FIN DEL COMBATE ===");
        if (!enemigo.isEstaVivo()) {
            System.out.printf("%s ha derrotado a %s%n", heroe.getNombre(), enemigo.getNombre());
        } else if (!heroe.isEstaVivo()) {
            System.out.printf("%s ha caido ante %s%n", heroe.getNombre(), enemigo.getNombre());
        } else {
            System.out.printf("%s huye del combate%n", heroe.getNombre());
        }
    }

    /** Lee la opcion del menu validando la entrada con if-else, sin interrumpir el programa. */
    private static int leerOpcion(Scanner scanner) {
        System.out.println("1. Atacar  2. Usar pocion  3. Ver estado  4. Huir");
        System.out.print("Opcion: ");
        int opcion = -1;
        if (scanner.hasNextInt()) {
            opcion = scanner.nextInt();
            scanner.nextLine();
            if (opcion < OPCION_ATACAR || opcion > OPCION_HUIR) {
                System.out.println("Opcion fuera de rango.");
                opcion = -1;
            }
        } else {
            System.out.println("Entrada invalida: escribe un numero.");
            scanner.next();
        }
        return opcion;
    }

    private static void mostrarEstadoCombate(Personaje heroe, Enemigo enemigo, int ronda) {
        System.out.printf("%n=== Ronda %d ===%n", ronda);
        System.out.printf("HEROE  %-10s NV:%d  HP:%3d/%3d  ATK:%2d  Oro:%7.2f%n",
                heroe.getNombre(), heroe.getNivel(), heroe.getPuntosVida(),
                heroe.getVidaMaxima(), heroe.getPuntosAtaque(), heroe.getOro());
        System.out.printf("ENEMIGO %-10s NV:%d  HP:%3d/%3d  ATK:%2d%n",
                enemigo.getNombre(), enemigo.getNivel(), enemigo.getPuntosVida(),
                enemigo.getVidaMaxima(), enemigo.getPuntosAtaque());
    }

    private static void mostrarEstadoDetalle(Personaje heroe, Enemigo enemigo) {
        System.out.println("--- Estado del heroe ---");
        System.out.printf("Nombre: %s | Nivel: %d | Vida: %d/%d | Ataque: %d | Oro: %.2f | Vivo: %s%n",
                heroe.getNombre(), heroe.getNivel(), heroe.getPuntosVida(),
                heroe.getVidaMaxima(), heroe.getPuntosAtaque(), heroe.getOro(), heroe.isEstaVivo());
        System.out.println("--- Estado del enemigo ---");
        System.out.printf("Nombre: %s | Nivel: %d | Vida: %d/%d | Ataque: %d | Oro: %.2f | Vivo: %s%n",
                enemigo.getNombre(), enemigo.getNivel(), enemigo.getPuntosVida(),
                enemigo.getVidaMaxima(), enemigo.getPuntosAtaque(), enemigo.getOro(), enemigo.isEstaVivo());
    }
}
