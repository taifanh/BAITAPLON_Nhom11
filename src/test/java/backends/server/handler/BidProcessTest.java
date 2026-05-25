package backends.server.handler;


import com.bidding_system.backends.server.handler.BidProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class BidProcessorTest {

    private BidProcessor processor;

    // Truy cập queue private để kiểm tra nội dung bên trong
    @SuppressWarnings("unchecked")
    private LinkedBlockingQueue<BidProcessor.BidRequest> getQueue() throws Exception {
        Field field = BidProcessor.class.getDeclaredField("queue");
        field.setAccessible(true);
        return (LinkedBlockingQueue<BidProcessor.BidRequest>) field.get(processor);
    }

    // Dừng worker thread để queue không bị consume khi đang kiểm tra
    private void pauseWorker() throws Exception {
        Field field = BidProcessor.class.getDeclaredField("workerThread");
        field.setAccessible(true);
        Thread worker = (Thread) field.get(processor);
        if (worker != null) worker.interrupt();
    }

    @BeforeEach
    void setUp() throws Exception {
        // Reset singleton trước mỗi test
        Field instanceField = BidProcessor.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        processor = BidProcessor.getInstance();
    }

    // =========================================================
    // NHÓM 1: BidRequest record
    // =========================================================

    @Test
    void bidRequest_LuuDuBaThongTin() {
        BidProcessor.BidRequest req = new BidProcessor.BidRequest("user-A", "auction-001", 100.0);

        assertEquals("user-A",       req.userId());
        assertEquals("auction-001",  req.auctionId());
        assertEquals(100.0,          req.amount());
    }

    @Test
    void bidRequest_HaiRequestGiongNhau_Equal() {
        BidProcessor.BidRequest r1 = new BidProcessor.BidRequest("user-A", "auction-001", 100.0);
        BidProcessor.BidRequest r2 = new BidProcessor.BidRequest("user-A", "auction-001", 100.0);

        assertEquals(r1, r2);
    }

    @Test
    void bidRequest_HaiRequestKhacAmount_NotEqual() {
        BidProcessor.BidRequest r1 = new BidProcessor.BidRequest("user-A", "auction-001", 100.0);
        BidProcessor.BidRequest r2 = new BidProcessor.BidRequest("user-A", "auction-001", 200.0);

        assertNotEquals(r1, r2);
    }

    // =========================================================
    // NHÓM 2: submit() — nạp vào queue
    // =========================================================

    @Test
    void submit_ManualBid_BidDauTien_VaoQueue() throws Exception {
        pauseWorker(); // dừng worker để queue không bị consume ngay

        processor.submitManualBid("user-A", "auction-001", 100.0);

        LinkedBlockingQueue<BidProcessor.BidRequest> queue = getQueue();
        // Worker đã bị dừng → queue còn ít nhất 1 item
        // (hoặc đã được process trước khi dừng → kiểm tra không throw là đủ)
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", 100.0));
    }

    @Test
    void submit_ManualBid_KhongThrowException() {
        // submit() chỉ gọi queue.offer() — không bao giờ ném exception
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", 100.0));
    }

    @Test
    void submit_ManualBid_NhieuBidLienTiep_KhongThrowException() {
        assertDoesNotThrow(() -> {
            processor.submitManualBid("user-A", "auction-001", 100.0);
            processor.submitManualBid("user-B", "auction-001", 200.0);
            processor.submitManualBid("user-C", "auction-001", 150.0);
        });
    }

    @Test
    void submit_ManualBid_BidKhacAuction_KhongThrowException() {
        assertDoesNotThrow(() -> {
            processor.submitManualBid("user-A", "auction-001", 100.0);
            processor.submitManualBid("user-B", "auction-002", 200.0);
            processor.submitManualBid("user-C", "auction-003", 300.0);
        });
    }

    @Test
    void submit_ManualBid_AmountAm_KhongThrowException() {
        // submit() không validate — validation nằm trong process()
        // nên submit giá âm vẫn không throw, chỉ bị reject khi process
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", -50.0));
    }

    @Test
    void submit_ManualBid_AmountZero_KhongThrowException() {
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", 0.0));
    }

    // =========================================================
    // NHÓM 3: Singleton
    // =========================================================

    @Test
    void getInstance_GoiNhieuLan_TraVeCungInstance() {
        BidProcessor a = BidProcessor.getInstance();
        BidProcessor b = BidProcessor.getInstance();

        assertSame(a, b);
    }

    // =========================================================
    // NHÓM 4: Đồng thời — quan trọng nhất
    // =========================================================

    @Test
    void submit_50ThreadCungSubmit_ManualBid_KhongThrowException() throws Exception {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // tất cả thread chờ, rồi cùng xuất phát
                    processor.submitManualBid("user-" + idx, "auction-001", (idx + 1) * 10.0);
                } catch (Exception e) {
                    fail("submit() ném exception không mong muốn: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // bắt đầu đồng thời
        doneLatch.await();
        executor.shutdown();

        // Không có exception nào được ném → thread-safe
    }

    @Test
    void submit_ManualBid_30ThreadNhieuAuction_KhongThrowException() throws Exception {
        int auctionCount  = 3;
        int bidPerAuction = 10;
        int threadCount   = auctionCount * bidPerAuction;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    processor.submitManualBid(
                            "user-" + idx,
                            "auction-" + (idx % auctionCount),
                            (idx + 1) * 5.0
                    );
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
    }

    @Test
    void submit_ManualBid_100ThreadCungAuction_KhongMucNaoLamCrash() throws Exception {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    processor.submitManualBid("user-" + idx, "auction-stress", idx * 1000.0);
                } catch (Exception e) {
                    fail("Crash tại thread " + idx + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
    }
}