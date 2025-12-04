@file:Suppress("UNCHECKED_CAST")

package io.jacob.episodive.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combines 3 flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow1 The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param transform The transform function to combine the latest values of the three flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the three flows.
 */
fun <T1, T2, T3, T4, T5, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    transform: suspend (T1, T2, T3, T4, T5) -> R
): Flow<R> = combine(flow1, flow2, flow3, flow4, flow5) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
    )
}

/**
 * Combines six flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow1 The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param transform The transform function to combine the latest values of the six flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the six flows.
 */
fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(flow1, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

/**
 * Combines seven flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow1 The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param transform The transform function to combine the latest values of the seven flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the seven flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<R> = combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
    )
}

/**
 * Combines eight flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow1 The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param flow8 The eighth flow.
 * @param transform The transform function to combine the latest values of the eight flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the eight flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R
): Flow<R> = combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
        args[7] as T8,
    )
}

/**
 * Combines nine flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow1 The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param flow8 The eighth flow.
 * @param flow9 The ninth flow.
 * @param transform The transform function to combine the latest values of the nine flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the nine flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    flow9: Flow<T9>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R
): Flow<R> =
    combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7, flow8, flow9) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
            args[7] as T8,
            args[8] as T9,
        )
    }

/**
 * Combines ten flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow1 The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param flow8 The eighth flow.
 * @param flow9 The ninth flow.
 * @param flow10 The tenth flow.
 * @param transform The transform function to combine the latest values of the nine flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the nine flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    flow9: Flow<T9>,
    flow10: Flow<T10>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) -> R,
): Flow<R> =
    combine(
        flow1,
        flow2,
        flow3,
        flow4,
        flow5,
        flow6,
        flow7,
        flow8,
        flow9,
        flow10
    ) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
            args[7] as T8,
            args[8] as T9,
            args[9] as T10,
        )
    }

/**
 * Combines eleven flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow1 The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param flow8 The eighth flow.
 * @param flow9 The ninth flow.
 * @param flow10 The tenth flow.
 * @param flow11 The eleventh flow.
 * @param transform The transform function to combine the latest values of the eleven flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the eleven flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    flow9: Flow<T9>,
    flow10: Flow<T10>,
    flow11: Flow<T11>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) -> R,
): Flow<R> =
    combine(
        flow1,
        flow2,
        flow3,
        flow4,
        flow5,
        flow6,
        flow7,
        flow8,
        flow9,
        flow10,
        flow11
    ) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
            args[7] as T8,
            args[8] as T9,
            args[9] as T10,
            args[10] as T11,
        )
    }

