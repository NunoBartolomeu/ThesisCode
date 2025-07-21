package com.ledger.app.dtos

import java.time.LocalDateTime

data class LoginRequest(val email: String, val passwordHash: ByteArray)
data class RegisterRequest(val email: String, val passwordHash: ByteArray, val fullName: String)
data class VerifyCodeRequest(val email: String, val code: String)
data class ValidateTokenRequest(val token: String)
data class FileDetailsDto(val originalFileName: String, val actualFileName: String, val fileSize: Long, val contentType: String?, val uploadedAt: Long, val lastAccessed: Long?, val ownerFullName: String?, val ownerEmail: String?)