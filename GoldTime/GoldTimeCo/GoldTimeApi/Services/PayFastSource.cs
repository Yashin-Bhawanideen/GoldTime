using System.Net;

namespace GoldTimeApi.Services;

public static class PayFastSource
{
    public const string SourceItem = "PayFastSourceIp";

    public static IPAddress? ClientIp(IPAddress? peer, string forwarded, IEnumerable<string> trustedProxyAddresses)
    {
        if (peer is null) return null;
        var trusted = trustedProxyAddresses.Select(x => IPAddress.TryParse(x, out var ip) ? Normalize(ip).ToString() : null).ToHashSet();
        var current = Normalize(peer);
        //only follows forwarded addresses through explicitly configured proxies
        foreach (var value in forwarded.Split(',').Reverse())
        {
            if (!trusted.Contains(current.ToString())) break;
            if (!IPAddress.TryParse(value.Trim(), out var next)) return null;
            current = Normalize(next);
        }
        return current;
    }

    private static IPAddress Normalize(IPAddress address) => address.IsIPv4MappedToIPv6 ? address.MapToIPv4() : address;

    public static bool IsPayFast(IPAddress? address)
    {
        if (address is null || (!address.IsIPv4MappedToIPv6 && address.AddressFamily != System.Net.Sockets.AddressFamily.InterNetwork)) return false;
        var bytes = address.MapToIPv4().GetAddressBytes();
        return (bytes[0] == 197 && bytes[1] == 97 && bytes[2] == 145 && bytes[3] is >= 144 and <= 159)
            || (bytes[0] == 41 && bytes[1] == 74 && bytes[2] == 179 && bytes[3] is >= 192 and <= 223)
            || (bytes[0] == 102 && bytes[1] == 216 && bytes[2] == 36 && (bytes[3] <= 15 || bytes[3] is >= 128 and <= 143))
            || address.MapToIPv4().Equals(IPAddress.Parse("144.126.193.139"));
    }
}

/* REFERENCE LIST
Payfast. n.d. Ports and IP addresses. [Online]. Available at:
https://developers.payfast.co.za/docs/itn-instant-transaction-notification/
[Accessed 1 October 2026].
*/
