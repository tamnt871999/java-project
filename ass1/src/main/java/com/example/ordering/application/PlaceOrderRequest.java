package com.example.ordering.application;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FORM OBJECT - du lieu tho ma Controller nhan tu View gui len.
 *
 * Toan bo la kieu nguyen thuy vi no den truc tiep tu HTML form hoac tu ban
 * phim. Viec kiem tra tinh hop le la trach nhiem cua Service, khong phai
 * cua Controller.
 */
public class PlaceOrderRequest {

    private String customerId;
    private String shippingAddress;
    /** productId -> so luong */
    private final Map<String, Integer> items = new LinkedHashMap<>();

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(String customerId, String shippingAddress) {
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
    }

    public PlaceOrderRequest addItem(String productId, int quantity) {
        if (quantity > 0) {
            items.merge(productId, quantity, Integer::sum);
        }
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Map<String, Integer> getItems() {
        return items;
    }
}
