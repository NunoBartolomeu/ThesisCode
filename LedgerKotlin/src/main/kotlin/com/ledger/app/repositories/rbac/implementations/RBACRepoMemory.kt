package com.ledger.app.repositories.rbac.implementations

import com.ledger.app.models.Permission
import com.ledger.app.models.Role
import com.ledger.app.repositories.rbac.RBACRepo
import java.util.concurrent.ConcurrentHashMap

class RBACRepoMemory: RBACRepo {
    private val roles = ConcurrentHashMap<String, Role>()
    private val userRoles = ConcurrentHashMap<String, MutableSet<String>>() // userId -> roleNames
    private val userPermissions = ConcurrentHashMap<String, MutableSet<Permission>>() // userId -> permissions

    override fun saveRole(role: Role): Boolean {
        roles[role.name] = role
        return true
    }

    override fun getRole(roleName: String): Role? = roles[roleName]

    override fun getAllRoles(): List<Role> = roles.values.toList()

    override fun updateRole(role: Role): Boolean = saveRole(role)

    override fun assignRoleToUser(userId: String, roleName: String): Boolean {
        userRoles.computeIfAbsent(userId) { mutableSetOf() }.add(roleName)
        return true
    }

    override fun removeRoleFromUser(userId: String, roleName: String): Boolean {
        userRoles[userId]?.remove(roleName)
        return true
    }

    override fun getUserRoles(userId: String): List<Role> =
        userRoles[userId]?.mapNotNull { roles[it] } ?: emptyList()

    override fun saveUserPermission(userId: String, permission: Permission): Boolean {
        userPermissions.computeIfAbsent(userId) { mutableSetOf() }.add(permission)
        return true
    }

    override fun removeUserPermission(userId: String, permission: Permission): Boolean {
        userPermissions[userId]?.remove(permission)
        return true
    }

    override fun getUserPermissions(userId: String): Set<Permission> =
        userPermissions[userId]?.toSet() ?: emptySet()

}