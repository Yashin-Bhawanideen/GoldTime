using System.Net;
using System.Net.Sockets;

namespace GoldTimeApi.Services;

public static class PayFastSource
{
    public const string SourceItem = "PayFastSourceIp";

    public static IPAddress? ClientIp(IPAddress? peer, string forwarded, IEnumerable<string> trustedProxyAddresses)
    {
        if (peer is null) return null;

        //proxies listed in PayFast:TrustedProxyAddresses are still trusted
        var trusted = new HashSet<string>();
        foreach (var address in trustedProxyAddresses)
            if (IPAddress.TryParse(address, out var ip)) trusted.Add(Normalize(ip).ToString());

        var current = Normalize(peer);

        //walks the forwarded addresses from the right, because the rightmost one is added by the nearest proxy
        //an address is only believed if the hop that supplied it is trusted (configured, or an internal address
        //such as the Azure front end); anything a caller writes further left is never reached
        foreach (var value in forwarded.Split(',', StringSplitOptions.RemoveEmptyEntries).Reverse())
        {
            if (!IsTrustedHop(current, trusted)) break;
            var next = ParseForwarded(value);
            if (next is null) return null;
            current = Normalize(next);
        }
        return current;
    }

    private static IPAddress Normalize(IPAddress address) => address.IsIPv4MappedToIPv6 ? address.MapToIPv4() : address;

    //a hop is trusted if it is configured, or if it is an internal address
    //a request from the internet cannot arrive from an internal address, so this means it came through the platform's own proxy
    private static bool IsTrustedHop(IPAddress address, HashSet<string> configured) =>
        configured.Contains(address.ToString()) || IsInternal(address);

    private static bool IsInternal(IPAddress address)
    {
        if (IPAddress.IsLoopback(address)) return true;

        if (address.AddressFamily == AddressFamily.InterNetwork)
        {
            var b = address.GetAddressBytes();
            return b[0] == 10
                || (b[0] == 172 && b[1] is >= 16 and <= 31)
                || (b[0] == 192 && b[1] == 168)
                || (b[0] == 169 && b[1] == 254);
        }

        if (address.AddressFamily == AddressFamily.InterNetworkV6)
            return address.IsIPv6LinkLocal || address.IsIPv6SiteLocal || (address.GetAddressBytes()[0] & 0xFE) == 0xFC;

        return false;
    }

    //Azure can add the client port to forwarded addresses (1.2.3.4:5678 or [2001:db8::1]:5678), so the port is removed before parsing
    private static IPAddress? ParseForwarded(string value)
    {
        value = value.Trim();

        if (value.StartsWith('['))
        {
            var end = value.IndexOf(']');
            if (end > 0) value = value[1..end];
        }
        else if (value.Count(c => c == ':') == 1)
        {
            value = value[..value.IndexOf(':')];
        }

        return IPAddress.TryParse(value, out var address) ? address : null;
    }

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