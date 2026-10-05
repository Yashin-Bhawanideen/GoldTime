using System.ComponentModel.DataAnnotations;
using Google.Cloud.Firestore;

namespace GoldTimeApi.Models;

public class RegisterRequest
{
    [Required, StringLength(80)] public string FirstName { get; set; } = "";
    [Required, StringLength(80)] public string Surname { get; set; } = "";
    [EmailAddress] public string? Email { get; set; }
    [Required, StringLength(30)] public string Phone { get; set; } = "";
}

[FirestoreData]
public class UserProfile
{
    [FirestoreProperty] public string Uid { get; set; } = "";
    [FirestoreProperty] public string FirstName { get; set; } = "";
    [FirestoreProperty] public string Surname { get; set; } = "";
    [FirestoreProperty] public string Email { get; set; } = "";
    [FirestoreProperty] public string Phone { get; set; } = "";
    [FirestoreProperty] public Timestamp CreatedAt { get; set; }
}

public record HeroDto(string Badge, string Title, string Subtitle, string? ImageUrl);
public record FeaturedAssetDto(string Id, string Name, string Cta, string? ImageUrl);
public record HomeDto(HeroDto Hero, List<FeaturedAssetDto> Featured);
