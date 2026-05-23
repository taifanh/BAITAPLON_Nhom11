package backends.launcher;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class GetExactIP {
    public static String getIP() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                // Bỏ qua các card mạng ảo hoặc đã tắt
                if (netint.isLoopback() || !netint.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    // Kiểm tra xem có phải IPv4 không (tránh lấy nhầm IPv6 chứa dấu ':')
                    if (!inetAddress.getHostAddress().contains(":")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
}