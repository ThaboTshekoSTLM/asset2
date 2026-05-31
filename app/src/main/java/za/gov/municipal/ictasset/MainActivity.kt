package za.gov.municipal.ictasset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import za.gov.municipal.ictasset.presentation.navigation.AssetApp
import za.gov.municipal.ictasset.presentation.theme.ICTAssetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ICTAssetApplication).container
        setContent {
            ICTAssetTheme {
                AssetApp(container = container)
            }
        }
    }
}
