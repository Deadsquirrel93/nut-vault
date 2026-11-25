package com.example.passwordvault.core.autofill

import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.example.passwordvault.PasswordVaultApplication
import com.example.passwordvault.R
import com.example.passwordvault.domain.model.CredentialRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NutVaultAutofillService : AutofillService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val parsed = AutofillStructureParser.parse(structure)
        if (!parsed.hasTargets) {
            callback.onSuccess(null)
            return
        }

        val job = serviceScope.launch {
            runCatching {
                val app = application as PasswordVaultApplication
                val container = app.container
                val settings = container.authRepository.getSettings()
                if (!settings.protectionEnabled) {
                    callback.onSuccess(null)
                    return@launch
                }

                val isUnlocked = container.sessionManager.isUnlocked.value
                val responseBuilder = FillResponse.Builder()

                if (isUnlocked) {
                    val records = container.vaultRepository.getAllCredentialRecords()
                    val ranked = AutofillStructureParser.rankCredentials(
                        credentials = records,
                        webDomain = parsed.webDomain,
                        packageName = parsed.packageName,
                    )
                    val matching = ranked.filter {
                        AutofillStructureParser.matchScore(it, parsed.webDomain, parsed.packageName) > 0
                    }.take(5)

                    matching.forEach { credential ->
                        responseBuilder.addDataset(
                            buildCredentialDataset(
                                packageName = packageName,
                                credential = credential,
                                usernameId = parsed.usernameId,
                                passwordId = parsed.passwordId,
                            ),
                        )
                    }
                    responseBuilder.addDataset(
                        buildAuthDataset(
                            parsed = parsed,
                            title = getString(R.string.autofill_choose_from_vault),
                            subtitle = getString(R.string.app_name),
                            requestCode = request.id + 1000,
                        ),
                    )
                } else {
                    responseBuilder.addDataset(
                        buildAuthDataset(
                            parsed = parsed,
                            title = getString(R.string.autofill_unlock_title),
                            subtitle = getString(R.string.autofill_unlock_subtitle),
                            requestCode = request.id,
                        ),
                    )
                }

                callback.onSuccess(responseBuilder.build())
            }.onFailure {
                callback.onSuccess(null)
            }
        }

        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildAuthDataset(
        parsed: ParsedAutofillRequest,
        title: String,
        subtitle: String,
        requestCode: Int,
    ): Dataset {
        val authIntent = Intent(this, AutofillAuthActivity::class.java).apply {
            putExtra(AutofillAuthActivity.EXTRA_USERNAME_ID, parsed.usernameId)
            putExtra(AutofillAuthActivity.EXTRA_PASSWORD_ID, parsed.passwordId)
            putExtra(AutofillAuthActivity.EXTRA_WEB_DOMAIN, parsed.webDomain)
            putExtra(AutofillAuthActivity.EXTRA_PACKAGE_NAME, parsed.packageName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            authIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val presentation = buildPresentation(packageName, title, subtitle)
        @Suppress("DEPRECATION")
        val builder = Dataset.Builder(presentation)
            .setAuthentication(pendingIntent.intentSender)
        @Suppress("DEPRECATION")
        parsed.usernameId?.let { builder.setValue(it, null) }
        @Suppress("DEPRECATION")
        parsed.passwordId?.let { builder.setValue(it, null) }
        return builder.build()
    }

    companion object {
        fun buildCredentialDataset(
            packageName: String,
            credential: CredentialRecord,
            usernameId: AutofillId?,
            passwordId: AutofillId?,
        ): Dataset {
            val presentation = buildPresentation(
                packageName = packageName,
                title = credential.title,
                subtitle = credential.login,
            )
            @Suppress("DEPRECATION")
            val builder = Dataset.Builder(presentation)
            @Suppress("DEPRECATION")
            usernameId?.let { builder.setValue(it, AutofillValue.forText(credential.login)) }
            @Suppress("DEPRECATION")
            passwordId?.let { builder.setValue(it, AutofillValue.forText(credential.password)) }
            return builder.build()
        }

        private fun buildPresentation(
            packageName: String,
            title: String,
            subtitle: String,
        ): RemoteViews = RemoteViews(packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_title, title)
            setTextViewText(R.id.autofill_subtitle, subtitle)
        }
    }
}
