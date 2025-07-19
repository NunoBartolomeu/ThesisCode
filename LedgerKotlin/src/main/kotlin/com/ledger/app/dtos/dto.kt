package com.ledger.app.dtos

data class LoginRequest(val email: String, val passwordHash: ByteArray)
data class RegisterRequest(val email: String, val passwordHash: ByteArray, val fullName: String)
data class VerifyCodeRequest(val email: String, val code: String)
data class ValidateTokenRequest(val token: String)
data class ChangePasswordRequest(val email: String, val oldPasswordHash: ByteArray, val newPasswordHash: ByteArray)
data class Verify2FARequest(val userId: String, val code: String)
data class RoleRequest(val name: String, val description: String)
data class RoleAssignmentRequest(val userId: String, val roleName: String)
data class PermissionRequest(val userId: String, val resource: String, val action: String)
data class RolePermissionRequest(val roleName: String, val resource: String, val action: String)
data class SavePublicKeyRequest(val userId: String, val publicKey: ByteArray)
