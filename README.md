# Auction Bidding System

## 1. Mô tả bài toán và phạm vi hệ thống

Đây là hệ thống đấu giá trực tuyến theo mô hình `client-server`. Người dùng có thể đăng ký tài khoản, đăng nhập, nạp tiền, gửi yêu cầu bán vật phẩm, tham gia đấu giá và theo dõi lịch sử đặt giá. Phía quản trị viên có thể duyệt yêu cầu đăng bán, quản lý kho vật phẩm, tạo phiên đấu giá và theo dõi lịch sử đấu giá.

Phạm vi hiện tại của hệ thống gồm:

- Ứng dụng desktop JavaFX cho người dùng và quản trị viên.
- Server socket xử lý kết nối TCP tại cổng `9999`.
- Lưu trữ dữ liệu bằng SQLite trong file `data/app.db`.
- Đồng bộ dữ liệu đấu giá, lịch sử bid và trạng thái vật phẩm giữa client và server.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt

### Công nghệ sử dụng

- Java `25`
- JavaFX `25`
- Maven
- SQLite JDBC
- Gson / Jackson
- JUnit 5

### Môi trường chạy

- Hệ điều hành: Windows, Linux, macOS
- JDK: khuyến nghị `JDK 25`
- Maven: có thể dùng Maven cài sẵn hoặc Maven Wrapper trong repo

### Yêu cầu cài đặt (với lập trình viên)

1. Cài `JDK 25`.
2. Cấu hình biến môi trường `JAVA_HOME` trỏ tới thư mục JDK.
3. Kiểm tra:

```bash
java -version
```

Nếu dùng Maven cài sẵn:

```bash
mvn -version
```

Nếu dùng Maven Wrapper:

- Windows: `.\mvnw.cmd`
- Linux/macOS: `./mvnw`

Lưu ý: trong môi trường hiện tại, Maven Wrapper chỉ chạy khi `JAVA_HOME` đã được cấu hình.

## 3. Cấu trúc thư mục và module chính

```text
.
├── data/                       # SQLite database, được tạo/sử dụng khi chạy server
├── dist/                       # Thư mục chứa những thứ người dùng chỉ cần tải về
├── src/
│   ├── main/
│   │   ├── java/com/bidding_system/backends/
│   │   │   ├── client/         # JavaFX client application, controller, session
│   │   │   ├── common/         # Model, message, constant dùng chung
│   │   │   ├── launcher/       # Entry point để chạy server/client
│   │   │   └── server/         # Server socket, service, DAO, auction handler
│   │   └── resources/
│   │       ├── css/            # Giao diện CSS
│   │       └── views/          # File FXML cho user/admin
│   └── test/                   # Unit test cho model, DAO, bidding flow
├── target/                     # Artifact build ra bởi Maven
├── pom.xml                     # Cấu hình Maven
├── mvnw / mvnw.cmd             # Maven Wrapper
└── README.md
```

### Các module chính

- `client`: giao diện người dùng, đăng nhập, đấu giá, lịch sử, nạp tiền, quản lý hồ sơ.
- `server`: xử lý socket, nghiệp vụ đấu giá, quản lý phiên đấu giá, thao tác cơ sở dữ liệu.
- `common`: DTO/message, model domain, constants dùng chung cho cả client và server.
- `launcher`: lớp khởi chạy riêng cho server và client.

## 4. Câu lệnh dòng lệnh để build và chạy chương trình

### Build project

#### Dùng Maven cài sẵn

```bash
mvn clean package
```

#### Dùng Maven Wrapper

Windows:

```powershell
.\mvnw.cmd clean package
```

Linux / macOS:

```bash
chmod +x mvnw
./mvnw clean package
```

Sau khi build thành công, thư mục `dist/` sẽ có:

- `dist/BiddingSystem-server.jar`
- `dist/BiddingSystem-client.jar`

## 5. Hướng dẫn chạy Server/Client theo thứ tự cụ thể

### Cách dùng cho mọi người 
1. Tải LauncherApp.zip trong mục release (https://github.com/taifanh/BAITAPLON_Nhom11/releases/tag/BiddingSystem)

2. Chạy file `Start-Server.bat` để chạy máy chủ (không dành cho user thông thường)

3. Chạy file `Start-Client.bat` để chạy chương trình đấu giá (dành cho user thông thường tham gia vào hệ thống đáu giá)
   
(`Note`: File .bat và .sh đã được tự động cài đặt môi trường cho người dùng nên người dùng không cần cài thêm gì cả. Người dùng chọn phiên bản phù hợp với hệ điều hành của máy `Wins` , `Linux / MacOs`)   
(`Note`: đảm bảo `server` đã được mở thì `client` mới có thể giao tiếp)

### Cách chạy khuyến nghị cho coder

1. Build project:

```bash
mvn clean package
```

2. Mở terminal thứ nhất, chạy server:

```bash
java -jar dist/BiddingSystem-server.jar
```

3. Chờ khi console hiện thông báo server đã khởi động ở cổng `9999`.

4. Mở terminal thứ hai, chạy client:

```bash
java -jar dist/BiddingSystem-client.jar
```

5. Nếu cần mở nhiều người dùng cùng lúc, có thể chạy lệnh client ở nhiều terminal khác nhau trên cùng máy.

### Ghi chú vận hành

- Client hiện mặc định kết nối tới `localhost:9999`.
- Khi chạy server lần đầu, hệ thống sẽ tự tạo thư mục/file dữ liệu SQLite nếu chưa tồn tại.
- `ServerLauncher` sẽ khởi tạo sẵn tài khoản admin mặc định:
  - Số điện thoại: `12345`
  - Mật khẩu: `admin`

## 6. Chức năng đã hoàn thành

### Phía người dùng

- Đăng ký tài khoản.
- Đăng nhập hệ thống.
- Nạp tiền vào tài khoản.
- Tạo và xem danh sách vật phẩm/phòng đấu giá.
- Tham gia đặt giá trong phiên đấu giá.
- Đấu giá nhiều sản phẩm cùng lúc.
- Đấu giá thủ công
- Đấu giá auto-bid.
- Xem lịch sử đấu giá cá nhân.
- Gửi yêu cầu đăng bán vật phẩm.
- Xem và quản lý yêu cầu bán của chính mình.
- Biểu đồ cột real-time.

### Phía quản trị viên

- Xem hồ sơ admin.
- Xem danh sách yêu cầu đăng bán.
- Duyệt hoặc từ chối yêu cầu đăng bán.
- Quản lý inventory theo trạng thái (`waiting`, `scheduled`, `in_progress`, ...).
- Tạo / bắt đầu / kết thúc phiên đấu giá.
- Xem lịch sử bid của từng phiên đấu giá.

### Phía hệ thống

- Giao tiếp client-server qua socket.
- Lưu trữ dữ liệu bằng SQLite.
- Phục hồi các phiên đấu giá còn hiệu lực khi server khởi động lại.
- Ghi nhận bid history và trạng thái vật phẩm theo tiến trình đấu giá.

## 7. Kiểm tra nhanh

Lệnh build đã được kiểm tra thành công trong môi trường hiện tại:

```bash
mvn -DskipTests package
```

Nếu muốn chạy test:

```bash
mvn test
```
## 8. Video Demo và Báo cáo dự án PDF

### Video demo
[Video demo](https://drive.google.com/file/d/1qtW49C_exPqWpYeOgFfCZAtpT5PFRL9f/view?fbclid=IwY2xjawSNE4ZleHRuA2FlbQIxMQBzcnRjBmFwcF9pZAwzNTA2ODU1MzE3MjgAAR4YAU6tTcJVtXAFUeNh9hgWhzuHgTPhvVR0wTQQM8Uv3thxooXrM8B_SgkAug_aem_D9uYoMC2ZVJJaaekuMcMGQ)
  
### Báo cáo dự án
[Báo Cáo](https://drive.google.com/file/d/1Qf8C9TpSu6KL164oEvBL7gShfgL-6Aqk/view?usp=sharing)
