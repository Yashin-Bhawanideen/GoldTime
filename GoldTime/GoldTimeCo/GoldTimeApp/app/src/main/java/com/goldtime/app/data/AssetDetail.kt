package com.goldtime.app.data

//data class: Kotlin automatically generates equals, hashCode, toString and copy for classes that only hold data
//holds everything the asset detail screen shows for one product
data class AssetDetail(
    val id: String,
    val name: String,
    val category: String,
    val imageUrl: String,
    val weight: String,
    val purity: String,
    val condition: String,
    val stock: String,
    val description: String
)
//object makes this a singleton, so the whole app reads the same catalog
object AssetCatalog {
    // TEMPORARY static data, matching BrowseScreen's temp-1/temp-2 ids.
    // Replace with API-backed data once the backend returns these fields.
    private val assets = listOf(
        AssetDetail(
            id = "temp-1",
            name = "1/2 oz Gold Krugerrand",
            category = "Bullion · Krugerrands",
            imageUrl = "",
            weight = "1 oz",
            purity = "22ct / 91.67%",
            condition = "New",
            stock = "12 available",
            description = "South African 1oz gold Krugerrand supplied in investment-grade condition."
        ),
        AssetDetail(
            id = "temp-2",
            name = "1oz Gold Bar",
            category = "Bullion · Bars",
            imageUrl = "",
            weight = "1 oz",
            purity = "24ct / 99.9%",
            condition = "New",
            stock = "8 available",
            description = "Investment-grade 1oz gold bar, cast and sealed with assay certificate."
        )
    )
//looks up an asset by its ID
    //returns the first match, or null if there is none, so the caller must handle a missing asset
    fun find(id: String): AssetDetail? = assets.firstOrNull { it.id == id }
}
