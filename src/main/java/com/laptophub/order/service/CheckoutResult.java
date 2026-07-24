package com.laptophub.order.service;

import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderItem;

import java.util.List;

public record CheckoutResult(Order order, List<OrderItem> items) {
}
