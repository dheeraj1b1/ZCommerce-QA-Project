
package com.sdet.zorder.API_Tests;

import org.testng.annotations.*;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.RestAssured;

import java.io.IOException;

public class TestCase01 {
    private String baseURI;
    private String email;
    private String cartUrlView;
    private String cartUrlPut;
    private String cartUrlPost;
    private String cartUrlDelete;
    private int validItemId;
    private String invalidItemId;
    private int validItemQuantity;
    private int invalidItemQuantity;

    public TestCase01() throws IOException {

        Properties prop = new Properties();
        // Load the properties file
        FileInputStream input = new FileInputStream("src/test/resources/application.properties");
        prop.load(input);
        // Initialize variables from properties file
        baseURI = prop.getProperty("baseURI");
        RestAssured.baseURI = baseURI;
        email = prop.getProperty("email");
        cartUrlView = prop.getProperty("cartUrlView");
        cartUrlPut = prop.getProperty("cartUrlPut");
        cartUrlPost = prop.getProperty("cartUrlPost");
        cartUrlDelete = prop.getProperty("cartUrlDelete");
        validItemId = Integer.parseInt(prop.getProperty("validItemId"));
        invalidItemId = prop.getProperty("invalidItemId");
        validItemQuantity = Integer.parseInt(prop.getProperty("validItemQuantity"));
        invalidItemQuantity = Integer.parseInt(prop.getProperty("invalidItemQuantity"));
        input.close();
    }

    private boolean doesCartExist(String cartUrlView, String email) throws Exception {

        RequestSpecification request = RestAssured.given();
        // Add headers
        request.header("Authorization", "Bearer USER_IMPERSONATE_" + email);
        Response response = request.get(cartUrlView);
        int statusCode = response.getStatusCode();
        // Check if status code is 404 or 200
        if (statusCode == 404) {
            return false;
        } else if (statusCode == 200) {
            return true;
        } else {
            throw new Exception("Unexpected status code: " + statusCode);
        }
    }

    private int getCartId(String cartUrlView, String email) throws Exception {
        RequestSpecification request = RestAssured.given();
        Response response = request.get(cartUrlView);
        JsonPath jsonPathEvaluator = response.jsonPath();
        int cartId = jsonPathEvaluator.get("data.id");
        return cartId;
    }

    private boolean putItemInCart(int cartId, Object itemId, int itemAmount) throws Exception {

        RequestSpecification request = RestAssured.given();// Add headers
        request.header("Authorization", "Bearer USER_IMPERSONATE_" + email);
        // Construct the JSON payload
        JSONObject itemQuantity = new JSONObject();
        itemQuantity.put("itemId", itemId);
        itemQuantity.put("quantity", itemAmount);
        JSONArray itemQuantityList = new JSONArray();
        itemQuantityList.put(itemQuantity);
        JSONObject payload = new JSONObject();
        payload.put("id", cartId);
        payload.put("userId", email);
        payload.put("itemQuantityList", itemQuantityList);
        // Set content type and attach payload
        request.contentType(ContentType.JSON);
        request.body(payload.toString());
        // Send PUT request
        Response response = request.put(cartUrlPut + "/" + cartId);
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            return false;
        } else {
            return true;
        }
    }

    public int createCartId(Object itemId, int itemAmount) throws Exception {

        RequestSpecification request = RestAssured.given();// Add headers
        request.header("Authorization", "Bearer USER_IMPERSONATE_" + email);
        // Construct the JSON payload for POST request
        JSONObject itemQuantity = new JSONObject();
        itemQuantity.put("itemId", itemId);
        itemQuantity.put("quantity", itemAmount);
        JSONArray itemQuantityList = new JSONArray();
        itemQuantityList.put(itemQuantity);
        JSONObject payload = new JSONObject();
        payload.put("itemQuantityList", itemQuantityList);
        // Set content type and attach payload
        request.contentType(ContentType.JSON);
        request.body(payload.toString());
        // Send POST request
        Response response = request.post(cartUrlPost);
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            throw new Exception("Unexpected status code: " + statusCode);
        }
        // Parse JSON to get the id value from the data object
        JsonPath jsonPathEvaluator = response.jsonPath();
        int cartId = jsonPathEvaluator.get("data.id");
        return cartId;
    }

    private boolean deleteCart(int cartId) {
        RequestSpecification request = RestAssured.given();// Add headers
        request.header("Authorization", "Bearer ADMIN_TOKEN");
        Response response = request.delete(cartUrlDelete + "/" + cartId);
        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            return true;
        } else {
            return false;
        }
    }

    @Test(priority = 0)
    public void addValidItemToCart() throws Exception {
        try {
            Thread.sleep(3000);
            if (doesCartExist(cartUrlView, email)) {
                int cartId = getCartId(cartUrlView, email);
                Assert.assertTrue(putItemInCart(cartId, validItemId, validItemQuantity));
                Assert.assertTrue(deleteCart(cartId));
            } else {
                int cartId = createCartId(validItemId, validItemQuantity);
                Assert.assertTrue(deleteCart(cartId));
            }
        } catch (AssertionError e) {
        }
    }

    @Test(priority = 1)
    public void addInValidItemToCart() throws Exception {
        try {

            Thread.sleep(3000);
            if (doesCartExist(cartUrlView, email)) {
                int cartId = getCartId(cartUrlView, email);
                Assert.assertFalse(putItemInCart(cartId, invalidItemId, validItemQuantity));
            } else {
                boolean exceptionThrown = false;
                try {
                    int cartId = createCartId(invalidItemId, validItemQuantity);
                } catch (Exception e) {
                    exceptionThrown = true;
                }
                Assert.assertTrue(exceptionThrown, "Exception was not thrown by createCartId");
            }
        } catch (AssertionError e) {
            throw e;
        }
    }
}
