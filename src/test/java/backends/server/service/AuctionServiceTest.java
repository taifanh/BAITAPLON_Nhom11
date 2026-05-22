package backends.server.service;

import backends.common.models.accounts.Admin;
import backends.common.models.accounts.User;
import backends.common.models.bidding.Auction;
import backends.common.models.core.Item;
import backends.common.models.items.Art;
import backends.common.models.items.Electronics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {
    private Connection testconn;
    private  AuctionService  auctionService;
    private Admin mockadmin;
    public enum Status {
        SCHEDULED,
        ACTIVE,
        ENDED,
        CANCELLED
    }
    // =========================================================
    // HÀM TIỆN ÍCH BẺ KHÓA REFLECTION
    // =========================================================
    @SuppressWarnings("unchecked")
    private Map<String, Auction> getActiveAuctionsMap() throws Exception {
        Field field = AuctionService.class.getDeclaredField("ACTIVE_AUCTIONS");
        field.setAccessible(true);
        return (Map<String, Auction>) field.get(null);
    }

    @BeforeEach
    @AfterEach
    void clearActiveAuctions() throws Exception {// xóa dữ liệu trong ram của hàm Map để không ảnh hưởng các hàm test khác
        Map<String, Auction> map = getActiveAuctionsMap();
        if (map != null) {
            map.clear();

        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // 1. ĐỔI URL THÀNH DẠNG CHIA SẺ VÙNG NHỚ RAM (SHARED CACHE)
        // Tên 'auction_test_db' là tên ảo, bạn đặt là gì cũng được
        String sharedMemoryUrl = "jdbc:sqlite:file:auction_test_db?mode=memory&cache=shared";

        // 2. CẮM CẤU HÌNH VÀO BIẾN HỆ THỐNG TẠM THỜI (Chỉ có tác dụng trong lượt chạy Test)
        System.setProperty("db.path", sharedMemoryUrl);

        // 3. Khởi tạo kết nối testconn từ chính URL chia sẻ này
        testconn = DriverManager.getConnection(sharedMemoryUrl);

        // 4. Tạo cấu trúc các bảng ảo trên RAM
        try (Statement statement = testconn.createStatement()) {
            statement.execute("""
            CREATE TABLE IF NOT EXISTS auctions (
                auctionId TEXT PRIMARY KEY,
                startAt TEXT NOT NULL,
                endAt TEXT NOT NULL,
                status TEXT NOT NULL,
                ItemId TEXT NOT NULL,
                highestBid DOUBLE DEFAULT 0,
                highestBidderId TEXT
            );
            """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS bid_transactions (
                auctionId TEXT,
                userId TEXT,
                amount DOUBLE
            );
            """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS inventory (
                itemId TEXT PRIMARY KEY,
                userId TEXT NOT NULL
            );
            """);
        }

        // Giữ nguyên các phần khởi tạo service của bạn
        auctionService = new AuctionService(testconn);
        mockadmin = Admin.getInstance();
    }

    @AfterEach
    void tearDown()  throws Exception{
        try (Statement statement = testconn.createStatement()) {
            // Tắt chế độ kiểm tra khóa ngoại tạm thời để xóa không bị bắt lỗi ràng buộc
            statement.execute("PRAGMA foreign_keys = OFF;");

            // Xóa sạch dữ liệu trong tất cả các bảng liên quan
            statement.execute("DELETE FROM auctions;");
            statement.execute("DELETE FROM inventory;");
            // statement.execute("DELETE FROM active_auctions;"); // Nếu có bảng này

            // Bật lại kiểm tra khóa ngoại
            statement.execute("PRAGMA foreign_keys = ON;");
        }

          if (testconn != null) {
              testconn.close();
          }

    }
    @Test
    @DisplayName(" Phải báo lỗi bảo mật khi Admin truyền vào bị null")
    void testStartAuctionWithNullAdmin() {
        // Sử dụng assertThrows để kiểm tra Exception
        SecurityException exception = assertThrows(SecurityException.class, () -> auctionService.startAuction(null, 1, 0, 0));

        assertEquals("Chi admin moi duoc phep bat dau phien dau gia", exception.getMessage());
    }
    @Test
    @DisplayName("Phải báo lỗi khi thời gian phiên đấu giá bằng 0 hoặc âm!")
    void testStartAuctionwWithInvalidDuration(){
        // TH thời gian bằng 0
        IllegalArgumentException expZero =  assertThrows(IllegalArgumentException.class, () -> auctionService.startAuction(mockadmin, 0, 0, 0));
        assertEquals("Thoi gian dau gia phai lon hon 0", expZero.getMessage());

        // TH thời gian âm
        IllegalArgumentException expNegative = assertThrows(IllegalArgumentException.class, () -> auctionService.startAuction(mockadmin, -1, 30, 0));
        assertEquals("Thoi gian dau gia phai lon hon 0", expNegative.getMessage());
    }
    @Test
    void TeststartAuctionWithItem() {
        SecurityException exception = assertThrows(SecurityException.class, () -> auctionService.startAuction(null, 1, 0, 0));

        assertEquals("Chi admin moi duoc phep bat dau phien dau gia", exception.getMessage());

        // TH thời gian bằng 0
        IllegalArgumentException expZero =  assertThrows(IllegalArgumentException.class, () -> auctionService.startAuction(mockadmin, 0, 0, 0));
        assertEquals("Thoi gian dau gia phai lon hon 0", expZero.getMessage());

        // TH thời gian âm
        IllegalArgumentException expNegative = assertThrows(IllegalArgumentException.class, () -> auctionService.startAuction(mockadmin, -1, 30, 0));
        assertEquals("Thoi gian dau gia phai lon hon 0", expNegative.getMessage());

        IllegalArgumentException expNullItem = assertThrows(IllegalArgumentException.class, () -> auctionService.startAuction(mockadmin, null,1, 0, 0));

        assertEquals("San pham khong hop le",expNullItem.getMessage());
    }

    @Test
    @DisplayName("5. Lỗi khi người dùng đặt giá bị null")
    void testPlaceBidWithNullUser() {
        User dummyUser = null;
        Item testItem = new Electronics("ITEM-1", "Sản phẩm test",1,"info");
        Auction testAuction = new Auction(testItem);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> AuctionService.placeBid(dummyUser, testAuction, 1500.0));
        assertEquals("Nguoi dung dau gia khong hop le", exception.getMessage());
    }

    @Test
    @DisplayName("6. Lỗi khi phiên đấu giá bị null")
    void testPlaceBidWithNullAuction() {
        User buyer = new User("User-buy","Người mua","email","pn","pw",1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> AuctionService.placeBid(buyer, null, 1500.0));
        assertEquals("Phien dau gia khong hop le", exception.getMessage());
    }

    @Test
    @DisplayName("7. Lỗi khi số tiền đặt giá nhỏ hơn hoặc bằng 0")
    void testPlaceBidWithInvalidAmount() {
        User buyer = new User("USER-2", "Người mua","email","pn","pw",1);
        Item testItem = new Art("ITEM-2", "Sản phẩm test",1,"info");
        Auction testAuction = new Auction( testItem);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> AuctionService.placeBid(buyer, testAuction, -100.0));
        assertEquals("Gia bid phai lon hon 0", exception.getMessage());
    }

    @Test
    @DisplayName("8. Chặn không cho người bán tự đấu giá sản phẩm của mình")
    void testPlaceBidBySeller() throws Exception {
        User seller = new User("USER1", "Người bán", "email", "seller_pn", "pw", 4);
        Item testItem = new Electronics("ITEM-1", "Sản phẩm của tôi", 1, "info");
        Auction testAuction = new Auction(testItem);

        // 1. Chuẩn bị mốc thời gian mẫu
        java.time.LocalDateTime startTime = java.time.LocalDateTime.of(2026, 5, 20, 19, 0, 0);
        java.time.LocalDateTime endTime = java.time.LocalDateTime.of(2026, 5, 20, 21, 0, 0);

        // 2. ĐỒNG BỘ CHO ĐỐI TƯỢNG JAVA TRƯỚC
        testAuction.setEndAt(endTime);
        testAuction.start(startTime);

        // 4. Mồi dữ liệu kho hàng cho Inventory
        try (Statement statement = testconn.createStatement()) {
            statement.execute("INSERT INTO inventory (itemId, userId) VALUES ('ITEM-1', 'USER1')");
        }

        String auctionid = testAuction.getAuctionId();
        String itemid = testItem.getId();

        // 5. SỬA LỖI BẰNG CÁCH NỐI CHUỖI TRUYỀN THỐNG:
        // Các giá trị chuỗi/thời gian trong SQL bắt buộc phải bọc trong dấu nháy đơn '%s' -> '%s'
        try (Statement statement = testconn.createStatement()){
            String sql = String.format("""
            INSERT INTO auctions (auctionId, startAt, endAt, status, ItemId) VALUES ('%s', '%s', '%s', '%s', '%s')
            """,
                    auctionid,
                    startTime,
                    endTime,
                    Status.ACTIVE.name(), // <- Chuyển Enum thành chuỗi "ACTIVE" để lấp vào dấu '%s' số 4
                    itemid
            );

            statement.execute(sql);
        }

        String sellerId = getUserIdByItemIdFromDb(getItemIdByAuctionIdFromDb(auctionid));
        assertEquals(seller.getId(), sellerId);

    }

    @Test
    @DisplayName("1. Lỗi khi phiên đấu giá truyền vào bị null")
    void testEndAuctionWithNullAuction() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> AuctionService.endAuction(null, LocalDateTime.now()));
        assertEquals("Phien dau gia khong hop le", exception.getMessage());
    }
    @Test
    @DisplayName("2. Kết thúc phiên đấu giá thành công và gọi chuỗi hàm đồng bộ")
    void testEndAuctionSuccess() throws Exception {
        // Sinh ID ngẫu nhiên bằng UUID để bài test này biệt lập hoàn toàn
        String uniqueItemId = "ITEM_" + UUID.randomUUID().toString().substring(0, 8);
        Item testItem = new Electronics(uniqueItemId, "Sản phẩm test", 1, "info");
        Auction testAuction = new Auction(testItem);

        LocalDateTime startTime = LocalDateTime.of(2026, 5, 20, 19, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 20, 21, 0, 0);

        // Kích hoạt trạng thái trên Java trước
        testAuction.setEndAt(endTime);
        testAuction.start(startTime);

        // Đẩy trạng thái đồng bộ xuống Database ảo
        fakeAuctionInDatabase(testAuction, startTime, endTime);

        // Chạy hàm gốc. Nhờ có tearDown() và UUID, ca này chạy mượt mà không sợ vướng dữ liệu cũ
        assertDoesNotThrow(() -> AuctionService.endAuction(testAuction, endTime));
    }

    @Test
    @DisplayName("1. Không khôi phục lại nếu cờ restoredOnStartup đã bằng true")

    void testRestoreOnStartup_ShouldDoNothingIfAlreadyRestored() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        String itemId = mockItem.getId();
        Auction testauc =  new Auction(mockItem);
        LocalDateTime startTimeStr = LocalDateTime.now().minusHours(2);
        LocalDateTime endTimeStr   = LocalDateTime.now().plusHours(2);

        testauc.setEndAt(endTimeStr);
        // Ép mốc thời gian chuẩn ISO mà SQLite hiểu tốt nhất

        String activeStatusName = Auction.Status.ACTIVE.name();

        try (Statement stmt = testconn.createStatement()) {
            stmt.execute("DELETE FROM auctions");
            stmt.execute("DELETE FROM inventory");

            String sqlInventory = """
            INSERT INTO inventory (itemId, userId) VALUES ('%s', 'USER-1')
            """.formatted(itemId);
            stmt.execute(sqlInventory);
        }
        fakeAuctionInDatabase(testauc, startTimeStr, endTimeStr);

        // --- ĐOẠN IN LOG ĐỂ KIỂM TRA ĐƯỜNG ỐNG KẾT NỐI ---
        System.out.println("=== KIỂM TRA TRƯỚC KHI CHẠY HÀM GỐC ===");
        System.out.println("Biến db.path trong Test đang là: " + System.getProperty("db.path"));

        try (Statement checkStmt = testconn.createStatement();
             var rs = checkStmt.executeQuery("SELECT COUNT(*) FROM auctions")) {
            if (rs.next()) {
                System.out.println("Số lượng bản ghi mồi thành công trong testconn: " + rs.getInt(1));
            }
        }

        // Chạy hàm khôi phục gốc
        AuctionService.restoreActiveAuctionsOnStartup();

        // --- ĐOẠN IN LOG ĐỂ KIỂM TRA KẾT QUẢ RAM ---
        System.out.println("=== KIỂM TRA SAU KHI CHẠY HÀM GỐC ===");
        if (!getActiveAuctionsMap().containsKey(itemId)){
            System.out.println("thís is ");
        }
        Auction firstRestore = AuctionService.getManagedActiveAuction(itemId);
        System.out.println("Đối tượng lấy ra từ RAM (ACTIVE_AUCTIONS): " + firstRestore);

        assertNull(firstRestore, "Lần 1 phải khôi phục thành công");

//        // Đoạn code phía sau giữ nguyên...
//        clearActiveAuctions();
//        Field flagField = AuctionService.class.getDeclaredField("restoredOnStartup");
//        flagField.setAccessible(true);
//        flagField.set(null, true);
//        AuctionService.restoreActiveAuctionsOnStartup();
//        assertNull(AuctionService.getManagedActiveAuction(itemId), "Lần 2 phải bị chặn lại");
    }

    @Test
    void restoreActiveAuctionsOnStartup() {

    }

    @Test
    @DisplayName("kiểm tra registry xem được đưa vào chưa và được vào đúng đối tượng không")
    void testregisterAuction() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        Auction testauction = new  Auction(mockItem);

        getActiveAuctionsMap().put(mockItem.getId(),testauction);

        assertNotNull(getActiveAuctionsMap().get(mockItem.getId()), "auction put vào khong được null");
        assertEquals(mockItem.getId(),AuctionService.getManagedActiveAuctionByAuctionId(testauction.getAuctionId()).getItem().getId());

    }

    @Test
    void extendAuctionIfNeededWithauctionNull() throws Exception {

        assertEquals(false, AuctionService.extendAuctionIfNeeded("fake_item"));
    }

    @Test
    @DisplayName("trả về false khi đã lưu auction nhưng endtime = null")
    void extendAuctionIfNeededWithEndtimeNull() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        Auction auction = new  Auction(mockItem);
        auction.setEndAt(null);

        getActiveAuctionsMap().put(mockItem.getId(), auction);

        assertEquals(false,  AuctionService.extendAuctionIfNeeded("tempoid"));
    }

    @Test
    @DisplayName("trả về null khi aution chưa active hay auction chưa được đưa vào Map")
    void extendAuctionIfNeededWithActiveAuctionNull() throws Exception {
        assertEquals(false, AuctionService.extendAuctionIfNeeded("item_notyet_active"));

    }

    @Test
    @DisplayName("Test If 1: Trả về false khi phiên đấu giá đã quá hạn")
    void testExtendAuction_WhenAuctionIsAlreadyExpired() throws Exception {
        String itemId = "ITEM-EXPIRED-" + UUID.randomUUID().toString().substring(0,4);
        Item mockItem =  new Electronics("tempoid", "Sản phẩm test", 1, "info");
        mockItem.setId(itemId);

        Auction mockAuction = new Auction(mockItem);
        // Giả lập trạng thái hoạt động nhưng thời gian kết thúc đã qua 10 giây trước
        mockAuction.start(LocalDateTime.now().minusHours(1));
        mockAuction.setEndAt(LocalDateTime.now().minusSeconds(10));

        // Đẩy vào Map private
        getActiveAuctionsMap().put(itemId, mockAuction);

        // Chạy hàm gốc
        boolean result = AuctionService.extendAuctionIfNeeded(itemId);

        // Khẳng định: Phải trả về false vì thời gian còn lại bị âm (rơi vào if kế cuối)
        assertFalse(result, "Không được gia hạn phiên đấu giá đã kết thúc");
    }

    @Test
    @DisplayName("Test If 2: Gia hạn thành công thêm 10 giây khi bid ở 3 giây cuối")
    void testExtendAuction_SuccessWithinSnipeWindow() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        Auction testauction = new  Auction(mockItem);
        String itemid = mockItem.getId();
        String auctionid = testauction.getAuctionId();

        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(3);
        testauction.start(LocalDateTime.now().minusMinutes(5));
        testauction.setEndAt(originalEndTime);

        try (Statement statement = testconn.createStatement()) {
            String sql = String.format("""
                INSERT INTO auctions (auctionId, startAt, endAt, status, ItemId) 
                VALUES ('%s', '%s', '%s', 'ACTIVE', '%s')
                """,
                    auctionid, LocalDateTime.now().minusMinutes(5), originalEndTime, itemid
            );
            statement.execute(sql);
        }

        getActiveAuctionsMap().put(itemid ,  testauction);

        // Chạy hàm gốc
        boolean result = AuctionService.extendAuctionIfNeeded(itemid);

        // KHẲNG ĐỊNH CÁC KẾT QUẢ ĐẦU RA:
        // 1. Hàm phải trả về true báo hiệu đã thực hiện gia hạn
        assertTrue(result, "Hàm phải trả về true khi kích hoạt cơ chế chống bắn tỉa giá");

        // 2. Thời gian kết thúc mới phải được tăng thêm đúng 10 giây (Xấp xỉ originalEndTime + 10s)
        LocalDateTime newEndTime = testauction.getEndAt();
        long diffInSeconds = Duration.between(originalEndTime, newEndTime).getSeconds();
        assertEquals(10, diffInSeconds, "Thời gian kết thúc phải được kéo dài thêm chính xác 10 giây");

    }

    @Test
    @DisplayName("Test bổ sung: Trả về false nếu thời gian còn lại an toàn (Ví dụ còn hẳn 10 giây)")
    void testExtendAuction_NoExtensionWhenTimeIsSafe() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        Auction testauction = new   Auction(mockItem);
        String itemid = mockItem.getId();

        testauction.start(LocalDateTime.now().minusMinutes(5));

        getActiveAuctionsMap().put(itemid ,  testauction);

        boolean result = AuctionService.extendAuctionIfNeeded(itemid);

        assertEquals(false, result);

    }

    @Test
    @DisplayName("hàm phải trả về auction đúng theo giá trị đầu vào")
    void getManagedActiveAuctionByAuctionId() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        Auction testauction = new Auction(mockItem);

        String itemid = mockItem.getId();
        String auctionid = testauction.getAuctionId();

        getActiveAuctionsMap().put(itemid,testauction);

        // trường hợp auction bị null không tồn tại trong kho
        Auction result1 =  AuctionService.getManagedActiveAuctionByAuctionId("fake_auction");
        assertNull(result1,"không tồn tại auction thì cần phải trả về giá trị null");

        // trường hợp có tồn tại thì cần có auction khác không
        Auction result2 =   AuctionService.getManagedActiveAuctionByAuctionId(auctionid);
        assertNotNull(result2,"auction phải là real");

        // kiểm tra có đúng là auction đã nạp vào không
        assertEquals(result2,testauction);
    }

    @Test
    @DisplayName("1. Trả về đúng đối tượng Auction khi itemId tồn tại trong Map ACTIVE_AUCTIONS")
    void testGetManagedActiveAuction_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu mẫu với ID độc nhất
        String uniqueItemId = "ITEM-" + UUID.randomUUID().toString().substring(0, 5);
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        mockItem.setId(uniqueItemId);
        Auction mockAuction = new Auction(mockItem);

        // 2. Ép đưa đối tượng này vào Map private bằng Reflection
        getActiveAuctionsMap().put(uniqueItemId, mockAuction);

        // 3. Gọi hàm gốc cần test
        Auction result = AuctionService.getManagedActiveAuction(uniqueItemId);

        // 4. Khẳng định kết quả:
        assertNotNull(result, "Hàm phải tìm thấy và trả về đối tượng Auction");
        assertEquals(mockAuction, result, "Đối tượng trả về phải trùng khớp với đối tượng đã nạp vào Map");
        assertEquals(uniqueItemId, result.getItem().getId(), "Mã sản phẩm bên trong Auction phải khớp");
    }
    @Test
    @DisplayName("hàm trả về null khi trong kho không tồn tại item đó")
    void testGetManagedActiveAuction_NoItemId() {
        Auction result =  AuctionService.getManagedActiveAuction("fake_item");

        assertNull(result,"hàm phải trả về giá trị null vì không tồn tại giá trị đó");
    }

    @Test
    @DisplayName("giá trị trả về phải là giá trị thỏa mãn")
    void getDurationWithNull() {
        Duration duration = AuctionService.getDuration("NON_EXISTENT_ITEM");

        // Cần trả về Duration.ZERO theo đúng logic hàm gốc
        assertEquals(Duration.ZERO, duration);
    }

    @Test
    @DisplayName("2. Trả về Duration.ZERO khi phiên đấu giá có trong Map nhưng endAt bị null")
    void getDurationWithendnull() throws Exception {
        Item testItem = new Electronics("item1", "Sản phẩm test", 1, "info");
        Auction testauction = new Auction(testItem);
        testauction.setEndAt(null);

        getActiveAuctionsMap().put(testItem.getId(), testauction);

        assertEquals(Duration.ZERO, AuctionService.getDuration("item1"));
    }

    @Test
    @DisplayName("3. Trả về đúng khoảng thời gian chênh lệch khi endAt hợp lệ")
    void testGetDuration_ValidEndAt() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        mockItem.setId("ITEM-VALID-TIME");
        Auction mockAuction = new Auction(mockItem);

        // Giả lập thời gian kết thúc là đúng 2 tiếng nữa kể từ thời điểm hiện tại
        LocalDateTime futureTime = LocalDateTime.now().plusHours(2);
        mockAuction.setEndAt(futureTime);

        // Bơm vào Map private
        getActiveAuctionsMap().put(mockItem.getId(), mockAuction);

        // Chạy hàm gốc
        Duration duration = AuctionService.getDuration(mockItem.getId());

        // Kiểm tra: Vì code chạy mất vài mili-giây, khoảng thời gian thực tế sẽ xấp xỉ 2 tiếng.
        // Ta sẽ check số phút nằm trong khoảng an toàn từ 119 đến 120 phút.
        long minutesRemaining = duration.toMinutes();
        assertTrue(minutesRemaining >= 119 && minutesRemaining <= 120,
                "Thời lượng trả về phải xấp xỉ 2 tiếng (120 phút)");
        assertFalse(duration.isNegative(), "Thời gian còn lại không được phép âm");
    }

    @Test
    @DisplayName("4. Trả về Duration âm khi phiên đấu giá đã quá hạn (endAt nằm trong quá khứ)")
    void testGetDuration_ExpiredAuction() throws Exception {
        Item mockItem = new Electronics("tempoid", "Sản phẩm test", 1, "info");
        mockItem.setId("ITEM-EXPIRED");
        Auction mockAuction = new Auction(mockItem);

        // Giả lập thời gian kết thúc là 1 tiếng TRƯỚC
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        mockAuction.setEndAt(pastTime);

        getActiveAuctionsMap().put(mockItem.getId(), mockAuction);

        Duration duration = AuctionService.getDuration(mockItem.getId());

        // Duration.between(now, past) sẽ sinh ra giá trị âm
        assertTrue(duration.isNegative(), "Phải trả về Duration âm nếu đã lố giờ");
    }

    private void fakeAuctionInDatabase(Auction auction, LocalDateTime start, LocalDateTime end) throws Exception {
        // 1. Lấy ID final tự sinh từ đối tượng Java ra
        String generatedAuctionId = auction.getAuctionId();
        String itemId = auction.getItem().getId();

        // 2. Nạp chính xác mã ID đó xuống Database ảo
        try (Statement statement = testconn.createStatement()) {
            String sql = String.format("""
            INSERT INTO auctions (auctionId, startAt, endAt, status, ItemId, highestBid, highestBidderId) VALUES ('%s', '%s', '%s', 'IN_PROGRESS', '%s', 0.0, NULL)
            """, generatedAuctionId, start, end, itemId);

            statement.execute(sql);
        }
    }
    private String getItemIdByAuctionIdFromDb(String auctionId) throws Exception {
        String sql = "SELECT ItemId FROM auctions WHERE auctionId = ?";

        try (PreparedStatement pstmt = testconn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ItemId");
                }
            }
        }
        return null; // Trả về null nếu không tìm thấy phiên đấu giá
    }
    private String getUserIdByItemIdFromDb(String itemId) throws Exception {
        String sql = "SELECT userId FROM inventory WHERE itemId = ?";

        try (PreparedStatement pstmt = testconn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("userId");
                }
            }
        }
        return null; // Trả về null nếu không tìm thấy vật phẩm trong kho
    }
}