package com.lux.ex086retrofit2imageupload;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RetrofitService {

    @Multipart
    @POST("05Retrofit/fileUpload.php")
    Call<String> uploadImage(@Part MultipartBody.Part file);

}
