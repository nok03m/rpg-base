package com.rpg.core;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorEfectos;
import com.rpg.concurrency.GestorTurno;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.EstadoPartida;
import com.rpg.persistencia.GestorArchivos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    /** Segundos que tiene el jugador para elegir su accion (temporizador de turno). */
    private static final int SEGUNDOS_LIMITE_TURNO = 15;
    /** Intervalo del auto-guardado demonio en segundo plano. */
    private static final long INTERVALO_AUTOGUARDADO_MS = 15000;
    /** Intervalo entre ticks de los efectos DoT/HoT en hilos. */
    private static final long INTERVALO_TICK_EFECTO_MS = 1500;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final PerfilJugador perfil = new PerfilJugador();
    private final Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final BatallaTablero batallaTablero;
    private final GestorArchivos gestorArchivos = new GestorArchivos();
    // TALLER ARCHIVOS (seccion 5): ultimoGuardado sigue siendo la copia en memoria del estado;
    // ahora guardarProgreso ademas la vuelca a disco (log .txt) y serializa la partida (.dat).
    private String ultimoGuardado = "";

    public MotorJuego(Consola consola) {
        this(consola, INTERVALO_AUTOGUARDADO_MS);
    }

    /** Constructor con intervalo configurable: usado por las pruebas para acelerar el daemon. */
    public MotorJuego(Consola consola, long intervaloAutoguardadoMs) {
        this.consola = consola;
        this.batallaTablero = new BatallaTablero(consola, registro);
        // TALLER ARCHIVOS (seccion 1): carpeta partidas/ y registro_combate.txt listos antes de jugar.
        gestorArchivos.prepararCarpeta();
        // TALLER CONCURRENCIA (seccion 4): hilo demonio de auto-guardado. setDaemon(true) se
        // llama dentro del constructor, ANTES de start(); la JVM no espera a los demonios al
        // cerrar, asi que la aplicacion sale de inmediato aunque este bucle while (true) siga vivo.
        new AutoGuardadoDaemon(this, intervaloAutoguardadoMs).start();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        // TALLER CONCURRENCIA (seccion 2): durante el combate, cada efecto DoT/HoT corre en
        // su propio hilo. El oyente conecta el dominio (aplicar/quitar efecto) con los hilos.
        GestorEfectos gestorEfectos = new GestorEfectos(INTERVALO_TICK_EFECTO_MS, registro);
        jugador.setOyenteEfectos(gestorEfectos);
        enemigo.setOyenteEfectos(gestorEfectos);
        try {
            boolean huyo = combatir(jugador, enemigo);
            cerrarCombate(jugador, enemigo, huyo);
        } finally {
            // Al terminar el combate se interrumpen todos los hilos de efectos que queden
            // (veneno, quemadura, regeneracion...). El finally garantiza que no queden
            // hilos sueltos ni huya el jugador, ni gane, ni caiga.
            gestorEfectos.detenerTodo();
            jugador.setOyenteEfectos(null);
            enemigo.setOyenteEfectos(null);
        }
        guardarProgreso();
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        guardarProgreso();
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    public void batallaEnTablero() {
        registro.limpiar();
        batallaTablero.jugar();
        guardarProgreso();
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                case TURNO_PERDIDO -> {
                    // TALLER CONCURRENCIA (seccion 1): el temporizador vencio; el turno se pierde.
                    registro.agregar("%s pierde el turno por tiempo agotado", jugador.getNombre());
                    yield true;
                }
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /**
     * Pide la accion al jugador con un temporizador de turno en su propio hilo.
     * TALLER CONCURRENCIA (seccion 1): la lectura por teclado sigue bloqueando (un Scanner
     * sobre System.in NO es interrumpible con interrupt(): limitacion real de este disenio),
     * pero si el tiempo se agota el callback lo marca y, cuando el jugador escribe algo,
     * esa respuesta se descarta y el turno se pierde. Si responde a tiempo, el temporizador
     * se cancela con interrupt() antes de procesar su eleccion.
     */
    private Accion pedirAccion() {
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");
        GestorTurno temporizador = new GestorTurno(SEGUNDOS_LIMITE_TURNO, () ->
                System.out.printf("%n[Tiempo] Se agotaron los %d segundos del turno...%n", SEGUNDOS_LIMITE_TURNO));
        temporizador.iniciar();
        int opcion = consola.leerEntero("Accion: ", 0, 4);
        temporizador.cancelar();
        if (temporizador.expiroTiempo()) {
            return Accion.TURNO_PERDIDO;
        }
        return switch (opcion) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d turnos)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    /** Guarda el progreso en memoria con mensaje de confirmacion (llamada manual). */
    public void guardarProgreso() {
        guardarProgreso(true);
    }

    /**
     * Copia las estadisticas y el historial del combate a la memoria ({@code ultimoGuardado});
     * aun no escribe ningun archivo.
     * TALLER CONCURRENCIA (seccion 4): ahora lo invocan DOS hilos (el principal al terminar
     * una pelea y el daemon AutoGuardadoDaemon cada cierto intervalo). synchronized evita
     * que ambos armen el texto a la vez y que getUltimoGuardado lea una copia a medio armar.
     * @param anunciar si false, no imprime confirmacion (lo usa el daemon)
     */
    public synchronized void guardarProgreso(boolean anunciar) {
        // TALLER ARCHIVOS (secciones 2 y 5): el texto se vuelca al log .txt (con append: se
        // conservan los combates anteriores) y el estado del juego se serializa en partida.dat.
        // La escritura a disco pasa por GestorArchivos, cuyos metodos van synchronized: el
        // demonio de auto-guardado y este hilo nunca escriben el archivo a la vez (seccion 7).
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();
        gestorArchivos.escribirLog(ultimoGuardado, true);
        gestorArchivos.guardarPartida(new EstadoPartida(perfil, gremio, bestiario.derrotadosSnapshot()));
        if (anunciar) {
            System.out.println("[Progreso guardado en disco]");
        }
    }

    public synchronized String getUltimoGuardado() {
        return ultimoGuardado;
    }

    // ---- archivos (TALLER ARCHIVOS)

    /** Seccion 1: muestra ruta relativa vs absoluta, existencia y tamanio de los archivos. */
    public void informacionArchivos() {
        gestorArchivos.informacionArchivos();
    }

    /** Seccion 3: lee el log de combate desde el .txt y lo muestra por consola. */
    public void verHistorial() {
        String historial = gestorArchivos.leerLog();
        if (historial == null || historial.isBlank()) {
            System.out.println("(El historial esta vacio)");
            return;
        }
        System.out.println("\n=== HISTORIAL LEIDO DEL ARCHIVO ===");
        System.out.print(historial);
    }

    /**
     * Seccion 5: carga la partida del .dat y restaura el estado del juego. Los Personaje
     * cargados conservan su referencia al Gremio serializado; al adoptarlos con
     * agregarMiembro, la relacion bidireccional se repara contra el gremio vigente.
     */
    public void cargarProgreso() {
        EstadoPartida guardada = gestorArchivos.cargarPartida();
        if (guardada == null) {
            return;
        }
        boolean cambioPerfil = perfil.restaurar(guardada.getPerfil().getVictorias(),
                guardada.getPerfil().getDerrotas(), guardada.getPerfil().getLogros());
        // Se REEMPLAZA el roster por el guardado: primero se vacia el gremio actual
        // (removerMiembro repara la asociacion inversa) y luego se adoptan los personajes
        // cargados. Si solo se agregara, cargar dos veces la misma partida duplicaria
        // miembros por identidad (contains usa ==, no equals).
        for (Personaje miembro : new ArrayList<>(gremio.getMiembros())) {
            gremio.removerMiembro(miembro);
        }
        // Copia ANTES de iterar: agregarMiembro repara la asociacion bidireccional y puede
        // llamar a unirseAGremio, que remueve al personaje del gremio CARGADO. Modificar esa
        // lista mientras se recorre lanzaria ConcurrentModificationException.
        List<Personaje> miembrosCargados = new ArrayList<>(guardada.getGremio().getMiembros());
        for (Personaje miembro : miembrosCargados) {
            gremio.agregarMiembro(miembro);
        }
        bestiario.restaurarDerrotados(guardada.getEnemigosDerrotados());
        System.out.printf("Partida cargada: %d victorias, %d derrotas, gremio %s con %d miembros.%n",
                perfil.getVictorias(), perfil.getDerrotas(), gremio.getNombre(), gremio.getMiembros().size());
        if (!cambioPerfil) {
            System.out.println("(El perfil cargado era identico al actual)");
        }
    }

    /** Seccion 4: borra la partida guardada (File.delete + Files.deleteIfExists) y reporta. */
    public boolean borrarPartidaGuardada() {
        return gestorArchivos.borrarPartida();
    }

    /** Usada por Main para pedir confirmacion solo si hay algo que borrar. */
    public boolean hayPartidaGuardada() {
        return gestorArchivos.existePartidaGuardada();
    }
}
