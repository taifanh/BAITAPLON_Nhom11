package backends.common.models.bidding;

import backends.common.models.accounts.User;
import backends.common.models.items.Art;
import backends.common.models.core.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private Item item;
    private User userA;
    private User userB;
    private User userC;

    // Tạo dữ liệu dùng chung trước mỗi test
    @BeforeEach
    void setUp() {
        item = new Art("item-001", "Tranh sơn dầu", 100.0, "Mô tả");
        auction = new Auction(item);

        LocalDateTime now = LocalDateTime.now();
        auction.schedule(now, Duration.ofHours(1));
        auction.start(now);

        userA = new User("user-A", "Nguyen A", "a@mail.com", "0901", "pass");
        userB = new User("user-B", "Nguyen B", "b@mail.com", "0902", "pass");
        userC = new User("user-C", "Nguyen C", "c@mail.com", "0903", "pass");
    }

    @Test
    void bidHopLe_NguoiDauBid_ThangVoiGiaDoDat() {
        // Người đầu tiên bid 200 → thắng với 200
        BidTransaction bid = new BidTransaction(userA, item, 200.0);
        auction.addBid(bid);

        assertEquals(200.0, auction.getCurrentHighestBid());
        assertEquals("user-A", auction.getCurrentHighestBidderId());
    }

    @Test
    void bidHopLe_NguoiSauBidCaoHon_GiaCapNhat() {
        // A bid 200, B bid 300 → B thắng với 300
        auction.addBid(new BidTransaction(userA, item, 200.0));
        auction.addBid(new BidTransaction(userB, item, 300.0));

        assertEquals(300.0, auction.getCurrentHighestBid());
        assertEquals("user-B", auction.getCurrentHighestBidderId());
    }

    @Test
    void bidKhongHopLe_BidThapHonGiaHienTai_BidBiTuChoi() {
        // A bid 300, B bid 200 → B bị từ chối vì thấp hơn
        auction.addBid(new BidTransaction(userA, item, 300.0));

        assertThrows(IllegalArgumentException.class, () -> {
            auction.addBid(new BidTransaction(userB, item, 200.0));
        });

        // Giá vẫn là của A
        assertEquals(300.0, auction.getCurrentHighestBid());
        assertEquals("user-A", auction.getCurrentHighestBidderId());
    }

    @Test
    void bidKhongHopLe_BidBangGiaHienTai_BidBiTuChoi() {
        // A bid 300, B cũng bid 300 → B bị từ chối (phải cao HƠN)
        auction.addBid(new BidTransaction(userA, item, 300.0));

        assertThrows(IllegalArgumentException.class, () -> {
            auction.addBid(new BidTransaction(userB, item, 300.0));
        });
    }

    @Test
    void bidKhongHopLe_BidSo0_BidBiTuChoi() {
        assertThrows(IllegalArgumentException.class, () -> {
            auction.addBid(new BidTransaction(userA, item, 0.0));
        });
    }

    @Test
    void auctionDaDong_KhongChapNhanBidMoi() {
        // Đóng auction rồi bid → bị từ chối
        auction.end(LocalDateTime.now().plusSeconds(1));

        assertThrows(IllegalStateException.class, () -> {
            auction.addBid(new BidTransaction(userA, item, 200.0));
        });
    }

    @Test
    void auctionBiHuy_KhongChapNhanBidMoi() {
        auction.cancel();

        assertThrows(IllegalStateException.class, () -> {
            auction.addBid(new BidTransaction(userA, item, 200.0));
        });
    }

    @Test
    void dongAuction_TrangThaiChuyenThanhENDED() {
        auction.end(LocalDateTime.now().plusSeconds(1));
        assertEquals(Auction.Status.ENDED, auction.getStatus());
    }

    @Test
    void dongAuction2Lan_LanHaiKhongThrowException() {
        // Gọi end() 2 lần không được crash
        auction.end(LocalDateTime.now().plusSeconds(1));
        assertDoesNotThrow(() -> auction.end(LocalDateTime.now().plusSeconds(2)));
    }

    @Test
    void huyAuction_TrangThaiChuyenThanhCANCELLED() {
        auction.cancel();
        assertEquals(Auction.Status.CANCELLED, auction.getStatus());
    }

    @Test
    void huyAuctionDaDong_ThrowException() {
        auction.end(LocalDateTime.now().plusSeconds(1));
        assertThrows(IllegalStateException.class, () -> auction.cancel());
    }


    @Test
    void danhSachBid_BanDau_Rong() {
        assertTrue(auction.getBidList().isEmpty());
    }

    @Test
    void danhSachBid_SauKhiBid_CoBidDo() {
        auction.addBid(new BidTransaction(userA, item, 200.0));
        assertEquals(1, auction.getBidList().size());
    }

    @Test
    void danhSachBid_KhongChoBenNgoaiSuaDoi() {
        // getBidList() trả về unmodifiable list
        auction.addBid(new BidTransaction(userA, item, 200.0));

        assertThrows(UnsupportedOperationException.class, () -> {
            auction.getBidList().clear();
        });
    }

    @Test
    void dongThoiBid_10ThreadCungBid_ChiMotNguoiThang() throws InterruptedException {
        // 10 thread cùng bid số tiền khác nhau → chỉ 1 người thắng cuối cùng
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // chờ tất cả sẵn sàng
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final double amount = i * 100.0; // 100, 200, ..., 1000
            final String userId = "user-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // chờ lệnh bắt đầu
                    User user = new User(userId, "User " + userId, userId + "@mail.com",
                            "09" + userId, "pass");
                    auction.addBid(new BidTransaction(user, item, amount));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // bid thấp hơn sẽ bị từ chối → bình thường
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // thả tất cả thread cùng lúc
        doneLatch.await();
        executor.shutdown();

        // Kết quả phải nhất quán: highest bid là 1 giá trị xác định
        assertTrue(auction.getCurrentHighestBid() > 0);
        assertNotNull(auction.getCurrentHighestBidderId());

        // Người thắng phải đúng là người bid cao nhất
        assertEquals(1000.0, auction.getCurrentHighestBid());
    }

    @Test
    void dongThoiBid_NhieuThreadBidCungMot_ChiMotNguoiThangVaBidListNhatQuan()
            throws InterruptedException {
        // Nhiều thread bid cùng lúc → danh sách bid không bị corrupt
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Double> amounts = new ArrayList<>();
        for (int i = 1; i <= threadCount; i++) amounts.add(i * 50.0);

        for (int i = 0; i < threadCount; i++) {
            final double amount = amounts.get(i);
            final int idx = i;
            executor.submit(() -> {
                try {
                    User user = new User("u" + idx, "User", "u@mail.com", "09" + idx, "pass");
                    auction.addBid(new BidTransaction(user, item, amount));
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Bid list không được có giá nào thấp hơn giá trước đó (đã được addBid validate)
        List<BidTransaction> bids = auction.getBidList();
        for (int i = 1; i < bids.size(); i++) {
            assertTrue(bids.get(i).getAmount() > bids.get(i - 1).getAmount(),
                    "Bid list bị sai thứ tự tại vị trí " + i);
        }

        // Giá cuối cùng phải khớp với bid cuối trong list
        if (!bids.isEmpty()) {
            assertEquals(
                    bids.get(bids.size() - 1).getAmount(),
                    auction.getCurrentHighestBid()
            );
        }
    }
}