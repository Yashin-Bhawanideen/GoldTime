package com.goldtime.app.ui.browse

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.data.AssetCatalog
import com.goldtime.app.data.CartRepository
import com.goldtime.app.data.FeaturedAsset
import com.goldtime.app.ui.components.RemoteImage
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

@Composable
fun AssetDetailScreen(
    assetId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val asset = AssetCatalog.find(assetId)

    if (asset == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GoldColors.Background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Item not found.", color = GoldColors.TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Go back",
                color = GoldColors.Gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onBack)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            RemoteImage(asset.imageUrl, Modifier.fillMaxSize())

            Box(
                modifier = Modifier
                    .padding(top = 40.dp, start = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1E1A10))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    asset.category,
                    color = GoldColors.Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                asset.name,
                color = Color.White,
                fontFamily = HeadingFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Get Quote",
                color = GoldColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                DetailStat(
                    label = "WEIGHT",
                    value = asset.weight,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                DetailStat(
                    label = "PURITY",
                    value = asset.purity,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                DetailStat(
                    label = "CONDITION",
                    value = asset.condition,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                DetailStat(
                    label = "STOCK",
                    value = asset.stock,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                asset.description,
                color = GoldColors.TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GoldColors.Gold)
                    .clickable {
                        Toast.makeText(context, "Quote request sent", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "GET QUOTE",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .clickable {
                        CartRepository.addItem(
                            FeaturedAsset(
                                id = asset.id,
                                name = asset.name,
                                imageUrl = asset.imageUrl,
                                cta = "Get Quote"
                            )
                        )
                        Toast.makeText(context, "${asset.name} added to cart", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ADD TO CART",
                    color = GoldColors.Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161616))
            .padding(14.dp)
    ) {
        Text(label, color = GoldColors.TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}