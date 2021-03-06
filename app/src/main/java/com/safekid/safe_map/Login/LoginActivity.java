package com.safekid.safe_map.Login;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Bundle;

import com.safekid.safe_map.http.CommonMethod;
import com.safekid.safe_map.MainActivity;
import com.safekid.safe_map.common.ProfileData;
import com.safekid.safe_map.http.RequestHttpURLConnection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.safekid.safe_map.R;
import com.kakao.auth.ApiErrorCode;
import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.LoginButton;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.usermgmt.response.model.Profile;
import com.kakao.usermgmt.response.model.UserAccount;
import com.kakao.util.OptionalBoolean;
import com.kakao.util.exception.KakaoException;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;

public class LoginActivity extends AppCompatActivity {
    private LoginButton login;
    private final String TAG = "LoginActivity";
    private SessionCallback sessionCallback = new SessionCallback();;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button ChildLoginButton = findViewById(R.id.child_login);
        login = findViewById(R.id.login);

        ChildLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ChildLoginActivity.class);
                startActivity(intent);
                finish();

            }
        });

        session = Session.getCurrentSession();
        session.addCallback(sessionCallback);
        session.checkAndImplicitOpen();

        login.setOnClickListener(v -> {
            if (Session.getCurrentSession().checkAndImplicitOpen()) {
                Log.d(TAG, "onClick: ????????? ??????????????????");
                // ????????? ????????? ?????? (?????? ?????????.)
                sessionCallback.sessionRequest();

            } else {
                Log.d(TAG, "onClick: ????????? ????????????");
                // ????????? ????????? ?????? (?????? ??????.)
                session.open(AuthType.KAKAO_LOGIN_ALL, LoginActivity.this);
            }
        });

        getAppKeyHash();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.i("KAKAO_API", "onActivityResult " + requestCode+ ":"+ resultCode+"::"+ data);
        // ????????????|????????? ??????????????? ?????? ????????? ????????? SDK??? ??????
        if(Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ?????? ?????? ??????
        Session.getCurrentSession().removeCallback(sessionCallback);
    }

    private class SessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() {
            sessionRequest();
        }

        // ???????????? ????????? ??????
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            Toast.makeText(getApplicationContext(), "????????? ?????? ????????? ??????????????????. ????????? ????????? ??????????????????: "+exception.toString(), Toast.LENGTH_SHORT).show();
            Log.e("SessionCallback :: ", "onSessionOpenFailed : " + exception.getMessage());
        }

        // ????????? ?????? ??????
        public void sessionRequest() {
            UserManagement.getInstance().me(new MeV2ResponseCallback() {
                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Toast.makeText(getApplicationContext(),"????????? ???????????????. ?????? ????????? ?????????: "+errorResult.getErrorMessage(),Toast.LENGTH_SHORT).show();
                    Log.e("KAKAO_API", "????????? ?????? ??????: " + errorResult);
                }

                @Override
                public void onFailure(ErrorResult errorResult) {
                    int result = errorResult.getErrorCode();

                    if(result == ApiErrorCode.CLIENT_ERROR_CODE) {
                        Toast.makeText(getApplicationContext(), "???????????? ????????? ??????????????????. ?????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(),"????????? ?????? ????????? ??????????????????: "+errorResult.getErrorMessage(),Toast.LENGTH_SHORT).show();
                    }

                    Log.e("KAKAO_API", "????????? ?????? ?????? ??????: " + errorResult);
                }

                @Override
                public void onSuccess(MeV2Response result) {
                    Intent intent = null;

                    Log.i("KAKAO_API", "????????? ?????????: " + result.getId());
                    String id = String.valueOf(result.getId());
                    UserAccount kakaoAccount = result.getKakaoAccount();

                    if (kakaoAccount != null) {
                        // ?????????
                        String email = kakaoAccount.getEmail();

                        // ????????? (?????????, ????????? ?????? ??????
                        Profile profile = kakaoAccount.getProfile();
                        new ProfileData(id, profile.getNickname(), profile.getProfileImageUrl(), profile.getThumbnailImageUrl());

                        // login ???????????? ????????? ?????? IP?????? ?????? ??? ?????? ??????, (?????? ?????? ??? ?????? ?????? ?????? ???)
                        login(id, profile.getNickname());
                        String childNum = fetchChildNum(ProfileData.getUserId());
                        if (childNum.equals("")) {
                            intent = new Intent(getApplicationContext(), Signup.class);
                            Log.i("?????????", "wjwd: " + "("+ childNum + ")");
                        } else {
                            intent = new Intent(getApplicationContext(), MainActivity.class);
                            Log.i("?????????", "?????? ????????? ?????????: " + "("+ childNum + ")");
                        }

                        intent.putExtra("userId", id);
                        intent.putExtra("nickName", profile.getNickname());
                        intent.putExtra("profile", profile.getProfileImageUrl());
                        intent.putExtra("thumbnail", profile.getThumbnailImageUrl());

                        if(result.getKakaoAccount().hasEmail() == OptionalBoolean.TRUE)
                            intent.putExtra("email", result.getKakaoAccount().getEmail());
                        else
                            intent.putExtra("email", "none");

                        // LOGGING
                        if (profile ==null){
                            Log.d("KAKAO_API", "onSuccess:profile null ");
                        }else{
                            Log.d("KAKAO_API", "onSuccess:getProfileImageUrl "+profile.getProfileImageUrl());
                            Log.d("KAKAO_API", "onSuccess:getThumbnailImageUrl "+profile.getThumbnailImageUrl());
                            Log.d("KAKAO_API", "onSuccess:getNickname "+profile.getNickname());
                        }
                        if (email != null) {
                            Log.d("KAKAO_API", "onSuccess:email "+email);
                        }
                    }else{
                        Log.i("KAKAO_API", "onSuccess: kakaoAccount null");
                    }
                    startActivity(intent);
                    finish();
                }
            });
        }

        // ????????? ????????????
        public void login(String userId, String userName) {
            Log.w("login","????????? ?????????");
            try {
                Log.w("????????? ?????????",userId+", "+userName);

                CustomTask task = new CustomTask();
                String result = task.execute(userId,userName).get();
                Log.w("?????????",result);

            } catch (Exception e) {
                Log.w("????????? ??????", e);
            }
        }

    }

    class CustomTask extends AsyncTask<String, Void, String> {
        String sendMsg, receiveMsg;
        @Override
        // doInBackground??? ???????????? ?????? ???????????? ????????? ?????? ?????????
        protected String doInBackground(String... strings) {
            try {
                String str;
                URL url = new URL(CommonMethod.ipConfig +"/api/member");  // ?????? ????????? ????????????(localhost ??????.)
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");                              //???????????? POST ???????????? ???????????????.
                conn.setDoOutput(true);

                // ????????? ?????? ??? ????????? ?????????.
                OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
                sendMsg = "userId="+strings[0]+"&userName="+strings[1]; // GET???????????? ????????? POST??? ?????? ex) "id=admin&pwd=1234";
                osw.write(sendMsg);                           // OutputStreamWriter??? ?????? ??????
                osw.flush();
                Log.i("?????? ???", "test");
                // jsp??? ????????? ??? ??????, ???????????? ?????? ??? ??????.
                if(conn.getResponseCode() == conn.HTTP_OK) {
                    InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuffer buffer = new StringBuffer();
                    Log.i("?????? ??????", "test1");
                    while ((str = reader.readLine()) != null) {
                        buffer.append(str);
                        Log.i("?????? ??????", "test2");
                    }
                    Log.i("?????? ??????", "test3"+buffer);
                    receiveMsg = buffer.toString();
                    Log.i("?????? ??????", receiveMsg);
                } else {    // ????????? ????????? ????????? ???????????? ??????
                    Log.i("?????? ??????", conn.getResponseCode()+"??????");
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // ???????????? ?????? ?????? ???????????????.
            return receiveMsg;
        }
    }

    public String fetchChildNum(String userId){
        String url = CommonMethod.ipConfig + "/api/fetchChildNum";
        String rtnStr= "";

        try{
            String jsonString = new JSONObject()
                    .put("userId", userId)
                    .toString();
            //REST API
            RequestHttpURLConnection.NetworkAsyncTask networkTask = new RequestHttpURLConnection.NetworkAsyncTask(url, jsonString);
            rtnStr = networkTask.execute().get();
//            Toast.makeText(Signup.this, "?????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
            Log.i(TAG, String.format("????????? childNum: (%s)", rtnStr));
        }catch(Exception e){
            e.printStackTrace();
        }
        return rtnStr;
    }



    private void getAppKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.e("Hash key", something);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString());
        }
    }
}