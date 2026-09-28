package io.jacob.episodive.core.designsystem.component

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PathMeasure
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.util.lerp
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt
import android.graphics.Path as AndroidPath

/*
 * 임의의 두 ImageVector 사이를 모양 그대로 잇는 모핑.
 *
 * 경로 명령끼리는 보간할 수 없다 — 아이콘마다 경로 수도, 명령 구성도, 선(stroke)/채움(fill)
 * 여부도 다르다(외곽선 하트는 선 경로 셋, 채운 하트는 채움 경로 하나). 그래서 둘을 먼저 같은
 * 형식으로 푼다.
 *
 * 1. 선은 그 선이 칠하는 영역의 윤곽으로 바꾸고(`Paint.getFillPath`) 채움과 함께 하나로 합친다
 *    (`Path.op(UNION)`). 이제 아이콘은 "겹치지 않는 닫힌 윤곽들" 이다.
 * 2. 윤곽마다 둘레를 같은 수의 점으로 고르게 뽑는다. 윤곽이 다른 윤곽 안에 몇 겹 들어 있는지로
 *    채움/구멍을 가르고, 방향을 그에 맞게 맞춘다 — 채움과 구멍의 방향이 반대여야 NonZero 로
 *    칠할 때 구멍이 뚫리고, 모핑 도중 채움끼리 겹쳐도 구멍이 생기지 않는다.
 * 3. 채움은 채움끼리, 구멍은 구멍끼리 가까운 것부터 짝짓는다. 짝이 없는 윤곽은 제 중심의 한
 *    점과 짝지어 그 자리에서 자라나거나 오그라든다. 외곽선 → 채움 전환에서 속이 차오르는 것은
 *    안쪽 구멍 윤곽이 이렇게 오그라들기 때문이다.
 * 4. 짝마다 점 번호를 돌려 가며 두 윤곽이 가장 가깝게 겹치는 시작점을 고른다. 그래야 모핑 중에
 *    윤곽이 비틀리지 않는다.
 *
 * 점을 고르게 뽑으므로 날카로운 모서리는 조금 깎인다. 그래서 이 모양은 **전환 중에만** 그리고,
 * 멈춰 있을 때는 원본 벡터를 그대로 그린다([MorphIcon]).
 */

private const val SamplesPerContour = 96

/** 이보다 넓이가 작은 윤곽은 버린다(단위: 뷰포트를 1×1 로 본 넓이). 합성 과정의 부스러기다. */
private const val MinContourArea = 1e-5f

internal class MorphContour(
    val xs: FloatArray,
    val ys: FloatArray,
    val isHole: Boolean,
) {
    val area: Float = signedArea(xs, ys)
    val cx: Float = xs.average().toFloat()
    val cy: Float = ys.average().toFloat()
}

/** 좌표는 뷰포트를 0..1 로 정규화한 값이다. 뷰포트가 다른 두 아이콘도 그대로 짝지을 수 있다. */
internal class MorphShape(val contours: List<MorphContour>)

internal class MorphPlan(private val pairs: List<ContourPair>) {

    /** [progress] 시점의 모양을 [target] 에 채운다. [width]·[height] 는 그릴 영역 크기다. */
    fun fill(progress: Float, width: Float, height: Float, target: Path) {
        target.rewind()
        for (pair in pairs) {
            for (i in 0 until SamplesPerContour) {
                val x = lerp(pair.fromX[i], pair.toX[i], progress) * width
                val y = lerp(pair.fromY[i], pair.toY[i], progress) * height
                if (i == 0) target.moveTo(x, y) else target.lineTo(x, y)
            }
            target.close()
        }
    }

    /**
     * [progress] 시점의 모양을 새 출발점으로 떼어 낸다. 전환 도중 목표가 다시 바뀌면 지금
     * 보이는 모양에서 이어 가야 모양이 튀지 않는다.
     */
    fun shapeAt(progress: Float): MorphShape = MorphShape(
        pairs.mapNotNull { pair ->
            val xs = FloatArray(SamplesPerContour) { lerp(pair.fromX[it], pair.toX[it], progress) }
            val ys = FloatArray(SamplesPerContour) { lerp(pair.fromY[it], pair.toY[it], progress) }
            MorphContour(xs, ys, pair.isHole).takeIf { abs(it.area) >= MinContourArea }
        }
    )
}

internal class ContourPair(
    val fromX: FloatArray,
    val fromY: FloatArray,
    val toX: FloatArray,
    val toY: FloatArray,
    val isHole: Boolean,
)

private val shapeCache = WeakHashMap<ImageVector, MorphShape>()

/**
 * 아이콘을 모핑용 모양으로 푼다. 아이콘은 싱글턴이라 한 번 푼 것을 계속 쓴다.
 *
 * 경로 연산이 되지 않는 환경(Robolectric 의 기본 그래픽 모드)에서는 빈 모양이 나온다. 그때
 * [morphPlan] 은 null 을 돌려주고 아이콘은 모핑 없이 바뀐다.
 */
internal fun ImageVector.toMorphShape(): MorphShape = synchronized(shapeCache) {
    shapeCache.getOrPut(this) {
        runCatching { buildMorphShape(this) }.getOrElse { MorphShape(emptyList()) }
    }
}

private val planCache = HashMap<Pair<ImageVector, ImageVector>, MorphPlan?>()

/**
 * 멈춘 아이콘끼리의 모핑 계획. 같은 쌍이면 결과가 늘 같아 한 번 만든 것을 다시 쓴다(좋아요를
 * 켰다 껐다 할 때마다 짝짓기를 새로 하지 않는다). 아이콘은 싱글턴이라 쌍의 수가 정해져 있다.
 */
internal fun cachedMorphPlan(from: ImageVector, to: ImageVector): MorphPlan? =
    synchronized(planCache) {
        planCache.getOrPut(from to to) { morphPlan(from.toMorphShape(), to.toMorphShape()) }
    }

/** 두 모양을 이을 수 없으면(어느 한쪽이 비었으면) null. */
internal fun morphPlan(from: MorphShape, to: MorphShape): MorphPlan? {
    if (from.contours.isEmpty() || to.contours.isEmpty()) return null
    val pairs = ArrayList<ContourPair>()
    for (isHole in listOf(false, true)) {
        matchContours(
            from = from.contours.filter { it.isHole == isHole },
            to = to.contours.filter { it.isHole == isHole },
            isHole = isHole,
            into = pairs,
        )
    }
    return MorphPlan(pairs)
}

private fun buildMorphShape(vector: ImageVector): MorphShape {
    val merged = AndroidPath()
    collect(vector.root, Matrix(), merged)
    return MorphShape(
        classify(
            sample(merged, scaleX = 1f / vector.viewportWidth, scaleY = 1f / vector.viewportHeight)
        )
    )
}

/** 그룹 변환은 `GroupComponent` 와 같은 순서로 쌓는다. */
private fun collect(group: VectorGroup, parent: Matrix, into: AndroidPath) {
    val matrix = Matrix(parent).apply {
        preTranslate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        preRotate(group.rotation)
        preScale(group.scaleX, group.scaleY)
        preTranslate(-group.pivotX, -group.pivotY)
    }
    for (node in group) {
        when (node) {
            is VectorGroup -> collect(node, matrix, into)
            is VectorPath -> addFilledArea(node, matrix, into)
        }
    }
}

private fun addFilledArea(node: VectorPath, matrix: Matrix, into: AndroidPath) {
    val source = node.pathData.toPath().apply { fillType = node.pathFillType }.asAndroidPath()

    if (node.fill != null && node.fillAlpha > 0f) {
        into.op(AndroidPath(source).apply { transform(matrix) }, AndroidPath.Op.UNION)
    }
    if (node.stroke != null && node.strokeAlpha > 0f && node.strokeLineWidth > 0f) {
        // 굵기는 변환 전 좌표에서 매겨야 그룹 스케일이 선 굵기에도 걸린다.
        val outline = AndroidPath()
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = node.strokeLineWidth
            strokeMiter = node.strokeLineMiter
            strokeCap = node.strokeLineCap.toAndroidCap()
            strokeJoin = node.strokeLineJoin.toAndroidJoin()
        }.getFillPath(source, outline)
        outline.transform(matrix)
        into.op(outline, AndroidPath.Op.UNION)
    }
}

private fun StrokeCap.toAndroidCap(): Paint.Cap = when (this) {
    StrokeCap.Round -> Paint.Cap.ROUND
    StrokeCap.Square -> Paint.Cap.SQUARE
    else -> Paint.Cap.BUTT
}

private fun StrokeJoin.toAndroidJoin(): Paint.Join = when (this) {
    StrokeJoin.Round -> Paint.Join.ROUND
    StrokeJoin.Bevel -> Paint.Join.BEVEL
    else -> Paint.Join.MITER
}

private class RawContour(val xs: FloatArray, val ys: FloatArray)

private fun sample(path: AndroidPath, scaleX: Float, scaleY: Float): List<RawContour> {
    val measure = PathMeasure(path, true)
    val position = FloatArray(2)
    val contours = ArrayList<RawContour>()
    do {
        val length = measure.length
        if (length <= 0f) continue
        val xs = FloatArray(SamplesPerContour)
        val ys = FloatArray(SamplesPerContour)
        for (i in 0 until SamplesPerContour) {
            measure.getPosTan(length * i / SamplesPerContour, position, null)
            xs[i] = position[0] * scaleX
            ys[i] = position[1] * scaleY
        }
        if (abs(signedArea(xs, ys)) >= MinContourArea) contours += RawContour(xs, ys)
    } while (measure.nextContour())
    return contours
}

/**
 * 몇 겹 안에 들어 있는지로 채움/구멍을 가르고 방향을 맞춘다(채움은 넓이 양수, 구멍은 음수).
 * 합성 결과의 방향 규칙에 기대지 않는다 — 직접 맞춰야 짝지은 두 윤곽의 방향이 늘 같다.
 */
private fun classify(raw: List<RawContour>): List<MorphContour> = raw.mapIndexed { index, contour ->
    val depth = raw.indices.count { other -> other != index && liesInside(contour, raw[other]) }
    val isHole = depth % 2 == 1
    val positive = signedArea(contour.xs, contour.ys) > 0f
    if (positive == !isHole) {
        MorphContour(contour.xs, contour.ys, isHole)
    } else {
        MorphContour(contour.xs.reversedArray(), contour.ys.reversedArray(), isHole)
    }
}

/** 한 점만 보지 않고 둘레의 여러 점에 물어 과반으로 정한 표본 수. */
private const val InsideVotes = 9

/**
 * [inner] 가 [outer] 안에 있는가. 합성 뒤의 윤곽은 서로 가로지르지 않지만 한 점에서 맞닿을 수는
 * 있다. 윤곽 위의 한 점만 물으면 그 점이 마침 맞닿은 자리일 때 답이 멋대로 나와, 채움이 구멍으로
 * 뒤집혀 모핑 도중 그 자리가 비어 보인다. 둘레에 고르게 흩어진 점들의 과반으로 정한다.
 */
private fun liesInside(inner: RawContour, outer: RawContour): Boolean {
    val step = inner.xs.size / InsideVotes
    val inside = (0 until InsideVotes).count { vote ->
        val i = vote * step
        contains(outer, inner.xs[i], inner.ys[i])
    }
    return inside * 2 > InsideVotes
}

/** 짝수-홀수 규칙의 점 포함 판정. */
private fun contains(polygon: RawContour, x: Float, y: Float): Boolean {
    var inside = false
    var j = polygon.xs.size - 1
    for (i in polygon.xs.indices) {
        val xi = polygon.xs[i]
        val yi = polygon.ys[i]
        val xj = polygon.xs[j]
        val yj = polygon.ys[j]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

private fun signedArea(xs: FloatArray, ys: FloatArray): Float {
    var sum = 0f
    var j = xs.size - 1
    for (i in xs.indices) {
        sum += xs[j] * ys[i] - xs[i] * ys[j]
        j = i
    }
    return sum / 2f
}

/**
 * 비용이 작은 쌍부터 짝짓는다(중심 사이 거리 + 크기 차이). 윤곽 수가 많아야 열 남짓이라 탐욕
 * 매칭으로 충분하다. 남은 윤곽은 제 중심의 한 점과 짝지어 그 자리에서 자라거나 오그라든다.
 */
private fun matchContours(
    from: List<MorphContour>,
    to: List<MorphContour>,
    isHole: Boolean,
    into: MutableList<ContourPair>,
) {
    val candidates = ArrayList<Triple<Int, Int, Float>>(from.size * to.size)
    for (i in from.indices) {
        for (j in to.indices) {
            val a = from[i]
            val b = to[j]
            val cost = hypot(a.cx - b.cx, a.cy - b.cy) +
                    abs(sqrt(abs(a.area)) - sqrt(abs(b.area)))
            candidates += Triple(i, j, cost)
        }
    }
    candidates.sortBy { it.third }

    val usedFrom = BooleanArray(from.size)
    val usedTo = BooleanArray(to.size)
    for ((i, j, _) in candidates) {
        if (usedFrom[i] || usedTo[j]) continue
        usedFrom[i] = true
        usedTo[j] = true
        into += aligned(from[i], to[j], isHole)
    }
    from.forEachIndexed { i, a ->
        if (!usedFrom[i]) into += ContourPair(a.xs, a.ys, point(a.cx), point(a.cy), isHole)
    }
    to.forEachIndexed { j, b ->
        if (!usedTo[j]) into += ContourPair(point(b.cx), point(b.cy), b.xs, b.ys, isHole)
    }
}

private fun point(value: Float) = FloatArray(SamplesPerContour) { value }

/** [to] 의 시작점을 돌려 [from] 과 가장 가깝게 겹치는 배치를 고른다. */
private fun aligned(from: MorphContour, to: MorphContour, isHole: Boolean): ContourPair {
    val n = SamplesPerContour
    var bestOffset = 0
    var bestCost = Float.MAX_VALUE
    for (offset in 0 until n) {
        var cost = 0f
        for (i in 0 until n) {
            val k = (i + offset) % n
            val dx = from.xs[i] - to.xs[k]
            val dy = from.ys[i] - to.ys[k]
            cost += dx * dx + dy * dy
        }
        if (cost < bestCost) {
            bestCost = cost
            bestOffset = offset
        }
    }
    val toX = FloatArray(n) { to.xs[(it + bestOffset) % n] }
    val toY = FloatArray(n) { to.ys[(it + bestOffset) % n] }
    return ContourPair(from.xs, from.ys, toX, toY, isHole)
}
