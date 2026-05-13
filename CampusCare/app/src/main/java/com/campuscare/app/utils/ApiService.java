package com.campuscare.app.utils;

import com.campuscare.app.models.AuthResponse;
import com.campuscare.app.models.ChatMessage;
import com.campuscare.app.models.Notification;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.UploadResponse;
import com.campuscare.app.models.User;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body Map<String, String> request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body Map<String, String> request);

    @POST("api/auth/forgot-password")
    Call<Void> forgotPassword(@Body Map<String, String> request);

    @POST("api/auth/reset-password")
    Call<Void> resetPassword(@Body Map<String, String> request);

    @POST("api/auth/change-password")
    Call<Void> changePassword(@Header("X-Auth-Token") String token,
                              @Body Map<String, String> request);

    @POST("api/auth/admin/create-user")
    Call<Map<String, Object>> adminCreateUser(@Header("X-Auth-Token") String token,
                                              @Body Map<String, String> body);

    @GET("api/auth/users")
    Call<List<Map<String, Object>>> adminListUsers(@Header("X-Auth-Token") String token);

    @GET("api/repairs/mine")
    Call<List<Ticket>> getMyRepairs(@Header("X-Auth-Token") String token);

    @GET("api/repairs/{id}")
    Call<Ticket> getRepairById(@Header("X-Auth-Token") String token,
                               @Path("id") Long id);

    @POST("api/repairs")
    Call<Ticket> createRepair(@Header("X-Auth-Token") String token,
                              @Body Map<String, Object> request);

    @DELETE("api/repairs/{id}")
    Call<Void> deleteRepair(@Header("X-Auth-Token") String token,
                            @Path("id") Long id);

    @POST("api/repairs/{id}/rate")
    Call<Ticket> rateRepair(@Header("X-Auth-Token") String token,
                            @Path("id") Long id,
                            @Body Map<String, Object> request);

    @PATCH("api/repairs/{id}/photo")
    Call<Ticket> updateRepairPhoto(@Header("X-Auth-Token") String token,
                                   @Path("id") Long id,
                                   @Body Map<String, String> body);

    @GET("api/repairs/available")
    Call<List<Ticket>> getAvailableRepairs(@Header("X-Auth-Token") String token);

    @GET("api/repairs/assigned")
    Call<List<Ticket>> getAssignedRepairs(@Header("X-Auth-Token") String token);

    @PATCH("api/repairs/{id}/accept")
    Call<Ticket> acceptRepair(@Header("X-Auth-Token") String token,
                              @Path("id") Long id);

    @PATCH("api/repairs/{id}/task")
    Call<Ticket> updateRepairTask(@Header("X-Auth-Token") String token,
                                  @Path("id") Long id,
                                  @Body Map<String, Object> request);

    @GET("api/repairs")
    Call<List<Ticket>> getAllRepairs(@Header("X-Auth-Token") String token);

    @PATCH("api/repairs/{id}/assign")
    Call<Ticket> assignRepair(@Header("X-Auth-Token") String token,
                              @Path("id") Long id,
                              @Body Map<String, Object> body);

    @PATCH("api/repairs/{id}/admin-reply")
    Call<Ticket> saveAdminReply(@Header("X-Auth-Token") String token,
                                @Path("id") Long id,
                                @Body Map<String, String> body);

    @GET("api/users/technicians")
    Call<List<User>> getTechnicians(@Header("X-Auth-Token") String token);

    @DELETE("api/auth/users/{id}")
    Call<Void> deleteUser(@Header("X-Auth-Token") String token,
                          @Path("id") Long id);

    @GET("api/notifications")
    Call<List<Notification>> getNotifications(@Header("X-Auth-Token") String token);

    @PATCH("api/notifications/{id}/read")
    Call<Void> markNotificationRead(@Header("X-Auth-Token") String token,
                                    @Path("id") Long id);

    @PATCH("api/notifications/read-all")
    Call<Void> markAllNotificationsRead(@Header("X-Auth-Token") String token);

    @DELETE("api/notifications/{id}")
    Call<Void> deleteNotification(@Header("X-Auth-Token") String token,
                                  @Path("id") Long id);

    @GET("api/chat/{ticketId}/{channel}")
    Call<List<ChatMessage>> getChatMessages(@Header("X-Auth-Token") String token,
                                            @Path("ticketId") Long ticketId,
                                            @Path("channel") String channel);

    @POST("api/chat/{ticketId}/{channel}")
    Call<ChatMessage> sendChatMessage(@Header("X-Auth-Token") String token,
                                      @Path("ticketId") Long ticketId,
                                      @Path("channel") String channel,
                                      @Body Map<String, String> body);

    @Multipart
    @POST("api/uploads/photo")
    Call<UploadResponse> uploadPhoto(@Header("X-Auth-Token") String token,
                                     @Part MultipartBody.Part file);
}