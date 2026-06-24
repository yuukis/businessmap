package com.github.yuukis.businessmap.app

/**
 * Runs [MainActivity]'s real startup path while reporting every runtime
 * permission as missing, regardless of permission state left by another test.
 */
class FakeMissingPermissionsMainActivity : MainActivity() {

    override fun isPermissionGranted(permission: String): Boolean = false
}
