/**
 * TALLER ARCHIVOS: persistencia del juego.
 * Contiene {@link com.rpg.persistencia.GestorArchivos} (File, BufferedWriter/FileWriter,
 * BufferedReader/FileReader, borrado con File.delete y Files.deleteIfExists, y la
 * serializacion con ObjectOutputStream/ObjectInputStream, todo con try-with-resources)
 * y {@link com.rpg.persistencia.EstadoPartida}, el recipiente Serializable del estado
 * de la partida (perfil, gremio y derrotas del bestiario).
 */
package com.rpg.persistencia;
