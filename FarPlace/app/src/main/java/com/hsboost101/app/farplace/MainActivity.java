package com.hsboost101.app.farplace;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by mobaohanbing on 4/2/16.
 */
public class MainActivity extends AppCompatActivity {

    private TextView positionTextView;

    private TextView farTextView;

    private LocationManager locationManager;

    private String provider;

    public static final int SHOW_LOCATION = 0;

    public static final int SHOW_FAR_LOCATION = 1;

    public static final int SHOW_EMPTY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        positionTextView = (TextView) findViewById(R.id.position_text_view);
        farTextView = (TextView) findViewById(R.id.far_position);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providerList = locationManager.getProviders(true);
        if(providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if(providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(this, "No location provider to use", Toast.LENGTH_SHORT).show();
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if(location != null) {
            showLocation(location);
        }
        locationManager.requestLocationUpdates(provider, 5000, 1, locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void showLocation(final Location location) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    double nowLati = location.getLatitude();
                    double nowLong = location.getLongitude();

                    double farLati = -nowLati;

                    double farLong;

                    if(nowLong <= 0) {
                        farLong = 180 + nowLong;
                    } else {
                        farLong = nowLong - 180;
                    }

                    StringBuilder urlNow = new StringBuilder();

                    StringBuilder urlFar = new StringBuilder();

                    urlNow.append("http://maps.googleapis.com/maps/api/geocode/json?latlng=");
                    urlNow.append(location.getLatitude()).append(",");
                    urlNow.append(location.getLongitude());
                    urlNow.append("&sensor=false");

                    urlFar.append("http://maps.googleapis.com/maps/api/geocode/json?latlng=");
                    urlFar.append(farLati).append(",");
                    urlFar.append(farLong);
                    urlFar.append("&sensor=false");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(urlNow.toString());
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    if(httpResponse.getStatusLine().getStatusCode() == 200) {
                        HttpEntity entity = httpResponse.getEntity();
                        String response = EntityUtils.toString(entity);

                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray resultArray = jsonObject.getJSONArray("results");

                        if(resultArray.length() > 0) {
                            JSONObject subObject = resultArray.getJSONObject(0);
                            String address = subObject.getString("formatted_address");
                            Message message = new Message();
                            message.what = SHOW_LOCATION;
                            message.obj = address;
                            handler.sendMessage(message);
                        }
                    }

                    HttpGet httpFarGet = new HttpGet(urlFar.toString());
                    HttpResponse httpFarResponse = httpClient.execute(httpFarGet);
                    if(httpFarResponse.getStatusLine().getStatusCode() == 200) {
                        HttpEntity entityFar = httpFarResponse.getEntity();
                        String responseFar = EntityUtils.toString(entityFar);
                        JSONObject jsonObject = new JSONObject(responseFar);
                        JSONArray resultArray = jsonObject.getJSONArray("results");

                        if(resultArray.length() > 0) {
                            JSONObject subObject = resultArray.getJSONObject(0);
                            String address = subObject.getString("formatted_address");
                            Message messageFar = new Message();
                            messageFar.what = SHOW_FAR_LOCATION;
                            messageFar.obj = address;
                            handler.sendMessage(messageFar);
                        } else {
                            Message messageEmpty = new Message();
                            messageEmpty.what = SHOW_EMPTY;
                            messageEmpty.obj =Double.toString(farLong) + " , " + Double.toString(farLati);
                            handler.sendMessage(messageEmpty);
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_LOCATION:
                    String currentPosition = (String) msg.obj;
                    positionTextView.setText(currentPosition);
                    break;

                case SHOW_FAR_LOCATION:
                    String farPosition = (String) msg.obj;
                    farTextView.setText(farPosition);
                    break;

                case SHOW_EMPTY:
                    String farEmpty = (String) msg.obj;

                    farTextView.setText("There is no city here, the longitude and " +
                            "latitude is " + farEmpty);


                default:
                    break;
            }
        }
    };
}
