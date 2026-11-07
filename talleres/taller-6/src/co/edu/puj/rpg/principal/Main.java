package co.edu.puj.rpg.principal;

import co.edu.puj.rpg.modelo.Personaje;
import co.edu.puj.rpg.modelo.enemigo.Enemigo;

import java.util.NoSuchElementException;
import java.util.Scanner;

/** Interfaz por consola: menu principal con tienda, bucle de combate, formateo printf e historial con StringBuilder. */
public class Main {

    private static final int OPCION_TIENDA = 1;
    private static final int OPCION_COMBATIR = 2;
    private static final int OPCION_SALIR = 0;

    private static final int OPCION_ATACAR = 1;
    private static final int OPCION_POCION = 2;
    private static final int OPCION_ESTADO = 3;
    private static final int OPCION_HUIR = 4;

    private static final int CURACION_POCION = 30;
    private static final int CURACION_POCION_GRANDE = 60;
    private static final double COSTO_POCION_GRANDE = 20.0;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Personaje heroe = new Personaje("Aragorn", 100, 15, 50.0);
            System.out.println("=== SIMULADOR DE BATALLA / ARENA DE HEROES ===");
            int opcion;
            do {
                mostrarMenuPrincipal(heroe);
                opcion = leerOpcionEntre(scanner, OPCION_SALIR, OPCION_COMBATIR);
                if (opcion == OPCION_TIENDA) {
                    abrirTienda(scanner, heroe);
                } else if (opcion == OPCION_COMBATIR) {
                    combatir(scanner, heroe, new Enemigo("Goblin", 80, 12, 25.0));
                }
            } while (opcion != OPCION_SALIR);
            System.out.println("Hasta luego.");
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    // ---- menu principal

    private static void mostrarMenuPrincipal(Personaje heroe) {
        System.out.printf("%n%s | Nivel %d | HP %d/%d | Oro %.2f | Pociones: %d (grandes: %d)%n",
                heroe.getNombre(), heroe.getNivel(), heroe.getPuntosVida(),
                heroe.getVidaMaxima(), heroe.getOro(), heroe.getPociones(), heroe.getPocionesGrandes());
        System.out.println("1. Tienda  2. Combatir  0. Salir");
    }

    private static void abrirTienda(Scanner scanner, Personaje heroe) {
        System.out.println("\n=== TIENDA ===");
        System.out.printf("Oro disponible: %.2f%n", heroe.getOro());
        System.out.printf("Pocion Grande de Salud (+%d HP): %.2f de oro%n",
                CURACION_POCION_GRANDE, COSTO_POCION_GRANDE);
        System.out.print("Comprar? (1. Si / 2. No): ");
        if (scanner.hasNextInt() && scanner.nextInt() == 1) {
            scanner.nextLine();
            double nuevoSaldo = heroe.getOro() - COSTO_POCION_GRANDE;
            if (heroe.setOro(nuevoSaldo)) {
                heroe.agregarPocionGrande();
                System.out.printf("Compra exitosa. Oro: %.2f | Pociones grandes: %d%n",
                        heroe.getOro(), heroe.getPocionesGrandes());
            } else {
                System.out.println("Transaccion rechazada: saldo insuficiente.");
            }
        } else {
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            System.out.println("Venta cancelada.");
        }
    }

    // ---- combate

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
                    boolean critico = danoCausado >= heroe.getPuntosAtaque() * 2;
                    historial.append(String.format("Ronda %d: %s ataca a %s y causa %d de dano%s%n",
                            ronda, heroe.getNombre(), enemigo.getNombre(), danoCausado,
                            critico ? " (GOLPE CRITICO)" : ""));
                    if (!enemigo.isEstaVivo()) {
                        historial.append(String.format("Ronda %d: %s cae derrotado%n",
                                ronda, enemigo.getNombre()));
                    }
                    turnoConsumido = true;
                    break;
                }
                case OPCION_POCION: {
                    if (heroe.getPociones() == 0 && heroe.getPocionesGrandes() == 0) {
                        System.out.println("No tienes pociones.");
                        break;
                    }
                    int vidaAntes = heroe.getPuntosVida();
                    if (heroe.getPocionesGrandes() > 0) {
                        heroe.usarPocion(CURACION_POCION_GRANDE);
                        historial.append(String.format("Ronda %d: %s usa una pocion grande de salud (%d -> %d de vida)%n",
                                ronda, heroe.getNombre(), vidaAntes, heroe.getPuntosVida()));
                    } else {
                        heroe.usarPocion(CURACION_POCION);
                        historial.append(String.format("Ronda %d: %s usa una pocion (%d -> %d de vida)%n",
                                ronda, heroe.getNombre(), vidaAntes, heroe.getPuntosVida()));
                    }
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
            procesarVictoria(heroe, enemigo);
        } else if (!heroe.isEstaVivo()) {
            System.out.printf("%s ha caido ante %s%n", heroe.getNombre(), enemigo.getNombre());
        } else {
            System.out.printf("%s huye del combate%n", heroe.getNombre());
        }
    }

    private static void procesarVictoria(Personaje heroe, Enemigo enemigo) {
        System.out.printf("%s ha derrotado a %s%n", heroe.getNombre(), enemigo.getNombre());
        heroe.setOro(heroe.getOro() + enemigo.getOro());
        System.out.printf("Botin: +%.2f de oro (total: %.2f)%n", enemigo.getOro(), heroe.getOro());
        int nivelAntes = heroe.getNivel();
        int experienciaGanada = enemigo.getVidaMaxima() + enemigo.getPuntosAtaque() * 2;
        heroe.ganarExperiencia(experienciaGanada);
        System.out.printf("Experiencia: +%d (acumulada %d/%d)%n",
                experienciaGanada, heroe.getExperiencia(), heroe.getUmbralExperiencia());
        if (heroe.getNivel() > nivelAntes) {
            System.out.printf("%s sube de nivel: ahora es nivel %d%n", heroe.getNombre(), heroe.getNivel());
        }
    }

    // ---- lectura validada

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

    private static int leerOpcionEntre(Scanner scanner, int minimo, int maximo) {
        System.out.print("Opcion: ");
        int opcion = -1;
        if (scanner.hasNextInt()) {
            opcion = scanner.nextInt();
            scanner.nextLine();
            if (opcion < minimo || opcion > maximo) {
                System.out.println("Opcion fuera de rango.");
                opcion = -1;
            }
        } else {
            System.out.println("Entrada invalida: escribe un numero.");
            scanner.next();
        }
        return opcion;
    }

    // ---- salidas formateadas

    private static void mostrarEstadoCombate(Personaje heroe, Enemigo enemigo, int ronda) {
        System.out.printf("%n=== Ronda %d ===%n", ronda);
        System.out.printf("HEROE   %-10s NV:%d  HP:%3d/%3d  ATK:%2d  Oro:%7.2f%n",
                heroe.getNombre(), heroe.getNivel(), heroe.getPuntosVida(),
                heroe.getVidaMaxima(), heroe.getPuntosAtaque(), heroe.getOro());
        System.out.printf("ENEMIGO %-10s NV:%d  HP:%3d/%3d  ATK:%2d%n",
                enemigo.getNombre(), enemigo.getNivel(), enemigo.getPuntosVida(),
                enemigo.getVidaMaxima(), enemigo.getPuntosAtaque());
    }

    private static void mostrarEstadoDetalle(Personaje heroe, Enemigo enemigo) {
        System.out.println("--- Estado del heroe ---");
        System.out.printf("Nombre: %s | Nivel: %d | Vida: %d/%d | Ataque: %d | Oro: %.2f | Pociones: %d (grandes: %d) | Vivo: %s%n",
                heroe.getNombre(), heroe.getNivel(), heroe.getPuntosVida(),
                heroe.getVidaMaxima(), heroe.getPuntosAtaque(), heroe.getOro(),
                heroe.getPociones(), heroe.getPocionesGrandes(), heroe.isEstaVivo());
        System.out.println("--- Estado del enemigo ---");
        System.out.printf("Nombre: %s | Nivel: %d | Vida: %d/%d | Ataque: %d | Oro: %.2f | Vivo: %s%n",
                enemigo.getNombre(), enemigo.getNivel(), enemigo.getPuntosVida(),
                enemigo.getVidaMaxima(), enemigo.getPuntosAtaque(), enemigo.getOro(), enemigo.isEstaVivo());
    }
}
