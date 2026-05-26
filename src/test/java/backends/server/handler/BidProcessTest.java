package backends.server.handler;

import com.bidding_system.backends.server.handler.BidProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BidProcessorTest {

    private BidProcessor processor;

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Reset singleton trước mỗi test để tránh state leak */
    @BeforeEach
    void setUp() throws Exception {
        Field instanceField = BidProcessor.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
        processor = BidProcessor.getInstance();
    }

    /**
     * Lấy queue nội bộ bằng reflection.
     * Worker vẫn chạy — chỉ dùng để quan sát, không nên assert size
     * vì worker có thể consume item ngay sau khi offer.
     */
    @SuppressWarnings("unchecked")
    private LinkedBlockingQueue<BidProcessor.BidRequest> getQueue() throws Exception {
        Field field = BidProcessor.class.getDeclaredField("queue");
        field.setAccessible(true);
        return (LinkedBlockingQueue<BidProcessor.BidRequest>) field.get(processor);
    }

    // =========================================================
    // NHÓM 1: BidRequest — factory methods & record equality
    // =========================================================

    @Nested
    @DisplayName("BidRequest record")
    class BidRequestTest {

        @Test
        @DisplayName("manual() — lưu đủ userId, auctionId, amount; isAuto=false, maxBid=0")
        void manual_LuuDuThongTin() {
            BidProcessor.BidRequest req = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);

            assertEquals("user-A",      req.userId());
            assertEquals("auction-001", req.auctionId());
            assertEquals(100.0,         req.amount());
            assertFalse(req.isAuto());
            assertEquals(0.0,           req.maxBid());
        }

        @Test
        @DisplayName("auto() — lưu đủ userId, auctionId, maxBid; isAuto=true, amount=0")
        void auto_LuuDuThongTin() {
            BidProcessor.BidRequest req = BidProcessor.BidRequest.auto("user-B", "auction-002", 500.0);

            assertEquals("user-B",      req.userId());
            assertEquals("auction-002", req.auctionId());
            assertEquals(500.0,         req.maxBid());
            assertTrue(req.isAuto());
            assertEquals(0.0,           req.amount());
        }

        @Test
        @DisplayName("Hai manual request cùng tham số → equals()")
        void manual_HaiRequestGiongNhau_Equal() {
            BidProcessor.BidRequest r1 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
            BidProcessor.BidRequest r2 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("Hai auto request cùng tham số → equals()")
        void auto_HaiRequestGiongNhau_Equal() {
            BidProcessor.BidRequest r1 = BidProcessor.BidRequest.auto("user-A", "auction-001", 200.0);
            BidProcessor.BidRequest r2 = BidProcessor.BidRequest.auto("user-A", "auction-001", 200.0);

            assertEquals(r1, r2);
        }

        @Test
        @DisplayName("Manual vs Auto cùng user/auction → không equal (isAuto khác)")
        void manual_vs_auto_NotEqual() {
            BidProcessor.BidRequest manual = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
            BidProcessor.BidRequest auto   = BidProcessor.BidRequest.auto("user-A", "auction-001", 100.0);

            assertNotEquals(manual, auto);
        }

        @Test
        @DisplayName("Manual khác amount → không equal")
        void manual_KhacAmount_NotEqual() {
            BidProcessor.BidRequest r1 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
            BidProcessor.BidRequest r2 = BidProcessor.BidRequest.manual("user-A", "auction-001", 200.0);

            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Auto khác maxBid → không equal")
        void auto_KhacMaxBid_NotEqual() {
            BidProcessor.BidRequest r1 = BidProcessor.BidRequest.auto("user-A", "auction-001", 300.0);
            BidProcessor.BidRequest r2 = BidProcessor.BidRequest.auto("user-A", "auction-001", 400.0);

            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Manual khác userId → không equal")
        void manual_KhacUserId_NotEqual() {
            BidProcessor.BidRequest r1 = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
            BidProcessor.BidRequest r2 = BidProcessor.BidRequest.manual("user-B", "auction-001", 100.0);

            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("toString() chứa các field chính")
        void toString_ChuaCacField() {
            BidProcessor.BidRequest req = BidProcessor.BidRequest.manual("user-A", "auction-001", 100.0);
            String str = req.toString();

            assertTrue(str.contains("user-A"));
            assertTrue(str.contains("auction-001"));
            assertTrue(str.contains("100.0"));
        }
    }

    // =========================================================
    // NHÓM 2: submitManualBid() — offer vào queue
    // =========================================================

    @Nested
    @DisplayName("submitManualBid()")
    class SubmitManualBidTest {

        @Test
        @DisplayName("Bid hợp lệ — không throw")
        void bidHopLe_KhongThrow() {
            assertDoesNotThrow(() ->
                    processor.submitManualBid("user-A", "auction-001", 100.0));
        }

        @Test
        @DisplayName("Amount âm — submit không throw (validation trong process())")
        void amountAm_KhongThrow() {
            assertDoesNotThrow(() ->
                    processor.submitManualBid("user-A", "auction-001", -50.0));
        }

        @Test
        @DisplayName("Amount = 0 — submit không throw")
        void amountZero_KhongThrow() {
            assertDoesNotThrow(() ->
                    processor.submitManualBid("user-A", "auction-001", 0.0));
        }

        @Test
        @DisplayName("Amount = Double.MAX_VALUE — submit không throw")
        void amountMaxDouble_KhongThrow() {
            assertDoesNotThrow(() ->
                    processor.submitManualBid("user-A", "auction-001", Double.MAX_VALUE));
        }

        @Test
        @DisplayName("Nhiều bid liên tiếp cùng auction — không throw")
        void nhieuBidLienTiep_KhongThrow() {
            assertDoesNotThrow(() -> {
                processor.submitManualBid("user-A", "auction-001", 100.0);
                processor.submitManualBid("user-B", "auction-001", 200.0);
                processor.submitManualBid("user-C", "auction-001", 300.0);
            });
        }

        @Test
        @DisplayName("Bid ở nhiều auction khác nhau — không throw")
        void nhieuAuction_KhongThrow() {
            assertDoesNotThrow(() -> {
                processor.submitManualBid("user-A", "auction-001", 100.0);
                processor.submitManualBid("user-B", "auction-002", 200.0);
                processor.submitManualBid("user-C", "auction-003", 300.0);
            });
        }

        @Test
        @DisplayName("Queue nhận được item sau khi submit (queue accessible)")
        void submit_QueueAccessible_KhongThrow() throws Exception {
            // Chỉ verify queue tồn tại và accessible; worker có thể đã consume
            LinkedBlockingQueue<BidProcessor.BidRequest> queue = getQueue();
            assertNotNull(queue);
            assertDoesNotThrow(() -> processor.submitManualBid("user-A", "auction-001", 100.0));
        }
    }

    // =========================================================
    // NHÓM 3: submitAutoBid() — offer vào queue
    // =========================================================

    @Nested
    @DisplayName("submitAutoBid()")
    class SubmitAutoBidTest {

        @Test
        @DisplayName("Auto bid hợp lệ — không throw")
        void autoBidHopLe_KhongThrow() {
            assertDoesNotThrow(() ->
                    processor.submitAutoBid("user-A", "auction-001", 500.0));
        }

        @Test
        @DisplayName("maxBid âm — submit không throw")
        void maxBidAm_KhongThrow() {
            assertDoesNotThrow(() ->
                    processor.submitAutoBid("user-A", "auction-001", -100.0));
        }

        @Test
        @DisplayName("maxBid = 0 — submit không throw")
        void maxBidZero_KhongThrow() {
            assertDoesNotThrow(() ->
                    processor.submitAutoBid("user-A", "auction-001", 0.0));
        }

        @Test
        @DisplayName("Nhiều auto bid liên tiếp cùng user — không throw")
        void nhieuAutoBidLienTiep_KhongThrow() {
            assertDoesNotThrow(() -> {
                processor.submitAutoBid("user-A", "auction-001", 300.0);
                processor.submitAutoBid("user-A", "auction-001", 400.0); // tăng maxBid
                processor.submitAutoBid("user-A", "auction-001", 500.0); // tăng tiếp
            });
        }

        @Test
        @DisplayName("Auto bid và manual bid xen kẽ — không throw")
        void autoBid_ManualBid_XenKe_KhongThrow() {
            assertDoesNotThrow(() -> {
                processor.submitManualBid("user-A", "auction-001", 100.0);
                processor.submitAutoBid("user-B",  "auction-001", 300.0);
                processor.submitManualBid("user-C", "auction-001", 250.0);
                processor.submitAutoBid("user-D",  "auction-001", 400.0);
            });
        }
    }

    // =========================================================
    // NHÓM 4: Singleton
    // =========================================================

    @Nested
    @DisplayName("Singleton")
    class SingletonTest {

        @Test
        @DisplayName("getInstance() gọi nhiều lần — trả về cùng instance")
        void getInstance_NhieuLan_CungInstance() {
            BidProcessor a = null;
            try {
                a = BidProcessor.getInstance();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BidProcessor b = null;
            try {
                b = BidProcessor.getInstance();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BidProcessor c = null;
            try {
                c = BidProcessor.getInstance();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            assertSame(a, b);
            assertSame(b, c);
        }

        @Test
        @DisplayName("getInstance() không trả về null")
        void getInstance_KhongNull() {
            try {
                assertNotNull(BidProcessor.getInstance());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("getInstance() thread-safe — 20 thread cùng gọi, cùng instance")
        void getInstance_ThreadSafe_CungInstance() throws Exception {
            int threadCount = 20;
            BidProcessor[] results = new BidProcessor[threadCount];
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch  = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        results[idx] = BidProcessor.getInstance();
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

            for (BidProcessor result : results) {
                assertSame(results[0], result, "getInstance() phải trả về cùng một instance");
            }
        }
    }

    // =========================================================
    // NHÓM 5: Concurrency — submit từ nhiều thread
    // =========================================================

    @Nested
    @DisplayName("Concurrency")
    class ConcurrencyTest {

        @Test
        @DisplayName("50 thread cùng submitManualBid — không crash, không exception")
        void submit_50Thread_ManualBid_KhongCrash() throws Exception {
            int threadCount = 50;
            runConcurrentSubmit(threadCount, idx ->
                    processor.submitManualBid("user-" + idx, "auction-001", (idx + 1) * 10.0));
        }

        @Test
        @DisplayName("50 thread cùng submitAutoBid — không crash, không exception")
        void submit_50Thread_AutoBid_KhongCrash() throws Exception {
            int threadCount = 50;
            runConcurrentSubmit(threadCount, idx ->
                    processor.submitAutoBid("user-" + idx, "auction-001", (idx + 1) * 100.0));
        }

        @Test
        @DisplayName("100 thread cùng auction — không crash")
        void submit_100Thread_CungAuction_KhongCrash() throws Exception {
            runConcurrentSubmit(100, idx ->
                    processor.submitManualBid("user-" + idx, "auction-stress", idx * 1000.0));
        }

        @Test
        @DisplayName("30 thread, 3 auction — phân tán đều, không crash")
        void submit_30Thread_3Auction_KhongCrash() throws Exception {
            int auctionCount = 3;
            runConcurrentSubmit(30, idx ->
                    processor.submitManualBid(
                            "user-" + idx,
                            "auction-" + (idx % auctionCount),
                            (idx + 1) * 5.0));
        }

        @Test
        @DisplayName("Mixed manual + auto từ nhiều thread — không crash")
        void submit_MixedBid_NhieuThread_KhongCrash() throws Exception {
            int threadCount = 60;
            runConcurrentSubmit(threadCount, idx -> {
                if (idx % 2 == 0) {
                    processor.submitManualBid("user-" + idx, "auction-001", (idx + 1) * 50.0);
                } else {
                    processor.submitAutoBid("user-" + idx, "auction-001", (idx + 1) * 200.0);
                }
            });
        }

        @Test
        @DisplayName("Cùng user spam auto bid tăng maxBid liên tục — không crash")
        void submit_SameUser_SpamAutoBid_TangMaxBid_KhongCrash() throws Exception {
            // Đây là scenario bug: USER2 spam auto bid tăng maxBid
            int threadCount = 20;
            runConcurrentSubmit(threadCount, idx ->
                    processor.submitAutoBid("user-spam", "auction-001", (idx + 1) * 100.0));
        }

        @Test
        @DisplayName("Tất cả submit thành công — đếm được qua AtomicInteger")
        void submit_DemSoLanThanhCong() throws Exception {
            int threadCount = 40;
            AtomicInteger successCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch  = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        processor.submitManualBid("user-" + idx, "auction-001", idx * 10.0);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // không increment → sẽ fail assertion
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertEquals(threadCount, successCount.get(),
                    "Tất cả " + threadCount + " submit phải thành công");
        }

        // ── Helper ────────────────────────────────────────────────────────────

        @FunctionalInterface
        interface SubmitAction {
            void run(int idx) throws Exception;
        }

        private void runConcurrentSubmit(int threadCount, SubmitAction action) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch  = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        action.run(idx);
                    } catch (Exception e) {
                        fail("Thread " + idx + " ném exception: " + e.getMessage());
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
}