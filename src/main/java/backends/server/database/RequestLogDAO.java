package backends.server.database;

import backends.common.models.Extra.IdGenerator;
import backends.common.messages.Common.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestLogDAO {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final Path DATA_DIRECTORY = Path.of("data");
    static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("app.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_REQUEST_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS request_log (
                request_id TEXT PRIMARY KEY,
                id_user TEXT,
                request_type TEXT ,
                request_info TEXT,
                send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                selected BOOLEAN,
                status TEXT DEFAULT 'PENDING'
            )
            """;

    public RequestLogDAO(){
        try{
            initializeRequest_Log();

        } catch (SQLException | IOException e) {
            throw new IllegalStateException(" KHONG THE KHOI TAO request database");
        }
    }
    // ============dùng cho khi add new item ============================
    public synchronized static String save_request(Message message) throws  IOException {
        String requestId = "REQ" + IdGenerator.nextId();
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO request_log (request_id, id_user , request_type , request_info, selected, status ) VALUES (?, ? , ? , ? , ?, ?)""")
        ){
          statement.setString(1, requestId);
          statement.setString(2,message.Id_user);
          statement.setString(3,message.messageType);
          statement.setString(4, message.payloadJson);
          statement.setBoolean(5,false);
          statement.setString(6, STATUS_PENDING);
          statement.executeUpdate();
          return requestId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<RequestRecord> getRequestsByType(String requestType) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, id_user, request_type, request_info ,send_at, selected, status
                     FROM request_log
                     WHERE request_type = ? AND status = ?
                     ORDER BY send_at ASC
                     """)) {
            statement.setString(1, requestType);
            statement.setString(2, STATUS_PENDING);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestRecord> requests = new ArrayList<>();
                while (resultSet.next()) {
                    requests.add(new RequestRecord(
                            resultSet.getString("request_id"),
                            resultSet.getString("id_user"),
                            resultSet.getString("request_type"),
                            resultSet.getString("request_info"),
                            resultSet.getString("send_at"),
                            resultSet.getBoolean("selected"),
                            resultSet.getString("status")
                    ));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay danh sach request", e);
        }
    }

    public RequestRecord findByRequestId(String requestId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, id_user, request_type, request_info ,send_at, selected, status
                     FROM request_log
                     WHERE request_id = ?
                     """)) {
            statement.setString(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new RequestRecord(
                        resultSet.getString("request_id"),
                        resultSet.getString("id_user"),
                        resultSet.getString("request_type"),
                        resultSet.getString("request_info"),
                        resultSet.getString("send_at"),
                        resultSet.getBoolean("selected"),
                        resultSet.getString("status"));
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay request theo id", e);
        }
    }
    public String getStatusById(String request_id) {
        String sql = "SELECT status FROM request_log WHERE request_id = ?";
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
                     UPDATE request_log
                     SET status = ?, selected = false
                     WHERE request_id = ?
                     """)) {
            statement.setString(1, status);
            statement.setString(2, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat trang thai request", e);
        }
    }

    public synchronized void deleteRequests(List<String> requestIds) throws IOException {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM request_log
                     WHERE request_id = ?
                     """)) {
            for (String requestId : requestIds) {
                statement.setString(1, requestId);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IOException("Khong the xoa request", e);
        }
    }

    public String getUserbyRequestId(String requestId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id_user
                     FROM request_log
                     WHERE request_id = ?
                     """)) {
            statement.setString(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getString("id_user");
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay user id theo request id", e);
        }
    }
    public synchronized void removeRequest(String requestId) throws IOException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
             DELETE FROM request_log
             WHERE request_id = ?
""")){
            statement.setString(1,requestId);
            if (request_exist(requestId)) {
                statement.addBatch();
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean request_exist(String requestId) throws IOException {
        try( Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
            SELECT EXISTS(
            SELECT 1
            FROM request_log
            WHERE request_id = ?)
            AS is_exists;
""")){
            statement.setString(1,requestId);
            try(ResultSet resultSet = statement.executeQuery()){
                return resultSet.next() && resultSet.getInt(1) ==1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private synchronized void initializeRequest_Log() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try(Connection conn = openConnection();
            Statement statement = conn.createStatement()){
            statement.executeUpdate(CREATE_REQUEST_TABLE_SQL);
            ensureStatusColumnExists(conn);
        }
    }
    private void ensureStatusColumnExists(Connection connection) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "request_log", "status")) {
            if (!columns.next()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE request_log ADD COLUMN status TEXT DEFAULT 'PENDING'");
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE request_log SET status = 'PENDING' WHERE status IS NULL OR status = ''");
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

    public record RequestRecord(String requestId, String userId, String requestType, String requestInfo,String time, boolean selected, String status) {
    }
}
