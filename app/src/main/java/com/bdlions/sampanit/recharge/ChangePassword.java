package com.bdlions.sampanit.recharge;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ChangePassword extends AppCompatActivity {
    private static String sessionId = "";
    private static String baseUrl = "";
    private static int userId = 0;
    private static EditText editTextPassword, editTextConfirmPassword;
    private static Button buttonChangePassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        editTextPassword = (EditText) findViewById(R.id.etUserNewPassword);
        editTextConfirmPassword = (EditText) findViewById(R.id.etUserConfirmPassword);
        try
        {
            baseUrl = getIntent().getExtras().getString("BASE_URL");
            userId = getIntent().getExtras().getInt("USER_ID");
            sessionId = getIntent().getExtras().getString("SESSION_ID");
        }
        catch(Exception ex)
        {
            Toast.makeText(getApplicationContext(), "Invalid arguments.", Toast.LENGTH_SHORT).show();
            return;
        }
        onClickListener();
    }
    public void onClickListener()
    {
        buttonChangePassword = (Button) findViewById(R.id.bChangePassword);
        buttonChangePassword.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try
                        {
                            final String password = editTextPassword.getText().toString();
                            final String confirmPassword = editTextConfirmPassword.getText().toString();
                            if(!password.equals(confirmPassword))
                            {
                                Toast.makeText(getApplicationContext(), "New password and confirm password mismatched.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            final ProgressDialog progress = new ProgressDialog(ChangePassword.this);
                            progress.setTitle("Processing");
                            progress.setMessage("Wait while updating password...");
                            progress.show();
                            Thread changePasswordThread = new Thread() {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                                        StrictMode.setThreadPolicy(policy);
                                        HttpClient client = new DefaultHttpClient();
                                        HttpPost post = new HttpPost(baseUrl+"androidapp/auth/update_password");

                                        List<NameValuePair> nameValuePairs = new ArrayList<>();

                                        nameValuePairs.add(new BasicNameValuePair("pwd", password));
                                        nameValuePairs.add(new BasicNameValuePair("user_id", "" + userId+""));
                                        nameValuePairs.add(new BasicNameValuePair("session_id", "" + sessionId));

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

                                            }
                                            if(responseCode == 2000){
                                                progress.dismiss();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(getBaseContext(), "Password is updated successfully.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                            else
                                            {
                                                progress.dismiss();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(getBaseContext(), "Unable to update password. Please try again later.", Toast.LENGTH_SHORT).show();
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
                                    catch (Exception ex)
                                    {
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
                            changePasswordThread.start();
                        }
                        catch (Exception ex){
                            Toast.makeText(getApplicationContext(), "Internal Error.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
}
