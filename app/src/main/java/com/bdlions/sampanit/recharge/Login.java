package com.bdlions.sampanit.recharge;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bdlions.sampanit.database.DatabaseHelper;
import com.bdlions.sampanit.database.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Login extends AppCompatActivity {
    private ConnectivityManager cm;
    private NetworkInfo netInfo;
    private static EditText etOPCode, etLoginUserName, etPassword;
    private static String baseUrl = "";
    private static int localUserId = 0;
    private static String sessionId = "";
    private static String pinCode = "";
    private static String userName = "";
    private static String password = "";
    private static Button buttonLogin;
    private static DatabaseHelper eRchargeDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        etOPCode = (EditText) findViewById(R.id.etOPCode);
        etLoginUserName = (EditText) findViewById(R.id.etLoginUserName);
        etPassword = (EditText) findViewById(R.id.etPassword);

        eRchargeDB = DatabaseHelper.getInstance(this);
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        netInfo = cm.getActiveNetworkInfo();

        if(netInfo == null || !netInfo.isConnected()){
            Toast.makeText(getApplicationContext(), "Please connect to internet first.", Toast.LENGTH_SHORT).show();
        }
        else if(eRchargeDB.checkLogin() != false)
        {
            JSONObject localUserInfo =  eRchargeDB.getUserInfo();
            //opening the app with internet connection, previously logged in
            if(netInfo != null && netInfo.isConnected()){
                try {
                    localUserId = (int) localUserInfo.get("userId");
                    baseUrl = (String) localUserInfo.get("baseUrl");
                    sessionId = (String) localUserInfo.get("sessionId");
                    pinCode = (String) localUserInfo.get("pinCode");
                    userName = (String) localUserInfo.get("userName");
                    password = (String) localUserInfo.get("password");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                getUserInfo();
            }
            //opening the app without internet connection, previously logged in
            else
            {
                Toast.makeText(getApplicationContext(), "Please connect to internet first.", Toast.LENGTH_SHORT).show();
            }
        }
        onClickButtonLoginListener();
    }

    public void getUserInfo(){
        try
        {
            final ProgressDialog progress = new ProgressDialog(Login.this);
            progress.setTitle("Connecting");
            progress.setMessage("Connecting to server ...");
            progress.show();
            Thread bkashThread = new Thread() {
                @Override
                public void run()
                {
                    try
                    {
                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                        StrictMode.setThreadPolicy(policy);
                        HttpClient client = new DefaultHttpClient();
                        HttpPost post = new HttpPost(baseUrl + "androidapp/auth/get_user_basic_info");
                        List<NameValuePair> nameValuePairs = new ArrayList<>();
                        nameValuePairs.add(new BasicNameValuePair("user_id", localUserId+""));
                        nameValuePairs.add(new BasicNameValuePair("user_name", userName));
                        nameValuePairs.add(new BasicNameValuePair("password", password));
                        nameValuePairs.add(new BasicNameValuePair("pin_code", pinCode));
                        nameValuePairs.add(new BasicNameValuePair("session_id", sessionId));
                        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        HttpResponse response = client.execute(post);
                        BufferedReader rd = new BufferedReader
                                (new InputStreamReader(response.getEntity().getContent()));
                        String result = "";
                        String line = "";
                        while ((line = rd.readLine()) != null) {
                            result += line;
                        }
                        if(result != null) {
                            JSONObject resultEvent = new JSONObject(result.toString());

                            int responseCode = 0;
                            try
                            {
                                responseCode = (int)resultEvent.get("response_code");
                            }
                            catch(Exception ex)
                            {
                                progress.dismiss();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getBaseContext(), "Invalid response from the server.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            if(responseCode == 2000){
                                try
                                {
                                    JSONObject jsonResultEvent = (JSONObject) resultEvent.get("result_event");
                                    sessionId = jsonResultEvent.get("session_id").toString();
                                    String tempUserInfo = jsonResultEvent.get("user_info").toString();

                                    Intent intent = new Intent(getBaseContext(), RechargeMenu.class);
                                    intent.putExtra("BASE_URL", baseUrl);
                                    intent.putExtra("USER_INFO", tempUserInfo);
                                    intent.putExtra("CURRENT_BALANCE", jsonResultEvent.get("current_balance").toString());
                                    intent.putExtra("SESSION_ID", jsonResultEvent.get("session_id").toString());
                                    //getting service id list
                                    JSONArray serviceIdList = jsonResultEvent.getJSONArray("service_id_list");
                                    int[] serviceList = new int[serviceIdList.length()];
                                    for (int i = 0; i < serviceIdList.length(); i++)
                                    {
                                        int serviceId = (int)serviceIdList.get(i);
                                        serviceList[i] = serviceId;
                                    }
                                    intent.putExtra("service_list", serviceList);
                                    startActivity(intent);
                                    progress.dismiss();

                                }
                                catch(Exception ex)
                                {
                                    System.out.println(ex.toString());
                                    progress.dismiss();
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(getBaseContext(), "Invalid response from the server..", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            }
                            else
                            {
                                //remove user info from the db
                                //show a proper message that credential expired or wrong and allow user to login again.
                                eRchargeDB.deleteUserInfo(localUserId);
                                progress.dismiss();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getBaseContext(), "Please login again.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                        else
                        {
                            progress.dismiss();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getBaseContext(), "Invalid response from the server...", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    catch (Exception ex) {
                        progress.dismiss();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getBaseContext(), "Check your internet connection.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    progress.dismiss();
                }
            };
            bkashThread.start();
        }
        catch (Exception ex){
            Toast.makeText(getApplicationContext(), "Internal server error.", Toast.LENGTH_SHORT).show();
        }


    }

    public void onClickButtonLoginListener(){
        buttonLogin = (Button)findViewById(R.id.bLogin);
        buttonLogin.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        try
                        {
                            cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                            netInfo = cm.getActiveNetworkInfo();
                            if(netInfo == null || !netInfo.isConnected())
                            {
                                Toast.makeText(getApplicationContext(), "Please connect to internet first.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            final String username = etLoginUserName.getText().toString();
                            final String password = etPassword.getText().toString();
                            final String opcode = etOPCode.getText().toString();
                            if(username == null || username.equals(""))
                            {
                                Toast.makeText(getApplicationContext(), "Please assign user name.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if(password == null || password.equals(""))
                            {
                                Toast.makeText(getApplicationContext(), "Please assign password.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if(opcode == null || opcode.equals(""))
                            {
                                Toast.makeText(getApplicationContext(), "Please assign opcode.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            final ProgressDialog progressInit = new ProgressDialog(Login.this);
                            progressInit.setTitle("Login");
                            progressInit.setMessage("Authenticating user...");
                            progressInit.show();

                            final Thread initThread = new Thread() {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                                        StrictMode.setThreadPolicy(policy);
                                        HttpClient client = new DefaultHttpClient();
                                        HttpPost post = new HttpPost("http://212.24.103.134:4040/getbaseurl");

                                        List<NameValuePair> nameValuePairs = new ArrayList<>();
                                        nameValuePairs.add(new BasicNameValuePair("code", etOPCode.getText().toString()));

                                        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                                        HttpResponse response = client.execute(post);
                                        // Get the response
                                        BufferedReader rd = new BufferedReader
                                                (new InputStreamReader(response.getEntity().getContent()));
                                        String result = "";
                                        String line = "";
                                        while ((line = rd.readLine()) != null) {
                                            result += line;
                                        }
                                        if(result != null) {
                                            JSONObject resultEvent = new JSONObject(result.toString());
                                            int responseCode = (int)resultEvent.get("responseCode");
                                            if(responseCode == 2000){
                                                baseUrl = (String) resultEvent.get("result");
                                                if( baseUrl == null || baseUrl.equals(""))
                                                {
                                                    progressInit.dismiss();
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(getBaseContext(), "Invalid Code.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                                //progressInit.dismiss();
                                                try
                                                {
                                                    //final ProgressDialog progressLogin = new ProgressDialog(Login.this);
                                                    //progressLogin.setTitle("Login");
                                                    //progressLogin.setMessage("Authenticating user...");
                                                    //progressLogin.show();

                                                    final Thread loginThread = new Thread() {
                                                        @Override
                                                        public void run()
                                                        {
                                                            try
                                                            {
                                                                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                                                                StrictMode.setThreadPolicy(policy);
                                                                HttpClient client = new DefaultHttpClient();
                                                                //HttpPost post = new HttpPost("http://50.18.235.96:3030/processqrcode");
                                                                //HttpPost post = new HttpPost("http://122.144.10.249/rechargeserver/welcome/app_test");
                                                                HttpPost post = new HttpPost(baseUrl+"androidapp/auth/login");

                                                                List<NameValuePair> nameValuePairs = new ArrayList<>();
                                                                nameValuePairs.add(new BasicNameValuePair("email", etLoginUserName.getText().toString()));
                                                                nameValuePairs.add(new BasicNameValuePair("password", etPassword.getText().toString()));

                                                                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                                                                HttpResponse response = client.execute(post);
                                                                // Get the response
                                                                BufferedReader rd = new BufferedReader
                                                                        (new InputStreamReader(response.getEntity().getContent()));
                                                                String result = "";
                                                                String line = "";
                                                                while ((line = rd.readLine()) != null) {
                                                                    result += line;
                                                                }
                                                                if(result != null) {
                                                                    JSONObject resultEvent = new JSONObject(result.toString());
                                                                    int responseCode = (int)resultEvent.get("response_code");
                                                                    String message = (String) resultEvent.get("message");
                                                                    if(responseCode == 2000){
                                                                        JSONObject jsonResultEvent = (JSONObject) resultEvent.get("result_event");


                                                                        Utils utils = new Utils();
                                                                        //String  hashPassword =  utils.computeSHAHash(etPassword.getText().toString());
                                                                        String tempUserInfo = jsonResultEvent.get("user_info").toString();
                                                                        JSONObject jsonUserInfo  = new JSONObject(tempUserInfo);
                                                                        int tempUserId =    Integer.parseInt(jsonUserInfo.get("user_id").toString());
                                                                        String sessionId = jsonResultEvent.get("session_id").toString();
                                                                        boolean localResponse = eRchargeDB.createUser(tempUserId, etLoginUserName.getText().toString(), etPassword.getText().toString(), etOPCode.getText().toString(), baseUrl, sessionId);


                                                                        Intent intent = new Intent(getBaseContext(), PinCode.class);
                                                                        intent.putExtra("BASE_URL", baseUrl);
                                                                        intent.putExtra("USER_ID", tempUserId);
                                                                        //intent.putExtra("CURRENT_BALANCE", jsonResultEvent.get("current_balance").toString());
                                                                        intent.putExtra("SESSION_ID", sessionId);
                                                                        //getting service id list
                                                                        /*JSONArray serviceIdList = jsonResultEvent.getJSONArray("service_id_list");
                                                                        int[] serviceList = new int[serviceIdList.length()];
                                                                        for (int i = 0; i < serviceIdList.length(); i++)
                                                                        {
                                                                            int serviceId = (int)serviceIdList.get(i);
                                                                            serviceList[i] = serviceId;
                                                                        }
                                                                        intent.putExtra("service_list", serviceList);*/
                                                                        startActivity(intent);
                                                                        progressInit.dismiss();
                                                                    }
                                                                    else if(responseCode == 5009)
                                                                    {
                                                                        progressInit.dismiss();
                                                                        runOnUiThread(new Runnable() {
                                                                            public void run() {
                                                                                Toast.makeText(getBaseContext(), "Unable to create your session. Please try again later.", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                    }
                                                                    else
                                                                    {
                                                                        progressInit.dismiss();
                                                                        runOnUiThread(new Runnable() {
                                                                            public void run() {
                                                                                Toast.makeText(getBaseContext(), "Authentication error.", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                        //Toast.makeText(null, message, Toast.LENGTH_SHORT).show();
                                                                    }

                                                                }
                                                                else
                                                                {
                                                                    progressInit.dismiss();
                                                                    runOnUiThread(new Runnable() {
                                                                        public void run() {
                                                                            Toast.makeText(getBaseContext(), "Invalid response from the server.", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                    //Toast.makeText(null, "Invalid response from the server.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                            catch(Exception ex)
                                                            {
                                                                progressInit.dismiss();
                                                                runOnUiThread(new Runnable() {
                                                                    public void run() {
                                                                        Toast.makeText(getBaseContext(), "Check your internet connection.", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    };
                                                    loginThread.start();
                                                }
                                                catch (Exception ex){
                                                    Toast.makeText(Login.this, "System error.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            else
                                            {
                                                progressInit.dismiss();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(getBaseContext(), "Invalid Code.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        }
                                        else
                                        {
                                            progressInit.dismiss();
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(getBaseContext(), "Invalid response from the server.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                    catch(Exception ex)
                                    {
                                        progressInit.dismiss();
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getBaseContext(), "Check your internet connection.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            };
                            initThread.start();
                        }
                        catch (Exception ex){
                            Toast.makeText(Login.this, "System error.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
}
