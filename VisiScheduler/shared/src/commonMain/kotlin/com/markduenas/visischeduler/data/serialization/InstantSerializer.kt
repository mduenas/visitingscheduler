package com.markduenas.visischeduler.data.serialization

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer for kotlin.time.Instant.
 * Serializes Instant as ISO-8601 string.
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlin.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

/**
 * Nullable Instant serializer.
 */
object NullableInstantSerializer : KSerializer<Instant?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlin.time.Instant?", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant?) {
        if (value != null) {
            encoder.encodeString(value.toString())
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): Instant? {
        return try {
            val str = decoder.decodeString()
            if (str.isEmpty()) null else Instant.parse(str)
        } catch (e: Exception) {
            null
        }
    }
}
