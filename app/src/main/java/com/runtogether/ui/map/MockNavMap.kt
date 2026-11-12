package com.runtogether.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.demo.DemoMapData
import com.runtogether.domain.JoinMode
import com.runtogether.domain.TrafficLevel
import com.runtogether.state.formatKm
import com.runtogether.ui.components.MemberAvatar
import com.runtogether.ui.theme.KoreanTextStyle
import com.runtogether.ui.theme.RtColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 시연용 그림 지도. 실제 지도 SDK 를 붙이기 전까지 쓰며, 화면에 ‘시연용 지도’라고 표시한다.
 * 고속도로 · 국도 · 마을 길, 시가지 · 숲 · 물, IC · 휴게소 · 지명, 교통 색, 모임 경로(주황)와 차 위치를 그린다.
 * 차 위치는 코스를 따라 간 거리(progressKm)로 놓는다 — 실제 지도에서는 좌표로 놓는다.
 */
object MockNavMapRenderer : GroupMapRenderer {
    override val isDemo: Boolean = true

    @Composable
    override fun Render(model: GroupMapModel, camera: MapCamera, modifier: Modifier) {
        MockNavMap(model, camera, modifier)
    }
}

// ---------------- 지도 좌표 (단위 km) ----------------

private data class V(val x: Double, val y: Double) {
    operator fun plus(o: V) = V(x + o.x, y + o.y)
    operator fun minus(o: V) = V(x - o.x, y - o.y)
    operator fun times(k: Double) = V(x * k, y * k)
}

/** 모임 경로(고속도로) 가운데 선: 가로는 출발지부터 거리(km), 세로는 완만한 굽이 */
private object Road {
    fun y(s: Double): Double = 0.8 * sin(s / 5.2) + 0.42 * sin(s / 1.75 + 0.6)
    private fun dy(s: Double): Double = 0.8 / 5.2 * cos(s / 5.2) + 0.42 / 1.75 * cos(s / 1.75 + 0.6)
    fun point(s: Double) = V(s, y(s))
    fun tangent(s: Double): V {
        val d = dy(s)
        val len = sqrt(1 + d * d)
        return V(1 / len, d / len)
    }

    fun normal(s: Double): V {
        val t = tangent(s)
        return V(-t.y, t.x)
    }

    /** 경로에서 옆으로 [lateral] km 떨어진 점 */
    fun at(s: Double, lateral: Double = 0.0): V = point(s) + normal(s) * lateral

    /** 국도는 고속도로 옆으로 1.1~1.9km 떨어져 나란히 간다 */
    fun national(s: Double): Double = DemoMapData.NATIONAL_SIDE * (1.5 + 0.4 * sin(s / 3.1 + 0.8))
}

/** 화면에 보이는 범위와 좌표 변환 (진행 방향이 화면 오른쪽 위를 향하게 돌린다) */
private class MapView(
    val widthPx: Float,
    val heightPx: Float,
    val density: Float,
    val centerKm: Double,
    val pxPerKm: Double,
    private val pivot: Offset,
    angle: Double,
) {
    private val origin = Road.point(centerKm)
    private val cosA = cos(angle)
    private val sinA = sin(angle)

    private val reachKm: Double = hypot(widthPx.toDouble(), heightPx.toDouble()) / pxPerKm * 0.8
    val fromKm: Double = centerKm - reachKm
    val toKm: Double = centerKm + reachKm

    /** 선을 그릴 때 점 간격 (km) */
    val stepKm: Double = (6.0 * density / pxPerKm).coerceIn(0.005, 0.5)

    fun screen(p: V): Offset {
        val dx = (p.x - origin.x) * pxPerKm
        val dy = (p.y - origin.y) * pxPerKm
        return Offset((pivot.x + dx * cosA - dy * sinA).toFloat(), (pivot.y + dx * sinA + dy * cosA).toFloat())
    }

    fun at(s: Double, lateral: Double = 0.0): Offset = screen(Road.at(s, lateral))

    /** 화면에서 경로가 나아가는 방향 (라디안) */
    fun angleAt(s: Double): Float {
        val a = at(s)
        val b = at(s + 0.05)
        return atan2(b.y - a.y, b.x - a.x)
    }

    /** 화면에서 경로 진행 방향의 오른쪽 */
    fun sideAt(s: Double): Offset {
        val a = angleAt(s)
        return Offset(-sin(a), cos(a))
    }

    fun inside(o: Offset, margin: Float = 0f): Boolean =
        o.x >= -margin && o.x <= widthPx + margin && o.y >= -margin && o.y <= heightPx + margin

    fun path(from: Double, to: Double, lateral: (Double) -> Double = { 0.0 }): Path {
        val path = Path()
        var s = from
        var first = true
        while (s <= to + 1e-9) {
            val o = at(s, lateral(s))
            if (first) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            first = false
            s += stepKm
        }
        return path
    }
}

/** 확대 단계 (dp/km). 자동 맞춤은 모임 전체가 보이는 가장 큰 단계 */
private val LEVELS_DP = floatArrayOf(24f, 34f, 48f, 68f, 96f, 136f, 192f, 272f)

/** 화면에서 경로가 오른쪽 위로 가는 각도 (도) */
private const val ROUTE_ANGLE_DEG = -58.0

private fun buildView(model: GroupMapModel, camera: MapCamera, w: Float, h: Float, density: Float): MapView {
    val me = model.me
    val onRoute = model.vehicles.filter { it.onPlannedRoute }
    val tail = onRoute.minOfOrNull { it.progressKm } ?: me?.progressKm ?: 0.0
    val lead = onRoute.maxOfOrNull { it.progressKm } ?: me?.progressKm ?: 0.0
    // 다음 집결지가 7km 안이면 함께 보이게 맞춘다 (쉬어가기 투표로 가까운 휴게소가 정해졌을 때 등)
    val nextKm = model.stops.firstOrNull { it.isNext }?.km
    val fitTo = if (nextKm != null && nextKm - lead in 0.0..7.0) nextKm else lead
    val withNext = fitTo > lead
    val spread = (fitTo - tail).coerceAtLeast(0.2)
    val visiblePx = h / 0.848f
    val fill = if (withNext) 0.8f else 0.45f
    var auto = 0
    for (i in LEVELS_DP.indices) if (spread * LEVELS_DP[i] * density <= fill * visiblePx) auto = i
    val index = (auto + camera.zoomStep).coerceIn(0, LEVELS_DP.lastIndex)
    val pxPerKm = LEVELS_DP[index] * density.toDouble()
    val follow = camera.followMe && me != null
    val centerKm = if (camera.followMe && me != null) me.progressKm + 0.16 * visiblePx / pxPerKm else (tail + fitTo) / 2
    val pivot = Offset(w * 0.42f, h * if (follow) 0.64f else if (withNext) 0.56f else 0.6f)
    val t = Road.tangent(centerKm)
    val angle = ROUTE_ANGLE_DEG * PI / 180.0 - atan2(t.y, t.x)
    return MapView(w, h, density, centerKm, pxPerKm, pivot, angle)
}

/** 같은 값을 넣으면 같은 0~1 값 (지도 모양이 그릴 때마다 바뀌지 않게) */
private fun hash01(a: Int, b: Int): Double {
    var x = a * 73856093 xor (b * 19349663)
    x = x xor (x ushr 13)
    x *= 0x5bd1e995
    x = x xor (x ushr 15)
    return (x and 0x7fffffff) / 2147483647.0
}

// ---------------- 지도 ----------------

@Composable
private fun MockNavMap(model: GroupMapModel, camera: MapCamera, modifier: Modifier) {
    BoxWithConstraints(modifier.background(RtColors.MapLand)) {
        val density = LocalDensity.current.density
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val view = remember(model, camera, w, h, density) { buildView(model, camera, w, h, density) }
        val measurer = rememberTextMeasurer()

        Canvas(Modifier.fillMaxSize()) {
            drawAreas(view)
            drawWater(view)
            drawBlocks(view)
            drawLocalRoads(view)
            drawNationalRoad(view)
            drawExpressway(view)
            drawRoute(view, model)
            drawMapLabels(view, measurer, model)
        }

        StopMarkers(view, model)
        VehicleMarkers(view, model)

        Row(
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ScaleBar(view)
            DemoMapBadge()
            TrafficLegend()
        }
    }
}

private fun trafficColor(level: TrafficLevel): Color = when (level) {
    TrafficLevel.SMOOTH -> RtColors.TrafficSmooth
    TrafficLevel.SLOW -> RtColors.TrafficSlow
    TrafficLevel.JAM -> RtColors.TrafficJam
    TrafficLevel.UNKNOWN -> RtColors.MapRoadEdge
}

/** 둥근 덩어리 (숲 · 공원 · 저수지) */
private fun blob(v: MapView, km: Double, lateral: Double, radiusKm: Double, seed: Int): Path {
    val c = Road.at(km, lateral)
    val n = 10
    val pts = (0 until n).map { i ->
        val a = 2 * PI * i / n
        val r = radiusKm * (0.72 + 0.45 * hash01(seed, i))
        v.screen(V(c.x + cos(a) * r * 1.3, c.y + sin(a) * r))
    }
    fun mid(p: Offset, q: Offset) = Offset((p.x + q.x) / 2, (p.y + q.y) / 2)
    val path = Path()
    var current = mid(pts.last(), pts[0])
    path.moveTo(current.x, current.y)
    for (i in 0 until n) {
        val control = pts[i]
        val end = mid(control, pts[(i + 1) % n])
        // 2차 곡선을 3차 곡선으로 (모서리 없이 둥글게)
        val c1 = current + (control - current) * (2f / 3f)
        val c2 = end + (control - end) * (2f / 3f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
        current = end
    }
    path.close()
    return path
}

private fun DrawScope.drawAreas(v: MapView) {
    val bucket = 1.3
    for (b in floor(v.fromKm / bucket).toInt()..ceil(v.toKm / bucket).toInt()) {
        val km = b * bucket + bucket * hash01(b, 7)
        val side = if (hash01(b, 1) < 0.5) -1.0 else 1.0
        val radius = 0.25 + 0.5 * hash01(b, 3)
        var lateral = side * (0.75 + 2.3 * hash01(b, 2))
        if (abs(lateral) - radius < 0.3) lateral = side * (radius + 0.3)
        val color = if (hash01(b, 4) < 0.55) RtColors.MapForest else RtColors.MapPark
        drawPath(blob(v, km, lateral, radius, b), color)
    }
}

/** 경로를 비스듬히 가로지르는 물길 위의 점 (t: 경로에서 떨어진 거리 km) */
private fun riverPoint(km: Double, t: Double): V {
    val base = Road.point(km)
    val n = Road.normal(km)
    val tg = Road.tangent(km)
    val along = 0.35 * t + 0.22 * sin(t * 1.4)
    return V(base.x + n.x * t + tg.x * along, base.y + n.y * t + tg.y * along)
}

private fun DrawScope.drawWater(v: MapView) {
    for (r in DemoMapData.reservoirs) {
        if (r.km < v.fromKm - 2 || r.km > v.toKm + 2) continue
        val path = blob(v, r.km, r.side * r.lateralKm, r.radiusKm, (r.km * 10).roundToInt())
        drawPath(path, RtColors.MapWater)
        drawPath(path, RtColors.MapWaterEdge, style = Stroke(1.5.dp.toPx()))
    }
    for (river in DemoMapData.rivers) {
        if (river.km < v.fromKm - 3 || river.km > v.toKm + 3) continue
        val path = Path()
        var t = -4.5
        var first = true
        while (t <= 4.5) {
            val o = v.screen(riverPoint(river.km, t))
            if (first) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            first = false
            t += 0.06
        }
        val width = max((0.07 * v.pxPerKm).toFloat(), 8.dp.toPx())
        drawPath(path, RtColors.MapWaterEdge, style = Stroke(width + 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, RtColors.MapWater, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 길 방향으로 놓인 네모 (건물) */
private fun quad(v: MapView, s: Double, lateral: Double, halfAlong: Double, halfAcross: Double): Path {
    val c = Road.at(s, lateral)
    val t = Road.tangent(s)
    val n = Road.normal(s)
    val corners = listOf(
        c + t * halfAlong + n * halfAcross,
        c + t * halfAlong - n * halfAcross,
        c - t * halfAlong - n * halfAcross,
        c - t * halfAlong + n * halfAcross,
    ).map { v.screen(it) }
    return Path().apply {
        moveTo(corners[0].x, corners[0].y)
        for (i in 1 until corners.size) lineTo(corners[i].x, corners[i].y)
        close()
    }
}

private fun DrawScope.drawBlocks(v: MapView) {
    val side = DemoMapData.NATIONAL_SIDE
    for (town in DemoMapData.towns) {
        if (town.km < v.fromKm - 2 || town.km > v.toKm + 2) continue
        val base = (town.km * 100).roundToInt()
        for (i in -5..5) for (j in -3..3) {
            if (j == 0) continue
            val seed = base + i * 31 + j * 7
            if (hash01(seed, 11) < 0.32) continue
            val s = town.km + i * 0.15 + 0.04 * (hash01(seed, 12) - 0.5)
            val lateral = Road.national(s) + side * j * 0.13 + 0.03 * (hash01(seed, 13) - 0.5)
            if (abs(lateral) < 0.4) continue // 고속도로 바로 옆은 비워 둔다
            val half = 0.045 + 0.035 * hash01(seed, 14)
            val path = quad(v, s, lateral, half, half * (0.7 + 0.5 * hash01(seed, 15)))
            drawPath(path, RtColors.MapBuilding)
            drawPath(path, RtColors.MapBuildingEdge, style = Stroke(1.dp.toPx()))
        }
    }
}

/** 경로를 옆으로 가로지르는 길: 옆으로 [a] → [b] km, 진행 방향으로 [slant] 만큼 기울게 */
private fun crossPath(v: MapView, s: Double, a: Double, b: Double, slant: Double = 0.0, wiggle: Double = 0.0): Path {
    val path = Path()
    val steps = 32
    for (i in 0..steps) {
        val f = i / steps.toDouble()
        val lateral = a + (b - a) * f
        val along = s + slant * lateral + wiggle * sin(f * 6.0)
        val o = v.at(along, lateral)
        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
    }
    return path
}

private fun DrawScope.drawLocalRoads(v: MapView) {
    val side = DemoMapData.NATIONAL_SIDE
    val roads = mutableListOf<Path>()
    // 마을 길
    for (town in DemoMapData.towns) {
        if (town.km < v.fromKm - 2 || town.km > v.toKm + 2) continue
        for (d in listOf(-0.62, -0.05, 0.5)) {
            val s = town.km + d
            roads += crossPath(v, s, Road.national(s) - side * 0.45, Road.national(s) + side * 0.95, slant = 0.12)
        }
        roads += v.path(town.km - 0.8, town.km + 0.8) { Road.national(it) + side * 0.42 }
    }
    // 들판을 가로지르는 길 (고속도로 밑으로 지난다)
    val bucket = 2.4
    for (b in floor(v.fromKm / bucket).toInt()..ceil(v.toKm / bucket).toInt()) {
        val s = b * bucket + bucket * 0.8 * hash01(b, 21)
        roads += crossPath(v, s, -2.8 - hash01(b, 22), 2.8 + hash01(b, 23), slant = 0.5 * (hash01(b, 24) - 0.5), wiggle = 0.15)
    }
    val casing = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fill = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    roads.forEach { drawPath(it, RtColors.MapRoadEdge, style = casing) }
    roads.forEach { drawPath(it, RtColors.MapRoad, style = fill) }
}

/** IC: 국도에서 고속도로로 이어지는 길과 작은 고리 */
private fun icRoads(v: MapView, icKm: Double): List<Path> {
    val link = Path()
    val steps = 28
    for (i in 0..steps) {
        val f = i / steps.toDouble()
        val ease = f * f * (3 - 2 * f)
        val s = icKm + 0.4 * (1 - f)
        val lateral = Road.national(s) * (1 - ease) + DemoMapData.NATIONAL_SIDE * 0.1 * ease
        val o = v.at(s, lateral)
        if (i == 0) link.moveTo(o.x, o.y) else link.lineTo(o.x, o.y)
    }
    val loopCenter = v.at(icKm - 0.12, DemoMapData.NATIONAL_SIDE * 0.2)
    val r = (0.08 * v.pxPerKm).toFloat()
    val loop = Path().apply { addOval(Rect(loopCenter, r)) }
    return listOf(link, loop)
}

private fun DrawScope.drawNationalRoad(v: MapView) {
    val main = v.path(v.fromKm, v.toKm) { Road.national(it) }
    val links = DemoMapData.interchanges
        .filter { it.km > v.fromKm - 1 && it.km < v.toKm + 1 }
        .flatMap { icRoads(v, it.km) }
    val casing = Stroke(13.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fill = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    (links + main).forEach { drawPath(it, RtColors.MapRoadEdge, style = casing) }
    (links + main).forEach { drawPath(it, RtColors.MapRoad, style = fill) }
    // 국도 교통 (가운데 색 선)
    for (span in DemoMapData.nationalTraffic) {
        val from = max(span.fromKm, v.fromKm)
        val to = min(span.toKm, v.toKm)
        if (to <= from) continue
        drawPath(v.path(from, to) { Road.national(it) }, trafficColor(span.level).copy(alpha = 0.8f), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Butt))
    }
}

private fun DrawScope.drawExpressway(v: MapView) {
    val p = v.path(v.fromKm, v.toKm)
    drawPath(p, RtColors.MapHighwayEdge, style = Stroke(22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(p, RtColors.MapHighway, style = Stroke(16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(p, RtColors.MapRoadEdge, style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
}

/** 모임 경로: 지난 길은 옅게, 앞길은 주황 + 진행 방향 화살표. 막히는 구간은 테두리를 교통 색으로. */
private fun DrawScope.drawRoute(v: MapView, model: GroupMapModel) {
    val me = model.me ?: return
    val onRoute = model.vehicles.filter { it.onPlannedRoute }
    val start = max((onRoute.minOfOrNull { it.progressKm } ?: me.progressKm) - 0.4, v.fromKm)
    val end = v.toKm
    if (end <= start) return
    val casing = Stroke(17.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val core = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(v.path(start, end), Color.White, style = casing)
    for (c in model.congestion) {
        val from = max(me.progressKm + c.offsetM / 1000.0, start)
        val to = min(me.progressKm + (c.offsetM + c.lengthM) / 1000.0, end)
        if (to > from) drawPath(v.path(from, to), trafficColor(c.level), style = casing)
    }
    val split = me.progressKm.coerceIn(start, end)
    if (split > start) drawPath(v.path(start, split), RtColors.MapRoutePassed, style = core)
    drawPath(v.path(split, end), RtColors.Accent, style = core)

    // 진행 방향 화살표
    val spacing = 72.dp.toPx() / v.pxPerKm
    val arm = 4.5.dp.toPx()
    val stroke = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    var s = split + spacing * 0.6
    while (s < end) {
        val c = v.at(s)
        if (v.inside(c, 20f)) {
            val a = v.angleAt(s)
            val tip = Offset(c.x + cos(a) * arm * 0.6f, c.y + sin(a) * arm * 0.6f)
            val p1 = Offset(tip.x - cos(a - 0.75f) * arm * 1.5f, tip.y - sin(a - 0.75f) * arm * 1.5f)
            val p2 = Offset(tip.x - cos(a + 0.75f) * arm * 1.5f, tip.y - sin(a + 0.75f) * arm * 1.5f)
            drawPath(Path().apply { moveTo(p1.x, p1.y); lineTo(tip.x, tip.y); lineTo(p2.x, p2.y) }, Color.White, style = stroke)
        }
        s += spacing
    }
}

// ---------------- 지도 글자 ----------------

private val TownStyle = KoreanTextStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RtColors.MapLabel, letterSpacing = 0.5.sp)
private val WaterStyle = KoreanTextStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF3F79A8))
private val ChipStyle = KoreanTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RtColors.MapLabelStrong)
private val ShieldStyle = KoreanTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
private val RestStyle = KoreanTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)

/** 흰 테두리 글자 (지도 위에서도 읽히게) */
private fun DrawScope.drawHaloText(tm: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = tm.measure(text, style)
    val topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
    val d = 1.4.dp.toPx()
    for ((dx, dy) in listOf(-d to 0f, d to 0f, 0f to -d, 0f to d, -d to -d, d to d, -d to d, d to -d)) {
        drawText(layout, color = Color.White, topLeft = Offset(topLeft.x + dx, topLeft.y + dy))
    }
    drawText(layout, topLeft = topLeft)
}

/** 흰 알약 글자 (IC · 도로 이름) */
private fun DrawScope.drawChip(tm: TextMeasurer, text: String, center: Offset, style: TextStyle = ChipStyle) {
    val layout = tm.measure(text, style)
    val padH = 7.dp.toPx()
    val padV = 3.dp.toPx()
    val w = layout.size.width + padH * 2
    val h = layout.size.height + padV * 2
    val topLeft = Offset(center.x - w / 2, center.y - h / 2)
    drawRoundRect(Color.White, topLeft, Size(w, h), CornerRadius(h / 2))
    drawRoundRect(RtColors.MapRoadEdge, topLeft, Size(w, h), CornerRadius(h / 2), style = Stroke(1.dp.toPx()))
    drawText(layout, topLeft = Offset(topLeft.x + padH, topLeft.y + padV))
}

/** 도로 번호판 (고속도로·국도) */
private fun DrawScope.drawShield(tm: TextMeasurer, number: String, center: Offset): Float {
    val layout = tm.measure(number, ShieldStyle)
    val w = max(layout.size.width + 12.dp.toPx(), 30.dp.toPx())
    val h = 21.dp.toPx()
    val topLeft = Offset(center.x - w / 2, center.y - h / 2)
    drawRoundRect(RtColors.MapShield, topLeft, Size(w, h), CornerRadius(5.dp.toPx()))
    drawRoundRect(Color.White, topLeft, Size(w, h), CornerRadius(5.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
    return w
}

/**
 * 도로 이름표를 놓을 자리: 안내 카드(왼쪽 위) · 상황 공유 줄(오른쪽) · 아래 표시줄을 피하고,
 * 차 · IC · 집결지와 겹치지 않는 곳 중 먼저 맞는 곳.
 */
private fun DrawScope.labelSpot(v: MapView, model: GroupMapModel, kms: List<Double>, lateral: (Double) -> Double): Double? {
    val left = 24.dp.toPx()
    val right = v.widthPx - 190.dp.toPx()
    val top = 230.dp.toPx()
    val bottom = v.heightPx - 90.dp.toPx()
    val markers = model.vehicles.map { vh -> if (vh.onPlannedRoute) v.at(vh.progressKm) else v.at(vh.progressKm, Road.national(vh.progressKm)) }
    val busyKm = DemoMapData.interchanges.map { it.km } + model.stops.map { it.km }
    return kms.firstOrNull { km ->
        val o = v.at(km, lateral(km))
        o.x in left..right && o.y in top..bottom &&
            markers.none { (it - o).getDistance() < 90.dp.toPx() } &&
            busyKm.none { abs(it - km) < 0.9 }
    }
}

private fun DrawScope.drawMapLabels(v: MapView, tm: TextMeasurer, model: GroupMapModel) {
    val side = DemoMapData.NATIONAL_SIDE
    val span = v.toKm - v.centerKm

    // 마을 이름
    for (town in DemoMapData.towns) {
        val name = town.name ?: continue
        val o = v.at(town.km, Road.national(town.km) + side * 0.62)
        if (v.inside(o, -24.dp.toPx())) drawHaloText(tm, name, o, TownStyle)
    }
    // 물 이름
    for (river in DemoMapData.rivers) {
        val name = river.name ?: continue
        val o = v.screen(riverPoint(river.km, -1.6 * side))
        if (v.inside(o, -24.dp.toPx())) drawHaloText(tm, name, o, WaterStyle)
    }
    // IC: 고속도로 위 점 + 반대쪽에 이름
    for (ic in DemoMapData.interchanges) {
        val name = ic.name ?: continue
        val o = v.at(ic.km)
        if (!v.inside(o, -12.dp.toPx())) continue
        drawCircle(Color.White, radius = 6.5.dp.toPx(), center = o)
        drawCircle(RtColors.MapLabelStrong, radius = 6.5.dp.toPx(), center = o, style = Stroke(2.dp.toPx()))
        val away = awayFromNational(v, ic.km)
        drawChip(tm, name, Offset(o.x + away.x * 34.dp.toPx(), o.y + away.y * 34.dp.toPx()))
    }
    // 휴게소 (코스에 없는 곳은 작은 표시만)
    for (stop in model.stops) {
        if (stop.isCourse) continue
        val o = v.at(stop.km)
        if (!v.inside(o, -16.dp.toPx())) continue
        val away = awayFromNational(v, stop.km)
        val badge = Offset(o.x + away.x * 26.dp.toPx(), o.y + away.y * 26.dp.toPx())
        val r = 10.dp.toPx()
        drawRoundRect(RtColors.MapShield, Offset(badge.x - r, badge.y - r), Size(r * 2, r * 2), CornerRadius(4.dp.toPx()))
        val layout = tm.measure("휴", RestStyle)
        drawText(layout, topLeft = Offset(badge.x - layout.size.width / 2f, badge.y - layout.size.height / 2f))
        drawChip(tm, stop.name, Offset(badge.x + away.x * 44.dp.toPx(), badge.y + away.y * 30.dp.toPx()))
    }
    // 고속도로 번호판 + 이름 (비어 있는 곳)
    val candidates = (-12..12).map { v.centerKm + span * it / 16.0 }.sortedBy { abs(it - (v.centerKm + span * 0.2)) }
    val roadKm = labelSpot(v, model, candidates) { 0.0 }
    if (roadKm != null) {
        val o = v.at(roadKm)
        val away = awayFromNational(v, roadKm)
        val at = Offset(o.x + away.x * 30.dp.toPx(), o.y + away.y * 30.dp.toPx())
        val w = drawShield(tm, DemoMapData.EXPRESSWAY_NO, at)
        val nameLayout = tm.measure(DemoMapData.EXPRESSWAY, ChipStyle)
        drawChip(tm, DemoMapData.EXPRESSWAY, Offset(at.x + w / 2 + 8.dp.toPx() + nameLayout.size.width / 2f + 7.dp.toPx(), at.y))
    }
    // 국도 번호판
    val nationalKm = labelSpot(v, model, candidates.filter { roadKm == null || abs(it - roadKm) > span * 0.25 }) { Road.national(it) }
    if (nationalKm != null) {
        val o = v.at(nationalKm, Road.national(nationalKm))
        val w = drawShield(tm, DemoMapData.NATIONAL_ROAD_NO, o)
        val nameLayout = tm.measure(DemoMapData.NATIONAL_ROAD, ChipStyle)
        drawChip(tm, DemoMapData.NATIONAL_ROAD, Offset(o.x + w / 2 + 8.dp.toPx() + nameLayout.size.width / 2f + 7.dp.toPx(), o.y))
    }
}

/** 화면에서 국도 반대쪽 방향 (이름표를 비어 있는 쪽에 둔다) */
private fun awayFromNational(v: MapView, km: Double): Offset {
    val n = v.sideAt(km)
    val road = v.at(km)
    val national = v.at(km, Road.national(km))
    val dot = (national.x - road.x) * n.x + (national.y - road.y) * n.y
    return if (dot > 0) Offset(-n.x, -n.y) else n
}

// ---------------- 지도 위 표시 (Compose) ----------------

/** 좌표(px)를 중심으로 놓는 표시 */
@Composable
private fun Pin(center: Offset, size: Dp, content: @Composable BoxScope.() -> Unit) {
    val half = with(LocalDensity.current) { (size / 2).toPx() }
    Box(
        Modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .size(size),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * 크기를 모르는 글자 알약을 좌표 가까이에 놓는다 — 지도 밖으로 나가지 않게.
 * [toSide] = 0 이면 [anchor] 를 가운데로, +1 이면 [anchor] 오른쪽에, -1 이면 왼쪽에 붙인다.
 */
@Composable
private fun MapLabelAt(anchor: Offset, view: MapView, toSide: Int = 0, gap: Dp = 0.dp, content: @Composable () -> Unit) {
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    Box(
        Modifier.layout { measurable, constraints ->
            val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(p.width, p.height) {
                val rawX = when {
                    toSide > 0 -> anchor.x + gapPx
                    toSide < 0 -> anchor.x - gapPx - p.width
                    else -> anchor.x - p.width / 2f
                }
                val x = rawX.coerceIn(8f, max(8f, view.widthPx - p.width - 8f))
                val y = (anchor.y - p.height / 2f).coerceIn(8f, max(8f, view.heightPx - p.height - 8f))
                p.place(x.roundToInt(), y.roundToInt())
            }
        },
    ) { content() }
}

@Composable
private fun MapChip(text: String, modifier: Modifier = Modifier, background: Color = Color.White, foreground: Color = RtColors.MapLabelStrong, border: Color = RtColors.MapRoadEdge) {
    Text(
        text,
        modifier
            .shadow(2.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = foreground,
        maxLines = 1,
    )
}

/** 집결지·목적지. 화면 밖이면 경로가 화면을 벗어나는 곳에 방향 표시 */
@Composable
private fun StopMarkers(view: MapView, model: GroupMapModel) {
    val me = model.me ?: return
    val density = LocalDensity.current.density
    for (stop in model.stops) {
        if (!stop.isCourse || stop.km < me.progressKm - 0.3) continue
        val o = view.at(stop.km)
        val destination = stop == model.stops.lastOrNull { it.isCourse }
        if (view.inside(o, -28f * density)) {
            Pin(o, 46.dp) {
                Box(
                    Modifier.fillMaxSize().shadow(4.dp, CircleShape).clip(CircleShape)
                        .background(if (stop.isNext) RtColors.Bg else RtColors.Raised)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (destination) Icons.Outlined.Place else Icons.Outlined.Flag,
                        contentDescription = null,
                        tint = if (stop.isNext) RtColors.AccentText else Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            val away = awayFromNational(view, stop.km)
            val label = if (stop.isNext && model.nextStopDistanceKm != null) "${stop.name} · ${formatKm(model.nextStopDistanceKm)}km" else stop.name
            MapLabelAt(Offset(o.x, o.y + away.y * 14f * density), view, toSide = if (away.x >= 0) 1 else -1, gap = 30.dp) {
                MapChip(label)
            }
        } else if (stop.isNext) {
            OffscreenStop(view, me.progressKm, stop, model.nextStopDistanceKm)
        }
    }
}

/** 화면 밖에 있는 다음 집결지: 경로가 화면을 벗어나는 곳에 이름 · 거리 · 방향 */
@Composable
private fun OffscreenStop(view: MapView, fromKm: Double, stop: MapStop, distanceKm: Double?) {
    val density = LocalDensity.current.density
    val box = Rect(20f * density, 100f * density, view.widthPx - 170f * density, view.heightPx - 80f * density)
    var s = fromKm
    val step = 6.0 * density / view.pxPerKm
    var last = view.at(s)
    while (s < stop.km) {
        val o = view.at(s + step)
        if (!box.contains(o)) break
        last = o
        s += step
    }
    val angle = view.angleAt(s) * 180f / PI.toFloat()
    MapLabelAt(last, view) {
        Row(
            Modifier
                .shadow(4.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(RtColors.Bg)
                .border(2.dp, Color.White, RoundedCornerShape(999.dp))
                .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stop.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RtColors.Ink, maxLines = 1)
            if (distanceKm != null) {
                Spacer(Modifier.width(6.dp))
                Text("${formatKm(distanceKm)}km", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RtColors.AccentText)
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = RtColors.Ink,
                modifier = Modifier.size(18.dp).rotate(angle),
            )
        }
    }
}

/** 차들: 겹치면 길 옆으로 살짝 비켜 놓는다. 내 차는 주황 화살표(진행 방향). */
@Composable
private fun VehicleMarkers(view: MapView, model: GroupMapModel) {
    val density = LocalDensity.current.density
    val positions = remember(view, model) {
        val placed = mutableListOf<Offset>()
        val result = mutableMapOf<String, Offset>()
        val minGap = 44f * density
        for (vh in model.vehicles.filter { it.onPlannedRoute }.sortedByDescending { it.progressKm }) {
            val base = view.at(vh.progressKm)
            val n = view.sideAt(vh.progressKm)
            var pos = base
            for (k in listOf(0, 1, -1, 2, -2)) {
                val cand = Offset(base.x + n.x * k * 30f * density, base.y + n.y * k * 30f * density)
                if (placed.none { (it - cand).getDistance() < minGap }) {
                    pos = cand
                    break
                }
            }
            placed += pos
            result[vh.member.id] = pos
        }
        for (vh in model.vehicles.filter { !it.onPlannedRoute }) {
            result[vh.member.id] = view.at(vh.progressKm, Road.national(vh.progressKm))
        }
        result
    }

    // 가장 많이 벌어진 곳
    val behind = model.largestGapBehindId?.let { positions[it] }
    val ahead = model.largestGapAheadId?.let { positions[it] }
    if (model.largestGapKm >= 1.0 && behind != null && ahead != null) {
        val mid = Offset((behind.x + ahead.x) / 2, (behind.y + ahead.y) / 2)
        val km = model.vehicles.firstOrNull { it.member.id == model.largestGapBehindId }?.progressKm ?: view.centerKm
        val away = awayFromNational(view, km)
        MapLabelAt(Offset(mid.x + away.x * 44f * density, mid.y + away.y * 44f * density), view) {
            MapChip("${formatKm(model.largestGapKm)}km 벌어짐", background = Color(0xFFFFF4D6), foreground = Color(0xFF6E4B00), border = Color(0xFFE9C66A))
        }
    }

    // 다른 길로 가는 차 (국도 위, 점선)
    for (vh in model.vehicles.filter { !it.onPlannedRoute }) {
        val o = positions[vh.member.id] ?: continue
        if (!view.inside(o, -20f * density)) continue
        Pin(o, 42.dp) { MemberAvatar(vh.member, 42.dp, dashed = true, fill = Color.White) }
        MapLabelAt(Offset(o.x, o.y + 38f * density), view) {
            MapChip("${vh.member.name} · 다른 길", foreground = Color(vh.member.color), border = Color(vh.member.color))
        }
    }

    // 같은 길로 가는 차 (뒤에서부터 그려서 앞차가 위에)
    for (vh in model.vehicles.filter { it.onPlannedRoute && !it.isMe }.sortedBy { it.progressKm }) {
        val o = positions[vh.member.id] ?: continue
        if (!view.inside(o, 24f * density)) continue
        Pin(o, 44.dp) {
            Box(Modifier.fillMaxSize().shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White).padding(3.dp)) {
                MemberAvatar(vh.member, 38.dp)
            }
            if (vh.member.joinMode == JoinMode.PHONE) {
                // 휴대폰 참여자 (위치는 휴대폰 GPS)
                Box(
                    Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(Color.White)
                        .border(1.dp, RtColors.MapRoadEdge, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhoneAndroid, contentDescription = "휴대폰 참여", tint = RtColors.MapLabelStrong, modifier = Modifier.size(12.dp))
                }
            }
        }
    }

    // 내 차
    val me = model.me ?: return
    val o = positions[me.member.id] ?: return
    val heading = view.angleAt(me.progressKm) * 180f / PI.toFloat()
    Pin(o, 64.dp) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(RtColors.Accent.copy(alpha = 0.16f)))
        Box(
            Modifier.size(44.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Navigation,
                contentDescription = "내 차",
                tint = RtColors.Accent,
                modifier = Modifier.size(30.dp).rotate(heading + 90f),
            )
        }
    }
}

// ---------------- 지도 아래 표시 ----------------

@Composable
private fun ScaleBar(view: MapView) {
    val density = view.density
    val km = listOf(0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0).firstOrNull { it * view.pxPerKm / density >= 56 } ?: 10.0
    val widthDp = (km * view.pxPerKm / density).toFloat()
    val label = if (km < 1) "${(km * 1000).roundToInt()} m" else "${km.roundToInt()} km"
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, RtColors.MapRoadEdge, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RtColors.MapLabelStrong)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(widthDp.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(RtColors.MapLabelStrong))
    }
}

/** 실제 지도가 아니라는 표시 (항상 보이게) */
@Composable
private fun DemoMapBadge() {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(RtColors.Overlay)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = RtColors.Sub, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("시연용 지도 · 실제 위치와 다를 수 있어요", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RtColors.Ink, maxLines = 1)
    }
}

@Composable
private fun TrafficLegend() {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, RtColors.MapRoadEdge, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("교통", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RtColors.MapLabel)
        for (level in listOf(TrafficLevel.SMOOTH, TrafficLevel.SLOW, TrafficLevel.JAM)) {
            Spacer(Modifier.width(9.dp))
            Box(Modifier.size(width = 14.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(trafficColor(level)))
            Spacer(Modifier.width(4.dp))
            Text(level.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RtColors.MapLabelStrong)
        }
    }
}
