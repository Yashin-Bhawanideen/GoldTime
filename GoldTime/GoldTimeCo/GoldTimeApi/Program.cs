using Google.Cloud.Firestore;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.IdentityModel.Tokens;
using Microsoft.AspNetCore.Authentication.JwtBearer;

var builder = WebApplication.CreateBuilder(args);

// ---- Firebase project id (appsettings.json locally, app setting Firebase__ProjectId on Azure) ----
var projectId = builder.Configuration["Firebase:ProjectId"];
if (string.IsNullOrWhiteSpace(projectId) || projectId == "YOUR_FIREBASE_PROJECT_ID")    //don't change to the actual friebase ID, this checks if the API can't find the Firebase project ID, it will throw the error message
{
    throw new InvalidOperationException(
        "Firebase:ProjectId is not set. Put your Firebase project id in appsettings.json " +
        "or set the environment variable Firebase__ProjectId.");
}

builder.Services.AddControllers();
builder.Services.AddScoped<GoldTimeApi.Services.IOrderStore, GoldTimeApi.Services.OrderStore>();
builder.Services.AddScoped<GoldTimeApi.Services.ISandboxPaymentStore, GoldTimeApi.Services.SandboxPaymentStore>();
builder.Services.AddHttpClient<GoldTimeApi.Services.PayFastService>(client => client.Timeout = TimeSpan.FromSeconds(20))
    .ConfigurePrimaryHttpMessageHandler(() => new HttpClientHandler { AllowAutoRedirect = false });

// ---- Firestore ----
// Azure:     the whole service-account JSON is stored in the app setting Firebase__CredentialsJson
// Local PC:  falls back to the GOOGLE_APPLICATION_CREDENTIALS environment variable
builder.Services.AddSingleton(_ =>
{
    var json = builder.Configuration["Firebase:CredentialsJson"];
    if (!string.IsNullOrWhiteSpace(json))
    {
        return new FirestoreDbBuilder { ProjectId = projectId, JsonCredentials = json }.Build();
    }

    return FirestoreDb.Create(projectId);
});

// ---- Firebase ID-token validation ----
var issuer = $"https://securetoken.google.com/{projectId}";
builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = issuer;
        options.MapInboundClaims = false; // keep Firebase claim names (sub, user_id, email)
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = issuer,
            ValidateAudience = true,
            ValidAudience = projectId,
            ValidateLifetime = true
        };
    });
builder.Services.AddAuthorization();

// ---- Needed when running behind Azure / Firebase Hosting / Cloud Run ----
builder.Services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = ForwardedHeaders.XForwardedFor
                             | ForwardedHeaders.XForwardedProto
                             | ForwardedHeaders.XForwardedHost;
    options.KnownNetworks.Clear();
    options.KnownProxies.Clear();
});

var app = builder.Build();

//captures the payment source before the existing forwarded-header middleware changes it
app.Use(async (context, next) =>
{
    var trusted = (builder.Configuration["PayFast:TrustedProxyAddresses"] ?? "").Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
    context.Items[GoldTimeApi.Services.PayFastSource.SourceItem] = GoldTimeApi.Services.PayFastSource.ClientIp(
        context.Connection.RemoteIpAddress, context.Request.Headers["X-Forwarded-For"].ToString(), trusted);
    await next();
});
app.UseForwardedHeaders();
app.UseStaticFiles(); // serves wwwroot/images/* at /images/*
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();
app.MapGet("/health", () => Results.Ok(new { status = "ok" }));

app.Run();
