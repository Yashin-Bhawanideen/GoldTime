using Google.Cloud.Firestore;
using GoldTimeApi.Models;

namespace GoldTimeApi.Services;

public interface IOrderStore
{
    Task<CheckoutOrder> CreateAsync(string userId, CreateOrderRequest request, CancellationToken cancellationToken);
    Task<CheckoutOrder?> FindAsync(string orderId, CancellationToken cancellationToken);
    Task<List<CheckoutOrder>> GetForUserAsync(string userId, CancellationToken cancellationToken);
}
//Firestore implementation of the order store
//primary constructor: the Firestore database and app configuration are injected through dependency injection
public class OrderStore(FirestoreDb db, IConfiguration configuration) : IOrderStore
{
    public async Task<CheckoutOrder> CreateAsync(string userId, CreateOrderRequest request, CancellationToken cancellationToken)
    {
        OrderPricing.Validate(request);
        var reference = db.Collection("orders").Document(OrderPricing.OrderId(userId, request.RequestId));

        //reads before writing; retrying the same request returns its saved order (Google, n.d.)
        return await db.RunTransactionAsync(async transaction =>
        {
            var existing = await transaction.GetSnapshotAsync(reference, cancellationToken);
            if (existing.Exists)
            {
                var saved = existing.ConvertTo<CheckoutOrder>();
                if (saved.UserId != userId || saved.RequestFingerprint != OrderPricing.Fingerprint(request))
                    throw new CheckoutException(409, "This request ID has already been used. Start a new checkout for changed details.");
                return saved;
            }
            //reads the delivery fee (in cents) from the app configuration
//if it is missing or not a number, this is a server problem, so return a 503
            if (!long.TryParse(configuration["Checkout:DeliveryFeeCents"], out var deliveryFee))
                throw new CheckoutException(503, "Delivery pricing is not configured. Please try again later.");

            //loads every ordered product from the products collection so prices and stock come from the database, not the app
//products that do not exist are skipped here and rejected later in OrderPricing.Create
            var products = new Dictionary<string, CheckoutProduct>();
            foreach (var item in request.Items)
            {
                var snapshot = await transaction.GetSnapshotAsync(db.Collection("products").Document(item.ProductId), cancellationToken);
                if (snapshot.Exists) products[item.ProductId] = snapshot.ConvertTo<CheckoutProduct>();
            }
            var order = OrderPricing.Create(userId, request, products, deliveryFee);
            transaction.Create(reference, order);
            return order;
        }, cancellationToken: cancellationToken);
    }

    public async Task<CheckoutOrder?> FindAsync(string orderId, CancellationToken cancellationToken)
    {
        var snapshot = await db.Collection("orders").Document(orderId).GetSnapshotAsync(cancellationToken);
        return snapshot.Exists ? snapshot.ConvertTo<CheckoutOrder>() : null;
    }

    public async Task<List<CheckoutOrder>> GetForUserAsync(
        string userId,
        CancellationToken cancellationToken)
    {
    //reads the order document directly by ID
        var snapshot = await db.Collection("orders")
            .WhereEqualTo("UserId", userId)
            .GetSnapshotAsync(cancellationToken);

        var orders = snapshot.Documents
            .Select(document => document.ConvertTo<CheckoutOrder>())
            .ToList();
        //returns the order if the document exists, otherwise null
        return orders
            .OrderByDescending(order => order.CreatedAtUtc)
            .ToList();
    }
}

/* REFERENCE LIST
Google. n.d. Transactions and batched writes. [Online]. Available at:
https://firebase.google.com/docs/firestore/manage-data/transactions [Accessed 1 October 2026].
Google. n.d. Class Transaction. [Online]. Available at:
https://cloud.google.com/dotnet/docs/reference/Google.Cloud.Firestore/latest/Google.Cloud.Firestore.Transaction
[Accessed 1 October 2026].
*/
