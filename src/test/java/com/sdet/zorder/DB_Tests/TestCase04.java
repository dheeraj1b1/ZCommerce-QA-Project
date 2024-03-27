
package com.sdet.zorder.DB_Tests;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.Assert;
import org.testng.annotations.*;

import com.sdet.zorder.API_Tests.TestCase01;

import io.restassured.RestAssured;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class TestCase04 {
    private String baseURI;
    private String dbURI;
    private String dbName;
    private String dbUsername;
    private String dbPwd;
    private String email;
    private String validAddress;

    public TestCase04() throws IOException, ClassNotFoundException {

        Properties prop = new Properties();
        // Load the properties file
        FileInputStream input = new FileInputStream("src/test/resources/application.properties");
        prop.load(input);
        // TODO: Load the JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        baseURI = prop.getProperty("baseURI");
        RestAssured.baseURI = baseURI;
        dbURI = prop.getProperty("dbURI");
        dbName = prop.getProperty("dbName");
        dbUsername = prop.getProperty("dbUsername");
        dbPwd = prop.getProperty("dbPwd");
        email = prop.getProperty("email");
        validAddress = prop.getProperty("validAddress");
    }

    @Test
    public void testOrderInsertAndCascadeDelete() {

        System.out.println("Starting test- Should not be able to breach foreign key constraints");
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://" + dbURI + dbName, dbUsername, dbPwd)) {
            conn.setAutoCommit(false); // Start transaction
            // Perform 10 insert operations into the orders table
            String insertOrderSQL = "INSERT INTO orders (created_at, updated_at, version, amount, billing_address, shipping_address, status, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            for (int i = 0; i < 10; i++) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertOrderSQL,
                        Statement.RETURN_GENERATED_KEYS)) {
                    // Populate prepared statement with appropriate values, example given
                    insertStmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
                    insertStmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                    insertStmt.setLong(3, 1L); // Version
                    insertStmt.setDouble(4, 100.0 * (i + 1)); // Example amount
                    insertStmt.setString(5, validAddress);
                    insertStmt.setString(6, validAddress);
                    insertStmt.setString(7, "IN_PROGRESS");
                    insertStmt.setString(8, email);
                    // TODO: Change the next line of code to adapt to DML statements
                    int affectedRows = insertStmt.executeUpdate();
                    Assert.assertEquals(affectedRows, 1, "Failed to insert into orders table");
                    String insertTrackingSQL = "INSERT INTO tracking (created_at, updated_at, version, tracking_status, url) VALUES (?, ?, ?, ?, ?)";
                    // Get the generated order ID to use in order_item_details insert
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long orderId = generatedKeys.getLong(1);

                            // Perform insert operation into order_item_details table
                            String insertOrderDetailsSQL = "INSERT INTO order_item_details (order_id, item_id, quantity) VALUES (?, ?, ?)";
                            try (PreparedStatement detailsStmt = conn.prepareStatement(insertOrderDetailsSQL)) {
                                detailsStmt.setLong(1, orderId);
                                detailsStmt.setLong(2, i + 1); // Item ID, simple incrementing value as example
                                detailsStmt.setInt(3, 10); // Quantity

                                affectedRows = detailsStmt.executeUpdate();
                                Assert.assertEquals(affectedRows, 1, "Failed to insert into order_item_details table.");
                            } catch (AssertionError e) {
                                System.out.println("Failed to insert into order_item_details table");
                                throw e;
                            }
                        } else {
                            System.out.println("Failed to retrieve order ID after insertion");
                            Assert.fail("Failed to retrieve order ID after insertion.");
                        }
                    }
                } catch (AssertionError e) {
                    System.out.println("Failed to insert into orders table");
                }
            }
            long orderIdToDelete = -1;
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT MAX(id) AS max_id FROM orders")) {
                // If there's a result, get the highest order id
                if (rs.next()) {
                    orderIdToDelete = rs.getLong("max_id");
                }
            } catch (SQLException e) {
                System.out.println("Error while querying");
            }
            try {
                String deleteOrderSQL = "DELETE FROM orders WHERE id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteOrderSQL)) {
                    deleteStmt.setLong(1, orderIdToDelete);
                    int affectedRows = deleteStmt.executeUpdate();
                    System.out.println("Deletion success! Foreign key constraint failed!");
                    Assert.fail("Deletion success! Foreign key constraint failed!");
                }
            } catch (SQLException e) {

                System.out.println("Deletion prevented- data integrity maintained");
                System.out.println("Cleaning up - Running rollback");
                try {
                    // TODO: Roll back the previous DML statement to prevent DB alteration
                    conn.rollback();

                    System.out.println(
                            "Test Case `testOrderInsertAndCascadeDelete` success. Rollback successful, no changes were made to the database");
                } catch (SQLException excep) {
                    System.out.println("Error occurred during rollback: " + excep.getMessage());
                }
            }
            conn.commit(); // Commit transaction if all operations were successful
        } catch (SQLException e) {
            System.out.println("SQLException occurred: " + e.getMessage());
            Assert.fail("SQLException occurred: " + e.getMessage());
        }
    }
}
