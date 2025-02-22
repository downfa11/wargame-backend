package com.ns.result.utils;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

@RequiredArgsConstructor
public class CustomRedisSerializer<T> implements RedisSerializer<T> {

    private final ObjectMapper objectMapper;
    private final TypeReference<T> typeReference;

    private final int minCompressionSize;


    public CustomRedisSerializer(ObjectMapper objectMapper, TypeReference<T> typeReference) {
        this.objectMapper = objectMapper;
        this.typeReference = typeReference;
        this.minCompressionSize = -1;
    }

    @Override
    public byte[] serialize(T value) throws SerializationException {
        if (value == null) {
            return null;
        }

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            if (minCompressionSize == -1 || bytes.length > minCompressionSize) {
                return encodeSnappy(bytes);
            }

            return bytes;
        } catch (IOException e) {
            throw new SerializationException("Error serializer");
        }
    }


    private byte[] encodeSnappy(byte[] original) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             SnappyOutputStream snappyOutputStream = new SnappyOutputStream(byteArrayOutputStream)) {

            snappyOutputStream.write(original);
            snappyOutputStream.flush();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ex) {
            throw new SerializationException("Error encode to snappy " + ex.getMessage());
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }

        try {
            if (isSnappyCompressed(bytes)) {
                byte[] decodeBytes = decodeSnappy(bytes);
                return objectMapper.readValue(decodeBytes, 0, decodeBytes.length, typeReference);
            }
            return objectMapper.readValue(bytes, 0, bytes.length, typeReference);
        } catch (IOException e) {
            throw new SerializationException("Error deserializer " + e.getMessage());
        }
    }

    private byte[] decodeSnappy(byte[] encoded) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encoded);
             SnappyInputStream snappyInputStream = new SnappyInputStream(byteArrayInputStream)) {

            return snappyInputStream.readAllBytes();
        } catch (IOException ex) {
            throw new SerializationException("Error decoding snappy: " + ex.getMessage());
        }
    }

    private boolean isSnappyCompressed(byte[] bytes) {
        if (bytes.length < 8) {
            return false;
        }

        // \x82SNAPPY\x00
        return bytes[0] == (byte) 0x82 && bytes[1] == 'S' &&
                bytes[2] == 'N' && bytes[3] == 'A' &&
                bytes[4] == 'P' && bytes[5] == 'P' &&
                bytes[6] == 'Y' && bytes[7] == (byte) 0x00;
    }
}

