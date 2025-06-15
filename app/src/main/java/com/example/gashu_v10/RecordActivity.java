package com.example.gashu_v10;

import android.Manifest;
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
import android.content.Intent;
import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION = 101;

    private TextView statusText, recognizedText;
    private ProgressBar voiceProgressBar;
    private LocationManager locationManager;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // 뷰 연결
        statusText = findViewById(R.id.statusText);
        recognizedText = findViewById(R.id.recognizedText);
        voiceProgressBar = findViewById(R.id.voiceProgressBar);

        // 위치 정보 요청
        getLocation();

        // 음성 인식 시작
        startSpeechRecognizer();
    }

    private void startSpeechRecognizer() {
        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                statusText.setText("음성인식 중입니다...");
                voiceProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                statusText.setText("음성 인식 오류가 발생했어요.");
                voiceProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String finalResult = matches.get(0);
                    recognizedText.setText(finalResult);
                    statusText.setText("인식되었어요!");
                    voiceProgressBar.setVisibility(View.GONE);

                    // 백엔드로 ....
                    sendDataToBackend(finalResult, latitude, longitude);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    recognizedText.setText(partial.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        recognizer.startListening(intent);
    }

    private void getLocation() {

        // 위치권한 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION);
            return;
        }

        // 위치 업데이트 물보는거
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        });
    }

    private void sendDataToBackend(String busNumber, double lat, double lng) {
        // TODO: Retrofit or HTTP 전송
        System.out.println("서버 전송: 버스번호=" + busNumber + ", 위도=" + lat + ", 경도=" + lng);
    }
}