import com.rpg.entities.Arma;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.OyenteEfectos;
import com.rpg.entities.Personaje;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.EstadoPartida;
import com.rpg.persistencia.GestorArchivos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Prueba automatica del taller de ARCHIVOS (secciones 1 a 6 + investigacion).
 * Se ejecuta desde la carpeta rpg-base para que las rutas relativas de
 * GestorArchivos (partidas/...) apunten ahi mismo.
 */
public class PruebaArchivos {

    private static int pruebasOk;

    public static void main(String[] args) throws IOException {
        GestorArchivos gestor = new GestorArchivos();
        limpiarEstadoPrevio();

        // ---- Seccion 1: clase File
        gestor.prepararCarpeta();
        File carpeta = new File(GestorArchivos.CARPETA_PARTIDAS);
        File log = new File(GestorArchivos.ARCHIVO_LOG);
        verificar("mkdirs creo la carpeta partidas/", carpeta.isDirectory());
        verificar("createNewFile dejo el registro vacio listo", log.exists() && log.length() == 0);
        // Portable: getAbsolutePath() usa el separador del SO (\ en Windows, / en Linux).
        // Se normaliza a / antes de comparar para que la asercion valga en ambos sistemas.
        verificar("getAbsolutePath resuelve la ruta relativa (portable, separadores del SO)",
                log.getAbsolutePath().replace('\\', '/').endsWith("partidas/registro_combate.txt"));

        // ---- Seccion 2: escritura con BufferedWriter + FileWriter
        String bloque1 = "=== COMBATE 1 ===\nHeroe vs Goblin\nVictoria!";
        verificar("escritura (sobrescribe) devolvio true", gestor.escribirLog(bloque1, false));
        String leido = gestor.leerLog();
        verificar("leerLog devuelve el bloque escrito", leido != null && leido.contains("Victoria!"));

        String bloque2 = "=== COMBATE 2 ===\nHeroe vs Ogro\nDerrota";
        verificar("escritura en modo append devolvio true", gestor.escribirLog(bloque2, true));
        String leido2 = gestor.leerLog();
        verificar("append conserva el combate anterior y agrega el nuevo",
                leido2 != null && leido2.contains("COMBATE 1") && leido2.contains("COMBATE 2"));

        // ---- Seccion 3: lectura con BufferedReader ( FileNotFoundException capturada )
        gestor.borrarLog();
        verificar("leerLog con archivo inexistente devuelve null sin caer", gestor.leerLog() == null);

        // ---- Seccion 5: serializacion de la partida
        PerfilJugador perfil = new PerfilJugador();
        perfil.registrarVictoria();
        perfil.registrarVictoria();
        perfil.registrarVictoria();
        perfil.registrarDerrota();
        perfil.desbloquearLogro("Primera Victoria");
        perfil.desbloquearLogro("Intocable");

        Gremio gremio = new Gremio("Guardianes del Alba");
        Guerrero heroe = new Guerrero("Aquilés".replace("é", "e"));
        Mago maga = new Mago("Merlina");
        gremio.agregarMiembro(heroe);
        gremio.agregarMiembro(maga);

        // transient: un oyente vivo (no serializable) NO debe romper el guardado ni viajar a disco
        heroe.setOyenteEfectos(new OyenteEfectos() {
            @Override
            public void efectoAplicado(Personaje objetivo, EstadoEfecto efecto) {
            }

            @Override
            public void efectoQuitado(Personaje objetivo, String nombreEfecto) {
            }
        });

        Map<String, Integer> derrotados = Map.of("Goblin", 2, "Ogro", 1);
        EstadoPartida estado = new EstadoPartida(perfil, gremio, derrotados);
        verificar("guardarPartida serializo el estado completo", gestor.guardarPartida(estado));

        EstadoPartida cargada = gestor.cargarPartida();
        verificar("cargarPartida devolvio el estado", cargada != null);
        verificar("perfil restaurado: 3 victorias y 1 derrota",
                cargada.getPerfil().getVictorias() == 3 && cargada.getPerfil().getDerrotas() == 1);
        verificar("logros restaurados en orden (TreeSet)",
                cargada.getPerfil().getLogros().toString().equals("[Intocable, Primera Victoria]"));
        verificar("gremio restaurado con nombre y 2 miembros",
                cargada.getGremio().getNombre().equals("Guardianes del Alba")
                        && cargada.getGremio().getMiembros().size() == 2);

        // Ciclo Personaje <-> Gremio: cada miembro cargado apunta de vuelta al mismo gremio cargado
        boolean cicloOk = true;
        for (Personaje miembro : cargada.getGremio().getMiembros()) {
            cicloOk &= miembro.getGremio() == cargada.getGremio();
        }
        verificar("ciclo Gremio <-> Personaje se conserva tras cargar", cicloOk);

        // Los datos del personaje (arma, pociones) sobreviven el viaje
        Personaje heroeCargado = cargada.getGremio().getMiembros().get(0);
        verificar("el heroe conserva su arma serializada",
                heroeCargado.getArma() != null && heroeCargado.getArma().getNombre().equals("Espada de Hierro"));

        // transient vuelve con su valor por defecto (null) al cargar
        verificar("oyenteEfectos (transient) vuelve como null", heroeCargado.getOyenteEfectos() == null);

        verificar("contador de derrotas del bestiario viaja en la partida",
                cargada.getEnemigosDerrotados().equals(Map.of("Goblin", 2, "Ogro", 1)));

        // ---- Investigacion: NotSerializableException al guardar un objeto no serializable
        boolean lanzoNotSerializable = false;
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream("partidas/malo.dat"))) {
            salida.writeObject(new Object() { });   // clase anonima sin Serializable
        } catch (NotSerializableException e) {
            lanzoNotSerializable = true;
        }
        verificar("guardar un objeto sin Serializable lanza NotSerializableException", lanzoNotSerializable);
        Files.deleteIfExists(Path.of("partidas/malo.dat"));

        // ---- Seccion 6: el juego no se cae con un .dat corrupto
        Files.write(Path.of(GestorArchivos.ARCHIVO_PARTIDA), new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        verificar("cargarPartida con archivo corrupto devuelve null sin caer", gestor.cargarPartida() == null);

        // ---- Endurecimiento: ClassCastException con un .dat valido pero de otro tipo
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(GestorArchivos.ARCHIVO_PARTIDA))) {
            salida.writeObject("esto no es una partida");
        }
        verificar("cargarPartida con tipo incorrecto devuelve null (ClassCastException)", gestor.cargarPartida() == null);

        // ---- Endurecimiento: el filtro de deserializacion bloquea clases ajenas al proyecto
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(GestorArchivos.ARCHIVO_PARTIDA))) {
            salida.writeObject(new Intruso());
        }
        verificar("el filtro de deserializacion bloquea clases ajenas (Intruso)", gestor.cargarPartida() == null);

        // ---- Endurecimiento: el filtro tambien pone TECHO a los recursos (maxarray).
        // byte[] es de java.base (permitido por clase), pero 20000 elementos superan el
        // maxarray=16384 del filtro: la deserializacion se rechaza igual. En este punto
        // todavia no hay .bak, asi que no hay respaldo que enmascare el rechazo.
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(GestorArchivos.ARCHIVO_PARTIDA))) {
            salida.writeObject(new byte[20000]);
        }
        verificar("el filtro pone techo a los recursos (array mayor que maxarray rechazado)", gestor.cargarPartida() == null);

        // ---- Endurecimiento: guardado atomico con respaldo .bak (y sin .tmp sobrante)
        gestor.guardarPartida(estado);   // 1a: el .dat anterior (basura) pasa al .bak
        gestor.guardarPartida(estado);   // 2a: el .bak ya contiene un estado VALIDO
        verificar("guardado atomico: .dat cargable, respaldo .bak presente y sin .tmp",
                gestor.existeRespaldo()
                        && !new File(GestorArchivos.ARCHIVO_PARTIDA_TMP).exists()
                        && gestor.cargarPartida() != null);

        // ---- Endurecimiento: recuperacion automatica desde el respaldo .bak
        Files.write(Path.of(GestorArchivos.ARCHIVO_PARTIDA), new byte[]{9, 9, 9, 9, 9, 9, 9, 9});
        EstadoPartida recuperada = gestor.cargarPartida();   // .dat corrupto -> carga el .bak
        verificar("carga con .dat corrupto se recupera sola desde el respaldo .bak",
                recuperada != null && recuperada.getPerfil().getVictorias() == 3);

        // ---- Endurecimiento: validacion de constructores del modelo
        boolean pocionOk = false;
        try { new Pocion("", 10, RaridadItem.COMUN); } catch (IllegalArgumentException e) { pocionOk = true; }
        try { new Pocion("X", -1, RaridadItem.COMUN); } catch (IllegalArgumentException e) { pocionOk &= true; }
        try { new Pocion("X", 10, null); } catch (NullPointerException e) { pocionOk &= true; }
        verificar("Pocion rechaza nombre vacio, curacion negativa y raridad nula", pocionOk);

        boolean efectoOk = false;
        try { new EstadoEfecto("X", null, 5, 3); } catch (NullPointerException e) { efectoOk = true; }
        try { new EstadoEfecto("X", TipoEfecto.DANIO_CONTINUO, 0, 3); } catch (IllegalArgumentException e) { efectoOk &= true; }
        try { new EstadoEfecto("X", TipoEfecto.DANIO_CONTINUO, 5, 0); } catch (IllegalArgumentException e) { efectoOk &= true; }
        verificar("EstadoEfecto rechaza tipo nulo, valor no positivo y duracion no positiva", efectoOk);

        boolean inventarioOk = false;
        try {
            new Inventario<Pocion>(3).agregar(null);
        } catch (CapacidadExcedidaException e) {
            // no es el error esperado: queda inventarioOk en false
        } catch (IllegalArgumentException e) {
            inventarioOk = true;
        }
        verificar("Inventario.agregar(null) lanza IllegalArgumentException", inventarioOk);

        // ---- Endurecimiento: PerfilJugador homogeneo con el resto del modelo
        PerfilJugador perfilLogros = new PerfilJugador();
        verificar("PerfilJugador ignora logros nulos o vacios (sin NPE del TreeSet)",
                !perfilLogros.desbloquearLogro(null)
                        && !perfilLogros.desbloquearLogro("   ")
                        && perfilLogros.getLogros().isEmpty());

        // ---- Seccion 4: borrado con File.delete() y Files.deleteIfExists()
        gestor.guardarPartida(estado);
        verificar("File.delete() borra y devuelve true", new File(GestorArchivos.ARCHIVO_PARTIDA).delete());
        verificar("Files.deleteIfExists devuelve false si ya no existe", !gestor.borrarConNio());
        gestor.guardarPartida(estado);
        verificar("Files.deleteIfExists devuelve true cuando el archivo existia", gestor.borrarConNio());

        // ---- Endurecimiento: borrado COMPLETO (privacidad): borrarPartida elimina el .dat,
        // el respaldo .bak y cualquier .tmp residual en una sola llamada.
        gestor.guardarPartida(estado);   // deja .dat y .bak
        verificar("borrarPartida elimina .dat, respaldo .bak y .tmp de una vez",
                gestor.borrarPartida()
                        && !new File(GestorArchivos.ARCHIVO_PARTIDA).exists()
                        && !new File(GestorArchivos.ARCHIVO_PARTIDA_BAK).exists()
                        && !new File(GestorArchivos.ARCHIVO_PARTIDA_TMP).exists());

        System.out.printf("%nRESULTADO: %d/%d VERIFICACIONES OK%n", pruebasOk, TOTAL);
        if (pruebasOk != TOTAL) {
            System.exit(1);
        }
    }

    private static final int TOTAL = 32;

    /** Clase Serializable AJENA al juego (fuera de com.rpg y del JDK): el filtro debe rechazarla. */
    private static final class Intruso implements java.io.Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;
    }

    private static void verificar(String descripcion, boolean condicion) {
        System.out.printf("[%s] %s%n", condicion ? "OK" : "FALLO", descripcion);
        if (condicion) {
            pruebasOk++;
        }
    }

    private static void limpiarEstadoPrevio() throws IOException {
        Files.deleteIfExists(Path.of(GestorArchivos.ARCHIVO_PARTIDA));
        Files.deleteIfExists(Path.of(GestorArchivos.ARCHIVO_LOG));
        File carpeta = new File(GestorArchivos.CARPETA_PARTIDAS);
        File[] restos = carpeta.listFiles();
        if (restos != null) {
            for (File resto : restos) {
                resto.delete();
            }
        }
    }
}
