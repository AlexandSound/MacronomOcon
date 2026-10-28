package com.example.macronomocon.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import com.example.macronomocon.data.mock.mockSeed
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.recipe.RecipeState
import com.example.macronomocon.ui.theme.MacronomOconTheme

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KitchenPreview() {
    val seed = mockSeed(LocalResources.current)
    MacronomOconTheme {
        KitchenScreen(seed.pantryItems, ViewState(RecipeState(seed.recipes)), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KitchenDarkPreview() {
    val seed = mockSeed(LocalResources.current)
    MacronomOconTheme(darkTheme = true) {
        KitchenScreen(seed.pantryItems, ViewState(RecipeState(seed.recipes)), {}, {}, {}, {})
    }
}
