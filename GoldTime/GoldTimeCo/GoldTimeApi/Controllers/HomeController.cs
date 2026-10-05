using GoldTimeApi.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GoldTimeApi.Controllers;

[ApiController]
[Route("api/home")]
[Authorize]
public class HomeController(IConfiguration config) : ControllerBase
{
    private const string HeroFile = "img_background.jpg";

    [HttpGet]
    public IActionResult Get()
    {
        var hero = new HeroDto(
            Badge: "Gold Time Co.",
            Title: "Crafting Futures,\nOne Ounce at a Time",
            Subtitle: "Timeless assets for enduring value",
            ImageUrl: ImageUrl(HeroFile));

        var featured = new List<FeaturedAssetDto>
        {
            new("krugerrand-half", "1/2 oz Gold Krugerrand",  "Get Quote", ImageUrl("1-2oz-Gold-KR.png")),
            new("silver-bar-100g",  "100g Minted Silver Bar", "Get Quote", ImageUrl("Silver-bar.jpeg")),
            new("gold-bar-1oz",     "1oz Gold Bar",           "Get Quote", ImageUrl("1oz-Gold-Bar.jpg")),
            new("rolex-gmt-ii",     "Rolex GMT Master II Men’s Watch", "Get Quote", ImageUrl("rolex-men-watch.jpg")),
        };

        return Ok(new HomeDto(hero, featured));
    }

   
    private string? ImageUrl(string fileName)
    {
        var baseUrl = config["Images:BaseUrl"];
        if (string.IsNullOrWhiteSpace(baseUrl)) return null;

        return baseUrl.TrimEnd('/') + "/" + Uri.EscapeDataString(fileName);
    }
}