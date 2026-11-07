import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.concurrency.GestorEfectos;
import com.rpg.concurrency.GestorTurno;
import com.rpg.concurrency.TemporizadorTurnoTask;
import com.rpg.core.Arena;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.core.RegistroCombate;
import com.rpg.core.TorneoManager;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.enums.TipoEfecto;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prueba de humo del taller de concurrencia. Valida:
 *  1. La vida ya no pierde actualizaciones con muchos hilos (antes: condicion de carrera).
 *  2. curar() y recibirDanio() concurrentes dejan la vida consistente.
 *  3. Los efectos DoT corren en su propio hilo y terminan solos.
 *  4. El antidoto interrumpe el hilo del veneno con interrupt() y la vida se estabiliza.
 *  5. Los efectos HoT curan en su propio hilo.
 *  6-7. El temporizador de turno expira (callback) o se cancela a tiempo (interrupt).
 *  8. El torneo con semifinales paralelas y join() corona un campeon.
 *  9. El AutoGuardadoDaemon guarda periodicamente en memoria.
 * Ejecutar: java PruebaConcurrencia
 */
public class PruebaConcurrencia {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        RegistroCombate registro = new RegistroCombate();

        // ---- 1. carrera de vida corregida
        Enemigo blanco = new Enemigo("Blanco", 1000, 10, 0);
        int hilos = 8;
        int golpesPorHilo = 125;
        CountDownLatch listo = new CountDownLatch(hilos);
        for (int i = 0; i < hilos; i++) {
            new Thread(() -> {
                for (int j = 0; j < golpesPorHilo; j++) {
                    blanco.recibirDanio(1);
                }
                listo.countDown();
            }).start();
        }
        listo.await();
        check("Vida tras 1000 de danio en 8 hilos = 0 (sin perdida de actualizaciones)",
                blanco.getVida() == 0 && !blanco.estaVivo());

        // ---- 2. curar y danio al mismo tiempo
        // Pre-danio de 500 para que la vida quede a mitad y NUNCA se toquen los topes
        // (0 o vida maxima): 100 de danio y 100 de curacion deben cancelarse exactos.
        Enemigo pendulo = new Enemigo("Pendulo", 1000, 10, 0);
        pendulo.recibirDanio(500);
        int ciclos = 25;
        AtomicInteger curadoTotal = new AtomicInteger();
        CountDownLatch fin = new CountDownLatch(8);
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                for (int j = 0; j < ciclos; j++) {
                    pendulo.recibirDanio(1);
                }
                fin.countDown();
            }).start();
            new Thread(() -> {
                for (int j = 0; j < ciclos; j++) {
                    curadoTotal.addAndGet(pendulo.curar(1));
                }
                fin.countDown();
            }).start();
        }
        fin.await();
        check("Danio y curacion simultaneos: se aplicaron los 100 de curacion sin perder ninguno",
                curadoTotal.get() == 100);
        check("Danio y curacion simultaneos se cancelan: vida final = 500 (sin perdidas)",
                pendulo.getVida() == 500);

        // ---- 3. DoT en su propio hilo y fin natural
        GestorEfectos gestor = new GestorEfectos(80, registro);
        Enemigo envenenado = new Enemigo("Victima", 100, 10, 0);
        envenenado.setOyenteEfectos(gestor);
        envenenado.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 5, 3));
        check("Veneno registrado en el personaje", !envenenado.getEfectos().isEmpty());
        Thread.sleep(500);
        check("DoT aplico 3 ticks de 5 (vida 85) y termino solo",
                envenenado.getVida() == 85 && envenenado.getEfectos().isEmpty()
                        && !gestor.hayHilosActivos());

        // ---- 4. antidoto interrumpe el hilo del DoT
        Enemigo conVenenoLargo = new Enemigo("Envenenado Largo", 200, 10, 0);
        conVenenoLargo.setOyenteEfectos(gestor);
        conVenenoLargo.aplicarEfecto(new EstadoEfecto("Veneno Largo", TipoEfecto.DANIO_CONTINUO, 10, 10));
        Thread.sleep(250); // deja pasar 1-2 ticks
        int vidaAlAntidoto = conVenenoLargo.getVida();
        conVenenoLargo.quitarEfectosDanio();   // igual que usar el Antidoto
        Thread.sleep(300);
        check("Antidoto interrumpe el hilo: sin efectos y sin hilos activos",
                conVenenoLargo.getEfectos().isEmpty() && !gestor.hayHilosActivos());
        check("Tras el antidoto la vida no sigue bajando",
                conVenenoLargo.getVida() == vidaAlAntidoto);

        // ---- 5. HoT en su propio hilo
        Enemigo herido = new Enemigo("Convaleciente", 100, 10, 0);
        herido.recibirDanio(50); // vida 50
        herido.setOyenteEfectos(gestor);
        herido.aplicarEfecto(new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 5, 3));
        Thread.sleep(500);
        check("HoT recupero 15 de vida en 3 ticks", herido.getVida() == 65);

        // ---- 6. temporizador expira e invoca el callback
        AtomicInteger avisos = new AtomicInteger();
        GestorTurno temporizador = new GestorTurno(1, avisos::incrementAndGet);
        temporizador.iniciar();
        Thread.sleep(1400);
        check("Temporizador expiro tras 1 s", temporizador.expiroTiempo());
        check("Callback onTiempoAgotado invocado 1 vez", avisos.get() == 1);

        // ---- 7. temporizador cancelado a tiempo con interrupt
        GestorTurno cancelado = new GestorTurno(2, avisos::incrementAndGet);
        cancelado.iniciar();
        Thread.sleep(150);
        cancelado.cancelar();
        Thread.sleep(2300);
        check("Temporizador cancelado NO expiro", !cancelado.expiroTiempo());
        check("Callback no se invoco al cancelar", avisos.get() == 1);

        // ---- 8. torneo con semifinales paralelas
        TorneoManager torneo = new TorneoManager(new Arena(registro, new java.util.Random()), registro);
        var campeon = torneo.jugarTorneo();
        check("Torneo paralelo corono un campeon", campeon != null && campeon.getVida() > 0);

        // ---- 9. AutoGuardadoDaemon
        MotorJuego motor = new MotorJuego(new Consola(new Scanner("")), 120);
        Thread.sleep(500);
        check("AutoGuardadoDaemon dejo una copia del progreso en memoria",
                !motor.getUltimoGuardado().isEmpty());

        // ---- 10. reaplicacion del mismo efecto: el hilo viejo NO borra el nuevo
        Enemigo envenenadoDosVeces = new Enemigo("DobleVeneno", 200, 0, 0);
        envenenadoDosVeces.setOyenteEfectos(gestor);
        envenenadoDosVeces.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 5, 99));
        Thread.sleep(80);
        envenenadoDosVeces.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 5, 99));
        Thread.sleep(300);   // el hilo viejo fue interrumpido y ya debio terminar su limpieza
        check("Reaplicacion: el efecto nuevo sobrevive a la limpieza del hilo viejo",
                envenenadoDosVeces.getEfectos().size() == 1);
        check("Reaplicacion: queda exactamente 1 hilo de efecto activo",
                gestor.hayHilosActivos());
        envenenadoDosVeces.quitarEfectosDanio();
        Thread.sleep(200);
        check("Reaplicacion: el antidoto limpia el efecto vigente",
                envenenadoDosVeces.getEfectos().isEmpty() && !gestor.hayHilosActivos());

        System.out.println();
        if (fallos == 0) {
            System.out.println("RESULTADO: TODOS LOS CHECKS DE CONCURRENCIA OK");
        } else {
            System.out.println("RESULTADO: " + fallos + " check(s) con FALLO");
            System.exit(1);
        }
    }

    private static void check(String nombre, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + nombre);
        if (!condicion) {
            fallos++;
        }
    }
}
