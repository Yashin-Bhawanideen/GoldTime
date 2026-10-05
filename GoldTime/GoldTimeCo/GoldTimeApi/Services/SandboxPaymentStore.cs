using Google.Cloud.Firestore;
using GoldTimeApi.Models;

namespace GoldTimeApi.Services;

public interface ISandboxPaymentStore
{
    Task<CheckoutOrder> PrepareAsync(string orderId, string userId, CancellationToken token);
    Task ConfirmAsync(PayFastNotification notification, CancellationToken token);
}

public class SandboxPaymentStore(FirestoreDb db) : ISandboxPaymentStore
{
    public Task<CheckoutOrder> PrepareAsync(string orderId, string userId, CancellationToken token) =>
        db.RunTransactionAsync(async transaction =>
        {
            var reference = db.Collection("orders").Document(orderId);
            var snapshot = await transaction.GetSnapshotAsync(reference, token);
            if (!snapshot.Exists) throw new CheckoutException(404, "Order not found.");
            var order = snapshot.ConvertTo<CheckoutOrder>();
            ValidateStart(order, userId, DateTime.UtcNow);
            order.PaymentEnvironment = "sandbox";
            transaction.Set(reference, order);
            return order;
        }, cancellationToken: token);

    public async Task ConfirmAsync(PayFastNotification notification, CancellationToken token)
    {
        await db.RunTransactionAsync(async transaction =>
        {
            var orderReference = db.Collection("orders").Document(notification.OrderId);
            var receiptReference = db.Collection("sandboxPaymentReceipts").Document(notification.PaymentId);
            var snapshot = await transaction.GetSnapshotAsync(orderReference, token);
            var receipt = await transaction.GetSnapshotAsync(receiptReference, token);
            if (!snapshot.Exists) throw new CheckoutException(400, "Unknown payment order.");
            var order = snapshot.ConvertTo<CheckoutOrder>();
            ValidateConfirmation(order, notification);
            if (receipt.Exists)
            {
                if (receipt.GetValue<string>("OrderId") != order.Id) throw new CheckoutException(409, "Payment reference is already in use.");
                if (order.Status != "sandbox_paid" || order.PayFastPaymentId != notification.PaymentId)
                    throw new CheckoutException(409, "Payment receipt needs review.");
                return true;
            }
            if (!string.IsNullOrEmpty(order.PayFastPaymentId)) throw new CheckoutException(409, "This order already has a different payment reference.");
            //records the receipt and status together so repeated notifications cannot apply twice
            order.Status = "sandbox_paid";
            order.PayFastPaymentId = notification.PaymentId;
            order.PaidAtUtc = DateTime.UtcNow;
            transaction.Create(receiptReference, new Dictionary<string, object>
            {
                ["OrderId"] = order.Id, ["AmountCents"] = notification.AmountCents,
                ["ReceivedAtUtc"] = order.PaidAtUtc.Value
            });
            transaction.Set(orderReference, order);
            return true;
        }, cancellationToken: token);
    }

    public static void ValidateConfirmation(CheckoutOrder order, PayFastNotification notification)
    {
        if (order.Id != notification.OrderId || order.PaymentEnvironment != "sandbox" || order.Currency != "ZAR"
            || order.TotalCents != notification.AmountCents || notification.Status != "COMPLETE"
            || order.Status is not ("pending_payment" or "sandbox_paid"))
            throw new CheckoutException(400, "Payment does not match the saved order.");
        if (!string.IsNullOrEmpty(order.PayFastPaymentId) && order.PayFastPaymentId != notification.PaymentId)
            throw new CheckoutException(409, "This order already has a different payment reference.");
    }

    public static void ValidateStart(CheckoutOrder order, string userId, DateTime nowUtc)
    {
        if (order.UserId != userId) throw new CheckoutException(404, "Order not found.");
        if (order.Status != "pending_payment" || order.CreatedAtUtc < nowUtc.AddMinutes(-30)
            || order.Currency != "ZAR" || order.TotalCents < 500)
            throw new CheckoutException(409, "This order cannot start a payment. Review your cart and start a new checkout.");
    }
}

/* REFERENCE LIST
Google. n.d. Transactions and batched writes. [Online]. Available at:
https://firebase.google.com/docs/firestore/manage-data/transactions [Accessed 1 October 2026].
*/
