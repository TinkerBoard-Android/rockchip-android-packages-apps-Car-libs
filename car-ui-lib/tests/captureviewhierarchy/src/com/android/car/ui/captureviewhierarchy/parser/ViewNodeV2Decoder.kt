/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.ui.captureviewhierarchy.parser

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.HashMap

/**
 * Decodes view hierarchy v2 protocol created by ViewHierarchyEncoder in Android framework.
 */
internal class ViewNodeV2Decoder(private val mBuf: ByteBuffer) {
    // Prefixes for simple primitives. These match the JNI definitions.
    private val SIG_BOOLEAN = 'Z'.toByte()
    private val SIG_BYTE = 'B'.toByte()
    private val SIG_SHORT = 'S'.toByte()
    private val SIG_INT = 'I'.toByte()
    private val SIG_LONG = 'J'.toByte()
    private val SIG_FLOAT = 'F'.toByte()
    private val SIG_DOUBLE = 'D'.toByte()

    // Prefixes for some commonly used objects
    private val SIG_STRING = 'R'.toByte()

    private val SIG_MAP = 'M'.toByte() // a map with an short key
    private val SIG_END_MAP: Short = 0

    fun hasRemaining(): Boolean {
        return mBuf.hasRemaining()
    }

    fun readObject(): Any {
        val sig = mBuf.get()
        return when (sig) {
            SIG_BOOLEAN -> mBuf.get().toInt() != 0
            SIG_BYTE -> mBuf.get()
            SIG_SHORT -> mBuf.short
            SIG_INT -> mBuf.int
            SIG_LONG -> mBuf.long
            SIG_FLOAT -> mBuf.float
            SIG_DOUBLE -> mBuf.double
            SIG_STRING -> readString()
            SIG_MAP -> readMap()
            else -> throw DecoderException(
              sig,
              mBuf.position() - 1
            )
        }
    }

    private fun readString(): String {
        val len = mBuf.short.toInt()
        val b = ByteArray(len)
        mBuf.get(b, 0, len)
        return String(b, Charset.forName("utf-8"))
    }

    private fun readMap(): Map<Short, Any> {
        val m = HashMap<Short, Any>()

        while (true) {
            val o = readObject()
            if (o !is Short) {
                break
            }

            if (o == SIG_END_MAP) {
                break
            }

            m[o] = readObject()
        }

        return m
    }

    class DecoderException : RuntimeException {
        constructor(
            seen: Byte,
            pos: Int
        ) : super(String.format("Unexpected byte %c seen at position %d", seen.toChar(), pos))

        constructor(msg: String) : super(msg)
    }
}
