package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.brand.scenes.Illustration
import com.elkrrai.feather.ui.brand.scenes.SceneKind
import com.elkrrai.feather.ui.theme.Space

/**
 * The first thing a new person sees: the egg hatching, and a line each for what a task is, and what
 * Today, Upcoming, Goals and Habits are. Shown once; any way of closing it counts as seen.
 */
@Composable
fun WelcomeDialog(onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Space.x6),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // the egg cracking, the owl peeking out, the sun coming up
                Illustration(SceneKind.FirstLaunch, size = 200.dp)
                Spacer(Modifier.height(Space.x3))
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.x4))
                WelcomeLine(R.string.welcome_task_lead, R.string.welcome_task_body)
                WelcomeLine(R.string.welcome_today_lead, R.string.welcome_today_body)
                WelcomeLine(R.string.welcome_upcoming_lead, R.string.welcome_upcoming_body)
                WelcomeLine(R.string.welcome_goals_lead, R.string.welcome_goals_body)
                WelcomeLine(R.string.welcome_habits_lead, R.string.welcome_habits_body)
                Spacer(Modifier.height(Space.x4))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.welcome_go))
                }
            }
        }
    }
}

@Composable
private fun WelcomeLine(lead: Int, body: Int) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(lead)) }
            append(" ")
            append(stringResource(body))
        },
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.x1),
    )
}
