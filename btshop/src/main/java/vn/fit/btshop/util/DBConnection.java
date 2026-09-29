package vn.fit.btshop.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mariadb://localhost:3306/storedb";
    private static final String USER = "root";
    private static final String PASSWORD = "sapassword"; // Đổi password tại đây nếu cần

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy MariaDB JDBC Driver", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}