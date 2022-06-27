package com.lux.ex086retrofit2imageupload;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lux.ex086retrofit2imageupload.databinding.ActivityMainBinding;

import java.io.File;
import java.net.URI;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSelect.setOnClickListener(view -> clickSelect());
        binding.btnUpload.setOnClickListener(view -> clickUpload());

        //외부저장소 사용에 대한 동적 퍼미션
        String[] permissions=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (checkSelfPermission(permissions[0])== PackageManager.PERMISSION_DENIED){
                requestPermissions(permissions,0);
        }
    }
    void clickSelect(){
        //사진 앱 or 갤러리앱을 실행해서 업로드 할 사진 선택 - 결과를 받기 위한 액티비티 실행
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");  //MIME 타입
        resultLauncher.launch(intent);
    }
    ActivityResultLauncher<Intent> resultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            //사진 앱에서 사진 선택을 안 하고 돌아왔을 수 있기 때문에
            if (result.getResultCode()==RESULT_OK){
                //선택된 이미지의 uri를 얻어오기
                //Intent intent=result.getData();
                //Uri uri=intent.getData();
                Uri uri=result.getData().getData();
                Glide.with(MainActivity.this).load(uri).into(binding.iv);

                //uri는 실제 파일의 경로 주소가 아니라 Android에서 사용하는 Resource(자원)의 DB주소임
                //- 일명 : content 주소
                //new AlertDialog.Builder(MainActivity.this).setMessage(uri.toString()).create().show();
                //서버에 파일을 업로드하려면 실제 물리적인 File 주소가 필요함. - 절대경로라고 부름

                //그래서 uri--> 절대주소(String)로 변환 [외부저장소에 대한 동적 퍼미션 필요]
                imgPath=getRealPathFromUri(uri);
                new AlertDialog.Builder(MainActivity.this).setMessage(imgPath.toString()).create().show();
            }
        }
    });

    //선택한 이미지의 절대경로 - 멤버 변수
    String imgPath;


    //Uri -- > 절대경로로 바꿔서 리턴시켜주는 메소드
    String getRealPathFromUri(Uri uri){
        String[] proj= {MediaStore.Images.Media.DATA};
        CursorLoader loader= new CursorLoader(this, uri, proj, null, null, null);
        Cursor cursor= loader.loadInBackground();
        int column_index= cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result= cursor.getString(column_index);
        cursor.close();
        return  result;
    }

    void clickUpload(){
        //Retrofit 라이브러리를 이용하여 이미지 업로드

        //1. Retrofit 객체 생성 - 서버로부터 echo 결과를 String으로 돌려받는
        Retrofit.Builder builder=new Retrofit.Builder();
        builder.baseUrl("http://sens0104.dothome.co.kr/");
        builder.addConverterFactory(ScalarsConverterFactory.create());
        Retrofit retrofit=builder.build();

        //2. Retrofit 동작에 대한 인터페이스 설계 및 추상메소드 정의한 후에 객체 생성
        RetrofitService retrofitService=retrofit.create(RetrofitService.class);

        //3. File을 MultiPartBody.Part 로 패킷화(포장) 하여 업로드 해줘야 함.
        File file=new File(imgPath);
        RequestBody requestBody= RequestBody.create(MediaType.parse("image/*"),file);
        MultipartBody.Part part=MultipartBody.Part.createFormData("img",file.getName(),requestBody); //식별자, 파일명, 요청바디
        //4. 추상 메소드를 실행하여 기능 네트워크 작업을 수행하는 call 객체를 리턴받기
        Call<String> call =retrofitService.uploadImage(part);

        //5. 네트워크 통신 시작
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                String s=response.body();
                new AlertDialog.Builder(MainActivity.this).setMessage(s).create().show();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(MainActivity.this, "fail"+t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "외부 저장소 사용 가능", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "이미지 파일의 업로드 불가능합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}