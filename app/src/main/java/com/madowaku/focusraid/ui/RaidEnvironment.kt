package com.madowaku.focusraid.ui
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Decorative scenery. Shared counts and contribution lights come from separate data. */
@Composable
internal fun RaidEnvironment(@DrawableRes resource: Int, modifier: Modifier = Modifier) {
    Image(painterResource(resource), null, modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}
