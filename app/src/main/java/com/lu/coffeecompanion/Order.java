package com.lu.coffeecompanion.models;

import java.util.ArrayList;

public class Order {
    private String orderId;
    private String userId;
    private String userName;
    private String userEmail;
    private ArrayList<OrderItem> items;
    private double total;
    private String status; // pending, processing, completed, cancelled
    private long timestamp;
    private String paymentMethod;
    private String deliveryAddress;

    public Order() {
        this.items = new ArrayList<>();
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    public Order(String orderId, String userId, String userName, String userEmail,
                 ArrayList<OrderItem> items, double total, String status, long timestamp,
                 String paymentMethod, String deliveryAddress) {
        this.orderId = orderId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.items = items != null ? items : new ArrayList<>();
        this.total = total;
        this.status = status;
        this.timestamp = timestamp;
        this.paymentMethod = paymentMethod;
        this.deliveryAddress = deliveryAddress;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public ArrayList<OrderItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<OrderItem> items) {
        this.items = items;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
}