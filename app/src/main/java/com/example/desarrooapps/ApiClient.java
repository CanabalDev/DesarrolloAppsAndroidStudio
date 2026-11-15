package com.example.desarrooapps;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ApiClient {
    private static final String BASE_URL = "https://desarrollodeapps-production.up.railway.app/";

    private ApiClient() {
    }

    static ApiResponse login(String username, String password) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + "api/usuarios/iniciar-sesion");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            JSONObject requestBody = new JSONObject();
            requestBody.put("nombreUsuario", username);
            requestBody.put("contrasena", password);

            byte[] body = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int statusCode = connection.getResponseCode();
            InputStream responseStream = statusCode >= 200 && statusCode < 400
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            return new ApiResponse(statusCode, readBody(responseStream));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static String errorMessage(String responseBody, int statusCode) {
        try {
            String message = new JSONObject(responseBody).optString("mensaje").trim();
            if (!message.isEmpty()) {
                return message;
            }
        } catch (JSONException ignored) {
        }
        return "No fue posible iniciar sesión (HTTP " + statusCode + ").";
    }

    private static String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    static final class ApiResponse {
        final int statusCode;
        final String body;

        ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
