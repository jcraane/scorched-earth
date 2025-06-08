package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame

@Composable
fun PlayerControls(
    game: ScorchedEarthGame,
    modifier: Modifier = Modifier.Companion,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        PlayerAndWindInfo(game)

        Spacer(modifier = Modifier.Companion.height(8.dp))

        AngleControl(game)

        PowerControl(game)

        MissileSelector(game)

        Spacer(modifier = Modifier.Companion.height(8.dp))

        ShieldSelector(game)

        Spacer(modifier = Modifier.Companion.height(8.dp))

        FireButton(game)
    }

}
