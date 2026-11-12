package com.example.erp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.erp.R
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.cardBorder
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor

/**
 * The "Sobre el creador" invitation.
 *
 * It asks for two very different things and they are kept apart on purpose:
 * a follow on Instagram and a star on the GitHub repository. Both links are
 * plain links, and the marks already in the app are reused instead of new
 * artwork, so the Instagram gradient keeps its colors and the GitHub mark
 * follows the theme, exactly like the links in Settings.
 */
@Composable
fun CreatorDialog(
    onDismiss: () -> Unit,
    onShown: () -> Unit = {}
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, cardBorderColor())
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Apoya al creador de Dolita",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // The avatar is circular like a profile picture, and cropped
                // from its center: an image that is not square would otherwise
                // be squashed into the circle.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(cardContainerColor())
                        .border(1.dp, accentColor().copy(alpha = 0.35f), CircleShape)
                ) {
                    Image(
                        painter = painterResource(R.drawable.creator_avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "¡Hola! 👋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Detrás de Dolita hay una persona que disfruta creando, " +
                        "aprendiendo y desarrollando proyectos tecnológicos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Si quieres conocer un poco más sobre mí y lo que hago " +
                        "fuera de esta aplicación, te invito a seguirme en Instagram " +
                        "y a darle una estrella ⭐ a mi repositorio en GitHub.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardContainerColor(),
                    border = BorderStroke(1.dp, cardBorderColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        CreatorLinkRow(
                            icon = R.drawable.ic_instagram,
                            title = "Mi perfil de Instagram",
                            action = "Seguir",
                            // Instagram keeps its own colors: tinting the mark
                            // collapses the gradient into a flat silhouette.
                            tintToTheme = false,
                            onClick = {
                                openExternal(context, AppLinks.INSTAGRAM)
                            }
                        )
                        HorizontalDivider(color = cardBorderColor())
                        CreatorLinkRow(
                            icon = R.drawable.ic_github,
                            title = "Mi perfil de GitHub",
                            action = "Seguir",
                            tintToTheme = true,
                            onClick = {
                                openExternal(context, AppLinks.GITHUB_PROFILE)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { openExternal(context, AppLinks.INSTAGRAM) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_instagram),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Seguirme en Instagram")
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { openExternal(context, AppLinks.GITHUB) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor()),
                    border = BorderStroke(1.dp, accentColor().copy(alpha = 0.45f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = null,
                        tint = accentColor(),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dame una estrella al repositorio ⭐")
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Gracias por utilizar la aplicación ❤️",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * One destination inside the invitation: its mark, its name and what the tap
 * promises, so the row says what happens instead of only where it goes.
 */
@Composable
private fun CreatorLinkRow(
    icon: Int,
    title: String,
    action: String,
    tintToTheme: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (tintToTheme) accentColor() else Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = action,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor()
        )
    }
}

/**
 * The end of the rate board: who made it, and the door to the invitation.
 *
 * It lives at the very bottom of the scroll, where a reader finishes reading,
 * and it uses the same hairline voice as every other affordance in the app
 * instead of a filled button that would outrank the rates.
 */
@Composable
fun CreatorFooter(onOpenCreatorInfo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hecho por @yournelvi",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        val shape = RoundedCornerShape(10.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(shape)
                .border(BorderStroke(1.dp, accentColor().copy(alpha = 0.4f)), shape)
                .clickable(onClick = onOpenCreatorInfo)
                .padding(horizontal = 18.dp, vertical = 11.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = accentColor(),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Conócenos",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor()
            )
        }
    }
}
