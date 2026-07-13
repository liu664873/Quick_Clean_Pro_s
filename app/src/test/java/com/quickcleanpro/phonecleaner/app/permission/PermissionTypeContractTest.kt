package com.quickcleanpro.phonecleaner.common.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionTypeContractTest {
    @Test
    fun `permission keys are unique and reversible`() {
        val keys = PermissionType.entries.map(PermissionType::key)

        assertEquals(keys.size, keys.distinct().size)
        PermissionType.entries.forEach { permission ->
            assertEquals(permission, PermissionType.fromKey(permission.key))
        }
    }
}
