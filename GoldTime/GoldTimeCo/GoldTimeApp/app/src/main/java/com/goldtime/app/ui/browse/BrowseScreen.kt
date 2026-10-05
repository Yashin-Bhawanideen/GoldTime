package com.goldtime.app.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.data.FeaturedAsset
import com.goldtime.app.ui.components.RemoteImage
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onAssetClick: (String) -> Unit
) {
    val assets = listOf(
        FeaturedAsset(
            id = "temp-1",
            name = "Gold Krugerrand",
            imageUrl = "",
            cta = "GET A QUOTE"
        ),
        FeaturedAsset(
            id = "temp-2",
            name = "1oz Gold Bar",
            imageUrl = "",
            cta = "GET A QUOTE"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldColors.TextPrimary
                )
            }

            Text(
                text = "BROWSE",
                color = GoldColors.Gold,
                fontFamily = HeadingFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = assets,
                key = { it.id }
            ) { asset ->
                BrowseAssetCard(
                    asset = asset,
                    onClick = { onAssetClick(asset.id) }
                )
            }
        }
    }
}

@Composable
private fun BrowseAssetCard(
    asset: FeaturedAsset,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF161616))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteImage(
            asset.imageUrl,
            Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(14.dp))
        )

        Spacer(Modifier.size(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = asset.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = asset.cta,
                color = GoldColors.Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}