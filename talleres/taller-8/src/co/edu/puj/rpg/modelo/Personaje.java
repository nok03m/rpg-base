package co.edu.puj.rpg.modelo;

/**
 * Heroe de la Arena RPG. Modela dos tipos de relaciones con sus vecinas:
 *  - Asociacion unidireccional (0..1) con Arma: el arma no conoce al dueno.
 *  - Composicion (1..1) con CorazonRPG: instanciado en el constructor y
 *    jamas reemplazable desde fuera.
 */
public class Personaje {

    private static final int SALUD_BASE_INICIAL = 100;
    private static final int MANA_BASE_INICIAL = 50;
    private static final int ATAQUE_POR_NIVEL = 5;

    private String nombre;
    private int nivel;
    private Arma armaEquipada;          // Asociacion unidireccional (0..1)
    private final CorazonRPG corazon;   // Composicion (1..1)

    public Personaje(String nombre, int nivel) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del personaje no puede estar vacio");
        }
        if (nivel <= 0) {
            throw new IllegalArgumentException("El nivel debe ser positivo");
        }
        this.nombre = nombre;
        this.nivel = nivel;
        this.armaEquipada = null; // Inicialmente desarmado
        // Composicion: el corazon nace y muere junto al personaje.
        this.corazon = new CorazonRPG(SALUD_BASE_INICIAL, MANA_BASE_INICIAL);
    }

    // ---- Asociacion unidireccional: Personaje -> Arma

    public void equiparArma(Arma nuevaArma) {
        if (nuevaArma == null) {
            System.out.println("No se puede equipar un arma inexistente.");
            return;
        }
        this.armaEquipada = nuevaArma;
        System.out.printf("%s equipa %s (dano base: %d).%n",
                this.nombre, nuevaArma.getNombre(), nuevaArma.getDanioBase());
    }

    public void desequiparArma() {
        if (this.armaEquipada != null) {
            System.out.printf("%s guarda su %s y combate desarmado.%n",
                    this.nombre, this.armaEquipada.getNombre());
            this.armaEquipada = null;
        } else {
            System.out.println(this.nombre + " no tiene arma para retirar.");
        }
    }

    public int calcularAtaqueTotal() {
        int ataqueBase = this.nivel * ATAQUE_POR_NIVEL;
        // Control obligatorio de referencia nula antes de acceder al arma.
        if (this.armaEquipada != null) {
            ataqueBase += this.armaEquipada.getDanioBase();
        }
        return ataqueBase;
    }

    // ---- Acceso al estado interno

    public String getNombre() {
        return nombre;
    }

    public int getNivel() {
        return nivel;
    }

    public Arma getArmaEquipada() {
        return armaEquipada;
    }

    /**
     * Mediacion de la composicion: el CorazonRPG nunca se expone hacia
     * fuera; solo se consultan sus atributos vitales a traves del personaje.
     */
    public int getSaludBase() {
        return this.corazon.getSaludBase();
    }

    public int getManaBase() {
        return this.corazon.getManaBase();
    }

    public void fortalecerCorazon(int saludExtra, int manaExtra) {
        this.corazon.fortalecer(saludExtra, manaExtra);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre)
          .append(" [nivel: ").append(nivel)
          .append(", ataque total: ").append(calcularAtaqueTotal());
        if (armaEquipada != null) {
            sb.append(", arma: ").append(armaEquipada.getNombre());
        } else {
            sb.append(", arma: desarmado");
        }
        sb.append(", corazon: ").append(this.corazon)
          .append(']');
        return sb.toString();
    }
}
