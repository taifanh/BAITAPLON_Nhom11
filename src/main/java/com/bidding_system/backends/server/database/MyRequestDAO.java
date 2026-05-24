package com.bidding_system.backends.server.database;

import com.bidding_system.backends.common.messages.Common.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyRequestDAO {
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SOLD = "SOLD";
    public static final String STATUS_UNSOLD = "UNSOLD";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    //                STT INTEGER PRIMARY KEY AUTOINCREMENT,
    private static final Path DATA_DIRECTORY = Path.of("data");
    static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("my_request.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_REQUEST_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS my_request (
                request_id TEXT PRIMARY KEY,
                id_user TEXT,
                request_type TEXT ,
                request_info TEXT,
                send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status TEXT
            )
            """;

    public MyRequestDAO(){
        try{
            initializeRequest_Log();

        } catch (SQLException | IOException e) {
            throw new IllegalStateException(" KHONG THE KHOI TAO request database");
        }
    }
    // DÙNG HÀM này khi tạo mới 1 request
    public synchronized static void save_myrequest(Message message, String requestId) throws  IOException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO my_request (request_id, id_user , request_type , request_info , status) VALUES (?, ? , ? , ?  , ?)""")
        ){
            statement.setString(1, requestId);
            statement.setString(2,message.Id_user);
            statement.setString(3,message.messageType);
            statement.setString(4, message.payloadJson);
            statement.setString(5,STATUS_PENDING);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } ;
    }
    public MyRequestDAO.RequestRecord findByRequestId(String requestId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, id_user, request_type, request_info ,send_at, status
                     FROM my_request
                     WHERE request_id = ?
                     """)) {
            statement.setString(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new MyRequestDAO.RequestRecord(
                        resultSet.getString("request_id"),
                        resultSet.getString("id_user"),
                        resultSet.getString("request_type"),
                        resultSet.getString("request_info"),
                        resultSet.getString("send_at"),
                        resultSet.getString("status"));
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay request theo id", e);
        }
    }
    public String getStatusById(String request_id) {
        String sql = "SELECT status FROM my_request WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, request_id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("status");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public synchronized void updateRequestStatus(String requestId, String status) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 UPDATE my_request
                 SET status = ?
                 WHERE request_id = ?
                 """)) {
            statement.setString(1, status);
            statement.setString(2, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat trang thai my_request ở bảng dữ liệu", e);
        }
    }

    public List<RequestRecord> getMyRequestsByType(String requestType) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, id_user, request_type, request_info, send_at ,status
                     FROM my_request
                     WHERE request_type = ?
                     ORDER BY send_at ASC
                     """)) {
            statement.setString(1, requestType);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestRecord> requests = new ArrayList<>();
                while (resultSet.next()) {
                    requests.add(new RequestRecord(
                            resultSet.getString("request_id"),
                            resultSet.getString("id_user"),
                            resultSet.getString("request_type"),
                            resultSet.getString("request_info"),
                            resultSet.getString("send_at"),
                            resultSet.getString("status")));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay danh sach request", e);
        }
    }

    public synchronized void deleteMyRequests(List<Integer> requestIds) throws IOException {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM my_request
                     WHERE STT = ?
                     """)) {
            for (Integer requestId : requestIds) {
                statement.setInt(1, requestId);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IOException("Khong the xoa request", e);
        }
    }
    public synchronized void remove_request(String requestId) throws  IOException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
              DELETE FROM my_request
              WHERE request_id = ?
""")){
            statement.setString(1,requestId);
            statement.addBatch();
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private synchronized void initializeRequest_Log() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try(Connection conn = openConnection();
            Statement statement = conn.createStatement()){
            statement.executeUpdate(CREATE_REQUEST_TABLE_SQL);
        }
    }
    private void ensureDataDirectoryExists() throws IOException {
        if (Files.notExists(DATA_DIRECTORY)) {
            Files.createDirectories(DATA_DIRECTORY);
        }
    }
    private static Connection openConnection() throws SQLException{
        return DriverManager.getConnection(DATABASE_URL);
    }

    public record RequestRecord(String requestId, String userId, String requestType, String requestInfo, String time,
                                String status) {
    }
}