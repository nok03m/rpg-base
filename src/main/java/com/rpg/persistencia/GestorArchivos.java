package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
/**
 * TALLER ARCHIVOS: toda la entrada/salida de archivos del juego vive aqui.
 *
 * == Investigacion obligatoria: FileWriter vs Files.writeString ==
 * FileWriter es un FLUJO (stream) de caracteres: se abre, se escribe poco a poco
 * (con BufferedWriter amortiguando) y uno mismo lo cierra; sirve para escribir
 * de a pedazos, para agregar (append) con FileWriter(file, true) y para archivos
 * grandes donde no conviene armar todo el texto en memoria. Files.writeString
 * (NIO, Java 11+) es UNA sola llamada que escribe todo el texto de golpe, con
 * opciones (APPEND, CREATE) y sin gestionar flujos; es ideal para contenido corto
 * que ya esta listo en un String. Aqui usamos FileWriter porque el taller pide
 * practicar el flujo con buffer y el modo append; para volcados puntuales de un
 * String corto, Files.writeString seria mas conciso.
 *
 * == Investigacion obligatoria: seguridad de Serializable ==
 * readObject() NUNCA debe usarse con datos que no sean de confianza: un archivo
 * .dat manipulado puede hacer que la deserializacion construya objetos arbitrarios
 * y ejecute codigo (gadget chains) o consumir memoria/CPU hasta colgar la JVM
 * (por ejemplo, listas anidadas que explotan al reconstruirse). Regla practica:
 * solo deserializar archivos que la propia aplicacion escribio. Alternativas mas
 * seguras para intercambiar datos: JSON (con una biblioteca que parse sin
 * instanciar clases arbitrarias), XML firmado o un formato de texto propio.
 *
 * == Concurrencia (seccion 7 del taller) ==
 * El hilo principal y el demonio AutoGuardadoDaemon llaman a estos metodos. Todos
 * los que tocan el MISMO archivo van synchronized sobre this (la instancia unica
 * compartida por el motor): dos hilos pueden escribir, pero nunca a la vez. Junto
 * con try-with-resources (que hace flush y close al salir del bloque) evita que
 * quede un archivo a medio escribir.
 */
public class GestorArchivos {

    /** Rutas RELATIVAS: se resuelven contra la carpeta desde donde se ejecuta el juego. */
    public static final String CARPETA_PARTIDAS = "partidas";
    public static final String ARCHIVO_LOG = CARPETA_PARTIDAS + "/registro_combate.txt";
    public static final String ARCHIVO_PARTIDA = CARPETA_PARTIDAS + "/partida.dat";
    /** Escritura temporal del guardado atomico: nunca se escribe sobre el .dat directamente. */
    public static final String ARCHIVO_PARTIDA_TMP = CARPETA_PARTIDAS + "/partida.dat.tmp";
    /** Copia de respaldo del guardado anterior (se conserva tras cada guardado exitoso). */
    public static final String ARCHIVO_PARTIDA_BAK = CARPETA_PARTIDAS + "/partida.dat.bak";
    /**
     * Filtro de deserializacion (mitigacion del riesgo de readObject, ver investigacion):
     * SOLO se aceptan clases del propio proyecto (com.rpg.**) y del JDK (java.base).
     * Cualquier otra clase del classpath queda bloqueada con InvalidClassException, lo que
     * cierra la puerta a gadget chains aunque alguien sustituya el .dat.
     */
    private static final ObjectInputFilter FILTRO_SEGURO =
            ObjectInputFilter.Config.createFilter("com.rpg.**;java.base/*;!*");
    /** Tamano maximo del log antes de rotarlo (empezar de cero) en el proximo guardado. */
    private static final long LIMITE_LOG_BYTES = 256 * 1024;

    /**
     * Seccion 1 (clase File): prepara la carpeta y el archivo de registro.
     * - mkdirs() crea la carpeta (y padres) solo si falta; si ya existe no falla.
     * - createNewFile() crea el .txt vacio y devuelve boolean; lanza IOException si algo rompe.
     */
    public void prepararCarpeta() {
        File carpeta = new File(CARPETA_PARTIDAS);
        if (carpeta.mkdirs()) {
            System.out.println("Carpeta creada: " + carpeta.getPath());
        }
        File log = new File(ARCHIVO_LOG);
        try {
            if (log.createNewFile()) {
                System.out.println("Archivo de registro creado: " + log.getPath());
            }
        } catch (IOException e) {
            // Sin tildes a proposito: mismo estilo que el resto de los mensajes de consola.
            System.out.println("No se pudo crear el registro de combate: " + e.getMessage());
        }
    }

    /**
     * Seccion 1: informacion de archivos con exists(), length(), getName() y getAbsolutePath().
     * Un File con ruta RELATIVA ("partidas/partida.dat") se resuelve contra la carpeta de
     * trabajo del proceso; uno con ruta ABSOLUTA ("/home/usuario/...") apunta igual desde
     * cualquier carpeta de trabajo. getAbsolutePath() muestra cual es la absoluta que
     * corresponde a la relativa.
     */
    public void informacionArchivos() {
        for (String ruta : new String[]{ARCHIVO_LOG, ARCHIVO_PARTIDA}) {
            File archivo = new File(ruta);
            System.out.printf("- %s | absoluta: %s | existe: %s | bytes: %d%n",
                    archivo.getName(), archivo.getAbsolutePath(), archivo.exists(), archivo.length());
        }
    }

    /**
     * Seccion 2: escribe el log del combate con BufferedWriter + FileWriter.
     * - agregar=false: FileWriter(file) SOBRESCRIBE el archivo.
     * - agregar=true : FileWriter(file, true) AGREGA al final sin borrar lo anterior.
     * - newLine() en lugar de concatenar "\n" a mano (usa el separador del sistema).
     * - try-with-resources: close() (con flush implicito) ocurre si o si, incluso con excepcion.
     */
    public boolean escribirLog(String contenido, boolean agregar) {
        boolean modoAppend = agregar;
        if (agregar) {
            // Rotacion simple: si el log crecio demasiado (el autoguardado agrega cada 15 s),
            // el proximo volcado empieza un archivo nuevo en vez de crecer sin limite.
            if (new File(ARCHIVO_LOG).length() > LIMITE_LOG_BYTES) {
                modoAppend = false;
                System.out.println("Registro rotado: supero " + LIMITE_LOG_BYTES + " bytes.");
            }
        }
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(ARCHIVO_LOG, modoAppend))) {
            // Se normaliza el separador ANTES de partir: si el contenido trae \r\n (Windows),
            // dividir por \n dejaria un \r residual al final de cada linea.
            for (String linea : contenido.replace("\r\n", "\n").split("\n", -1)) {
                escritor.write(linea);
                escritor.newLine();
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error al escribir el registro: " + e.getMessage());
            return false;
        }
    }

    /**
     * Seccion 3: lee el log con BufferedReader + FileReader y readLine() hasta null.
     * FileNotFoundException se captura ANTES que IOException (es la mas especifica):
     * si el archivo aun no existe se avisa con un mensaje claro y el juego sigue.
     * @return el contenido, o null si no se pudo leer
     */
    public String leerLog() {
        File archivo = new File(ARCHIVO_LOG);
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            StringBuilder contenido = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                contenido.append(linea).append(System.lineSeparator());
            }
            return contenido.toString();
        } catch (FileNotFoundException e) {
            System.out.println("El registro de combate aun no existe: juega un combate primero.");
            return null;
        } catch (IOException e) {
            System.out.println("Error al leer el registro: " + e.getMessage());
            return null;
        }
    }

    /**
     * Seccion 5: serializa la partida con ObjectOutputStream.writeObject().
     * synchronized: el daemon y el hilo principal comparten esta unica instancia, asi que
     * nunca dos escrituras a la vez sobre partida.dat (seccion 7).
     * NotSerializableException va en su propio catch (y antes de IOException, de la que es
     * subclase): dice exactamente QUE clase del grafo no se puede guardar.
     */
    public synchronized boolean guardarPartida(EstadoPartida estado) {
        // GUARDADO ATOMICO: se escribe todo en un .tmp (el .dat anterior queda intacto si
        // algo falla a mitad), y solo cuando el flujo cerro bien (flush implicito) se hace
        // el reemplazo con Files.move, que es atomico en el mismo sistema de archivos. La
        // copia anterior se conserva en .bak para recuperar la ultima partida valida.
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(ARCHIVO_PARTIDA_TMP))) {
            salida.writeObject(estado);
        } catch (NotSerializableException e) {
            new File(ARCHIVO_PARTIDA_TMP).delete();
            System.out.println("Hay un objeto que no es Serializable: " + e.getMessage());
            return false;
        } catch (IOException e) {
            new File(ARCHIVO_PARTIDA_TMP).delete();
            System.out.println("Error al guardar la partida: " + e.getMessage());
            return false;
        }
        try {
            File actual = new File(ARCHIVO_PARTIDA);
            if (actual.exists()) {
                Files.copy(Path.of(ARCHIVO_PARTIDA), Path.of(ARCHIVO_PARTIDA_BAK),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(Path.of(ARCHIVO_PARTIDA_TMP), Path.of(ARCHIVO_PARTIDA),
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            new File(ARCHIVO_PARTIDA_TMP).delete();
            System.out.println("Error al reemplazar la partida: " + e.getMessage());
            return false;
        }
    }

    /**
     * Seccion 5: carga la partida con ObjectInputStream.readObject() y cast al tipo correcto.
     * Orden de los catch: FileNotFoundException (mas especifica) -> ClassNotFoundException
     * (el .dat menciona una clase que ya no existe en el proyecto) -> IOException
     * (archivo corrupto o ilegible). Con cualquier problema devuelve null y el juego sigue.
     */
    public synchronized EstadoPartida cargarPartida() {
        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(ARCHIVO_PARTIDA))) {
            // El filtro bloquea toda clase fuera de com.rpg y del JDK: un .dat manipulado
            // con clases ajenas falla aqui con InvalidClassException (una IOException).
            entrada.setObjectInputFilter(FILTRO_SEGURO);
            Object objeto = entrada.readObject();
            return (EstadoPartida) objeto;
        } catch (FileNotFoundException e) {
            System.out.println("Todavia no hay partida guardada: " + e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("El archivo pertenece a una version antigua del juego: " + e.getMessage());
            return null;
        } catch (ClassCastException e) {
            // El .dat es una serializacion VALIDA pero de otro tipo de objeto (no EstadoPartida).
            System.out.println("El archivo guardado no es una partida de este juego: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.out.println("El archivo de partida esta danado o ilegible: " + e.getMessage());
            return null;
        }
    }

    /**
     * Seccion 4: borrado con File.delete() y con Files.deleteIfExists(Path).
     * Diferencia: File.delete() devuelve false sin decir por que (no existia, o la carpeta
     * no deja borrar); Files.deleteIfExists(Path) devuelve false solo si no existia y, si
     * falla por otra razon, lanza IOException con el motivo. Aqui se usan las dos y se
     * comparan sus resultados.
     */
    public synchronized boolean borrarPartida() {
        File archivo = new File(ARCHIVO_PARTIDA);
        boolean conFile = archivo.delete();
        boolean borrado = conFile;
        try {
            boolean conNio = Files.deleteIfExists(Path.of(ARCHIVO_PARTIDA));
            System.out.printf("Borrado: File.delete()=%s | Files.deleteIfExists()=%s%n", conFile, conNio);
            // Si File.delete() fallo pero NIO si borro el archivo, el resultado publico
            // correcto es true: la partida SI fue eliminada.
            borrado = conFile || conNio;
        } catch (IOException e) {
            // File.delete() ya intento el borrado; si NIO tambien falla, se explica el motivo.
            System.out.println("Files.deleteIfExists informo: " + e.getMessage());
        }
        return borrado;
    }

    /** Variante NIO pura (la usa la prueba automatica). @return true si borro un archivo que existia */
    public synchronized boolean borrarConNio() {
        try {
            return Files.deleteIfExists(Path.of(ARCHIVO_PARTIDA));
        } catch (IOException e) {
            System.out.println("No se pudo borrar: " + e.getMessage());
            return false;
        }
    }

    /** @return true si existe el .dat de la partida (para confirmar antes de borrar). */
    public boolean existePartidaGuardada() {
        return new File(ARCHIVO_PARTIDA).exists();
    }

    /** @return true si existe la copia de respaldo .bak (recuperacion tras un .dat corrupto). */
    public boolean existeRespaldo() {
        return new File(ARCHIVO_PARTIDA_BAK).exists();
    }

    /** Seccion 4: borra el log de combate con la variante que devuelve el motivo del fallo. */
    public boolean borrarLog() {
        try {
            return Files.deleteIfExists(Path.of(ARCHIVO_LOG));
        } catch (IOException e) {
            System.out.println("No se pudo borrar el registro: " + e.getMessage());
            return false;
        }
    }
}
