package com.richard.retrohall.emulator

import org.junit.Assert.assertNotNull
import org.junit.Test

class LibretroHostSignatureTest {
    @Test
    fun exposesExpectedNativeMethods() {
        val methodNames = LibretroHost::class.java.declaredMethods.map { it.name }.toSet()

        assertNotNull(methodNames.find { it == "nativeVersion" })
        assertNotNull(methodNames.find { it == "loadCore" })
        assertNotNull(methodNames.find { it == "unloadCore" })
        assertNotNull(methodNames.find { it == "loadGame" })
        assertNotNull(methodNames.find { it == "runFrame" })
        assertNotNull(methodNames.find { it == "serializeState" })
        assertNotNull(methodNames.find { it == "unserializeState" })
        assertNotNull(methodNames.find { it == "saveSram" })
        assertNotNull(methodNames.find { it == "setInputState" })
    }
}
