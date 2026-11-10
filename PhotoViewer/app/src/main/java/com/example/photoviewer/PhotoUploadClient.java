
package com.example.photoviewer;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PhotoUploadClient {

    private static final String BASE_URL =
            "https://samz21.pythonanywhere.com/";
    public static void uploadPhoto(
            Context context,
            Uri imageUri,
            String title,
            String description,
            String username,
            String password
    ) throws Exception {

        String token = getToken(username, password);

        String mimeType =
                context.getContentResolver().getType(imageUri);

        if (!"image/jpeg".equals(mimeType)
                && !"image/png".equals(mimeType)) {
            throw new IOException(
                    "Please select a JPEG or PNG photograph."
            );
        }

        String filename = "image/png".equals(mimeType)
                ? "photograph.png"
                : "photograph.jpg";

        String boundary =
                "NoirFrameBoundary" + System.currentTimeMillis();

        HttpURLConnection connection =
                (HttpURLConnection) new URL(
                        BASE_URL + "api_root/Post/"
                ).openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        connection.setRequestProperty(
                "Authorization", "Token " + token
        );

        connection.setRequestProperty(
                "Accept", "application/json"
        );

        connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=" + boundary
        );

        try {
            try (OutputStream output =
                         connection.getOutputStream()) {

                writeField(
                        output, boundary, "title", title
                );

                writeField(
                        output, boundary, "text", description
                );

                write(
                        output,
                        "--" + boundary + "\r\n"
                );

                write(
                        output,
                        "Content-Disposition: form-data; "
                                + "name=\"image\"; filename=\""
                                + filename + "\"\r\n"
                );

                write(
                        output,
                        "Content-Type: " + mimeType
                                + "\r\n\r\n"
                );

                try (InputStream imageStream =
                             context.getContentResolver()
                                     .openInputStream(imageUri)) {

                    if (imageStream == null) {
                        throw new IOException(
                                "Cannot open selected photograph."
                        );
                    }

                    byte[] buffer = new byte[8192];
                    int count;

                    while ((count = imageStream.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }

                write(output, "\r\n");

                write(
                        output,
                        "--" + boundary + "--\r\n"
                );

                output.flush();
            }

            int status = connection.getResponseCode();

            if (status != 201) {
                String details = readText(
                        connection.getErrorStream()
                );

                throw new IOException(
                        "Upload failed: HTTP " + status
                                + " " + details
                );
            }

        } finally {
            connection.disconnect();
        }
    }

    private static String getToken(
            String username,
            String password
    ) throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(
                        BASE_URL + "api-token-auth/"
                ).openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);

        connection.setRequestProperty(
                "Content-Type", "application/json"
        );

        connection.setRequestProperty(
                "Accept", "application/json"
        );

        JSONObject credentials = new JSONObject();

        credentials.put("username", username);
        credentials.put("password", password);

        try {
            try (OutputStream output =
                         connection.getOutputStream()) {

                write(
                        output,
                        credentials.toString()
                );
            }

            int status = connection.getResponseCode();

            if (status != 200) {
                throw new IOException(
                        "Authentication failed: HTTP " + status
                );
            }

            String response = readText(
                    connection.getInputStream()
            );

            JSONObject result = new JSONObject(response);

            String token = result.optString("token", "");

            if (token.isEmpty()) {
                throw new IOException(
                        "Authentication token is missing."
                );
            }

            return token;

        } finally {
            connection.disconnect();
        }
    }

    private static void writeField(
            OutputStream output,
            String boundary,
            String name,
            String value
    ) throws IOException {

        write(
                output,
                "--" + boundary + "\r\n"
        );

        write(
                output,
                "Content-Disposition: form-data; "
                        + "name=\"" + name + "\"\r\n"
        );

        write(
                output,
                "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
        );

        write(output, value + "\r\n");
    }

    private static void write(
            OutputStream output,
            String value
    ) throws IOException {

        output.write(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String readText(
            InputStream stream
    ) throws IOException {

        if (stream == null) {
            return "";
        }

        try (InputStream input = stream;
             ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            return new String(
                    output.toByteArray(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
