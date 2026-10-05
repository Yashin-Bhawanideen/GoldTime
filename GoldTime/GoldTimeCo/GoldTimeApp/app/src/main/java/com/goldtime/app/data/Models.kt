package com.goldtime.app.data

data class HeroContent(
    val badge: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?
)

data class FeaturedAsset(
    val id: String,
    val name: String,
    val cta: String,
    val imageUrl: String?
)

data class HomeData(
    val hero: HeroContent,
    val featured: List<FeaturedAsset>
) {
    companion object {
        /** Shown when the API can't be reached, so the screen still matches the design. */
        val Default = HomeData(
            hero = HeroContent(
                badge = "Gold Time Co.",
                title = "Crafting Futures,\nOne Ounce at a Time",
                subtitle = "Timeless assets for enduring value",
                imageUrl = null
            ),
            featured = listOf(
                FeaturedAsset("krugerrand-half", "1/2 oz Gold Krugerrand", "Get Quote", null),
                FeaturedAsset("silver-bar-100g", "100g Minted Silver Bar", "Get Quote", null),
                FeaturedAsset("gold-bar-1oz", "1oz Gold Bar", "Get Quote", null),
                FeaturedAsset("rolex-gmt-ii", "Rolex GMT Master II Men’s Watch", "Get Quote", null)
            )
        )
    }
}

class ApiException(message: String) : Exception(message)

data class UserProfile(
    val uid: String,
    val firstName: String,
    val surname: String,
    val email: String,
    val phone: String
)
