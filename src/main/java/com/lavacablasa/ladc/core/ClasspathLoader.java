package com.lavacablasa.ladc.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ClasspathLoader {
    public static byte[] load(String resource) {
        try (InputStream input = ClasspathLoader.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Resource not found " + resource);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load resource " + resource, e);
        }
    }

}
