package com.example.macronomocon.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.macronomocon.R

/** Resolve at the UI boundary so locale changes never leave stale translated text in state. */
enum class UiMessage(@param:StringRes val resourceId: Int) {
    ERROR_LOAD_YOUR_ACCOUNT(R.string.error_load_your_account),
    ERROR_REGISTER(R.string.error_register),
    ERROR_SIGN_IN(R.string.error_sign_in),
    ERROR_VERIFY_THE_CODE(R.string.error_verify_the_code),
    ERROR_SIGN_OUT(R.string.error_sign_out),
    ERROR_UPDATE_YOUR_ACCOUNT(R.string.error_update_your_account),
    ERROR_SAVE_PREFERENCES(R.string.error_save_preferences),
    ERROR_DELETE_YOUR_ACCOUNT(R.string.error_delete_your_account),
    ERROR_LOAD_COMMENTS(R.string.error_load_comments),
    ERROR_PUBLISH_THIS_COMMENT(R.string.error_publish_this_comment),
    ERROR_DELETE_THIS_COMMENT(R.string.error_delete_this_comment),
    ERROR_LIKE_THIS_COMMENT(R.string.error_like_this_comment),
    ERROR_LOAD_YOUR_PANTRY(R.string.error_load_your_pantry),
    ERROR_IDENTIFY_BARCODE(R.string.error_identify_barcode),
    ERROR_SEARCH_INGREDIENTS(R.string.error_search_ingredients),
    ERROR_ADD_THIS_PANTRY_ITEM(R.string.error_add_this_pantry_item),
    ERROR_REMOVE_PANTRY_ITEMS(R.string.error_remove_pantry_items),
    ERROR_REMOVE_EXPIRED_ITEMS(R.string.error_remove_expired_items),
    ERROR_RECORD_THIS_MEAL(R.string.error_record_this_meal),
    ERROR_SEARCH_RECIPES(R.string.error_search_recipes),
    ERROR_RETRIEVE_RECIPES(R.string.error_retrieve_recipes),
    ERROR_LOAD_THIS_RECIPE(R.string.error_load_this_recipe),
    ERROR_SAVE_THIS_FAVORITE(R.string.error_save_this_favorite),
    ERROR_BLOCK_THIS_RECIPE(R.string.error_block_this_recipe),
    ERROR_SAVE_THIS_RECIPE(R.string.error_save_this_recipe),
    ERROR_SHARE_THIS_RECIPE(R.string.error_share_this_recipe);

    @Composable
    fun resolve(): String = stringResource(resourceId)
}
