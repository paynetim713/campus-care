package com.campuscare.app.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static String baseUrl = "https://campus-care-backend-production.up.railway.app/";

    private static Retrofit  retrofit   = null;
    private static ApiService apiService = null;
    private static String    authToken  = null;

    public static void setAuthToken(String token) { authToken = token; }
    public static String getAuthToken()           { return authToken; }
    public static void clearAuthToken()           { authToken = null; }

    public static String getBaseUrl() {

        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static String toAbsoluteUrl(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;

        String base = getBaseUrl();
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    public static void clearAll() {
        authToken  = null;
        retrofit   = null;
        apiService = null;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(5,  TimeUnit.SECONDS)
                    .readTimeout(10,    TimeUnit.SECONDS)
                    .writeTimeout(10,   TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(String.class, new JsonDeserializer<String>() {
                        @Override
                        public String deserialize(JsonElement json, Type typeOfT,
                                                  JsonDeserializationContext ctx)
                                throws JsonParseException {
                            if (json == null || json.isJsonNull()) return null;
                            if (json.isJsonPrimitive()) {
                                JsonPrimitive prim = json.getAsJsonPrimitive();
                                if (prim.isNumber()) return String.valueOf(prim.getAsLong());
                                return prim.getAsString();
                            }
                            return json.toString();
                        }
                    })
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static ApiService getService() {
        if (apiService == null) apiService = getClient().create(ApiService.class);
        return apiService;
    }

    public static void updateBaseUrl(String newBaseUrl) {
        if (newBaseUrl != null && !newBaseUrl.isEmpty()) {
            baseUrl    = newBaseUrl.endsWith("/") ? newBaseUrl : newBaseUrl + "/";
            retrofit   = null;
            apiService = null;
        }
    }
}