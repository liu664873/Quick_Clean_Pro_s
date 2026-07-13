package com.quickcleanpro.phonecleaner.feature.files.shared.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXMutedText
import com.quickcleanpro.phonecleaner.common.ui.components.buttons.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXText
import com.quickcleanpro.phonecleaner.common.ui.components.CommonResultCheckIcon
import com.quickcleanpro.phonecleaner.common.ui.components.CommonResultContent

@Composable
internal fun FileManagerResultView(
    amount: String,
    unit: String,
    caption: String,
    onNavigateTool: (AppDestination) -> Unit,
    onContinue: () -> Unit,
) {
    CommonResultContent(
        onNavigateTool = onNavigateTool,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommonResultCheckIcon(size = 45.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(amount, color = CleanXText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(unit, color = CleanXText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(caption, color = CleanXMutedText, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(24.dp))
            CleanXPrimaryButton(
                text = stringResource(R.string.file_continue_managing),
                onClick = onContinue
            )
        }
    }
}
