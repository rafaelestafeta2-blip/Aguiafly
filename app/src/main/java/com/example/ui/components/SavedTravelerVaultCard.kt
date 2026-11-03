package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SavedTravelerProfile

@Composable
fun SavedTravelerVaultCard(
    savedProfile: SavedTravelerProfile?,
    saveToVaultOnCheckout: Boolean,
    onToggleSaveToVault: (Boolean) -> Unit,
    onSaveNowToVault: () -> Unit,
    onDeleteVaultProfile: () -> Unit,
    canSaveNow: Boolean,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFFFD54F)
    val hasSavedVault = savedProfile != null && savedProfile.isPopulated

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (hasSavedVault) gold.copy(alpha = 0.65f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("saved_traveler_vault_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasSavedVault) gold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (hasSavedVault) Icons.Default.CheckCircle else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (hasSavedVault) gold else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (hasSavedVault) {
                                "Cofre de Viajante Ativo • Preenchimento Automático"
                            } else {
                                "Cofre de Viajante AguiaFly"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSavedVault) gold else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (hasSavedVault) {
                                val maskedDoc = com.example.domain.security.PassportSecurityValidator.maskDocumentNumber(
                                    savedProfile?.documentNumber
                                )
                                "Passaporte $maskedDoc (protegido) e contato carregados da sua conta"
                            } else {
                                "Guarde seu passaporte e telefone com segurança para as próximas viagens"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Salvar dados automaticamente para próximas compras",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Protegido na sua conta AguiaFly (dados de cartão nunca são armazenados)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = saveToVaultOnCheckout,
                    onCheckedChange = onToggleSaveToVault,
                    modifier = Modifier.testTag("toggle_save_traveler_vault_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSaveNowToVault,
                    enabled = canSaveNow,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("save_traveler_vault_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasSavedVault) "Atualizar Meu Cofre" else "Salvar no Cofre Agora",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasSavedVault) {
                    TextButton(
                        onClick = onDeleteVaultProfile,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("delete_traveler_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Limpar Cofre",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpar")
                    }
                }
            }
        }
    }
}
