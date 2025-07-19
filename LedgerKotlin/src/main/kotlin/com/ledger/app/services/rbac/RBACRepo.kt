package com.ledger.app.services.rbac

import com.ledger.app.models.Permission
import com.ledger.app.models.Role

interface RBACRepo {
    // Role and permission data
    fun saveRole(role: Role): Boolean
    fun getRole(roleName: String): Role?
    fun getAllRoles(): List<Role>
    fun updateRole(role: Role): Boolean

    // User-role associations
    fun assignRoleToUser(userId: String, roleName: String): Boolean
    fun removeRoleFromUser(userId: String, roleName: String): Boolean
    fun getUserRoles(userId: String): List<Role>

    // Direct permissions
    fun saveUserPermission(userId: String, permission: Permission): Boolean
    fun removeUserPermission(userId: String, permission: Permission): Boolean
    fun getUserPermissions(userId: String): Set<Permission>
}