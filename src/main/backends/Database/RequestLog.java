package Database;

import models.Extra.IdGenerator;
import models.Extra.messages.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestLog {
    private static final Path DATA_DIRECTORY = Path.of("data");
    static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("request_log.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_REQUEST_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS request_log (
                request_id TEXT PRIMARY KEY,
                id_user TEXT,
                request_type TEXT ,
                request_info TEXT,
                selected BOOLEAN
            )
            """;

    public RequestLog(){
        try{
            initializeRequest_Log();

        } catch (SQLException | IOException e) {
            throw new IllegalStateException(" KHONG THE KHOI TAO request database");
        }
    }
    // ============dùng cho khi add new item ============================
    public static String save_request(Message message) throws  IOException {
        String requestId = "REQ" + IdGenerator.nextId();
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO request_log (request_id, id_user , request_type , request_info, selected ) VALUES (?, ? , ? , ? , ?)""")
        ){
          statement.setString(1, requestId);
          statement.setString(2,message.Id_user);
          statement.setString(3,message.messageType);
          statement.setString(4, message.payloadJson);
          statement.setBoolean(5,false);
          statement.executeUpdate();
          return requestId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<RequestRecord> getRequestsByType(String requestType) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, id_user, request_type, request_info , selected
                     FROM request_log
                     WHERE request_type = ?
                     ORDER BY request_id ASC
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
                            resultSet.getBoolean("selected")
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
                     SELECT request_id, id_user, request_type, request_info , selected
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
                        resultSet.getBoolean("selected"));
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay request theo id", e);
        }
    }

    public void deleteRequests(List<String> requestIds) throws IOException {
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
    public void set_selected_request(String request_id){
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
            UPDATE request_log
            SET selected = ?
            WHERE request_id = ?
""")){
            statement.setBoolean(1,true);
            statement.setString(2,request_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public List<RequestLog.RequestRecord> selected_requests() throws IOException{
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT request_id, id_user, request_type, request_info , selected
                     FROM request_log
                     WHERE selected = true
                    ORDER  BY request_id ASC
                    """)){
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestRecord> requests = new ArrayList<>();
                while(resultSet.next()){
                    requests.add(new RequestRecord(
                            resultSet.getString("request_id"),
                            resultSet.getString("id_user"),
                            resultSet.getString("request_type"),
                            resultSet.getString("request_info"),
                            resultSet.getBoolean("selected")
                    ));
            }
                return requests;
            }
        } catch (Exception e){
            throw new IOException("cannot load requests", e);
        }
    }

    private void initializeRequest_Log() throws IOException, SQLException {
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

    public record RequestRecord(String id, String userId, String requestType, String requestInfo, boolean selected) {
    }
}
