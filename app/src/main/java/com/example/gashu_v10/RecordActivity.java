package com.example.gashu_v10;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecordActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;

    private TextView statusText, recognizedText, backendResultText;
    private ProgressBar voiceProgress;

    private LocationManager locationManager;
    private double latitude = 0.0;
    private double longitude = 0.0;

    private final OkHttpClient client = new OkHttpClient();
    private final String backend = "http://15.164.161.30/message";

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        statusText = findViewById(R.id.statusText);
        recognizedText = findViewById(R.id.recognizedText);
        backendResultText = findViewById(R.id.backendResultText);
        voiceProgress = findViewById(R.id.voiceProgress);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
            }
        });

        checkPermissions();
    }

    private void checkPermissions() {
        ArrayList<String> permissions = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.RECORD_AUDIO);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        } else {
            startEverything();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                startEverything();
            } else {
                Toast.makeText(this, "필수 권한이 허용되어야 합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startEverything() {
        getLocation();
        startSpeechRecognizer();
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        });
    }

    private void startSpeechRecognizer() {
        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                statusText.setText("음성 인식 중...");
                voiceProgress.setVisibility(View.VISIBLE);
            }

            @Override public void onError(int error) {
                statusText.setText("다시 말해주세요😭 (오류 코드: " + error + ")");
                voiceProgress.setVisibility(View.GONE);
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0).trim();
                    recognizedText.setText(text);
                    statusText.setText("음성 인식 완료!");

                    if (text.isEmpty()) {
                        backendResultText.setText("⚠️ 인식된 텍스트 없습니다. 다시 시도해주세요.");
                        voiceProgress.setVisibility(View.GONE);
                        return;
                    }

                    sendToBackend(text);
                }
                voiceProgress.setVisibility(View.GONE);
            }

            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty())
                    recognizedText.setText(partial.get(0));
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizer.startListening(intent);
    }

    private void sendToBackend(String voiceText) {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", "0001");
            json.put("user_message", voiceText);
            json.put("user_lat", String.valueOf(latitude));
            json.put("user_lon", String.valueOf(longitude));

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder().url(backend).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> backendResultText.setText("❌ 서버 요청 실패: " + e.getMessage()));
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String res = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonRes = new JSONObject(res);
                            String reply;

                            if (jsonRes.has("message")) {
                                reply = jsonRes.getString("message");
                            } else if (jsonRes.has("detail")) {
                                JSONArray details = jsonRes.getJSONArray("detail");
                                if (details.length() > 0) {
                                    JSONObject firstDetail = details.getJSONObject(0);
                                    String msg = firstDetail.optString("msg", "에러");
                                    String type = firstDetail.optString("type", "");
                                    reply = "⚠️ 오류: " + msg + (type.isEmpty() ? "" : " (" + type + ")");
                                } else {
                                    reply = "⚠️ 오류 응답: detail 배열이 비어 있음";
                                }
                            } else {
                                reply = "⚠️ 알 수 없는 응답 형식";
                            }

                            backendResultText.setText("서버 응답: " + reply);
                            if (tts != null) {
                                tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null);
                            }

                        } catch (Exception e) {
                            backendResultText.setText("응답 파싱 오류: " + e.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            backendResultText.setText("예외 발생: " + e.getMessage());
        }
    }
}