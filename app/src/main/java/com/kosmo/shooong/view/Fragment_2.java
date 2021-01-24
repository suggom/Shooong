package com.kosmo.shooong.view;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kosmo.shooong.R;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin;
import com.mapbox.mapboxsdk.style.layers.Layer;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
//요구사항 : 위젯 연동(위젯은 혼자


//과제 : 켤때부터 스탭수 받아오기
//1]Fragement상속
//※androidx.fragment.app.Fragment 상속
public class Fragment_2 extends Fragment implements SensorEventListener, OnMapReadyCallback, PermissionsListener {
    private PermissionsManager permissionsManager;
    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private LocationEngine locationEngine;
    protected LocationManager locationManager;
    private static int mSteps = 0;
    private SensorManager sensorManager;
    private Sensor stepCountSensor;
    private int mCounterSteps = 0;
    JsonArray coordinates;
    JsonArray coordinates_;
    List<LatLng> lineLatLngList;
    Button recordRoute, uploadRoute;
    boolean locflag = false;
    double speed, deltime, X_, Y_;
    private MapView mapView;
    public MapboxMap mapboxMap;
    String walkdata = "", nowday = "";
    TextView tvStepCount, nowspeed;
    LocationChange loc = new LocationChange(this);
    private String id;
    private String name;
    private String pwd;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i("com.kosmo.shooong", "onAttach:2");
    }

    //map의 asynctask
    private class NaviOnMapReadyCallback implements OnMapReadyCallback{
        @Override
        public void onMapReady(@NonNull final MapboxMap mapboxMap) {
            Fragment_2.this.mapboxMap = mapboxMap;

            mapboxMap.setStyle(Style.LIGHT, new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap, style);
                    localizationPlugin.setMapLanguage(Locale.KOREA);
                    Layer settlementLabelLayer = style.getLayer("settlement-label");
                    settlementLabelLayer.setProperties(textField("{name_korea}"));
                    initCoordinates();
                    enableLocationComponent(style);
                }
            });
        }
        private void initCoordinates() {
            lineLatLngList = new ArrayList<>();
            coordinates = new JsonArray();
            coordinates_ = new JsonArray();
            coordinates.add(coordinates_);
        }

        @SuppressWarnings({"MissingPermission"})
        private void enableLocationComponent(@NonNull Style loadedMapStyle) {
            // Check if permissions are enabled and if not request
            if (PermissionsManager.areLocationPermissionsGranted(getContext())) {
                // Get an instance of the component
                LocationComponent locationComponent = mapboxMap.getLocationComponent();
                // Activate with options
                locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions.builder(getContext(), loadedMapStyle).build());
                // Enable to make component visible
                locationComponent.setLocationComponentEnabled(true);

                // Set the component's camera mode
                locationComponent.setCameraMode(CameraMode.TRACKING);

                // Set the component's render mode
                locationComponent.setRenderMode(RenderMode.COMPASS);
            } else {
                permissionsManager = new PermissionsManager(Fragment_2.this);
                permissionsManager.requestLocationPermissions(getActivity());
            }
        }
    }

    //2]onCreateView()오버 라이딩
    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i("com.kosmo.shooong", "onCreateView:2");
        //위젯 활성화
        Mapbox.getInstance(getContext(), getString(R.string.mapbox_access_token));

        //레이아웃 전개]
        final View view = inflater.inflate(R.layout.tablayout_2, null, false);
        nowspeed = view.findViewById(R.id.nowSpeed);
        tvStepCount = view.findViewById(R.id.textviewstep);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER); // 0부터 세고싶으면  TYPE_STEP_COUNTER
        if (stepCountSensor == null) {
            Toast.makeText(getContext(), "만보기를 지원하지 않는 기기입니다", Toast.LENGTH_SHORT).show();
        }
        mCounterSteps = 0;
        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new NaviOnMapReadyCallback());

        recordRoute = view.findViewById(R.id.route_record);
        uploadRoute = view.findViewById(R.id.route_upload);

        //버튼에 리스너 부착
        recordRoute.setOnClickListener(recordListener);
        uploadRoute.setOnClickListener(uploadListener);
        return view;
    }///////////////onCreateView

    private View.OnClickListener uploadListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };

    //버튼 이벤트 처리]
    private View.OnClickListener recordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new ShooongAsyncTask().execute(
                    "http://192.168.0.15:8080/shoong/android/member/json",id,pwd);
        }
    };//////////////////OnClickListener

    //PolyLine Draw & GeoJson IO 스레드 정의
    private class ShooongAsyncTask extends AsyncTask<String,Void,String>{

        private WeakReference<Fragment_2> weakReference;

        @Override
        protected void onPreExecute() {
            locflag = !locflag;

            if(locflag) {
                recordRoute.setText("측정 종료하기");
            } else {
                recordRoute.setText("측정 시작하기");
                //지오제이슨 생성
                try {
                    setGeoJson();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ContentValues values = new ContentValues();
                FrameLayout container = Fragment_2.this.mapView;//findViewById(R.id.mapView);
                container.buildDrawingCache();
                Bitmap captureView = container.getDrawingCache();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "image_1024.JPG");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
                ContentResolver contentResolver = getContext().getContentResolver();
                Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                FileOutputStream fos;
                Log.i("com.kosmo.shooong", "클릭");
                try {
                    ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);
                    fos = new FileOutputStream(pdf.getFileDescriptor());

                    captureView.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                } catch (Exception e) { e.printStackTrace(); }
                Toast.makeText(getContext(), "Captured!", Toast.LENGTH_LONG).show();
            }
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            double latitude;
            double longitude;
            if(recordRoute.getText().equals("측정 종료하기")){//서버에 전송하기
                Log.i("com.kosmo.shooong", "종료하기 클릭");
                Log.i("com.kosmo.shooong", "서버에 전송");
                return null;
            }
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "권한이 수락되지 않았습니다. 다시 시도해주세요", Toast.LENGTH_LONG).show();
                return null;
            }
            Location loctemp =  mapboxMap.getLocationComponent().getLastKnownLocation();
            latitude = loctemp.getLatitude();
            longitude = loctemp.getLongitude();
            Log.i("com.kosmo.shooong", "latlng " + latitude +" - "+longitude);
            double distance = 0.0;
            while (locflag) { // 스레드
                try {
                    // locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE); //퉷
                    // 와이파이 되어있으면 네트워크가 좋고 네트워크
                    Location location = mapboxMap.getLocationComponent().getLastKnownLocation();
                    float locToMeter = location.distanceTo(loctemp); //2초전에 저장한 위치랑 현재위치 비교해서 m로 반환함
                    distance = distance + locToMeter;
                    Log.i("com.kosmo.shooong", "loctoMe " + locToMeter);
                    if(speed>1&&speed<180)//움직이지 않거나 GPS 신호가 잡히지 않을때는 speed 고정
                        nowspeed.setText(String.format(" %.1f km/s\n %d m 이동" ,(float)speed, (int)distance));
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Log.i("com.kosmo.shooong", "latlng " + latitude +" - "+longitude);

                    speed = locToMeter*1.8; //2초에 한번씩 불러옴
                    lineLatLngList.add(new LatLng(latitude, longitude)); //선을 연결할 좌표를 추가해줌
                    JsonArray nowLatLng = new JsonArray();
                    nowLatLng.add(longitude);
                    nowLatLng.add(latitude);
                    coordinates_.add(nowLatLng);
                    mHandler.sendEmptyMessage(200); //지도에 선을 그어주는 함수 추가
                    //X_ = latitude;Y_=longitude;
                    deltime =  System.currentTimeMillis();
                    loctemp = location; // 위치 저장해서 2초뒤에 비교할거
                } catch (Exception e) {
//                                Toast.makeText(getContext(), "에러 발생 : " + e, Toast.LENGTH_LONG).show();
                    Log.i("com.kosmo.shooong", "에러 발생 " + e);
                }
            }
            SystemClock.sleep(1000);
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            //doInBackground()메소드 완료 후 처리할 코드
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            //UI 변경 - DrawLine
            super.onProgressUpdate(values);
        }
    }/////////////////ShooongAsyncTask

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (locflag){
                switch (msg.what) {
                    case 200:
                        Log.d("com.kosmo.shooong", "선그리기요청");

                        mapboxMap.addPolyline(new PolylineOptions()
                                .addAll(lineLatLngList)
                                .color(Color.parseColor("#F67A42"))
                                .width(3));
                        break;
                }
            }else{
                for(Polyline p : mapboxMap.getPolylines()){
                    mapboxMap.removePolyline(p);}
                lineLatLngList = new ArrayList<>();
            }
        }
    };

    private void setGeoJson() throws IOException {
        FileOutputStream outputStream;
        JsonObject geoJson;
        JsonObject properties;
        JsonObject geometry;
        //JsonArray coordinates;
        SharedPreferences preferences = getContext().getSharedPreferences("loginInfo", Activity.MODE_PRIVATE);
        id=preferences.getString("id",null);//아이디 얻기
        name=preferences.getString("name",null);
        Log.i("com.kosmo.shooong",id);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh:mm");
        nowday = format.format(System.currentTimeMillis()); //현재 날짜 얻어오기
        String filename = name+"_"+nowday+".json";//파일이름 지정
        String filepath = "/data/data/com.kosmo.shooong/files/"+filename; //안드로이드 내부 저장소(앱 삭제되면 같이 삭제 / 다른 앱에서 접근못함)
        File jsonfile = new File(filepath);
        if (!jsonfile.exists()) { //파일 없으면 빈 파일 생성
            jsonfile.createNewFile();
            geoJson = new JsonObject();
            properties = new JsonObject();
            geometry = new JsonObject();
            //coordinates = new JsonArray();
            geometry.addProperty("type","MultiLineString");
            geometry.add("coordinates",coordinates);
            properties.addProperty("userId",id);
            properties.addProperty("userName",name);
            properties.addProperty("time",nowday);
            geoJson.addProperty("type","Feature");
            geoJson.add("properties",properties);
            geoJson.add("geometry",geometry);
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(geoJson.toString().getBytes());
            outputStream.close();
        }
        /*
        FileReader filereader = new FileReader(txtfile);
        int singleCh = 0;
        String walkdata = "";
        while ((singleCh = filereader.read()) != -1) { // 파일 읽어오기(한글자씩)
            walkdata = walkdata + (char) singleCh;
        }
        filereader.close();

        String[] daywork = walkdata.split("/");   //날짜별 split로 구분 ex) 2020-07-15,1000 >>/<< 2020-07-14,2000/
        if (daywork[0].substring(0, 10).equals(nowday)) { // 현재 날짜와 구분하여 다르면 새 날짜 추가
            daywork[0] = nowday + "," + mSteps;
        }
        String stringa = walkdata;
        if (!stringa.substring(0, 10).equals(nowday)) {
            stringa = nowday + "," + mSteps + "/" + stringa;
        } else {
            //첫번째 / 찾아서 그 전까지 replace
            int index = stringa.indexOf("/");
            stringa = stringa.replace(stringa.substring(0, index), nowday + "," + mSteps);
        }
        // beforeDayStep = Integer.parseInt(stringa.split("/")[1].split(",")[1]);
        //데이터 수동으로 넣을라면 여기 넣으면 됨
        //stringa ="2020-07-20,4177/2020-07-19,6671/2020-07-17,5715/2020-07-16,8560/2020-07-15,9248/2020-07-14,5776/2020-07-13,6681/2020-07-12,3140/";
        try {
            outputStream = getContext().openFileOutput(filenamea, Context.MODE_PRIVATE);
            outputStream.write(stringa.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
         */
    }

    public static CharSequence getsteps() {
        if (mSteps == 0) {
            return (CharSequence) "앱을 실행시켜주세요";
        } else
            return "현재 걸음 수 : " + (CharSequence) Integer.toString(mSteps);
    }

    //걸음수 초기화 되지 않음
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (mCounterSteps < 1) {
                // initial value
                mCounterSteps = (int) event.values[0];
            }
            //  mSteps = (int) event.values[0] - mCounterSteps; // 앱 켤때마다 카운터 0으로 초기화 시킴
            tvStepCount.setText("현재 걸음 수 : " + (int) event.values[0]);

            mSteps = (int) event.values[0];

            NotificationManager notificationManager=(NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = null;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelID = "channel_01"; //알림채널 식별자
                String channelName = "MyChannel01"; //알림채널의 이름(별명)
                NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
                builder = new NotificationCompat.Builder(getContext(), channelID);
            }else{
                //알림 건축가 객체 생성
                builder= new NotificationCompat.Builder(getContext(), null);
            }


            builder.setSmallIcon(android.R.drawable.star_off);
            builder.setContentTitle("Shooong");//알림창 제목
            builder.setContentText(mSteps + "걸음");//알림창 내용
            Bitmap bm= BitmapFactory.decodeResource(getResources(),R.drawable.icon_s);
            builder.setLargeIcon(bm);//매개변수가 Bitmap을 줘야한다.
            Notification notification=builder.build();
            notification.vibrate = new long[] {-1};
            notification.flags = Notification.FLAG_ONLY_ALERT_ONCE;
            notificationManager.notify(1, notification);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    public void onResume() { //처음 켤때
        super.onResume();
        sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mapView.onResume();
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {

    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {

    }

    private static class LocationChange
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<Fragment_2> activityWeakReference;
        LocationChange(Fragment_2 activity) {
            Log.i("com.kosmo.shooong", "생성자");
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            Fragment_2 activity = activityWeakReference.get();
            Log.i("com.kosmo.shooong", result.getLastLocation()+","+result.getLocations());

            if (activity != null) {
                Location location = result.getLastLocation();

                Toast.makeText(activity.getContext(), ""+location.getLatitude() + location.getLongitude(), Toast.LENGTH_SHORT).show();
                if (location == null) {
                    Toast.makeText(activity.getContext(), ""+location.getLatitude() + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create a Toast which displays the new location's coordinates

                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                    Toast.makeText(activity.getContext(), ""+location.getLatitude() + location.getLongitude(), Toast.LENGTH_SHORT).show();
                }
            }



        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            Fragment_2 activity = activityWeakReference.get();

        }
    }


}