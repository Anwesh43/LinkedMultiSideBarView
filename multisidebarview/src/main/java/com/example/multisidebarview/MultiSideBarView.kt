package com.example.multisidebarview

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context
import android.app.Activity

val colors : Array<Int> = arrayOf(
    "#F44336",
    "#2196F3",
    "#8BC34A",
    "#E91E63",
    "#03A9F4"
).map {
    Color.parseColor(it)
}.toTypedArray()
val delay : Long = 20
val sizeFactor : Float = 8.9f
val backColor : Int = Color.parseColor("#BDBDBD")
val parts : Int = 4
val scGap : Float = 0.02f / parts


fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawMultiSideBar(scale : Float, w : Float, h : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / sizeFactor
    val sf : Float = scale.sinify()
    save()
    translate(w / 2, h / 2)
    for (j in 0..1) {
        save()
        scale(1f, 1f - 2 * j)
        translate(0f, h / 2  * (1 - sf.divideScale(j, parts)))
        drawRect(RectF(-w / 2, 0f, w / 2, size), paint)
        restore()
    }
    for (j in 0..1) {
        save()
        scale(1f - 2 * j, 1f - 2 * j)
        translate(w / 2 - w * sf.divideScale(2 + j, parts), size)
        drawRect(RectF(0f, 0f, w, size), paint)
        restore()
    }
    restore()
}
fun Canvas.drawMSBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    drawMultiSideBar(scale, w, h, paint)
}

class MultiSideBarView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class MSBNode(var i : Int, val state : State = State()) {

        private var next : MSBNode? = null
        private var prev : MSBNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = MSBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawMSBNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : MSBNode {
            var curr : MSBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class MultiSideBar(var i : Int) {

        private var curr : MSBNode = MSBNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : MultiSideBarView) {

        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val animator : Animator = Animator(view)
        private val msb : MultiSideBar = MultiSideBar(0)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            msb.draw(canvas, paint)
            animator.animate {
                msb.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            msb.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : MultiSideBarView {
            val view : MultiSideBarView = MultiSideBarView(activity)
            activity.setContentView(view)
            return view
        }
    }
}