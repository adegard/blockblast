package com.example.blockblast.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.example.blockblast.game.GameEngine
import com.example.blockblast.game.Piece
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    companion object {
        private const val GRID = 8
    }

    var bestScore: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private val engine = GameEngine()

    private val density: Float = resources.displayMetrics.density

    private fun dp(v: Number): Float = v.toFloat() * density

    // ---- paints ----
    private val bgPaint = Paint().apply { color = 0xFF0F1216.toInt() }
    private val boardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A2028.toInt() }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF262E38.toInt() }
    private val cellStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2E3844.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1)
    }
    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blockShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint().apply { color = 0xB3000000.toInt() }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A2028.toInt() }
    private val resetRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5A6472.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
    }
    private val resetFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF232B36.toInt()
        style = Paint.Style.FILL
    }
    private val resetArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5A6472.toInt()
        style = Paint.Style.FILL
    }
    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B9EFF.toInt() }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = dp(18)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7686.toInt()
        textSize = dp(13)
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.12f
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF2F5FA.toInt()
        textSize = dp(38)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val bestValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF2F5FA.toInt()
        textSize = dp(24)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val comboTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD23F.toInt()
        textSize = dp(15)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val floatTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD23F.toInt()
        textSize = dp(22)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val gameOverTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF2F5FA.toInt()
        textSize = dp(34)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val gameOverScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD23F.toInt()
        textSize = dp(40)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8A94A3.toInt()
        textSize = dp(14)
        textAlign = Paint.Align.CENTER
    }

    // ---- metrics ----
    private var headerTop = 0f
    private var headerH = dp(96)
    private var comboBarTop = 0f
    private var comboBarH = dp(24)
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardSize = 0f
    private var cell = 0f
    private var trayTop = 0f
    private var trayBottom = 0f
    private val slotLeft = FloatArray(3)
    private var slotW = 0f
    private var topInset = 0f
    private var bottomInset = 0f
    private var resetCx = 0f
    private var resetCy = 0f
    private var resetR = dp(17)
    private var playAgainRect = RectF()

    private val roundRect = RectF()

    // ---- interaction state ----
    private var dragging = false
    private var dragSlot = -1
    private var dragPiece: Piece? = null
    private var dragX = 0f
    private var dragY = 0f
    private var snapRow = -1
    private var snapCol = -1
    private var snapValid = false
    private var ghostVisible = false

    // ---- animation state ----
    private var clearFlashStart = 0L
    private val flashCells = ArrayList<FlashCell>()
    private val floatingTexts = ArrayList<FloatingText>()
    private var framePending = false

    private data class FlashCell(val row: Int, val col: Int, val color: Int)
    private data class FloatingText(val text: String, val x: Float, val y: Float, val start: Long)

    private val needsAnim: Boolean
        get() = clearFlashActive() ||
            floatingTexts.isNotEmpty() ||
            (engine.combo > 0 && SystemClock.uptimeMillis() <= engine.comboDeadlineMs) ||
            dragging ||
            engine.gameOver

    init {
        isClickable = true
        setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                topInset = bars.top.toFloat()
                bottomInset = bars.bottom.toFloat()
            } else {
                @Suppress("DEPRECATION")
                topInset = insets.systemWindowInsetTop.toFloat()
                @Suppress("DEPRECATION")
                bottomInset = insets.systemWindowInsetBottom.toFloat()
            }
            requestLayout()
            insets
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = dp(16)
        val gap = dp(12)
        val trayH = dp(150)
        val wF = w.toFloat()
        val hF = h.toFloat()

        headerTop = topInset
        headerH = dp(96)
        comboBarTop = headerTop + headerH + dp(4)
        comboBarH = dp(24)

        val availableH = hF - comboBarTop - comboBarH - trayH - bottomInset - pad - gap
        val availableW = wF - pad * 2
        cell = min(availableW, availableH) / GRID
        boardSize = cell * GRID
        boardLeft = (wF - boardSize) / 2f
        boardTop = comboBarTop + comboBarH + pad

        trayTop = boardTop + boardSize + gap
        trayBottom = hF - bottomInset - pad

        slotW = (wF - pad * 2 - gap * 2) / 3f
        for (i in 0 until 3) slotLeft[i] = pad + i * (slotW + gap)

        resetCx = wF - pad - dp(4) - resetR
        resetCy = headerTop + headerH * 0.5f

        val bw = dp(220)
        val bh = dp(52)
        playAgainRect = RectF(
            (wF - bw) / 2f,
            hF * 0.60f,
            (wF + bw) / 2f,
            hF * 0.60f + bh,
        )
    }

    // ================= drawing =================

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        drawHeader(canvas)
        drawComboBar(canvas, now)
        drawBoard(canvas)
        drawBlocks(canvas)
        drawClearFlash(canvas, now)
        drawGhost(canvas)
        drawTray(canvas)
        drawDraggedPiece(canvas)
        drawFloatingTexts(canvas, now)
        if (engine.gameOver) drawGameOver(canvas)

        if (needsAnim) scheduleNextFrame()
    }

    private fun drawHeader(canvas: Canvas) {
        val yLabel = headerTop + headerH * 0.42f
        val yValue = headerTop + headerH * 0.82f
        val pad = dp(16)

        canvas.drawText("SCORE", width / 2f, yLabel, labelPaint)
        val fontSize = if (engine.score >= 10000000) dp(30) else if (engine.score >= 1000000) dp(34) else dp(38)
        scorePaint.textSize = fontSize
        canvas.drawText(engine.score.toString(), width / 2f, yValue, scorePaint)

        canvas.drawText("BEST", pad, yLabel, labelPaint)
        bestValuePaint.textSize = if (bestScore >= 10000000) dp(18) else dp(24)
        canvas.drawText(bestScore.toString(), pad, yValue, bestValuePaint)

        drawResetButton(canvas)
    }

    private fun drawResetButton(canvas: Canvas) {
        canvas.drawCircle(resetCx, resetCy, resetR, resetFillPaint)
        canvas.drawCircle(resetCx, resetCy, resetR - dp(2), resetRingPaint)
        val r = resetR - dp(6)
        val rect = RectF(resetCx - r, resetCy - r, resetCx + r, resetCy + r)
        canvas.drawArc(rect, -30f, 285f, false, resetRingPaint)
        val a = (-30f) * (Math.PI / 180.0)
        val tx = resetCx + r * cos(a).toFloat()
        val ty = resetCy + r * sin(a).toFloat()
        val path = Path()
        path.moveTo(tx, ty)
        path.lineTo(tx + dp(7), ty - dp(1))
        path.lineTo(tx + dp(2), ty + dp(5))
        path.close()
        canvas.drawPath(path, resetArrowPaint)
    }

    private fun drawComboBar(canvas: Canvas, now: Long) {
        if (engine.combo <= 0 || now > engine.comboDeadlineMs) return
        val remaining = (engine.comboDeadlineMs - now) / GameEngine.COMBO_WINDOW_MS.toFloat()
        val w = dp(110)
        val x = width / 2f
        val left = x - w / 2f
        val y = comboBarTop + dp(2)
        val h = dp(8)
        val track = RectF(left, y, left + w, y + h)
        val fill = RectF(left, y, left + w * remaining, y + h)

        canvas.drawText("COMBO x${engine.combo}", x, comboBarTop + comboBarH - dp(2), comboTextPaint)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF232B36.toInt() }
        canvas.drawRoundRect(track, h / 2f, h / 2f, bg)
        val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (remaining < 0.3f) 0xFFFF5963.toInt() else 0xFFFFD23F.toInt()
        }
        canvas.drawRoundRect(fill, h / 2f, h / 2f, fg)
    }

    private fun drawBoard(canvas: Canvas) {
        roundRect.set(boardLeft - dp(6), boardTop - dp(6), boardLeft + boardSize + dp(6), boardTop + boardSize + dp(6))
        canvas.drawRoundRect(roundRect, dp(16), dp(16), boardBgPaint)
        for (r in 0 until GRID) {
            for (c in 0 until GRID) {
                drawCell(canvas, r, c)
            }
        }
    }

    private fun drawCell(canvas: Canvas, row: Int, col: Int) {
        val gap = cell * 0.08f
        val left = boardLeft + col * cell + gap
        val top = boardTop + row * cell + gap
        val size = cell - gap * 2
        roundRect.set(left, top, left + size, top + size)
        canvas.drawRoundRect(roundRect, size * 0.28f, size * 0.28f, cellPaint)
    }

    private fun drawBlocks(canvas: Canvas) {
        for (r in 0 until GRID) {
            for (c in 0 until GRID) {
                val color = engine.grid[r][c]
                if (color != 0) drawBlock(canvas, boardLeft + c * cell, boardTop + r * cell, cell, color, 255)
            }
        }
    }

    private fun drawClearFlash(canvas: Canvas, now: Long) {
        if (!clearFlashActive()) return
        val t = (now - clearFlashStart) / 220f
        val alpha = (255 * (1f - t)).toInt().coerceIn(0, 255)
        for (fc in flashCells) {
            drawBlock(canvas, boardLeft + fc.col * cell, boardTop + fc.row * cell, cell, fc.color, alpha)
        }
    }

    private fun clearFlashActive(): Boolean {
        if (flashCells.isEmpty()) return false
        return SystemClock.uptimeMillis() - clearFlashStart < 220
    }

    private fun drawGhost(canvas: Canvas) {
        val piece = dragPiece ?: return
        if (!ghostVisible) return
        val base = if (snapValid) piece.color else 0xFFFF5252.toInt()
        for (cellPos in piece.cells) {
            val left = boardLeft + (snapCol + cellPos.col) * cell
            val top = boardTop + (snapRow + cellPos.row) * cell
            drawBlock(canvas, left, top, cell, base, if (snapValid) 110 else 130)
        }
    }

    private fun drawTray(canvas: Canvas) {
        val pad = dp(10)
        for (i in 0 until 3) {
            roundRect.set(slotLeft[i], trayTop, slotLeft[i] + slotW, trayBottom)
            canvas.drawRoundRect(roundRect, dp(16), dp(16), panelPaint)
            if (i >= engine.tray.size) continue
            if (i == dragSlot) continue
            val piece = engine.tray[i]
            drawPieceCentered(canvas, piece, slotLeft[i] + slotW / 2f, (trayTop + trayBottom) / 2f, scaleForTray(piece), 255)
        }
    }

    private fun scaleForTray(piece: Piece): Float {
        val maxW = slotW * 0.62f
        val maxH = (trayBottom - trayTop) * 0.52f
        return min(1f, min(maxW / (piece.w * cell), maxH / (piece.h * cell)))
    }

    private fun drawDraggedPiece(canvas: Canvas) {
        val piece = dragPiece ?: return
        val alpha = if (ghostVisible && !snapValid) 160 else 255
        drawPieceCentered(canvas, piece, dragX, dragY, 1f, alpha)
    }

    private fun drawPieceCentered(canvas: Canvas, piece: Piece, cx: Float, cy: Float, scale: Float, alpha: Int) {
        val w = piece.w * cell * scale
        val h = piece.h * cell * scale
        val left = cx - w / 2f
        val top = cy - h / 2f
        for (cellPos in piece.cells) {
            val x = left + cellPos.col * cell * scale
            val y = top + cellPos.row * cell * scale
            drawBlock(canvas, x, y, cell * scale, piece.color, alpha)
        }
    }

    private fun drawBlock(canvas: Canvas, left: Float, top: Float, size: Float, color: Int, alpha: Int) {
        val gap = size * 0.10f
        val x = left + gap
        val y = top + gap
        val s = size - gap * 2
        val r = size * 0.24f
        val a = (alpha * 255).shr(8) and 0xFF

        blockShadowPaint.alpha = (38 * a) / 255
        canvas.drawRoundRect(x + dp(1.5f), y + dp(2.5f), x + s + dp(1.5f), y + s + dp(2.5f), r, r, blockShadowPaint)

        blockPaint.color = (color and 0x00FFFFFF) or (a shl 24)
        canvas.drawRoundRect(x, y, x + s, y + s, r, r, blockPaint)

        val clip = Path().apply { addRoundRect(x, y, x + s, y + s, r, r, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        highlightPaint.color = 0xFFFFFF or ((a / 3) shl 24)
        canvas.drawRect(x, y, x + s, y + s * 0.45f, highlightPaint)
        shadePaint.color = (a / 4) shl 24
        canvas.drawRect(x, y + s * 0.72f, x + s, y + s, shadePaint)
        canvas.restore()
    }

    private fun drawFloatingTexts(canvas: Canvas, now: Long) {
        val it = floatingTexts.iterator()
        while (it.hasNext()) {
            val ft = it.next()
            val t = (now - ft.start) / 800f
            if (t >= 1f) {
                it.remove()
                continue
            }
            floatTextPaint.alpha = (255 * (1f - t)).toInt().coerceIn(0, 255)
            canvas.drawText(ft.text, ft.x, ft.y - 48 * t, floatTextPaint)
        }
        floatTextPaint.alpha = 255
    }

    private fun drawGameOver(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        val cy = height * 0.36f
        canvas.drawText("GAME OVER", width / 2f, cy, gameOverTitlePaint)
        canvas.drawText(engine.score.toString(), width / 2f, cy + dp(56), gameOverScorePaint)
        canvas.drawText("BEST  $bestScore", width / 2f, cy + dp(84), subTextPaint)

        canvas.drawRoundRect(playAgainRect, dp(26), dp(26), buttonBgPaint)
        val base = buttonTextPaint
        base.textSize = dp(18)
        canvas.drawText("PLAY AGAIN", playAgainRect.centerX(), playAgainRect.centerY() - (base.ascent() + base.descent()) / 2f, base)
    }

    // ================= touch =================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (engine.gameOver) {
                    if (playAgainRect.contains(x, y)) restartGame()
                    return true
                }
                if (x in resetCx - resetR - dp(8)..resetCx + resetR + dp(8) &&
                    y in resetCy - resetR - dp(8)..resetCy + resetR + dp(8)
                ) {
                    restartGame()
                    return true
                }
                val slot = slotAt(x, y)
                if (slot in 0 until engine.tray.size) {
                    dragging = true
                    dragSlot = slot
                    dragPiece = engine.tray[slot]
                    dragX = x
                    dragY = y
                    updateSnap()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    dragX = x
                    dragY = y
                    updateSnap()
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    if (snapValid) {
                        val snapshot = Array(GRID) { engine.grid[it].copyOf() }
                        val result = engine.place(dragPiece!!, snapRow, snapCol, SystemClock.uptimeMillis())
                        if (result != null) {
                            flashCells.clear()
                            for (r in result.clearedRows)
                                for (c in 0 until GRID) flashCells.add(FlashCell(r, c, snapshot[r][c]))
                            for (c in result.clearedCols)
                                for (r in 0 until GRID) flashCells.add(FlashCell(r, c, snapshot[r][c]))
                            clearFlashStart = SystemClock.uptimeMillis()
                            if (result.points > 0) {
                                floatingTexts.add(FloatingText("+${result.points}", dragX, boardTop + snapRow * cell, clearFlashStart))
                            }
                            if (engine.score > bestScore) bestScore = engine.score
                        }
                    }
                    dragging = false
                    dragPiece = null
                    dragSlot = -1
                    ghostVisible = false
                    invalidate()
                }
            }
        }
        return true
    }

    private fun slotAt(x: Float, y: Float): Int {
        if (y < trayTop - dp(20) || y > trayBottom + dp(20)) return -1
        for (i in 0 until 3) {
            if (x >= slotLeft[i] && x <= slotLeft[i] + slotW) return i
        }
        return -1
    }

    private fun updateSnap() {
        val piece = dragPiece ?: return
        val w = piece.w * cell
        val h = piece.h * cell
        val centerX = dragX
        val centerY = dragY
        val rawRow = ((centerY - h / 2f - boardTop) / cell).roundToInt()
        val rawCol = ((centerX - w / 2f - boardLeft) / cell).roundToInt()

        val nearX = dragX >= boardLeft - w && dragX <= boardLeft + boardSize + w
        val nearY = dragY >= boardTop - h && dragY <= boardTop + boardSize + h
        if (nearX && nearY) {
            snapRow = rawRow.coerceIn(0, GRID - piece.h)
            snapCol = rawCol.coerceIn(0, GRID - piece.w)
            snapValid = engine.canPlace(piece, snapRow, snapCol)
            ghostVisible = true
        } else {
            ghostVisible = false
            snapValid = false
        }
    }

    private fun restartGame() {
        engine.restart()
        flashCells.clear()
        floatingTexts.clear()
        dragging = false
        dragPiece = null
        dragSlot = -1
        ghostVisible = false
        invalidate()
    }

    // ================= animation loop =================

    private fun scheduleNextFrame() {
        if (framePending) return
        framePending = true
        postOnAnimation {
            framePending = false
            invalidate()
            if (needsAnim) scheduleNextFrame()
        }
    }
}
