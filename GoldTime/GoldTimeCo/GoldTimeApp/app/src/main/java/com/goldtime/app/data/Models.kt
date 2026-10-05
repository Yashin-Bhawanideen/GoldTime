package com.goldtime.app.data

//data class: Kotlin automatically generates equals, hashCode, toString and copy for classes that only hold data
//the content of the banner at the top of the home screen
data class HeroContent(
    val badge: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?
)
//one featured product shown on the home screen
data class FeaturedAsset(
    val id: String,
    val name: String,
    val cta: String,
    val imageUrl: String?
)
//everything the home screen needs: the hero banner and the list of featured assets
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
//custom exception for errors from the API or the app's own checks
//its message is written for the user, and toFriendlyMessage() shows it on screen
class ApiException(message: String) : Exception(message)

data class UserProfile(
    val uid: String,
    val firstName: String,
    val surname: String,
    val email: String,
    val phone: String
)
