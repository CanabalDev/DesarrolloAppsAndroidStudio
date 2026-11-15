package com.example.desarrooapps;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class ApiClient {
    private static final String BASE_URL = "https://desarrollodeapps-production.up.railway.app/";

    private ApiClient() {
    }

    static ApiResponse login(String username, String password) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("nombreUsuario", username);
        requestBody.put("contrasena", password);
        return request("POST", "api/usuarios/iniciar-sesion", requestBody);
    }

    static ApiResponse getUsers() throws IOException {
        return request("GET", "api/usuarios", null);
    }

    static List<Usuario> parseUsers(String responseBody) throws JSONException {
        JSONArray array = new JSONArray(responseBody);
        List<Usuario> users = new ArrayList<>(array.length());
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            users.add(new Usuario(
                    item.optString("id"),
                    item.optString("nombreUsuario"),
                    item.optString("nombre"),
                    item.optString("correo")
            ));
        }
        return users;
    }

    private static ApiResponse request(String method, String path, JSONObject requestBody)
            throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");

            if (requestBody != null) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                byte[] body = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body);
                }
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
