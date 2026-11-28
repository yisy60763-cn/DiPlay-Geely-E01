package com.shilapi.xcertplay.instrument.fly

import android.view.Surface

private const val MAX_RESTORE_ATTEMPTS = 3

/**
 * Moves the centre decoder onto a texture and back.
 *
 * The decoder must release the centre Surface before EGL creates a window on it, and must be
 * pointed back at that Surface before the mirror is destroyed. The callbacks arrive on the thread
 * the caller used to deliver `replaceOutput`'s result.
 */
internal class FlyOutputHandoff(
    private val replaceOutput: (Surface?, (Boolean) -> Unit) -> Unit,
    private val openMirror: (Surface, Surface, (Surface) -> Unit, () -> Unit) -> Unit,
    private val closeMirror: () -> Unit,
    /** The detach and attach results, for the picture chain. Restores are not reported. */
    private val onStep: (String, Boolean) -> Unit = { _, _ -> },
) {
    private enum class Phase { IDLE, DETACHING, OPENING, ATTACHING, SHOWING, RESTORING }

    private var center: Surface? = null
    private var generation = 0
    private var phase = Phase.IDLE
    private val afterRestore = mutableListOf<() -> Unit>()
    private var restoreExhausted = false
    private var restoreInFlight = false

    fun begin(center: Surface, cluster: Surface, onReady: () -> Unit, onFailed: () -> Unit) {
        val ticket = ++generation
        this.center = center
        phase = Phase.DETACHING
        restoreExhausted = false
        restoreInFlight = false
        replaceOutput(null) { detached ->
            if (ticket != generation) return@replaceOutput
            onStep("detach", detached)
            if (!detached) {
                fail(ticket, onFailed)
                return@replaceOutput
            }
            phase = Phase.OPENING
            openMirror(center, cluster, { texture ->
                if (ticket != generation) return@openMirror
                phase = Phase.ATTACHING
                replaceOutput(texture) { attached ->
                    if (ticket != generation) return@replaceOutput
                    onStep("attach", attached)
                    if (!attached) {
                        fail(ticket, onFailed)
                        return@replaceOutput
                    }
                    phase = Phase.SHOWING
                    onReady()
                }
            }, {
                if (ticket != generation) return@openMirror
                fail(ticket, onFailed)
            })
        }
    }

    fun end(onDone: () -> Unit) {
        if (phase == Phase.IDLE) {
            onDone()
            return
        }
        afterRestore += onDone
        if (phase == Phase.RESTORING) {
            // A previous bounded retry sequence may have exhausted. A later end() call
            // serves as an explicit recovery trigger; never close EGL without success.
            if (restoreExhausted && !restoreInFlight) {
                val back = center ?: return
                val ticket = generation
                restoreExhausted = false
                restoreCenter(ticket, back, closeAfterRestore = true)
            }
            return
        }
        val ticket = ++generation
        phase = Phase.RESTORING
        val back = center
        if (back == null) {
            closeMirror()
            finish()
            return
        }
        restoreCenter(ticket, back, closeAfterRestore = true)
    }

    private fun fail(ticket: Int, onFailed: () -> Unit) {
        if (ticket != generation) return
        afterRestore += onFailed
        phase = Phase.RESTORING
        val back = center
        if (back == null) {
            finish()
            return
        }
        restoreCenter(ticket, back, closeAfterRestore = true)
    }

    /**
     * Never tear down EGL while the decoder may still be writing into its texture.
     * A rejected restore is retried; after attempts are exhausted, the mirror is retained.
     * A later end() call can restart the bounded recovery sequence.
     */
    private fun restoreCenter(
        ticket: Int,
        back: Surface,
        closeAfterRestore: Boolean,
        attemptsLeft: Int = MAX_RESTORE_ATTEMPTS,
    ) {
        if (ticket != generation || restoreInFlight) return
        restoreInFlight = true
        replaceOutput(back) { restored ->
            if (ticket != generation) return@replaceOutput
            restoreInFlight = false
            if (!restored) {
                if (attemptsLeft > 1) {
                    restoreCenter(ticket, back, closeAfterRestore, attemptsLeft - 1)
                } else {
                    restoreExhausted = true
                }
                return@replaceOutput
            }
            restoreExhausted = false
            if (closeAfterRestore) closeMirror()
            finish()
        }
    }

    private fun finish() {
        phase = Phase.IDLE
        center = null
        restoreExhausted = false
        restoreInFlight = false
        val pending = afterRestore.toList()
        afterRestore.clear()
        pending.forEach { it() }
    }
}
