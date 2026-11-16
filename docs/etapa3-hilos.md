# Etapa 3 — Cantidad y responsabilidad de los hilos

## 1. Invariantes de transición (los tres flujos)

| Invariante | Flujo | Transiciones |
|---|---|---|
| IT1 | Tarjeta crédito/débito | T0 T1 T2 T3 T9 |
| IT2 | Alto riesgo | T0 T4 T5 T9 |
| IT3 | Transferencia bancaria | T0 T6 T7 T8 T9 |

Los tres comparten el tramo de entrada (T0) y el de salida (T9).

## 2. Aplicación de los casos de la consigna

**Caso 1 — conflicto.** En P1 compiten T1, T4 y T6 (cada una inicia un invariante distinto).
Por eso: un grupo de hilos para lo que está *antes* del conflicto (T0) y luego **un hilo por invariante**
(T1-T2-T3, T4-T5 y T6-T7-T8).

**Caso 2 — join.** T3, T5 y T8 desembocan en P9. Después del join queda un solo camino (T9) y
hay que poner **tantos hilos como tokens simultáneos** pueda haber en P9.

**Cuántos tokens simultáneos.** El invariante de plaza
M(P0)+M(P1)+M(P2)+M(P3)+M(P4)+M(P5)+M(P6)+M(P9) = 3 dice que en el sistema hay 3 transacciones en total
(los 3 tokens iniciales de P0). Como máximo las 3 pueden estar juntas en P0 (→ 3 hilos para T0)
o juntas en P9 (→ 3 hilos para T9).

**Por qué un solo hilo por flujo.** Los recursos compartidos lo limitan a una transacción por rama:
- M(P7)+M(P2)+M(P3)+M(P4) = 1 → tarjeta y alto riesgo nunca están activos a la vez.
- M(P8)+M(P4)+M(P5)+M(P6) = 1 → alto riesgo y transferencia nunca están activos a la vez.

Cada rama tiene a lo sumo un token en sus plazas, así que un hilo por rama alcanza (más hilos no
podrían trabajar en paralelo).

## 3. Resultado: 9 hilos

| Hilo | Cant. | Transiciones | Justificación |
|---|---|---|---|
| Admision | 3 | T0 | Previo al conflicto; hasta 3 tokens en P0 |
| Tarjeta | 1 | T1 T2 T3 | Un hilo por invariante (Caso 1); P7 permite 1 token |
| AltoRiesgo | 1 | T4 T5 | Un hilo por invariante; P7 y P8 permiten 1 token |
| Transferencia | 1 | T6 T7 T8 | Un hilo por invariante; P8 permite 1 token |
| Liquidacion | 3 | T9 | Posterior al join (Caso 2); hasta 3 tokens en P9 |

Gráfico con colores (requisito de la consigna, equivalente a la Figura 2): `docs/hilos-psp.png` / `.svg`.

> Supuesto a confirmar con `RedDePetri.xml` en PIPE: P0 arranca con 3 tokens. Si fueran otros,
> cambia solo `PlanHilos.psp(tokensP0)`.

## 4. Cómo funciona el ciclo de vida (para explicar en el informe y en la defensa)

- Cada hilo (`HiloTransiciones`) repite en bucle su secuencia llamando a `monitor.fireTransition(t)`.
  Los tres hilos de flujo esperan a la vez en T1, T4 y T6: así, cuando llega un token a P1,
  el monitor tiene un conflicto real que resolver con la `Politica`.
- **Finalización limpia** (`ContadorInvariantes` + `GestorHilos`):
  1. Compuerta de admisión: los hilos de Admision piden permiso (atómico) antes de cada T0 y solo se
     conceden 200. Así no quedan transacciones a medias y el log tiene exactamente 200 invariantes completos.
  2. Las transacciones admitidas recorren su flujo hasta T9; cada T9 se cuenta.
  3. Al llegar a 200 T9, el gestor interrumpe a los hilos que siguen esperando en el monitor
     (`fireTransition` devuelve `false`), hace `join` de todos y verifica que ninguno siga vivo.
  4. Red de seguridad: timeout contra deadlocks y aborto inmediato si un hilo muere con excepción.
- El monitor sigue siendo agnóstico: no sabe qué transición admite ni cuál liquida; eso vive en `PlanHilos`.
