package com.magictap.ui

/**
 * Navigation destinations. An empty id argument means "create new" (the editors treat
 * blank as null).
 */
object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"

    const val ARG_PROFILE_ID = "profileId"
    const val ARG_PC_ID = "pcId"

    const val PROFILE_EDIT = "profile_edit"
    const val PROFILE_EDIT_PATTERN = "$PROFILE_EDIT?$ARG_PROFILE_ID={$ARG_PROFILE_ID}"
    fun profileNew() = "$PROFILE_EDIT?$ARG_PROFILE_ID="
    fun profileEdit(profileId: String) = "$PROFILE_EDIT?$ARG_PROFILE_ID=$profileId"

    const val PC_EDIT = "pc_edit"
    const val PC_EDIT_PATTERN = "$PC_EDIT/{$ARG_PROFILE_ID}?$ARG_PC_ID={$ARG_PC_ID}"
    fun pcNew(profileId: String) = "$PC_EDIT/$profileId?$ARG_PC_ID="
    fun pcEdit(profileId: String, pcId: String) = "$PC_EDIT/$profileId?$ARG_PC_ID=$pcId"
}
