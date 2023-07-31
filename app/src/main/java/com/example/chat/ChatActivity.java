package com.example.chat;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends Activity {
    private LinearLayout chatLayout;
    private Logger logger;
    private EditText inputField;
    private Button sendButton;
    private Button changeButton;

    private boolean isSelfBubble = true; // 기본적으로 자신의 말풍선으로 시작

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatLayout = findViewById(R.id.chat_layout);
        inputField = findViewById(R.id.input_field);
        sendButton = findViewById(R.id.send_button);
        changeButton = findViewById(R.id.change_button);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        inputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        changeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBubble();
            }
        });
    }

    private void sendMessage() {
        String message = inputField.getText().toString();
        if (!message.isEmpty()) {
            // 백그라운드 스레드에서 네트워크 작업 수행
            new SendToServerTask().execute(message);
        }
    }

    // AsyncTask를 사용하여 백그라운드에서 네트워크 작업을 처리
    private class SendToServerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String message = params[0];
            return sendToServer(message);
        }

        @Override
        protected void onPostExecute(String response) {
            // 네트워크 작업 완료 후 UI 업데이트
            addMessageToChatArea(response, !isSelfBubble);
        }
    }
    private String sendToServer(String message) {
        //동렬아 여기 url 안드로이드 스튜디오랑 스프링 서버 같은 로컬로 안 돌리면 172.20.10.5:8080 =>localhost로 바꿔야됨 ㅇㅇ
        String baseUrl = "http://172.20.10.5:8080/api/v1/chat-gpt";
        try {
            URL url = new URL(baseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                }
                bufferedReader.close();


                return response.toString();
            } else {
                return "Error";
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("My app", "Error occurred:", e);
            return "Request failed";
        }
    }

    private void addMessageToChatArea(String message, boolean isSelf) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setBackgroundResource(R.drawable.chat_bubble);
        textView.setPadding(20, 10, 20, 10); // 원하는 패딩 값으로 조정
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(10, 10, 10, 10); // 원하는 마진 값으로 조정

        // 말풍선 정렬 설정 (자신의 말풍선인 경우 오른쪽, 상대방의 말풍선인 경우 왼쪽)
        if (isSelf) {
            layoutParams.gravity = Gravity.END; // 오른쪽으로 정렬
        } else {
            layoutParams.gravity = Gravity.START; // 왼쪽으로 정렬
        }

        textView.setLayoutParams(layoutParams);

        chatLayout.addView(textView);
    }

    private void toggleBubble() {
        isSelfBubble = !isSelfBubble;
        String buttonText = isSelfBubble ? "Change to Friend's Bubble" : "Change to My Bubble";
        changeButton.setText(buttonText);
    }
}