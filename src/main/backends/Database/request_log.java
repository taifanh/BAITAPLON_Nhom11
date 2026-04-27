package Database;

import models.Extra.messages.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class request_log {
    private static final Path DATA_DIRECTORY = Path.of("data");
    static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("request_log.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_REQUEST_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS request_log (
                STT INTEGER PRIMARY KEY AUTOINCREMENT,
                id_user TEXT,
                request_type TEXT ,
                request_info TEXT
            )
            """;

    public request_log(){
        try{
            initializeRequest_Log();

        } catch (SQLException | IOException e) {
            throw new IllegalStateException(" KHONG THE KHOI TAO request database");
        }
    }

    public static void save_request(Message message) throws  IOException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO request_log (id_user , request_type , request_info) VALUES (? , ? , ?)""")
        ){
          statement.setString(1,message.Id_user);
          statement.setString(2,message.messageType);
          statement.setString(3, message.payloadJson);

          statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } ;
    }

    public List<RequestRecord> getRequestsByType(String requestType) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT STT, id_user, request_type, request_info
                     FROM request_log
                     WHERE request_type = ?
                     ORDER BY STT ASC
                     """)) {
            statement.setString(1, requestType);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestRecord> requests = new ArrayList<>();
                while (resultSet.next()) {
                    requests.add(new RequestRecord(
                            resultSet.getInt("STT"),
                            resultSet.getString("id_user"),
                            resultSet.getString("request_type"),
                            resultSet.getString("request_info")
                    ));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay danh sach request", e);
        }
    }

    public void deleteRequests(List<Integer> requestIds) throws IOException {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM request_log
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

    public record RequestRecord(int id, String userId, String requestType, String requestInfo) {
    }
}
