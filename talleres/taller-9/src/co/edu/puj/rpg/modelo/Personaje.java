package co.edu.puj.rpg.modelo;

import co.edu.puj.rpg.exception.CapacidadExcedidaException;
import co.edu.puj.rpg.exception.SinManaException;
import co.edu.puj.rpg.exception.VidaInsuficienteException;
import co.edu.puj.rpg.modelo.enums.EstadoPersonaje;
import co.edu.puj.rpg.modelo.enums.TipoPersonaje;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Heroe de la Arena RPG. El tipo y el estado del personaje dejan de ser
 * cadenas sueltas o banderas booleanas: ahora son propiedades fuertemente
 * tipadas (TipoPersonaje / EstadoPersonaje) y los calculos de atributos
 * consumen los bonificadores encapsulados dentro del enum asignado.
 */
public class Personaje {

    private static final int VIDA_BASE = 100;
    private static final int MANA_BASE = 50;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_LIBRO_HABILIDADES = 5;

    private String nombre;
    private final TipoPersonaje tipoPersonaje;
    private EstadoPersonaje estado;
    private int vidaMaxima;
    private int vida;
    private int manaMaximo;
    private int mana;
    private final Set<String> habilidades = new HashSet<>();

    public Personaje(String nombre, TipoPersonaje tipoPersonaje) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (tipoPersonaje == null) {
            throw new IllegalArgumentException("El tipo de personaje es obligatorio");
        }
        this.nombre = nombre;
        this.tipoPersonaje = tipoPersonaje;
        this.estado = EstadoPersonaje.VIVO;
        this.vidaMaxima = VIDA_BASE;
        this.vida = VIDA_BASE;
        this.manaMaximo = MANA_BASE;
        this.mana = MANA_BASE;
    }

    // ---- calculo de atributos con los bonificadores del enum

    public double getAtaqueTotal() {
        return ATAQUE_BASE + this.tipoPersonaje.getBonificadorAtaqueBase();
    }

    public double getDefensaTotal() {
        return DEFENSA_BASE + this.tipoPersonaje.getBonificadorDefensaBase();
    }

    // ---- libro de habilidades (HashSet: unicidad sin orden)

    /**
     * Intenta agregar la habilidad al conjunto. Retorna true si fue agregada
     * con exito y false si ya existia (rechazo del duplicado, sin alterar la
     * coleccion). Si el libro alcanzo su capacidad maxima lanza
     * CapacidadExcedidaException.
     */
    public boolean aprenderHabilidad(String habilidad) throws CapacidadExcedidaException {
        if (habilidad == null || habilidad.isBlank()) {
            throw new IllegalArgumentException("La habilidad no puede estar vacia");
        }
        if (this.habilidades.size() >= CAPACIDAD_LIBRO_HABILIDADES) {
            throw new CapacidadExcedidaException(
                    "El libro de habilidades de " + this.nombre, CAPACIDAD_LIBRO_HABILIDADES);
        }
        return this.habilidades.add(habilidad);
    }

    /** @return vista no modificable del conjunto de habilidades aprendidas */
    public Set<String> obtenerHabilidades() {
        return Collections.unmodifiableSet(this.habilidades);
    }

    // ---- reglas de disparo de excepciones de dominio

    /**
     * Lanza un hechizo consumiendo mana. Regla de negocio: si el costo de
     * mana supera el mana actual se lanza SinManaException (verificada).
     */
    public String lanzarHechizo(int costoMana) throws SinManaException {
        if (costoMana < 0) {
            throw new IllegalArgumentException("El costo de mana no puede ser negativo");
        }
        if (costoMana > this.mana) {
            throw new SinManaException(costoMana, this.mana);
        }
        this.mana -= costoMana;
        return String.format("%s ejecuta su ataque especial (-%d de mana, le quedan %d)",
                this.nombre, costoMana, this.mana);
    }

    /**
     * Accion de costo sobre la vida propia. Regla de negocio: si el costo es
     * mayor o igual a la vida actual se lanza VidaInsuficienteException
     * (no verificada).
     */
    public void consumirVida(int costoVida) {
        if (costoVida < 0) {
            throw new IllegalArgumentException("El costo de vida no puede ser negativo");
        }
        if (costoVida >= this.vida) {
            throw new VidaInsuficienteException(this.nombre, costoVida, this.vida);
        }
        this.vida -= costoVida;
    }

    /**
     * Dano de combate: el estado reemplaza a las banderas de vida/muerte.
     * Al llegar a cero el personaje pasa a EstadoPersonaje.DERROTADO.
     */
    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El dano no puede ser negativo");
        }
        this.vida = Math.max(0, this.vida - cantidad);
        if (this.vida == 0) {
            this.estado = EstadoPersonaje.DERROTADO;
        }
    }

    public boolean estaVivo() {
        return this.estado != EstadoPersonaje.DERROTADO;
    }

    // ---- getters

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipoPersonaje() {
        return tipoPersonaje;
    }

    public EstadoPersonaje getEstado() {
        return estado;
    }

    public int getVida() {
        return vida;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public int getMana() {
        return mana;
    }

    public int getManaMaximo() {
        return manaMaximo;
    }
}
