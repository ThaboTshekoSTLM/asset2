package za.gov.municipal.ictasset.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = MunicipalGreen,
    onPrimary = Color.White,
    secondary = MunicipalBlue,
    onSecondary = Color.White,
    tertiary = MunicipalGold,
    onTertiary = MunicipalInk,
    background = MunicipalMist,
    onBackground = MunicipalInk,
    surface = Color.White,
    onSurface = MunicipalInk,
    error = MunicipalError
)

@Composable
fun ICTAssetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
