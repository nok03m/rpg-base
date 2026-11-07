package com.rpg.core;

import com.rpg.entities.Enemigo;
import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.exceptions.SinManaException;
import com.rpg.exceptions.VidaInsuficienteException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.util.ArrayList;
import java.util.List;

/**
 * Mision integradora: un escuadron de heroes enfrenta a un grupo de enemigos
 * en un tablero 5x5 hasta eliminarlos o alcanzar el cofre del centro.
 */
public class BatallaTablero {

    private static final int COFRE_X = 2;
    private static final int COFRE_Y = 2;
    private static final char SIMBOLO_COFRE = 'C';
    private static final char SIMBOLO_ENEMIGO = 'E';
    private static final int FUERZA_ESCUDO_THORIN = 10;
    private static final int MANA_GANDALF = 60;
    private static final int CAPACIDAD_PROVISIONES = 5;
    private static final int POCIONES_INICIALES = 3;
    private static final int CURACION_POCION = 30;

    private final Consola consola;
    private final RegistroCombate registro;

    public BatallaTablero(Consola consola, RegistroCombate registro) {
        this.consola = consola;
        this.registro = registro;
    }

    public void jugar() {
        Tablero tablero = inicializarTerreno();
        Escuadron heroes = inicializarHeroes();
        List<Personaje> enemigos = inicializarEnemigos();
        Inventario<Pocion> provisiones = inicializarProvisiones();
        Personaje heroePrincipal = heroes.obtener(0);
        posicionar(tablero, heroes, enemigos);

        registro.agregar("%n=== MISION: EL COFRE DEL CENTRO ===");
        registro.agregar("Elimina a los enemigos o lleva a %s hasta el cofre [C].", heroePrincipal.getNombre());

        boolean victoria = false;
        boolean derrota = false;
        int ronda = 1;
        do {
            registro.agregar("%n--- Ronda %d ---%n", ronda);
            procesarInicioDeRonda(heroes, enemigos);
            renderizar(tablero, heroes, enemigos);
            boolean misionCumplida = turnoJugadores(heroes, enemigos, provisiones, tablero, heroePrincipal);
            if (!misionCumplida) {
                turnoEnemigos(heroes, enemigos);
            }
            limpiarBajas(heroes, enemigos, tablero);
            victoria = enemigos.isEmpty() || alcanzaCofre(heroePrincipal);
            derrota = heroes.estaVacio() || heroes.totalVida() <= 0;
            ronda++;
        } while (!victoria && !derrota);

        renderizar(tablero, heroes, enemigos);
        if (victoria) {
            registro.agregar("%n¡VICTORIA! El escuadron %s cumple la mision.", heroes.getNombre());
        } else {
            registro.agregar("%nDERROTA. El escuadron %s ha caido en combate.", heroes.getNombre());
        }
    }

    // ---- inicializacion

    private Tablero inicializarTerreno() {
        Tablero tablero = new Tablero();
        tablero.colocar(COFRE_X, COFRE_Y, SIMBOLO_COFRE);
        return tablero;
    }

    private Escuadron inicializarHeroes() {
        Escuadron heroes = new Escuadron("Guardianes del Alba");
        heroes.agregar(new Guerrero("Thorin", FUERZA_ESCUDO_THORIN, 0, 0));
        heroes.agregar(new Mago("Gandalf", MANA_GANDALF, 1, 1));
        return heroes;
    }

    private List<Personaje> inicializarEnemigos() {
        List<Personaje> enemigos = new ArrayList<>();
        Enemigo goblin = new Enemigo("Goblin Saqueador", 55, 9, 2);
        goblin.moverA(0, 4);
        Enemigo orco = new Enemigo("Orco Berserker", 70, 12, 5);
        orco.moverA(4, 0);
        Enemigo nigromante = new Enemigo("Nigromante", 60, 11, 3);
        nigromante.moverA(4, 4);
        enemigos.add(goblin);
        enemigos.add(orco);
        enemigos.add(nigromante);
        return enemigos;
    }

    private Inventario<Pocion> inicializarProvisiones() {
        Inventario<Pocion> provisiones = new Inventario<>(CAPACIDAD_PROVISIONES);
        try {
            for (int i = 0; i < POCIONES_INICIALES; i++) {
                provisiones.agregar(new Pocion("Pocion de Vida", CURACION_POCION, RaridadItem.COMUN));
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudieron cargar las provisiones: " + e.getMessage());
        }
        return provisiones;
    }

    private void posicionar(Tablero tablero, Escuadron heroes, List<Personaje> enemigos) {
        for (Personaje heroe : heroes) {
            tablero.colocar(heroe.getPosicionX(), heroe.getPosicionY(), simboloDe(heroe));
        }
        for (Personaje enemigo : enemigos) {
            tablero.colocar(enemigo.getPosicionX(), enemigo.getPosicionY(), SIMBOLO_ENEMIGO);
        }
    }

    private char simboloDe(Personaje personaje) {
        if (personaje.getTipo() == TipoPersonaje.GUERRERO) {
            return 'G';
        }
        if (personaje.getTipo() == TipoPersonaje.MAGO) {
            return 'M';
        }
        return SIMBOLO_ENEMIGO;
    }

    // ---- fases de la ronda

    private void procesarInicioDeRonda(Escuadron heroes, List<Personaje> enemigos) {
        for (Personaje heroe : heroes) {
            heroe.alIniciarTurno();
            registrarEfectos(heroe);
        }
        for (Personaje enemigo : enemigos) {
            enemigo.alIniciarTurno();
            registrarEfectos(enemigo);
        }
    }

    private void registrarEfectos(Personaje personaje) {
        for (String mensaje : personaje.procesarEfectos()) {
            registro.agregar(mensaje);
        }
    }

    private void renderizar(Tablero tablero, Escuadron heroes, List<Personaje> enemigos) {
        tablero.mostrar();
        System.out.println("Leyenda: [G] Guerrero  [M] Mago  [E] Enemigo  [C] Cofre  [.] Vacia");
        for (Personaje heroe : heroes) {
            String mana = heroe instanceof Mago mago ? "  Mana: " + mago.getMana() : "";
            System.out.printf("  %s%s%n", FormatoCombate.resumen(heroe), mana);
        }
        for (Personaje enemigo : enemigos) {
            System.out.printf("  %s%n", FormatoCombate.resumen(enemigo));
        }
    }

    /** @return true si la mision termino durante la fase del jugador */
    private boolean turnoJugadores(Escuadron heroes, List<Personaje> enemigos,
                                   Inventario<Pocion> provisiones, Tablero tablero, Personaje heroePrincipal) {
        for (Personaje heroe : heroes) {
            if (!heroe.estaVivo()) {
                continue;
            }
            registro.agregar("%nTurno de %s", heroe.getNombre());
            turnoDeHeroe(heroe, enemigos, provisiones, tablero);
            if (enemigos.isEmpty() || alcanzaCofre(heroePrincipal)) {
                return true;
            }
        }
        return false;
    }

    private void turnoDeHeroe(Personaje heroe, List<Personaje> enemigos,
                              Inventario<Pocion> provisiones, Tablero tablero) {
        boolean turnoGastado;
        do {
            System.out.println("1. Atacar  2. Usar inventario  3. Mover en tablero");
            int opcion = consola.leerEntero("Accion: ", 1, 3);
            turnoGastado = switch (opcion) {
                case 1 -> atacarEnemigo(heroe, enemigos);
                case 2 -> usarProvision(heroe, provisiones);
                default -> moverHeroe(heroe, tablero);
            };
        } while (!turnoGastado);
    }

    private boolean atacarEnemigo(Personaje heroe, List<Personaje> enemigos) {
        List<Personaje> vivos = new ArrayList<>();
        for (Personaje enemigo : enemigos) {
            if (enemigo.estaVivo()) {
                vivos.add(enemigo);
            }
        }
        if (vivos.isEmpty()) {
            System.out.println("No quedan enemigos.");
            return false;
        }
        for (int i = 0; i < vivos.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, FormatoCombate.resumen(vivos.get(i)));
        }
        int eleccion = consola.leerEntero("Enemigo a atacar: ", 1, vivos.size());
        return ejecutarEspecial(heroe, vivos.get(eleccion - 1));
    }

    private boolean ejecutarEspecial(Personaje atacante, Personaje objetivo) {
        try {
            registro.agregar(atacante.realizarAtaqueEspecial(objetivo));
            registro.agregar("   %s", FormatoCombate.resumen(objetivo));
            return true;
        } catch (SinManaException | VidaInsuficienteException e) {
            System.out.println(atacante.getNombre() + " no puede usar su habilidad: " + e.getMessage());
            return false;
        }
    }

    private boolean usarProvision(Personaje heroe, Inventario<Pocion> provisiones) {
        if (provisiones.estaVacio()) {
            System.out.println("No quedan provisiones.");
            return false;
        }
        List<Pocion> pociones = provisiones.getItems();
        for (int i = 0; i < pociones.size(); i++) {
            System.out.printf("  %d. %s (+%d de vida)%n", i + 1, pociones.get(i).getNombre(),
                    pociones.get(i).getCuracionReal());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, provisiones.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(heroe.usarPocion(provisiones.quitar(eleccion - 1)));
        return true;
    }

    private boolean moverHeroe(Personaje heroe, Tablero tablero) {
        System.out.println("1. Norte  2. Sur  3. Este  4. Oeste  0. Cancelar");
        int direccion = consola.leerEntero("Direccion: ", 0, 4);
        if (direccion == 0) {
            return false;
        }
        int x = heroe.getPosicionX();
        int y = heroe.getPosicionY();
        int nuevoX = x;
        int nuevoY = y;
        switch (direccion) {
            case 1 -> nuevoY--;
            case 2 -> nuevoY++;
            case 3 -> nuevoX++;
            default -> nuevoX--;
        }
        if (!tablero.esPosicionValida(nuevoX, nuevoY)) {
            System.out.println("No puedes salir del tablero.");
            return false;
        }
        char destino = tablero.obtener(nuevoX, nuevoY);
        if (destino != Tablero.CELDA_VACIA && destino != SIMBOLO_COFRE) {
            System.out.println("Esa celda esta ocupada.");
            return false;
        }
        liberarCelda(tablero, x, y);
        heroe.moverA(nuevoX, nuevoY);
        tablero.colocar(nuevoX, nuevoY, simboloDe(heroe));
        registro.agregar("%s se mueve a la posicion (%d, %d).", heroe.getNombre(), nuevoX, nuevoY);
        tablero.mostrar();
        return true;
    }

    private void turnoEnemigos(Escuadron heroes, List<Personaje> enemigos) {
        registro.agregar("%n--- Turno de los enemigos ---");
        for (Personaje enemigo : enemigos) {
            if (!enemigo.estaVivo()) {
                continue;
            }
            Personaje objetivo = heroeMasCercano(heroes, enemigo);
            if (objetivo == null) {
                return;
            }
            ejecutarEspecial(enemigo, objetivo);
        }
    }

    /** IA enemiga: ataca al heroe vivo mas cercano (distancia Manhattan). */
    private Personaje heroeMasCercano(Escuadron heroes, Personaje enemigo) {
        Personaje masCercano = null;
        int distanciaMinima = Integer.MAX_VALUE;
        for (Personaje heroe : heroes) {
            if (!heroe.estaVivo()) {
                continue;
            }
            int distancia = Math.abs(heroe.getPosicionX() - enemigo.getPosicionX())
                    + Math.abs(heroe.getPosicionY() - enemigo.getPosicionY());
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercano = heroe;
            }
        }
        return masCercano;
    }

    private void limpiarBajas(Escuadron heroes, List<Personaje> enemigos, Tablero tablero) {
        for (Personaje heroe : heroes) {
            if (!heroe.estaVivo()) {
                liberarCelda(tablero, heroe.getPosicionX(), heroe.getPosicionY());
                registro.agregar("%s ha caido.", heroe.getNombre());
            }
        }
        heroes.removerCaidos();
        for (Personaje enemigo : enemigos) {
            if (!enemigo.estaVivo()) {
                liberarCelda(tablero, enemigo.getPosicionX(), enemigo.getPosicionY());
                registro.agregar("%s ha sido eliminado.", enemigo.getNombre());
            }
        }
        enemigos.removeIf(personaje -> !personaje.estaVivo());
    }

    /** Vacia una celda y vuelve a pintar el cofre si estaba debajo. */
    private void liberarCelda(Tablero tablero, int x, int y) {
        tablero.limpiar(x, y);
        if (x == COFRE_X && y == COFRE_Y) {
            tablero.colocar(x, y, SIMBOLO_COFRE);
        }
    }

    private boolean alcanzaCofre(Personaje heroePrincipal) {
        return heroePrincipal.estaVivo()
                && heroePrincipal.getPosicionX() == COFRE_X
                && heroePrincipal.getPosicionY() == COFRE_Y;
    }
}
