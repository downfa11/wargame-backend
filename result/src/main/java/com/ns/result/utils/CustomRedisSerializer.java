package com.ns.result.utils;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyOutputStream;

@RequiredArgsConstructor
public class CustomRedisSerializer<T> implements RedisSerializer<T> {

    private static final byte[] SNAPPY_MAGIC_BYTES = new byte[]{(byte) 0xFF, (byte) 0xF3};

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

        try{
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            if (minCompressionSize == -1 || bytes.length > minCompressionSize) {
                return encodeSnappy(bytes);
            }

            return bytes;
        } catch(IOException e){
            throw new SerializationException("Error serializer");
        }
    }


    private byte[] encodeSnappy(byte[] original) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             SnappyOutputStream snappyOutputStream = new SnappyOutputStream(byteArrayOutputStream)) {

            snappyOutputStream.write(original);
            snappyOutputStream.close();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ex) {
            throw new SerializationException("Error encode to snappy", ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }

        try{
            if (isSnappyCompressed(bytes)) {
                byte[] decodeBytes = decodeSnappy(bytes);
                return objectMapper.readValue(decodeBytes, 0, decodeBytes.length, typeReference);
            }
            return objectMapper.readValue(bytes, 0, bytes.length, typeReference);
        } catch(IOException e){
            throw new SerializationException("Error deserializer");
        }
    }

    private byte[] decodeSnappy(byte[] encoded) {
        try {
            return Snappy.uncompress(encoded);
        } catch (IOException ex) {
            throw new SerializationException("Error decode snappy", ex);
        }
    }

    private boolean isSnappyCompressed(byte[] bytes){
        return bytes.length > 2 && bytes[0] == SNAPPY_MAGIC_BYTES[0] && bytes[1] == SNAPPY_MAGIC_BYTES[1];
    }
}

