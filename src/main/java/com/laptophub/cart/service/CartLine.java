package com.laptophub.cart.service;

import com.laptophub.cart.entity.CartItem;
import com.laptophub.catalog.entity.ProductVariant;

// Ghép 1 CartItem với ProductVariant hiện tại của nó — dùng để controller
// build response có giá/tên đọc live, không cần CartItem tự lưu giá.
public record CartLine(CartItem item, ProductVariant variant) {
}
