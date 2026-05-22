package backends.server.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class BidBatchProcessorTest {

    private BidBatchProcessor processor;

    // Truy cập pendingBids private để kiểm tra nội dung bên trong
    @SuppressWarnings("unchecked")
    private Map<String, List<BidBatchProcessor.PendingBid>> getPendingBids()
            throws Exception {
        Field field = BidBatchProcessor.class.getDeclaredField("pendingBids");
        field.setAccessible(true);
        return (Map<String, List<BidBatchProcessor.PendingBid>>) field.get(processor);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Tạo instance mới mỗi test bằng cách reset singleton
        Field instanceField = BidBatchProcessor.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        processor = BidBatchProcessor.getInstance();
    }

    // =========================================================
    // NHÓM 1: submitBid cơ bản
    // =========================================================

    @Test
    void submitBid_BidDauTien_DuocThemVaoPendingList() throws Exception {
        processor.submitBid("user-A", "auction-001", 100.0);

        Map<String, List<BidBatchProcessor.PendingBid>> pending = getPendingBids();

        assertTrue(pending.containsKey("auction-001"));
        assertEquals(1, pending.get("auction-001").size());
        assertEquals(100.0, pending.get("auction-001").get(0).amount());
        assertEquals("user-A", pending.get("auction-001").get(0).userId());
    }

    @Test
    void submitBid_NhieuBidCungAuction_TatCaDuocGom() throws Exception {
        processor.submitBid("user-A", "auction-001", 100.0);
        processor.submitBid("user-B", "auction-001", 200.0);
        processor.submitBid("user-C", "auction-001", 150.0);

        Map<String, List<BidBatchProcessor.PendingBid>> pending = getPendingBids();
        assertEquals(3, pending.get("auction-001").size());
    }

    @Test
    void submitBid_BidKhacAuction_TachRiengTungAuction() throws Exception {
        processor.submitBid("user-A", "auction-001", 100.0);
        processor.submitBid("user-B", "auction-002", 200.0);
        processor.submitBid("user-C", "auction-003", 300.0);

        Map<String, List<BidBatchProcessor.PendingBid>> pending = getPendingBids();

        assertEquals(3, pending.size()); // 3 auction khác nhau
        assertEquals(1, pending.get("auction-001").size());
        assertEquals(1, pending.get("auction-002").size());
        assertEquals(1, pending.get("auction-003").size());
    }

    @Test
    void submitBid_ThongTinBidDuocLuuChinhXac() throws Exception {
        long before = System.currentTimeMillis();
        processor.submitBid("user-A", "auction-001", 500.0);
        long after = System.currentTimeMillis();

        BidBatchProcessor.PendingBid bid =
                getPendingBids().get("auction-001").get(0);

        assertEquals("user-A", bid.userId());
        assertEquals("auction-001", bid.auctionId());
        assertEquals(500.0, bid.amount());
        assertTrue(bid.receivedAt() >= before && bid.receivedAt() <= after);
    }

    // =========================================================
    // NHÓM 2: Đồng thời (quan trọng nhất)
    // =========================================================

    @Test
    void submitBid_NhieuThreadCungSubmit_KhongMucBidNao() throws Exception {
        // 50 thread cùng submitBid vào cùng 1 auction → không bid nào bị mất
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    processor.submitBid("user-" + idx, "auction-001", (idx + 1) * 10.0);
                } catch (Exception e) {
                    fail("submitBid ném exception không mong muốn: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Tất cả 50 bid phải có trong pending list
        Map<String, List<BidBatchProcessor.PendingBid>> pending = getPendingBids();
        assertEquals(threadCount, pending.get("auction-001").size(),
                "Bị mất bid khi submit đồng thời");
    }

    @Test
    void submitBid_NhieuThreadNhieuAuction_MoiAuctionDuSoBid() throws Exception {
        // 30 thread submit vào 3 auction khác nhau (10 bid/auction)
        int bidPerAuction = 10;
        int auctionCount = 3;
        int threadCount = bidPerAuction * auctionCount;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            final String auctionId = "auction-" + (idx % auctionCount);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    processor.submitBid("user-" + idx, auctionId, (idx + 1) * 5.0);
                } catch (Exception e) {
                    fail("Exception: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Map<String, List<BidBatchProcessor.PendingBid>> pending = getPendingBids();
        for (int i = 0; i < auctionCount; i++) {
            assertEquals(bidPerAuction, pending.get("auction-" + i).size(),
                    "Auction " + i + " bị mất bid");
        }
    }

    // =========================================================
    // NHÓM 3: flushAuction
    // =========================================================

    @Test
    void flushAuction_SauFlush_PendingListCuaAuctionDoRong() throws Exception {
        processor.submitBid("user-A", "auction-001", 100.0);
        processor.submitBid("user-B", "auction-001", 200.0);

        // flushAuction sẽ cố process nhưng DB không có → exception nội bộ
        // Điều quan trọng là pending list phải được xóa
        try {
            processor.flushAuction("auction-001");
        } catch (Exception ignored) {}

        Map<String, List<BidBatchProcessor.PendingBid>> pending = getPendingBids();
        assertFalse(pending.containsKey("auction-001"),
                "Pending list phải rỗng sau khi flush");
    }

    @Test
    void flushAuction_AuctionKhongCoTrongPending_KhongThrowException() {
        // Flush auction không tồn tại trong pending → không crash
        assertDoesNotThrow(() -> processor.flushAuction("auction-khong-ton-tai"));
    }
}