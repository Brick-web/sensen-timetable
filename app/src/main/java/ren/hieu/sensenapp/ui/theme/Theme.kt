package ren.hieu.sensenapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SenSenLightScheme = lightColorScheme(
    primary = SenSenAccent,
    onPrimary = SenSenBg,
    background = SenSenBg,
    surface = SenSenToolbarBg,
    onBackground = SenSenTextPrimary,
    onSurface = SenSenTextPrimary,
    outline = SenSenToolbarBorder,
)

@Composable
fun SenSenSchoolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SenSenLightScheme,
        typography = Typography,
        content = content,
    )
}
