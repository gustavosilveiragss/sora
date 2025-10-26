package com.sora.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sora.android.R

data class PasswordRequirement(
    val text: String,
    val isMet: Boolean
)

@Composable
fun PasswordRequirementsIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val requirements = listOf(
        PasswordRequirement(
            stringResource(R.string.password_req_min_length),
            password.length >= 6
        ),
        PasswordRequirement(
            stringResource(R.string.password_req_uppercase),
            password.any { it.isUpperCase() }
        ),
        PasswordRequirement(
            stringResource(R.string.password_req_lowercase),
            password.any { it.isLowerCase() }
        ),
        PasswordRequirement(
            stringResource(R.string.password_req_number),
            password.any { it.isDigit() }
        ),
        PasswordRequirement(
            stringResource(R.string.password_req_special),
            password.any { !it.isLetterOrDigit() }
        )
    )

    val leftColumnRequirements = requirements.take(3)
    val rightColumnRequirements = requirements.drop(3)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leftColumnRequirements.forEach { requirement ->
                PasswordRequirementItem(requirement = requirement)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rightColumnRequirements.forEach { requirement ->
                PasswordRequirementItem(requirement = requirement)
            }
        }
    }
}

@Composable
private fun PasswordRequirementItem(
    requirement: PasswordRequirement,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (requirement.isMet) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (requirement.isMet) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = requirement.text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (requirement.isMet) FontWeight.Medium else FontWeight.Normal
            ),
            color = if (requirement.isMet) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}