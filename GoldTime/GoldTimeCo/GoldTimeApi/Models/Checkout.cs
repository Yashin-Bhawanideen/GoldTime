using System.ComponentModel.DataAnnotations;
using Google.Cloud.Firestore;

namespace GoldTimeApi.Models;

public class CreateOrderRequest
{
    public Guid RequestId { get; set; }
    [Required, MinLength(1), MaxLength(50)] public List<OrderItemRequest> Items { get; set; } = [];
    [Required] public DeliveryAddress Delivery { get; set; } = new();
    [Required, RegularExpression("^(CARD|INSTANT_EFT)$")] public string PaymentMethod { get; set; } = "";
}

public class OrderItemRequest
{
    [Required, RegularExpression("^[a-zA-Z0-9_-]{1,100}$")] public string ProductId { get; set; } = "";
    [Range(1, 99)] public int Quantity { get; set; }
}

[FirestoreData]
public class DeliveryAddress
{
    [FirestoreProperty, Required, StringLength(100)] public string FullName { get; set; } = "";
    [FirestoreProperty, Required, StringLength(30)] public string Phone { get; set; } = "";
    [FirestoreProperty, Required, StringLength(200)] public string StreetAddress { get; set; } = "";
    [FirestoreProperty, Required, StringLength(100)] public string City { get; set; } = "";
    [FirestoreProperty, Required, StringLength(30)] public string Province { get; set; } = "";
    [FirestoreProperty, Required, RegularExpression("^[0-9]{4}$")] public string PostalCode { get; set; } = "";

    public DeliveryAddress Trimmed() => new()
    {
        FullName = FullName.Trim(), Phone = Phone.Trim(), StreetAddress = StreetAddress.Trim(),
        City = City.Trim(), Province = Province.Trim(), PostalCode = PostalCode.Trim()
    };
}

[FirestoreData]
public class CheckoutProduct
{
    [FirestoreProperty] public string Name { get; set; } = "";
    [FirestoreProperty] public long UnitPriceCents { get; set; }
    [FirestoreProperty] public bool Available { get; set; }
    [FirestoreProperty] public long StockQuantity { get; set; }
}

[FirestoreData]
public class OrderLine
{
    [FirestoreProperty] public string ProductId { get; set; } = "";
    [FirestoreProperty] public string Name { get; set; } = "";
    [FirestoreProperty] public int Quantity { get; set; }
    [FirestoreProperty] public long UnitPriceCents { get; set; }
    [FirestoreProperty] public long LineTotalCents { get; set; }
}

[FirestoreData]
public class CheckoutOrder
{
    [FirestoreProperty] public string Id { get; set; } = "";
    [FirestoreProperty] public string UserId { get; set; } = "";
    [FirestoreProperty] public string RequestFingerprint { get; set; } = "";
    [FirestoreProperty] public List<OrderLine> Items { get; set; } = [];
    [FirestoreProperty] public DeliveryAddress Delivery { get; set; } = new();
    [FirestoreProperty] public string PaymentMethod { get; set; } = "";
    [FirestoreProperty] public string Status { get; set; } = "pending_payment";
    [FirestoreProperty] public string Currency { get; set; } = "ZAR";
    [FirestoreProperty] public long SubtotalCents { get; set; }
    [FirestoreProperty] public long DeliveryFeeCents { get; set; }
    [FirestoreProperty] public long TotalCents { get; set; }
    [FirestoreProperty] public DateTime CreatedAtUtc { get; set; }
    [FirestoreProperty] public string PaymentEnvironment { get; set; } = "";
    [FirestoreProperty] public string PayFastPaymentId { get; set; } = "";
    [FirestoreProperty] public DateTime? PaidAtUtc { get; set; }
}
