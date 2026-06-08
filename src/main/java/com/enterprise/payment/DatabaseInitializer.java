//package com.enterprise.payment;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.stereotype.Component;
//
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.sql.Statement;
//
//@Component
//public class DatabaseInitializer {
//
//    @PostConstruct
//    public void init() throws SQLException {
//
//        Connection connection =
//                DriverManager.getConnection(
//                        "jdbc:postgresql://localhost:5432/postgres",
//                        "postgres",
//                        "password"
//                );
//
//        Statement statement = connection.createStatement();
//
//        statement.execute(
//                "CREATE DATABASE payment_db"
//        );
//    }
//}
