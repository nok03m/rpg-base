package co.edu.puj.rpg.modelo;

/**
 * Arma equipable por un Personaje. En la asociacion unidireccional
 * Personaje -> Arma, esta clase desconoce por completo a su dueno:
 * no guarda ninguna referencia de vuelta.
 */
public class Arma {

    private String nombre;
    private int danioBase;

    public Arma(String nombre, int danioBase) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del arma no puede estar vacio");
        }
        if (danioBase <= 0) {
            throw new IllegalArgumentException("El dano base del arma debe ser positivo");
        }
        this.nombre = nombre;
        this.danioBase = danioBase;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDanioBase() {
        return danioBase;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append(" (dano base: ").append(danioBase).append(')');
        return sb.toString();
    }
}
