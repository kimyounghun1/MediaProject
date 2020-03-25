package com.example.mediaproject.Activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaproject.Adapter.TourSearchAdapter;
import com.example.mediaproject.AirQuality.AirApiService;
import com.example.mediaproject.AirQuality.Airapi;
import com.example.mediaproject.Data.TourSearchData;
import com.example.mediaproject.Data.WeatherTypeData;
import com.example.mediaproject.GpsTracker;
import com.example.mediaproject.R;
import com.example.mediaproject.StationApi.StationApi;
import com.example.mediaproject.StationApi.StationApiService;
import com.example.mediaproject.TourApi.LoadTourApi;
import com.example.mediaproject.TourApi.Model.TourDataRES;
import com.example.mediaproject.Weather.WeatherApi;
import com.example.mediaproject.Weather.WeatherApiService;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RecommendActivity extends BaseActivity implements OnMapReadyCallback {
    RecyclerView recyclerView;
    RecyclerView recyclerView2;
    RecyclerView recyclerView3;
    private com.example.mediaproject.Adapter.TourSearchAdapter TourSearchAdapter;
    private com.example.mediaproject.Adapter.TourSearchAdapter TourSearchAdapter2;
    private com.example.mediaproject.Adapter.TourSearchAdapter TourSearchAdapter3;
    protected GoogleMap mMap;
    public double latitude;
    public double longitude;
    private TextView fineDust;
    private TextView ultraFineDust;
    private TextView temperatures;
    private TextView weather_info;
    private Spinner spinner;
    ArrayList<String> arrayList;
    ArrayAdapter<String> arrayAdapter;

    //지도 관련 변수 & 권한설정
    private static final String ROOT_URL = "http://openapi.airkorea.or.kr/openapi/services/rest/ArpltnInforInqireSvc/";

    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.activity_recommend);

        fineDust= (TextView) findViewById(R.id.FineDust);
        ultraFineDust= (TextView) findViewById(R.id.UltraFineDust);
        temperatures = (TextView) findViewById(R.id.temperatures);
        weather_info = (TextView) findViewById(R.id.weather);

        arrayList = new ArrayList<>();
        arrayList.add("1km");
        arrayList.add("1.5km");
        arrayList.add("2km");
        arrayList.add("3km");
        arrayList.add("4km");

        arrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,
                arrayList);

        spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(1);

        //권한 확인
        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        } else {

            checkRunTimePermission();
        }

        //gps 정보 불러오기
        gpsTracker = new GpsTracker(RecommendActivity.this);

        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();


        String address = getCurrentAddress(latitude, longitude);

        String locaddr[] = address.split(" ");
        Log.d("shit", address);
        final Gson gson = new GsonBuilder()
                .setLenient()
                .create();


        Retrofit retrofit2 = new Retrofit.Builder()
                .baseUrl("http://openapi.airkorea.or.kr/openapi/services/rest/MsrstnInfoInqireSvc/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        StationApiService stationApiService = retrofit2.create(StationApiService.class);

        Call<StationApi> getst = stationApiService.getStation(10, 1, locaddr[3]);
        getst.enqueue(new Callback<StationApi>() {
            @Override
            public void onResponse(Call<StationApi> call, Response<StationApi> response) {
                int stationNum = response.body().getTotalCount();
                String closestStation = "";
                double distance = Double.MAX_VALUE;

                Location phoneloc = new Location("phoneloc");
                phoneloc.setLatitude(latitude);
                phoneloc.setLongitude(longitude);

                for (int i = 0; i < stationNum; i++) {
                    Location locationstation = new Location("station");
                    locationstation.setLongitude(Double.parseDouble(response.body().getList().get(i).getDmY()));
                    locationstation.setLatitude(Double.parseDouble(response.body().getList().get(i).getDmX()));

                    double tempdist = phoneloc.distanceTo(locationstation);
                    if (tempdist <= distance) {
                        distance = tempdist;
                        closestStation = response.body().getList().get(i).getStationName();
                        Log.d("airair", closestStation);
                    }
                }
                int meter = (int) Math.round(distance);


                //날씨 받아오기
                Retrofit retrofit3 = new Retrofit.Builder()
                        .baseUrl("http://api.openweathermap.org/data/2.5/")
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();
                WeatherApiService weatherApiService = retrofit3.create(WeatherApiService.class);
                Call<WeatherApi> getWeather = weatherApiService.getWeather(latitude,longitude);
                getWeather.enqueue(new Callback<WeatherApi>() {
                    @Override
                    public void onResponse(Call<WeatherApi> call, Response<WeatherApi> response) {
                        String weather = response.body().getWeather().get(0).getMain();
                        Double temperature = response.body().getMain().getTemp()-273.15;
                        int int_temperature = (int) Math.round(temperature);
                        temperatures.setText(Integer.toString(int_temperature)+"°C");

                        WeatherTypeData weatherTypeData = new WeatherTypeData();
                        HashMap<String, String> weatherMap = weatherTypeData.WeatherType;
                        String weatherKor = weatherMap.get(weather);
                        weather_info.setText(weatherKor);
                    }


                    @Override
                    public void onFailure(Call<WeatherApi> call, Throwable t) {
                        Log.d("shit","날씨 받아오는 것 실패 ㅠㅠㅠㅠㅠㅠ");
                    }
                });


                //미세먼지
                Retrofit retrofit1 = new Retrofit.Builder()
                        .baseUrl(ROOT_URL)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();

                AirApiService airApiService = retrofit1.create(AirApiService.class);

                Call<Airapi> getair = airApiService.getInfo(1, 1, closestStation, "DAILY", 1.3);
                getair.enqueue(new Callback<Airapi>() {
                    @Override
                    public void onResponse(Call<Airapi> call, Response<Airapi> response) {
                        if (response.code() == 200) {
                            //int size = response.body().getList()
                        }

                        // 버그 발생, index 값 조정 필요
//                        String fd = response.body().getList().get(0).getPm10Grade1h();
//                        String ufd = response.body().getList().get(0).getPm25Grade1h();
//
//                        if(fd.equals("1")) fineDust.setText("좋음");
//                        else if(fd.equals("2")) fineDust.setText("보통");
//                        else if(fd.equals("3")) fineDust.setText("나쁨");
//                        else if(fd.equals("4")) fineDust.setText("매우나쁨");
//
//                        if(ufd.equals("1")) ultraFineDust.setText("좋음");
//                        else if(ufd.equals("2")) ultraFineDust.setText("보통");
//                        else if(ufd.equals("3")) ultraFineDust.setText("나쁨");
//                        else if(ufd.equals("4")) ultraFineDust.setText("매우나쁨");
//
//                        Log.d("airair", response.body().getList().get(0).getPm10Grade1h());
//                        Log.d("airair", response.body().getList().get(0).getPm25Grade1h());
                    }

                    @Override
                    public void onFailure(Call<Airapi> call, Throwable t) {
                        Log.d("airair", t.getMessage());
                    }
                });
//                getair.enqueue(new Callback<Airapi>() {
//                    @Override
//                    public void onResponse(Call<Airapi> call, Response<Airapi> response) {
//                        Log.d("airair",response.body().getList().get(0).getPm25Grade());
//                        Log.d("airair",response.body().getList().get(0).getPm10Grade());
//                    }
//
//                    @Override
//                    public void onFailure(Call<Airapi> call, Throwable t) {
//                        Log.d("airair",t.getMessage());
//                    }
//                });
            }

            @Override
            public void onFailure(Call<StationApi> call, Throwable t) {
                Log.d("airair", t.getMessage());
            }
        });
        //여기까지 날씨, 미세먼지


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        recyclerView = findViewById(R.id.LocationRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //recyclerView.addOnItemTouchListener(new RecyclerTouch);

        recyclerView2 = findViewById(R.id.LocationRecyclerView2);
        recyclerView2.setHasFixedSize(true);
        recyclerView2.setLayoutManager(new LinearLayoutManager(this));

        recyclerView3 = findViewById(R.id.LocationRecyclerView3);
        recyclerView3.setHasFixedSize(true);
        recyclerView3.setLayoutManager(new LinearLayoutManager(this));


    } // onCreate end

    @Override
    protected void onResume() {
        super.onResume();
        setSelected(R.id.navigation_menu1);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap;

        LatLng CurrentLoc = new LatLng(latitude, longitude);
        Log.d("shit", CurrentLoc.toString());
        //final MarkerOptions markerOptions = new MarkerOptions();
        //markerOptions.position(CurrentLoc);
        //mMap.addMarker(markerOptions);
        mMap.setMyLocationEnabled(true);


        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(CurrentLoc);
        mMap.moveCamera(cameraUpdate);
        Log.d("shit", "카메라 옮겨짐.");

        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

        Call<TourDataRES> Loc = LoadTourApi.getInstance().getService().getLocationBasedList("Y", "A", 999, 1, longitude, latitude, 1500);
        TourListChange(Loc);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(),arrayList.get(i)+"가 선택되었습니다.",
                        Toast.LENGTH_SHORT).show();

                if(arrayList.get(i).equals("1km")){
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                    Call<TourDataRES> Loc = LoadTourApi.getInstance().getService().getLocationBasedList("Y", "A", 999, 1, longitude, latitude, 1000);
                    TourListChange(Loc);
                } else if (arrayList.get(i).equals("1.5km")){
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                    Call<TourDataRES> Loc = LoadTourApi.getInstance().getService().getLocationBasedList("Y", "A", 999, 1, longitude, latitude, 1500);
                    TourListChange(Loc);
                } else if(arrayList.get(i).equals("2km")){
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                    Call<TourDataRES> Loc = LoadTourApi.getInstance().getService().getLocationBasedList("Y", "A", 999, 1, longitude, latitude, 2000);
                    TourListChange(Loc);
                } else if(arrayList.get(i).equals("3km")){
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                    Call<TourDataRES> Loc = LoadTourApi.getInstance().getService().getLocationBasedList("Y", "A", 999, 1, longitude, latitude, 3000);
                    TourListChange(Loc);
                } else if(arrayList.get(i).equals("4km")){
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                    Call<TourDataRES> Loc = LoadTourApi.getInstance().getService().getLocationBasedList("Y", "A", 999, 1, longitude, latitude, 4000);
                    TourListChange(Loc);
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    //gps + 권한 관련된 함수들
    public String getCurrentAddress(double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }


        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";

    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if (check_result) {

                //위치 값을 가져올 수 있음
                ;
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(RecommendActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                } else {

                    Toast.makeText(RecommendActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(RecommendActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    void checkRunTimePermission() {

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(RecommendActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(RecommendActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음


        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(RecommendActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(RecommendActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(RecommendActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(RecommendActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String ChageHttps(String text) {
        String trans = "";
        trans = text;
        if (trans != null) {
            trans = trans.replace("http://", "https://");
        } else {
            trans = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAKMAAAB7CAMAAAAv38DwAAAAOVBMVEX4+Pezuav7+/qvtaba3Nbz9PLQ08zm5+Pp6ue4vrGss6PDyL3w8O7Bxbrs7erT1s7JzcPf4tylrZwuo5cQAAAEK0lEQVR4nO2a3XqkIAyGJQERFIXe/8VuEpwfZ+w+bdcuHOQ76EwVy9tAAgkOg0qlUqlUKpVKpVKpVCqVSqX6PcFykGvN8y6YAh6VVmgNdRRYNC9CM/UFub4hEmRoTXUQjCeMBktXhgwniAZzD4ywy8czRoPu1qAho89JdGpGUr07b81cHJYUz6bhiT1xbMS4hK8RCmVqggjz1xEJ0jYYbpjO/eRT/X/Es5Xlr4prA8azsI0m5TEnPLkVG6yMJ4w4l7rbcfbdndB3wIim3GM1DNsrZA+MmJxA7JzgO2Q0MswwTL46B0zYHSMvd7CmSMLMJn1x/PaMuAmVqdcweG6SOmPkkS5PV1aekrEnRtkpumeimYlCV4y0GkM5GHahNhl7YiQAmJ+nX2Rq2xMjp4BwdJFNGb/PyBkgPM8+EzknfJ0ObRl59h1DDQUjyKYjRsOpwPNgS0x32BUj8iK9Pn4VZtsZ41yX63otZm7hDhlte0Zjak5lUwhhLtIgd7anIAbJBWBwzskW8jXf6YHRmOcK1PtGvA9GHN2OAbC+Jd99MBrE7Fca67XM75lhJ4ycdxnyGWPOctcWjOV7dQrZrP1vLd9CNE2KUuf15U/N2Kbu/BKk/464taqSbmeFnRNADKURIVnSefsVTUPLgjh8TQ0JVao+9BNH+G3ncXsR2e1VRl+Kd/By3+23b5/39vIMxamy7kHI7boUMdSD3rnW8Lycpxt7vz9FM8mBTeBubyUpyWhuxwnW8DOhPFcm8cqzBupMDoGEESaDMSTq8X4udGdkpIU+hJGrVHsbwsKYQkRc6Q/QHp1rqfHjYkZjFtjtSOZYaGrNDzvcGBMhEU7a7cgLepBvq8FME9KFSJYmxnB9bGdGHCsj1yNkv7o8dgo3xo0LkTnkyugoW0T5P3iPxLMAfB6djDVZ8+IFkuaj5fcjhJFGSi5CwjwcGW0gs4U01nS7xDAk3LhpkCrqbjxiDKXYYv3FjNOGqTKOWMeP9mbzK+MYFxe3rTLOBGars1WT243EY23EgWL+tMMfMjoTSz4wzu+MJZbpw1fGhc/faEJM1CAIY/hgpxGfGTPJft7jjxg9m4S7AhuN5CY8gK+MK25bdMLIJMQh85inhdhxZM+/+8yViJWRzzLYHGuUwgn4WmR8ZtyGMKcAlZHiFR/WmOrJhkI++RsyI/vML/g15SPs0BIfKXu2y1KeXuC5M9IcJYsJI/0rYyHXQE5lKEELxa0Wb3b0rHLlexYU17xMwFpXlMNfipLTI4YjM8aNuqcQOcYZqulI1aW9kWco9AtjVbwyy3E5cZxYUpJRdiPH9PywwhoShcExWJhCmsCGEegReb0HtiTFyJWLutnZtHB1bde1yeJ+mnov6AzuEILrDbh9e/x4fO6P7IezmkOoVCqVSqVSqVQqlUqlUqlU/6Y/sxooCVUeRkIAAAAASUVORK5CYII=";
        }

        return trans;
    }

    public void TourListChange(Call<TourDataRES> Loc){
        Loc.enqueue(new Callback<TourDataRES>() {
            @Override
            public void onResponse(Call<TourDataRES> call, Response<TourDataRES> response) {
                if (response.code() == 200) {
                    int size = response.body().getResponse().getBody().getItems().getItem().size();

                    ArrayList<TourSearchData> tourSearchData = new ArrayList<>();
                    ArrayList<TourSearchData> tour = new ArrayList<>();
                    ArrayList<TourSearchData> restor = new ArrayList<>();
                    ArrayList<TourSearchData> stay = new ArrayList<>();
                    MarkerOptions marker = new MarkerOptions();

                    tourSearchData.clear();
                    tour.clear();
                    restor.clear();
                    stay.clear();
                    mMap.clear();

                    for (int i = 0; i < size; i++) {
                        double tempLat = response.body().getResponse().getBody().getItems().getItem().get(i).getMapy();
                        double tempLng = response.body().getResponse().getBody().getItems().getItem().get(i).getMapx();
                        String temptitle = response.body().getResponse().getBody().getItems().getItem().get(i).getTitle();

                        tourSearchData.add(new TourSearchData(
                                response.body().getResponse().getBody().getItems().getItem().get(i).getDist(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getTitle(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getAddr1(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getAddr2(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getAreacode(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getBooktour(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getCat1(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getCat2(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getCat3(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getContentid(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getCreatedtime(),
                                ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage()),
                                ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage2()),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getMapx(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getMapy(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getMlevel(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getModifiedtime(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getReadcount(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getSigungucode(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getTel(),
                                response.body().getResponse().getBody().getItems().getItem().get(i).getZipcode()
                        ));

                        marker.position(new LatLng(tempLat, tempLng)).title(temptitle);
                        mMap.addMarker(marker);


                        if (response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid() == 12
                                || response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid() == 14
                                || response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid() == 15
                                || response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid() == 28
                                || response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid() == 38) { //관광지
                            tour.add(new TourSearchData(
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getDist(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getTitle(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAddr1(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAddr2(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAreacode(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getBooktour(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat1(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat2(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat3(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getContentid(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCreatedtime(),
                                    ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage()),
                                    ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage2()),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMapx(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMapy(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMlevel(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getModifiedtime(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getReadcount(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getSigungucode(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getTel(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getZipcode()));

                        } else if (response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid() == 39) { // 음식점
                            restor.add(new TourSearchData(
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getDist(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getTitle(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAddr1(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAddr2(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAreacode(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getBooktour(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat1(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat2(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat3(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getContentid(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCreatedtime(),
                                    ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage()),
                                    ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage2()),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMapx(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMapy(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMlevel(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getModifiedtime(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getReadcount(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getSigungucode(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getTel(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getZipcode()));

                        } else if (response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid() == 32) { // 숙박지
                            stay.add(new TourSearchData(
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getDist(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getTitle(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAddr1(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAddr2(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getAreacode(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getBooktour(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat1(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat2(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCat3(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getContentid(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getContenttypeid(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getCreatedtime(),
                                    ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage()),
                                    ChageHttps(response.body().getResponse().getBody().getItems().getItem().get(i).getFirstimage2()),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMapx(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMapy(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getMlevel(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getModifiedtime(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getReadcount(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getSigungucode(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getTel(),
                                    response.body().getResponse().getBody().getItems().getItem().get(i).getZipcode()));

                        } else {
                        }
                    } //for end
                    TourSearchAdapter = new TourSearchAdapter(tour);
                    recyclerView.setAdapter(TourSearchAdapter);
                    TourSearchAdapter.notifyDataSetChanged();

                    TourSearchAdapter2 = new TourSearchAdapter(restor);
                    recyclerView2.setAdapter(TourSearchAdapter2);
                    TourSearchAdapter2.notifyDataSetChanged();

                    TourSearchAdapter3 = new TourSearchAdapter(stay);
                    recyclerView3.setAdapter(TourSearchAdapter3);
                    TourSearchAdapter3.notifyDataSetChanged();

                } //if end
            } //onResponse end

            @Override
            public void onFailure(Call<TourDataRES> call, Throwable t) {
                Log.d("shit", t.getMessage());
            }
        });
    }

}