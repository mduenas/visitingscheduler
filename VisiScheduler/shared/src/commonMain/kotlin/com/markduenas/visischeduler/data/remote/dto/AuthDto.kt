package com.markduenas.visischeduler.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String? = null
)

@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

@Serializable
data class TokenRefreshRequestDto(
    val refreshToken: String
)
