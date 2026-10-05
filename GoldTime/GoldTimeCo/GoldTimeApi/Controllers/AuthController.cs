using System.Security.Claims;
using Google.Cloud.Firestore;
using GoldTimeApi.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GoldTimeApi.Controllers;

[ApiController]
[Route("api/auth")]
[Authorize]
public class AuthController(FirestoreDb db) : ControllerBase
{
    private string? Uid => User.FindFirstValue("user_id") ?? User.FindFirstValue("sub");

    /// <summary>
    /// Called by the Android app right after Firebase Auth creates the account.
    /// The Firebase ID token is validated, then the profile is stored in Firestore: users/{uid}
    /// </summary>
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        var uid = Uid;
        if (string.IsNullOrEmpty(uid)) return Unauthorized();

        var profile = new UserProfile
        {
            Uid = uid,
            FirstName = request.FirstName.Trim(),
            Surname = request.Surname.Trim(),
            Email = User.FindFirstValue("email") ?? request.Email ?? "",
            Phone = request.Phone.Trim(),
            CreatedAt = Timestamp.GetCurrentTimestamp()
        };

        await db.Collection("users").Document(uid).SetAsync(profile);
        return Ok(profile);
    }
// GET /api/credentials
// Returns every credential belonging to the authenticated user
    [HttpGet("me")]
    public async Task<IActionResult> Me()
    {
        var uid = Uid;
        if (string.IsNullOrEmpty(uid)) return Unauthorized();

        var snapshot = await db.Collection("users").Document(uid).GetSnapshotAsync();
        if (!snapshot.Exists) return NotFound(new { message = "Profile not found." });

        return Ok(snapshot.ConvertTo<UserProfile>());
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
