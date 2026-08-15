package com.magictap.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magictap.R
import com.magictap.ui.theme.BrandGradient

/**
 * The app wordmark: the MagicTap badge (power ring + bolt on the brand squircle) next to a
 * gradient "MagicTap" wordmark. Replaces the plain app-name text in the top bar.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "MagicTap",
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "MagicTap",
            style = MaterialTheme.typography.titleLarge.copy(
                brush = BrandGradient,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
        )
    }
}
