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

    static ApiResponse getUser(String userId) throws IOException {
        return request("GET", "api/usuario/" + userId, null);
    }

    static ApiResponse createUser(String username, String password, String email, String name)
            throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("nombreUsuario", username);
        requestBody.put("contrasena", password);
        requestBody.put("correo", email);
        requestBody.put("nombre", name);
        return request("POST", "api/usuarios", requestBody);
    }

    static ApiResponse updateUser(
            String userId,
            String username,
            String password,
            String email,
            String name
    ) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("nombreUsuario", username);
        requestBody.put("correo", email);
        requestBody.put("nombre", name);
        if (password != null && !password.trim().isEmpty()) {
            requestBody.put("contrasena", password);
        }
        return request("PUT", "api/usuario/" + userId, requestBody);
    }

    static ApiResponse deleteUser(String userId) throws IOException {
        return request("DELETE", "api/usuario/" + userId, null);
    }

    static Usuario parseUser(String responseBody) throws JSONException {
        JSONObject item = new JSONObject(responseBody);
        return new Usuario(
                item.optString("id"),
                item.optString("nombreUsuario"),
                item.optString("nombre"),
                item.optString("correo")
        );
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

    static ApiResponse getFarms() throws IOException {
        return request("GET", "api/fincas", null);
    }

    static ApiResponse getFarm(String farmId) throws IOException {
        return request("GET", "api/finca/" + farmId, null);
    }

    static ApiResponse createFarm(
            String name,
            double areaHa,
            double areaM2,
            String ownerId,
            String managerId,
            String country,
            String department,
            String city,
            boolean producesMilk,
            boolean producesCereals,
            boolean producesFruits,
            boolean producesVegetables
    ) throws IOException, JSONException {
        return request("POST", "api/fincas", farmRequestBody(
                name, areaHa, areaM2, ownerId, managerId, country, department, city,
                producesMilk, producesCereals, producesFruits, producesVegetables));
    }

    static ApiResponse updateFarm(
            String farmId,
            String name,
            double areaHa,
            double areaM2,
            String ownerId,
            String managerId,
            String country,
            String department,
            String city,
            boolean producesMilk,
            boolean producesCereals,
            boolean producesFruits,
            boolean producesVegetables
    ) throws IOException, JSONException {
        return request("PUT", "api/finca/" + farmId, farmRequestBody(
                name, areaHa, areaM2, ownerId, managerId, country, department, city,
                producesMilk, producesCereals, producesFruits, producesVegetables));
    }

    static ApiResponse deleteFarm(String farmId) throws IOException {
        return request("DELETE", "api/finca/" + farmId, null);
    }

    private static JSONObject farmRequestBody(
            String name,
            double areaHa,
            double areaM2,
            String ownerId,
            String managerId,
            String country,
            String department,
            String city,
            boolean producesMilk,
            boolean producesCereals,
            boolean producesFruits,
            boolean producesVegetables
    ) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("nombreFinca", name);
        body.put("areaHa", areaHa);
        body.put("areaM2", areaM2);
        body.put("propietarioId", ownerId);
        body.put("capatazId", nullableJsonValue(managerId));
        body.put("pais", nullableJsonValue(country));
        body.put("departamento", nullableJsonValue(department));
        body.put("ciudad", nullableJsonValue(city));
        body.put("produceLeche", producesMilk);
        body.put("produceCereales", producesCereals);
        body.put("produceFrutas", producesFruits);
        body.put("produceVerduras", producesVegetables);
        return body;
    }

    private static Object nullableJsonValue(String value) {
        return value == null || value.trim().isEmpty() ? JSONObject.NULL : value;
    }

    static List<Finca> parseFarms(String responseBody) throws JSONException {
        JSONArray array = new JSONArray(responseBody);
        List<Finca> farms = new ArrayList<>(array.length());
        for (int index = 0; index < array.length(); index++) {
            farms.add(parseFarm(array.getJSONObject(index)));
        }
        return farms;
    }

    static Finca parseFarm(String responseBody) throws JSONException {
        return parseFarm(new JSONObject(responseBody));
    }

    private static Finca parseFarm(JSONObject item) {
        return new Finca(
                item.optString("id"),
                item.optString("nombreFinca"),
                item.optDouble("areaHa", 0),
                item.optDouble("areaM2", 0),
                parseUserReference(item.optJSONObject("propietario")),
                parseUserReference(item.optJSONObject("capataz")),
                nullableString(item, "pais"),
                nullableString(item, "departamento"),
                nullableString(item, "ciudad"),
                item.optBoolean("produceLeche", false),
                item.optBoolean("produceCereales", false),
                item.optBoolean("produceFrutas", false),
                item.optBoolean("produceVerduras", false)
        );
    }

    private static Usuario parseUserReference(JSONObject item) {
        if (item == null) {
            return null;
        }
        return new Usuario(
                item.optString("id"),
                item.optString("nombreUsuario"),
                item.optString("nombre"),
                ""
        );
    }

    private static String nullableString(JSONObject item, String key) {
        return item.isNull(key) ? null : item.optString(key, null);
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
        return "La solicitud no pudo completarse (HTTP " + statusCode + ").";
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
