package com.ledger.app.services.rbac.implementations

import com.ledger.app.models.Permission
import com.ledger.app.models.Role
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.services.rbac.RBACRepo
import com.ledger.app.services.rbac.RBACService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB

class RBACServiceSpring(
    private val repo: RBACRepo,
    private val ledgerService: LedgerService,
): RBACService {
    private val RBAC_SYSTEM = "rbac"
    private val RBAC_LEDGER = "rbac_ledger"
    private val logger = ColorLogger("RbacService", RGB.ORANGE_SOFT, LogLevel.DEBUG)

    override fun hasPermission(userId: String, resource: String, action: String): Boolean {
        val direct = repo.getUserPermissions(userId).any { it.resource == resource && it.action == action }
        val viaRoles = repo.getUserRoles(userId).flatMap { it.permissions }.any { it.resource == resource && it.action == action }
        val ok = direct || viaRoles
        ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, userId, "User ${if (ok) "has" else "doesn't have"} permission to perform $action on $resource")
        logger.info("User $userId access to $resource:$action = $ok")
        return ok
    }

    override fun assignRoleToUser(userId: String, roleName: String) = repo.assignRoleToUser(userId, roleName).also {
        if (it) {
            logger.info("Role $roleName assigned to user $userId")
            ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, userId, "Assigned role $roleName to user")
        } else logger.warn("Role $roleName couldn't be assigned to user $userId")
    }

    override fun removeRoleFromUser(userId: String, roleName: String) = repo.removeRoleFromUser(userId, roleName).also {
        if (it) {
            logger.info("Role $roleName revoked to user $userId")
            ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, userId, "Revoked role $roleName to user")
        } else logger.info("Role $roleName could not be revoked to user $userId")
    }

    override fun assignPermissionToUser(userId: String, resource: String, action: String) =
        repo.saveUserPermission(userId, Permission(resource, action)).also {
            if (it) {
                logger.info("Permission $resource:$action granted to user $userId")
                ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, userId, "Granted permission $resource:$action to user")
            } else logger.warn("Permission $resource:$action couldn't be granted to user $userId")
        }

    override fun removePermissionFromUser(userId: String, resource: String, action: String) =
        repo.removeUserPermission(userId, Permission(resource, action)).also {
            if (it) {
                logger.info("Permission $resource:$action removed from user $userId")
                ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, userId, "Removed permission $resource:$action from user")
            } else logger.warn("Permission $resource:$action couldn't be removed from user $userId")
        }

    override fun createRole(name: String, description: String): Role? {
        val role = Role(name, description)
        return if (repo.saveRole(role)) {
            logger.info("Role $name was created")
            ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, null, "Created role $name")
            role
        } else {
            logger.error("Failed to create role $name")
            null
        }
    }

    override fun addPermissionToRole(roleName: String, resource: String, action: String): Boolean {
        val role = repo.getRole(roleName) ?: return logger.error("Cannot add permission, role $roleName not found").let { false }
        role.permissions + Permission(resource, action)
        return repo.updateRole(role).also {
            if (it) {
                logger.info("Permission $resource:$action added to role $roleName")
                ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, null, "Added permission $resource:$action to role $roleName")
            } else logger.warn("Permission $resource:$action couldn't be added to role $roleName")
        }
    }

    override fun removePermissionFromRole(roleName: String, resource: String, action: String): Boolean {
        val role = repo.getRole(roleName) ?: return logger.error("Cannot remove permission, role $roleName not found").let { false }
        role.permissions.remove(Permission(resource, action))
        return repo.updateRole(role).also {
            if (it) {
                logger.info("Permission $resource:$action removed from role $roleName")
                ledgerService.logSystemEvent(RBAC_LEDGER, RBAC_SYSTEM, null, "Removed permission $resource:$action from role $roleName")
            } else logger.warn("Permission $resource:$action couldn't be removed from role $roleName")
        }
    }

    override fun getUserRoles(userId: String): List<Role> =
        repo.getUserRoles(userId)

    override fun getUserPermissions(userId: String): Set<Permission> =
        repo.getUserPermissions(userId)

    override fun getRolesPermissions(roleName: String): Set<Permission> =
        repo.getRole(roleName)!!.permissions
}