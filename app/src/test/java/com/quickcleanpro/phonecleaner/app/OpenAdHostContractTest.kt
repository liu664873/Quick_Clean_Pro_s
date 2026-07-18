package com.quickcleanpro.phonecleaner.app

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAdHostContractTest {
    @Test
    fun activityResultsAlwaysCompleteContract() {
        val contract = OpenAdHostContract()

        assertEquals(Unit, contract.parseResult(Activity.RESULT_OK, null))
        assertEquals(Unit, contract.parseResult(Activity.RESULT_CANCELED, null))
    }
}
