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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;

    private TextView statusText, recognizedText;
    private ProgressBar voiceProgressBar;
    private LocationManager locationManager;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        statusText = findViewById(R.id.statusText);
        recognizedText = findViewById(R.id.recognizedText);
        voiceProgressBar = findViewById(R.id.voiceProgressBar);

        checkPermissions();
    }

    // 권한 확인 및 요청
    private void checkPermissions() {
        ArrayList<String> permissions = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        } else {
            startEverything();
        }
    }

    // 권한 승인 결과 콜백
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startEverything();
    }

    private void startEverything() {
        getLocation();
        startSpeechRecognizer();
    }

    // 위치 요청
    private void getLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override public void onLocationChanged(@NonNull Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        });
    }

    // 음성 인식
    private void startSpeechRecognizer() {
        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                statusText.setText("음성인식 중입니다...");
                voiceProgressBar.setVisibility(View.VISIBLE);
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                statusText.setText("샐패. (" + error + ")");
                voiceProgressBar.setVisibility(View.GONE);
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String finalResult = matches.get(0);
                    recognizedText.setText(finalResult);
                    statusText.setText("인식되었어요!");
                    voiceProgressBar.setVisibility(View.GONE);

                    // 서버로 데이터 전송
                    sendDataToBackend(finalResult, latitude, longitude);
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    recognizedText.setText(partial.get(0));
                }
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizer.startListening(intent);
    }

    // 서버로 전송 (현재는 로그만 출력)
    private void sendDataToBackend(String text, double lat, double lng) {
        System.out.println("서버 전송: 텍스트=" + text + ", 위도=" + lat + ", 경도=" + lng);

        // 백엔드... POST
    }
}