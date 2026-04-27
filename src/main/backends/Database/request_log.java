package Database;

import models.Extra.messages.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

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
}
