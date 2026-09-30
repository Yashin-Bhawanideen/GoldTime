using System.ComponentModel.DataAnnotations;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using GoldTimeApi.Models;

namespace GoldTimeApi.Services;

public class CheckoutException(int statusCode, string message) : Exception(message)
{
    public int StatusCode { get; } = statusCode;
}

public static class OrderPricing
{
    private static readonly string[] Provinces = ["Eastern Cape", "Free State", "Gauteng",
        "KwaZulu-Natal", "Limpopo", "Mpumalanga", "North West", "Northern Cape", "Western Cape"];

    public static void Validate(CreateOrderRequest request)
    {
        //checks the same delivery rules on the server, even if the app is bypassed
        if (!Valid(request) || request.RequestId == Guid.Empty || request.Items.Any(x => x is null || !Valid(x))
            || !Valid(request.Delivery) || request.Items.Select(x => x.ProductId).Distinct().Count() != request.Items.Count)
            throw new CheckoutException(400, "Provide valid items, delivery details, payment method and a request ID. Each product must appear once.");

        var phone = string.Concat(request.Delivery.Phone.Where(c => !" ()-".Contains(c))).TrimStart('+');
        if (request.Delivery.Phone.Count(c => c == '+') > 1
            || (request.Delivery.Phone.Contains('+') && !request.Delivery.Phone.TrimStart().StartsWith('+'))
            || phone.Length is < 7 or > 15 || phone.Any(c => c is < '0' or > '9')
            || !Provinces.Contains(request.Delivery.Province.Trim(), StringComparer.OrdinalIgnoreCase))
            throw new CheckoutException(400, "Provide a valid phone number and South African province.");
    }

    private static bool Valid(object value) => value is not null
        && Validator.TryValidateObject(value, new ValidationContext(value), null, true);

    public static string OrderId(string userId, Guid requestId) => Hash(userId + "/" + requestId.ToString("D"));

    public static string Fingerprint(CreateOrderRequest request) => Hash(JsonSerializer.Serialize(new
    {
        Items = request.Items.OrderBy(x => x.ProductId, StringComparer.Ordinal).ToArray(),
        Delivery = request.Delivery.Trimmed(), request.PaymentMethod
    }));

    private static string Hash(string value) => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(value))).ToLowerInvariant();

    public static CheckoutOrder Create(string userId, CreateOrderRequest request,
        IReadOnlyDictionary<string, CheckoutProduct> products, long deliveryFeeCents)
    {
        Validate(request);
        if (deliveryFeeCents is < 0 or > 1000000)
            throw new CheckoutException(503, "Delivery pricing is not configured. Please try again later.");

        var order = new CheckoutOrder
        {
            Id = OrderId(userId, request.RequestId), UserId = userId,
            RequestFingerprint = Fingerprint(request), Delivery = request.Delivery.Trimmed(),
            PaymentMethod = request.PaymentMethod, DeliveryFeeCents = deliveryFeeCents,
            CreatedAtUtc = DateTime.UtcNow
        };

        foreach (var item in request.Items)
        {
            if (!products.TryGetValue(item.ProductId, out var product) || !product.Available
                || product.StockQuantity < item.Quantity || string.IsNullOrWhiteSpace(product.Name)
                || product.UnitPriceCents is <= 0 or > 10000000000)
                throw new CheckoutException(409, "An item is unavailable or its price has not been configured. Please refresh your cart.");

            //stores cents as whole numbers and takes every price from the database
            order.Items.Add(new OrderLine
            {
                ProductId = item.ProductId, Name = product.Name, Quantity = item.Quantity,
                UnitPriceCents = product.UnitPriceCents, LineTotalCents = checked(product.UnitPriceCents * item.Quantity)
            });
        }
        order.SubtotalCents = order.Items.Sum(x => x.LineTotalCents);
        order.TotalCents = checked(order.SubtotalCents + order.DeliveryFeeCents);
        return order;
    }
}
