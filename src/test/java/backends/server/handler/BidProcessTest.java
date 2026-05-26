package backends.server.handler;

import com.bidding_system.backends.server.handler.BidProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("app.db")
class BidProcessorTest {

    private BidProcessor processor;

    @SuppressWarnings("unchecked")
    private LinkedBlockingQueue<BidProcessor.BidRequest> getQueue() throws Exception {
        Field field = BidProcessor.class.getDeclaredField("queue");
        field.setAccessible(true);
        return (LinkedBlockingQueue<BidProcessor.BidRequest>) field.get(processor);
    }

    private void pauseWorker() throws Exception {
        Field field = BidProcessor.class.getDeclaredField("workerThread");
        field.setAccessible(true);
        Thread worker = (Thread) field.get(processor);
        if (worker != null) worker.interrupt();
    }

    @BeforeEach
    void setUp() throws Exception {
        Field instanceField = BidProcessor.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        processor = BidProcessor.getInstance();
        pauseWorker();
    }

    @AfterEach
    void tearDown() throws Exception {
        pauseWorker();

        Field field = BidProcessor.class.getDeclaredField("workerThread");
        field.setAccessible(true);
        Thread worker = (Thread) field.get(processor);
        if (worker != null) {
            worker.join(1000);
        }
    }

    // =========================================================
    // NHÓM 1: BidRequest record
    // =========================================================

    @Test
    void bidRequest_Manual_LuuDuThongTin() {
        BidProcessor.BidRequest req = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);

        assertEquals("user-A",      req.userId());
        assertEquals("auction-001", req.auctionId());
        assertEquals(100.0,         req.amount());
        assertFalse(req.isAuto());
        assertEquals(0,             req.maxBid());
    }

    @Test
    void bidRequest_Auto_LuuDuThongTin() {
        BidProcessor.BidRequest req = BidProcessor.BidRequest.auto("user-A", "auction-001", 500.0);

        assertEquals("user-A",      req.userId());
        assertEquals("auction-001", req.auctionId());
        assertEquals(0,             req.amount());
        assertTrue(req.isAuto());
        assertEquals(500.0,         req.maxBid());
    }

    @Test
    void bidRequest_HaiRequestGiongNhau_Equal() {
        BidProcessor.BidRequest r1 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
        BidProcessor.BidRequest r2 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);

        assertEquals(r1, r2);
    }

    @Test
    void bidRequest_HaiRequestKhacAmount_NotEqual() {
        BidProcessor.BidRequest r1 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
        BidProcessor.BidRequest r2 = BidProcessor.BidRequest.manual("user-A", "auction-001", 200.0);

        assertNotEquals(r1, r2);
    }

    @Test
    void bidRequest_ManualVsAuto_NotEqual() {
        BidProcessor.BidRequest r1 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
        BidProcessor.BidRequest r2 = BidProcessor.BidRequest.auto("user-A", "auction-001", 100.0);

        assertNotEquals(r1, r2);
    }

    // =========================================================
    // NHÓM 2: submitManualBid / submitAutoBid — nạp vào queue
    // =========================================================

    @Test
    void submitManual_BidDauTien_KhongThrowException() throws Exception {
        pauseWorker();
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", 100.0));
    }

    @Test
    void submitAuto_BidDauTien_KhongThrowException() {
        assertDoesNotThrow(() -> processor.submitAutoBid("user-A", "auction-001", 500.0));
    }

    @Test
    void submitManual_KhongThrowException() {
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", 100.0));
    }

    @Test
    void submitManual_NhieuBidLienTiep_KhongThrowException() {
        assertDoesNotThrow(() -> {
            processor.submitManualBid("user-A", "auction-001", 100.0);
            processor.submitManualBid("user-B", "auction-001", 200.0);
            processor.submitManualBid("user-C", "auction-001", 150.0);
        });
    }

    @Test
    void submitManual_BidKhacAuction_KhongThrowException() {
        assertDoesNotThrow(() -> {
            processor.submitManualBid("user-A", "auction-001", 100.0);
            processor.submitManualBid("user-B", "auction-002", 200.0);
            processor.submitManualBid("user-C", "auction-003", 300.0);
        });
    }

    @Test
    void submitManual_AmountAm_KhongThrowException() {
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", -50.0));
    }

    @Test
    void submitManual_AmountZero_KhongThrowException() {
        assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", 0.0));
    }

    @Test
    void submitAuto_VaManual_CungAuction_KhongThrowException() {
        assertDoesNotThrow(() -> {
            processor.submitAutoBid("user-A", "auction-001", 500.0);
            processor.submitManualBid("user-B", "auction-001", 200.0);
        });
    }

    // =========================================================
    // NHÓM 3: Singleton
    // =========================================================

    @Test
    void getInstance_GoiNhieuLan_TraVeCungInstance() throws Exception {
        BidProcessor a = BidProcessor.getInstance();
        BidProcessor b = BidProcessor.getInstance();

        assertSame(a, b);
    }

    // =========================================================
    // NHÓM 4: Đồng thời
    // =========================================================

    @Test
    void submitManual_50ThreadCungSubmit_KhongThrowException() throws Exception {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    processor.submitManualBid("user-" + idx, "auction-001", (idx + 1) * 10.0);
                } catch (Exception e) {
                    fail("submitManualBid() ném exception không mong muốn: " + e.getMessage());
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
    void submitAuto_50ThreadCungSubmit_KhongThrowException() throws Exception {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    processor.submitAutoBid("user-" + idx, "auction-001", (idx + 1) * 100.0);
                } catch (Exception e) {
                    fail("submitAutoBid() ném exception không mong muốn: " + e.getMessage());
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
    void submit_30ThreadNhieuAuction_KhongThrowException() throws Exception {
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
                    if (idx % 2 == 0) {
                        processor.submitManualBid(
                                "user-" + idx,
                                "auction-" + (idx % auctionCount),
                                (idx + 1) * 5.0
                        );
                    } else {
                        processor.submitAutoBid(
                                "user-" + idx,
                                "auction-" + (idx % auctionCount),
                                (idx + 1) * 50.0
                        );
                    }
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
    void submit_100ThreadCungAuction_KhongMucNaoLamCrash() throws Exception {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (idx % 3 == 0) {
                        processor.submitAutoBid("user-" + idx, "auction-stress", idx * 1000.0);
                    } else {
                        processor.submitManualBid("user-" + idx, "auction-stress", idx * 100.0);
                    }
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