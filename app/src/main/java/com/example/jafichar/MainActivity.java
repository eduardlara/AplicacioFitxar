package com.example.jafichar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.JsonReader;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private EditText etIP;
    private EditText etPort;
    private EditText etUser;
    private TextView tv;
    private TextView tvResult;
    private Button btnFichar;
    private Button btnConsultar;
    private LocationRequest locationRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        etUser = findViewById(R.id.etUser);
        tv = findViewById(R.id.tv);
        tvResult = findViewById(R.id.tvResult);

        btnFichar = findViewById(R.id.btnFichar);
        btnConsultar = findViewById(R.id.btnConsultar);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(1000);

        btnConsultar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getByName();
                MyATConsulta listar = new MyATConsulta();
                listar.execute();
            }
        });
        btnFichar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
                //MyATFichar fichar = new MyATFichar();
                //fichar.execute(41.9, 2.2);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    getCurrentLocation();
                } else {
                    turnOnGPS();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                getCurrentLocation();
            }
        }
    }

    private void getCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    LocationServices.getFusedLocationProviderClient(MainActivity.this)
                            .requestLocationUpdates(locationRequest, new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult locationResult) {
                                    super.onLocationResult(locationResult);
                                    LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                            .removeLocationUpdates(this);
                                    if (locationResult != null && locationResult.getLocations().size() > 0) {
                                        int index = locationResult.getLocations().size() - 1;
                                        double latitud = locationResult.getLocations().get(index).getLatitude();
                                        double longitud = locationResult.getLocations().get(index).getLongitude();
                                        MyATFichar fichar = new MyATFichar();
                                        fichar.execute(latitud, longitud);

                                    }
                                }
                            }, Looper.getMainLooper());

                } else {
                    turnOnGPS();
                }

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void turnOnGPS() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(MainActivity.this, "GPS is already tured on", Toast.LENGTH_SHORT).show();

                } catch (ApiException e) {

                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(MainActivity.this, 2);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            //Device does not have location
                            break;
                    }
                }
            }
        });
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;
    }


    public class MyATConsulta extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... values) {
            String ip   = etIP.getText().toString();
            String port = etPort.getText().toString();
            String user = etUser.getText().toString();
            StringBuffer sb = new StringBuffer();
            try {
                URL url = new URL("http://" + ip + ":" + port + "/listar/" + user);
                //URL url = new URL("http://10.0.2.2:9090/listar/"+user);
                HttpURLConnection myConnection = (HttpURLConnection) url.openConnection();
                if (myConnection.getResponseCode() == 200) {
                    InputStream responseBody = myConnection.getInputStream();
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                    JsonReader jsonReader = new JsonReader(responseBodyReader);
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) { // Loop through all keys
                            String key = jsonReader.nextName(); // Fetch the next key
                            if (key.equals("id"))
                                sb.append("{" + jsonReader.nextString() + ", ");
                            else if (key.equals("nom"))
                                sb.append(jsonReader.nextString() + ", ");
                            else if (key.equals("fecha"))
                                sb.append(jsonReader.nextString() + ", ");
                            else if (key.equals("distancia"))
                                sb.append(jsonReader.nextString() + ", ");
                            else if (key.equals("comentario"))
                                sb.append(jsonReader.nextString() + "}\n");
                            else
                                jsonReader.skipValue(); // Skip values of other keys
                        }
                        jsonReader.endObject();
                    }
                } else {
                    tv.setText("No ha ido");
                }
            } catch (Exception e) {
                tv.setText(e.getMessage());
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String value) {
            tv.setText(value);
        }
    }

    public class MyATFichar extends AsyncTask<Double, Void, String> {
        @Override
        protected String doInBackground(Double... values) {
            String ip   = etIP.getText().toString();
            String port = etPort.getText().toString();
            String user = etUser.getText().toString();
            StringBuffer sb = new StringBuffer();
            try {
                URL url = new URL("http://" + ip + ":" + port + "/insertar");
                //URL url = new URL("http://10.0.2.2:9090/insertar");
                HttpURLConnection myConnection = (HttpURLConnection) url.openConnection();

                myConnection.setReadTimeout(10000);
                myConnection.setConnectTimeout(15000);
                myConnection.setRequestMethod("POST");
                myConnection.setDoInput(true);
                myConnection.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("nom", user)
                        .appendQueryParameter("latitud", String.valueOf(values[0]))
                        .appendQueryParameter("longitud", String.valueOf(values[1]));
                String query = builder.build().getEncodedQuery();

                OutputStream os = myConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();

                myConnection.connect();

                if (myConnection.getResponseCode() == 200) {
                    InputStream responseBody = myConnection.getInputStream();
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                    JsonReader jsonReader = new JsonReader(responseBodyReader);
                    //jsonReader.beginArray();
                    //jsonReader.hasNext());
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) { // Loop through all keys
                            String key = jsonReader.nextName(); // Fetch the next key
                            if (key.equals("id"))
                                sb.append("{"+jsonReader.nextString() + ", ");
                            else if (key.equals("nom"))
                                sb.append(jsonReader.nextString() + ", ");
                            else if (key.equals("fecha"))
                                sb.append(jsonReader.nextString() + ", ");
                            else if (key.equals("distancia"))
                                sb.append(jsonReader.nextString() + ", ");
                            else if (key.equals("comentario"))
                                sb.append(jsonReader.nextString() + "}\n");
                            else
                                jsonReader.skipValue(); // Skip values of other keys
                        }
                        jsonReader.endObject();

                } else {
                    tvResult.setText("No ha ido");
                }
            } catch (Exception e) {
                tvResult.setText(e.getMessage());
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String value) {
            tvResult.setText(value);
        }
    }
}