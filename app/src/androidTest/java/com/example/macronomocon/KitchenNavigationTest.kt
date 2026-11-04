package com.example.macronomocon

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.macronomocon.data.mock.MockAppContainer
import com.example.macronomocon.ui.MacronomoconApp
import com.example.macronomocon.ui.theme.MacronomOconTheme
import org.junit.Rule
import org.junit.Test

class KitchenNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private fun text(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun demoFlowAddsAnIngredientAndOpensNestedRecipeAndProfileViews() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            MacronomOconTheme { MacronomoconApp(MockAppContainer.getInstance(context.resources)) }
        }
        compose.onNodeWithText(text(R.string.nav_pantry)).performClick()
        compose.onNodeWithText(text(R.string.add_ingredient)).performClick()
        compose.onNodeWithText(text(R.string.scan_barcode)).performClick()
        compose.onNode(
            hasText(text(R.string.demo_rice)) and hasAnyAncestor(isDialog())
        ).performClick()
        compose.onNodeWithText(text(R.string.quantity_label)).performTextInput("100")
        compose.onNodeWithText(text(R.string.unit_label)).performTextInput("g")
        compose.onNodeWithText(text(R.string.expiry_label)).performScrollTo().performTextInput("7")
        compose.onNodeWithText(text(R.string.add_to_pantry)).performClick()
        compose.onNodeWithText(text(R.string.nav_recipes)).performClick()
        scrollToText(R.string.demo_bowl).performClick()
        compose.onNodeWithText(
            text(R.string.ingredients_title)
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        compose.onNodeWithText(text(R.string.nav_profile)).performClick()
        compose.onNodeWithText(text(R.string.demo_name)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.browse_saved)).performClick()
        scrollToText(R.string.demo_salad).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nav_kitchen)).performClick()
        scrollToText(R.string.demo_bowl).assertIsDisplayed()
    }

    /** Lazy lists compose only visible rows, so scroll the list itself until the row exists. */
    private fun scrollToText(id: Int): SemanticsNodeInteraction {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(id)))
        return compose.onNodeWithText(text(id))
    }
}
