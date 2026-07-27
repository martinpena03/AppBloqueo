package com.freno.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freno.app.domain.model.BlockDecision
import com.freno.app.domain.model.BlockReason
import com.freno.app.domain.util.TimeUtils

/** Pantalla de bloqueo: neutra, factual, sin refuerzos ni animaciones. */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra(EXTRA_NAME) ?: "Esta app"
        val reason = runCatching { BlockReason.valueOf(intent.getStringExtra(EXTRA_REASON) ?: "") }
            .getOrDefault(BlockReason.NONE)
        val until = intent.getLongExtra(EXTRA_UNTIL, -1L)

        setContent {
            com.freno.app.ui.theme.FrenoTheme {
                BlockScreen(
                    name = name,
                    reason = reason,
                    until = if (until > 0) until else null,
                    onClose = { goHome() }
                )
            }
        }
    }

    override fun onBackPressed() {
        goHome()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }

    companion object {
        private const val EXTRA_NAME = "name"
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_UNTIL = "until"

        fun launch(context: Context, decision: BlockDecision.Block) {
            val i = Intent(context, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_NAME, decision.targetName)
                putExtra(EXTRA_REASON, decision.reason.name)
                putExtra(EXTRA_UNTIL, decision.availableAgainAt ?: -1L)
            }
            context.startActivity(i)
        }
    }
}

@Composable
private fun BlockScreen(
    name: String,
    reason: BlockReason,
    until: Long?,
    onClose: () -> Unit
) {
    val bg = Color(0xFF111111)
    val fg = Color(0xFFE6E6E6)
    val muted = Color(0xFF8A8A8A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "Bloqueado", color = fg, fontSize = 30.sp, textAlign = TextAlign.Center)
            Text(
                text = name,
                color = muted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = reasonText(reason),
                color = fg,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            if (until != null) {
                Text(
                    text = "Disponible nuevamente a las ${TimeUtils.formatClock(until)}",
                    color = muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = fg
                ),
                modifier = Modifier
                    .padding(top = 40.dp)
                    .width(200.dp)
            ) {
                Text("Entendido")
            }
        }
    }
}

private fun reasonText(reason: BlockReason): String = when (reason) {
    BlockReason.SCHEDULE -> "Fuera del horario permitido."
    BlockReason.COOLDOWN -> "En periodo de enfriamiento."
    BlockReason.OPEN_LIMIT -> "Alcanzaste el máximo de aperturas de hoy."
    BlockReason.TIME_LIMIT -> "Alcanzaste el tiempo máximo de hoy."
    BlockReason.SCROLL_QUOTA -> "Alcanzaste tu cuota de reels/shorts."
    BlockReason.NO_TOKENS -> "Te quedaste sin tokens por hoy."
    BlockReason.INSUFFICIENT_TOKENS -> "No te alcanzan los tokens para abrir."
    BlockReason.NONE -> "Bloqueado."
}
