package com.example.hellocowcow.data.recovery

internal fun byteArrayOf(vararg values: Number): ByteArray =
  ByteArray(values.size) { index -> values[index].toByte() }
