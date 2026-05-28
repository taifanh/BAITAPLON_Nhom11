package backends.server.policy;

public class IncrementPolicy {
    public static double getIncrement(double currentPrice) {
        if (currentPrice <    100_000) return   5_000;  // dưới 100k
        if (currentPrice <    500_000) return  10_000;  // 100k – 500k
        if (currentPrice <  1_000_000) return  25_000;  // 500k – 1 triệu
        if (currentPrice <  5_000_000) return  50_000;  // 1 – 5 triệu
        if (currentPrice < 10_000_000) return 100_000;  // 5 – 10 triệu
        if (currentPrice < 50_000_000) return 500_000;  // 10 – 50 triệu
        if (currentPrice <100_000_000) return 1_000_000;// 50 – 100 triệu
        return 2_000_000;                               // trên 100 triệu
    }
}
