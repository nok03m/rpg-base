package com.rpg.entities;

import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TALLER ARCHIVOS (seccion 5): Personaje ahora ES Serializable, asi que la partida se puede
// guardar con ObjectOutputStream en un .dat. Todos los objetos alcanzables desde aqui tambien
// lo son (Arma, Gremio, Inventario, Pocion, EstadoEfecto; los enums lo son por definicion en Java).
// El ciclo Personaje <-> Gremio no es problema: la serializacion sigue las referencias una sola
// vez y al cargar recupera exactamente la misma red de objetos.
public abstract class Personaje implements ICombatiente, Serializable {

    // serialVersionUID: numero de version de la clase. Si al deserializar la clase cambio tanto
    // que el UID del archivo y el de la clase no coinciden, Java lanza InvalidClassException en
    // lugar de reconstruir un objeto a medio compatible: falla temprano y de forma clara.

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;

    @Serial
    private static final long serialVersionUID = 1L;

    protected String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    protected int vida;
    protected final int ataqueBase;
    private final int defensaBase;
    protected int posicionX;
    protected int posicionY;
    // TALLER CONCURRENCIA: volatile porque el estado se escribe desde varios candados
    // (el de vida y el monitor de efectos) y se lee desde cualquier hilo; volatile
    // garantiza que todos vean el ultimo valor escrito.
    private volatile EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;                  // asociacion 0..1
    private Gremio gremio;              // asociacion bidireccional 0..1
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); // composicion
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();

    // TALLER CONCURRENCIA (seccion 6): vida y estado los escriben varios hilos a la vez
    // (hilo principal, hilos de efectos DoT/HoT, torneo en paralelo). Se protegen con un
    // unico candado para que nunca queden inconsistentes entre si (por ejemplo, un
    // aturdimiento que revive a un MUERTO).
    //
    // ReentrantLock (investigacion obligatoria) en lugar de synchronized para la vida:
    // lock()/unlock() van SIEMPRE en try-finally; si unlock() no estuviera en el finally,
    // una excepcion dejaria el candado tomado para siempre y el juego se congelaria.
    // Cuando preferir ReentrantLock a synchronized (2-3 lineas): cuando necesites funciones
    // que synchronized no ofrece, como tryLock() con tiempo maximo de espera, lockInterruptibly()
    // (abortar la espera con interrupt()), fairness configurable o varias Condition. Para secciones
    // criticas simples como esta, synchronized seria igual de correcto; aca se usa ReentrantLock
    // para demostrar el mecanismo alternativo que pide la investigacion.
    private final ReentrantLock candadoVida = new ReentrantLock();

    // TALLER CONCURRENCIA (seccion 2): cuando el dominio aplica o quita un efecto, avisa a
    // este oyente para que el efecto corra en su propio hilo (o se cancele con interrupt()).
    // El Personaje no conoce hilos: solo esta interfaz. Puede ser null (juego secuencial).
    //
    // TALLER ARCHIVOS (seccion 5): transient. El oyente apunta a objetos vivos de hilos
    // (GestorEfectos y sus EfectoContinuoTask); si se intentara guardar con el oyente puesto,
    // ObjectOutputStream lanzaria NotSerializableException por esa rama del grafo. Un oyente
    // tampoco tiene sentido en disco: al cargar vuelve con su valor por defecto (null) y el
    // personaje queda en modo secuencial hasta que un combate nuevo lo conecte otra vez.
    private transient volatile OyenteEfectos oyenteEfectos;

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE, 0, 0);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        this(nombre, tipo, vidaMaxima, ataqueBase, defensaBase, 0, 0);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase,
                         int posicionX, int posicionY) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        validarPosicion(posicionX, posicionY);
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    private static void validarPosicion(int posicionX, int posicionY) {
        if (posicionX < 0 || posicionY < 0) {
            throw new IllegalArgumentException("La posicion no puede ser negativa");
        }
    }

    /** Cambia las coordenadas del personaje dentro del terreno de combate. */
    public void moverA(int posicionX, int posicionY) {
        validarPosicion(posicionX, posicionY);
        this.posicionX = posicionX;
        this.posicionY = posicionY;
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida

    /**
     * Reduce la vida del personaje. Antes de la version concurrente este metodo leia la vida,
     * calculaba y escribia en pasos separados SIN proteccion: con dos hilos (un veneno y un
     * ataque) ambos podian leer el mismo valor y uno de los dos danios se PERDIA
     * (condicion de carrera perdida de actualizacion). Hoy todo el leer-calcular-escribir
     * ocurre dentro del candado: las operaciones son atomicas entre si.
     */
    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        candadoVida.lock();
        try {
            setVida(vida - cantidad);
        } finally {
            // unlock() SIEMPRE en finally: sin esto, una excepcion dejaria el candado tomado.
            candadoVida.unlock();
        }
    }

    /** @return vida realmente recuperada */
    public int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        candadoVida.lock();
        try {
            if (estado == EstadoPersonaje.MUERTO) {
                return 0;
            }
            int vidaAntes = vida;
            setVida(vida + cantidad);
            return vida - vidaAntes;
        } finally {
            candadoVida.unlock();
        }
    }

    public void restaurarVida() {
        // El oyente (GestorEfectos) debe haber detenido los hilos de efectos antes de
        // restaurar (MotorJuego y TorneoManager llaman detenerTodo() al cerrar un combate).
        synchronized (this) {
            efectos.clear();
        }
        candadoVida.lock();
        try {
            estado = EstadoPersonaje.VIVO;
            vida = vidaMaxima;
        } finally {
            candadoVida.unlock();
        }
    }

    @Override
    public boolean estaVivo() {
        candadoVida.lock();
        try {
            return estado != EstadoPersonaje.MUERTO;
        } finally {
            candadoVida.unlock();
        }
    }

    /** Registra quien debe crear/interrumpir los hilos de efectos (o null para nada). */
    public void setOyenteEfectos(OyenteEfectos oyente) {
        this.oyenteEfectos = oyente;
    }

    /** TALLER ARCHIVOS: expone el oyente para comprobar en la prueba que transient vuelve como null. */
    public OyenteEfectos getOyenteEfectos() {
        return oyenteEfectos;
    }

    // ---- efectos DoT / HoT (hoy se procesan por turno, de forma secuencial)

    /**
     * Guarda el efecto y avisa al oyente para que arranque su hilo DoT/HoT
     * (EfectoContinuoTask). Si no hay oyente, el efecto queda registrado pero sin
     * ticks (modo secuencial).
     */
    public void aplicarEfecto(EstadoEfecto efecto) {
        EstadoEfecto copia = efecto.copia();
        OyenteEfectos oyente = oyenteEfectos;   // lectura unica: estable durante toda la operacion
        synchronized (this) {
            efectos.put(copia.getNombre(), copia);
            if (efecto.esDanio() && estado == EstadoPersonaje.VIVO) {
                estado = EstadoPersonaje.ENVENENADO;
            }
        }
        if (oyente != null) {
            // Fuera del monitor: el oyente arranca un hilo nuevo; no queremos candados
            // cruzados entre el Personaje y el GestorEfectos (riesgo de deadlock).
            oyente.efectoAplicado(this, copia);
        }
    }

    public boolean quitarEfecto(String nombreEfecto) {
        OyenteEfectos oyente;
        boolean quitado;
        synchronized (this) {
            quitado = efectos.remove(nombreEfecto) != null;
            if (quitado) {
                normalizarEstado();
            }
        }
        if (quitado) {
            oyente = oyenteEfectos;
            if (oyente != null) {
                // Fuera del monitor: el oyente interrumpe el hilo del efecto.
                oyente.efectoQuitado(this, nombreEfecto);
            }
        }
        return quitado;
    }

    /** Antidoto / purificacion: quita todos los DoT e interrumpe sus hilos. */
    public void quitarEfectosDanio() {
        List<String> nombresDanio;
        synchronized (this) {
            nombresDanio = new ArrayList<>();
            for (Map.Entry<String, EstadoEfecto> entrada : efectos.entrySet()) {
                if (entrada.getValue().esDanio()) {
                    nombresDanio.add(entrada.getKey());
                }
            }
        }
        for (String nombreEfecto : nombresDanio) {
            quitarEfecto(nombreEfecto);
        }
    }

    /**
     * REEMPLAZADO POR HILOS: antes este metodo aplicaba un tick de cada efecto turno a
     * turno desde Arena.iniciarTurno / BatallaTablero. Desde el taller de concurrencia
     * cada efecto tiene su propio hilo (EfectoContinuoTask) que aplica ticks cada cierto
     * intervalo y se cancela con interrupt(). Aqui ya no se procesa nada por turno.
     */

    private synchronized boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    private synchronized void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public void aturdir() {
        candadoVida.lock();
        try {
            if (estado != EstadoPersonaje.MUERTO) {
                estado = EstadoPersonaje.ATURDIDO;
            }
        } finally {
            candadoVida.unlock();
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public boolean consumirAturdimiento() {
        candadoVida.lock();
        try {
            boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
            if (estabaAturdido) {
                estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
            }
            return estabaAturdido;
        } finally {
            candadoVida.unlock();
        }
    }

    // ---- inventario, arma, gremio, habilidades

    public String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    /**
     * Fija la vida acotandola a 0..vidaMaxima y actualiza el estado. Reentrante:
     * si ya se esta dentro de recibirDanio/curar (mismo hilo con el candado tomado),
     * el lock() adicional no se bloquea a si mismo.
     */
    public void setVida(int nuevaVida) {
        candadoVida.lock();
        try {
            vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
            if (vida == 0) {
                estado = EstadoPersonaje.MUERTO;
            }
        } finally {
            candadoVida.unlock();
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public int getVida() {
        candadoVida.lock();
        try {
            return vida;
        } finally {
            candadoVida.unlock();
        }
    }

    public int getPosicionX() {
        return posicionX;
    }

    public int getPosicionY() {
        return posicionY;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public EstadoPersonaje getEstado() {
        return estado;
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    /** Copia instantanea de los efectos: segura de leer mientras los hilos los cambian. */
    public Collection<EstadoEfecto> getEfectos() {
        synchronized (this) {
            return new ArrayList<>(efectos.values());
        }
    }
}
