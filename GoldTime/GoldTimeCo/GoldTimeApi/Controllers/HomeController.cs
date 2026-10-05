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

    //Stored images are called from Azure blob storage to display in the app
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

   //this method allows to use only the name of the file image instead of using the full url of the imgae file
    private string? ImageUrl(string fileName)
    {
        var baseUrl = config["Images:BaseUrl"];
        if (string.IsNullOrWhiteSpace(baseUrl)) return null;

        return baseUrl.TrimEnd('/') + "/" + Uri.EscapeDataString(fileName);
    }
}
/*
 References
Gideon, 2012. Difference between ApiController and Controller in ASP.NET MVC. [Online] 
Available at: https://stackoverflow.com/questions/9494966/difference-between-apicontroller-and-controller-in-asp-net-mvc
Microsoft, 2024. Tutorial: Create a controller-based web API with ASP.NET Core. [Online] 
Available at: https://learn.microsoft.com/en-us/aspnet/core/tutorials/first-web-api?view=aspnetcore-10.0&tabs=visual-studio
Microsoft, 2026. Create web APIs with ASP.NET Core. [Online] 
Available at: https://learn.microsoft.com/en-us/aspnet/core/web-api/?view=aspnetcore-10.0


 */
