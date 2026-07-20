/**
 * Đăng ký, đăng nhập, refresh token và các luồng xác thực.
 * Chia theo tầng, cùng tinh thần {@code com.laptophub.common}:
 * {@code dto} (request/response/internal carrier), {@code service} (business
 * use case), {@code entity} (RefreshToken/RevokeReason), {@code repository},
 * {@code token} (cookie/crypto helper cho refresh token). AuthController và
 * AuthConfig ở lại package gốc — không có tầng riêng nào phù hợp hơn.
 */
package com.laptophub.auth;
