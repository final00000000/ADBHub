package com.zhang.adbhub.desktop.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.zhang.adbhub.desktop.utils.StringResources

@Composable
fun ScanDriveConsentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StringResources.get("scan.consent.title")) },
        text = { Text(StringResources.get("scan.consent.message")) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(StringResources.get("scan.consent.confirm"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringResources.get("scan.consent.cancel"))
            }
        }
    )
}
