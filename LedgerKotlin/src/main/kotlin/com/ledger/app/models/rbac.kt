package com.ledger.app.models

data class Role(
    val name: String,
    val description: String,
    val permissions: MutableSet<Permission> = mutableSetOf()
)

data class Permission(
    val resource: String,    // Format: "ledger:finance", "entry", "user", etc.
    val action: String       // Format: "read", "write", "create", "delete", etc.
)