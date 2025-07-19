package com.ledger.app.services.rbac

import com.ledger.app.models.Permission
import com.ledger.app.models.Role

interface RBACService {
    fun hasPermission(userId: String, resource: String, action: String): Boolean //Main checking function
    fun createRole(name: String, description: String): Role?
    fun assignRoleToUser(userId: String, roleName: String): Boolean
    fun removeRoleFromUser(userId: String, roleName: String): Boolean
    fun assignPermissionToUser(userId: String, resource: String, action: String): Boolean
    fun removePermissionFromUser(userId: String, resource: String, action: String): Boolean
    fun addPermissionToRole(roleName: String, resource: String, action: String): Boolean
    fun removePermissionFromRole(roleName: String, resource: String, action: String): Boolean
    fun getUserRoles(userId: String): List<Role>
    fun getUserPermissions(userId: String): Set<Permission>
    fun getRolesPermissions(roleName: String): Set<Permission>
}