package com.example.passwordvault.ui.navigation

object Routes {
    const val home = "home"
    const val settings = "settings"
    const val health = "health"
    const val folder = "folder/{folderId}"
    const val credential = "credential/{credentialId}"
    const val editor = "editor?credentialId={credentialId}&folderId={folderId}"

    fun folder(folderId: Long): String = "folder/$folderId"
    fun credential(credentialId: Long): String = "credential/$credentialId"
    fun createCredential(folderId: Long? = null): String = "editor?credentialId=-1&folderId=${folderId ?: -1}"
    fun editCredential(credentialId: Long, folderId: Long?): String =
        "editor?credentialId=$credentialId&folderId=${folderId ?: -1}"
}
