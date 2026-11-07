# Casos de estudio: violaciones de encapsulamiento

Transcripcion de los tres escenarios de la Seccion 6.1 del taller (Encapsulamiento
y Modificadores de Acceso, Unidad 2.2), con la causa raiz de cada falla y la forma
en que el disenio de este proyecto la previene.

Los archivos `CasoA.java`, `CasoB.java` y `CasoC.java` se transcriben tal como
aparecen en la guia. Viven fuera de `src/` porque el Caso A no compila: ese error
de compilador es justamente lo que hay que observar.

## Caso A: acceso directo no autorizado

`Main` intenta escribir y leer `heroe.puntosVida` directamente.

**Causa raiz:** `puntosVida` es `private` en `co.edu.puj.rpg.modelo.Personaje`
y `Main` esta en otro paquete (`co.edu.puj.rpg.principal`), de modo que el
compilador impide cualquier acceso directo al atributo.

**Error real del compilador** (transcribiendo el fragmento dentro del paquete
`principal`, con `javac`):

```
PruebaCasoA.java:8: error: puntosVida has private access in Personaje
        heroe.puntosVida = 200;
             ^
PruebaCasoA.java:9: error: puntosVida has private access in Personaje
        System.out.println("Vida: " + heroe.puntosVida);
                                           ^
2 errors
```

**Prevencion en el proyecto:** la unica via de escritura es el mutador
`setPuntosVida(int)`, que acota el valor al rango `0..vidaMaxima`; la lectura
se hace con `getPuntosVida()`.

## Caso B: bypassing de validaciones mediante constructor defectuoso

Este fragmento **si compila**: el error no es de compilador sino de disenio.
El constructor asigna `-50` directamente al atributo y deja `estaVivo = true`.

**Causa raiz:** el constructor escribe los atributos sin pasar por las
validaciones, creando un estado internamente incoherente (vida negativa con el
personaje vivo). Un constructor sin validacion es una puerta trasera que
reproduce exactamente lo que los setters impiden.

**Prevencion en el proyecto:** el constructor de `Personaje` valida sus
parametros (`vidaMaxima` positiva, estadisticas no negativas, nombre no vacio)
e inicializa el estado coherente (`puntosVida = vidaMaxima`, `estaVivo = true`);
ademas, `usarPocion` y `recibirDano` modifican la vida siempre a traves de
`setPuntosVida`, nunca por asignacion directa.

## Caso C: fuga de estado y asignacion fuera de limitos

`setPuntosVida(999999)` y `setPuntosVida(-100)` sobre un setter sin validacion.

**Causa raiz:** un mutador publico que acepta cualquier valor rompe las reglas
de negocio: la vida quedaria por encima de `vidaMaxima` o negativa, y
`estaVivo` no se actualizaria, dejando el estado inconsistente.

**Prevencion en el proyecto:** `setPuntosVida` implementa la logica de control
exigida por el taller:

- `vida <= 0`: fija `puntosVida = 0` y `estaVivo = false`.
- `vida > vidaMaxima`: fija `puntosVida = vidaMaxima`.
- valor intermedio valido: lo asigna y mantiene `estaVivo = true`.

## Verificacion de valores limite (Seccion 6.2, punto 3)

Prueba manual sobre `Personaje("Aragorn", 100, 15, 50.0)`:

```
setPuntosVida(-10)    -> vida=0 vivo=false
setPuntosVida(100000) -> vida=100 vivo=true
setPuntosVida(0)      -> vida=0 vivo=false
setPuntosVida(75)     -> vida=75 vivo=true
setOro(-5.0)          -> aceptada=false oro=50.0
setOro(120.0)         -> aceptada=true oro=120.0
ganarExperiencia(104) -> nivel=2 exp=4/100
constructor corto     -> nombre=Eowyn vida=100/100 ataque=10 oro=0.0
```

El objeto preserva la coherencia interna en todos los casos: la vida nunca
sale del rango `0..vidaMaxima`, `estaVivo` siempre refleja el estado real, el
oro nunca es negativo y el nivel nunca baja de 1.

## Verificacion en el IDE (Seccion 6.2, puntos 1 y 2)

- Al escribir `heroe.` en `Main`, el autocompletado solo ofrece la API publica
  (`atacar`, `usarPocion`, `recibirDano`, setters, getters): ningun campo de
  datos aparece porque todos son `private`.
- Cualquier intento de acceso directo (por ejemplo `heroe.nivel = 5`) reporta
  en IntelliJ el error `'nivel' has private access in
  'co.edu.puj.rpg.modelo.Personaje'`, equivalente al error de `javac`
  mostrado en el Caso A.
