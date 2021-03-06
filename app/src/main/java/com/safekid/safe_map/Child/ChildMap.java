package com.safekid.safe_map.Child;

import static android.app.PendingIntent.getActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.safekid.safe_map.http.CommonMethod;
import com.safekid.safe_map.FHome.Astar;
import com.safekid.safe_map.FHome.jPoint;
import com.safekid.safe_map.R;
import com.safekid.safe_map.common.ChildData;
import com.safekid.safe_map.common.ProfileData;
import com.safekid.safe_map.http.RequestHttpURLConnection;

import org.json.JSONObject;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import com.skt.Tmap.TMapCircle;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;

public class ChildMap extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    private static final int PERMISSION_RQST_SEND = 8282;
    String UUID = ChildData.getChildId();
    String parent_id = ProfileData.getUserId();
    Astar astar = new Astar();
    Astar astar_t = new Astar();

    Context mContext = ChildMap.this;


    // ?????? ??????
    final int onfoot = 0;
    final int alley = 1;
    final int traffic = 2;
    final int crosswalk = 3;

    // ?????? ??????
    double src_lat, src_lon, dst_lat, dst_lon;
    String src_name, dst_name;

    // ?????? ?????? ??????
    jPoint jp_src = new jPoint();
    jPoint jp_dst = new jPoint();


    TMapView tMapView = null;
    private TMapGpsManager tmapgps = null;
    RelativeLayout mapView;
    double latitude, longitude;

    private LocationManager manager;
    private static final int REQUEST_CODE_LOCATION = 2;

    ImageButton home, camera, call, qr;
    String number = "0100000000";
    File file;

    private TimerTask task;
    NotificationManager Nmanager;
    NotificationCompat.Builder builder;

    private static String CHANNEL_ID = "channel1";
    private static String CHANEL_NAME = "Channel1";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_map);

        //home = (ImageButton) findViewById(R.id.homeaddr);
        //camera = (ImageButton) findViewById(R.id.camera);
        call = (ImageButton) findViewById(R.id.call);

        // ????????? ?????? ????????? ?????? ??????
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getMyLocation();
        //gpsListener = new GPSListener();

        //mapview ??????
        tMapView = new TMapView(this);

        tMapView.setSKTMapApiKey("l7xx94f3b9ca30ba4d16850a60f2c3ebfdd5");
        //tMapView.setLocationPoint(latitude,longitude);
        //tMapView.setCenterPoint(latitude,longitude);
        tMapView.setCompassMode(true);
        tMapView.setIconVisibility(true);
        tMapView.setZoomLevel(18); // ????????? ??????
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);  //????????????
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        tMapView.setTrackingMode(true);
        tMapView.setSightVisible(true);

        mapView = (RelativeLayout) findViewById(R.id.childMapView2);
        mapView.addView(tMapView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        tmapgps = new TMapGpsManager(this);
        tmapgps.setMinTime(1000);
        tmapgps.setMinDistance(10);
        tmapgps.setProvider(tmapgps.GPS_PROVIDER); //????????? ??????????????? ??? ????????? ????????????.

        //????????? ??? ???????????????.
        //tmapgps.setProvider(tmapgps.GPS_PROVIDER); //gps??? ??? ????????? ????????????.
        tmapgps.OpenGps();

        // 1. ????????? ??????(?????? ??????) ????????????
        GetErrandData();

        // 2. ??????, ??????, ?????? ?????? ??????
        ParseInformations();

        // 3. ?????? ?????? ??????
        FindSafePath();

        // 4. ?????? ?????? ?????????
        ShowDangerZoneOnMap();

        // 5. ??????, ??????, ?????? ?????? ?????????
        ShowSrcMidDstOnMap();

        // 6. ?????? ?????? ????????? ????????? ( ?????????, ???????????? )
        ShowPathInfoOnMap();

        // ????????? ?????? ?????? 5??? ?????? ????????? ??????
        sendLocation();

        /*home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        File sdcard = Environment.getExternalStorageDirectory();
        file = new File(sdcard, "capture.jpg");
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                *//*Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getContext(), "com.safekid.safe_map.fileProvider",file));
                startActivityForResult(intent, 101);*//*
            }
        });*/

        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                number = fetchPhone(ProfileData.getUserId());
                if (number.equals("")) {
                    Toast.makeText(ChildMap.this, "????????? ??????????????? ???????????? ???????????????.", Toast.LENGTH_LONG).show();
                } else {
                    Intent tt = new Intent("android.intent.action.DIAL", Uri.parse("tel:" + number));
                    startActivity(tt);
                }

            }
        });
    }


    private void ParseInformations() {
        astar.ParseNode(mContext);
        astar.ParseLinks(mContext);
        astar.ParseDanger(mContext);

        astar_t.ParseNode(mContext);
        astar_t.ParseLinks(mContext);
        astar_t.ParseDanger(mContext);
    }

    private void FindSafePath() {
        // 1. ?????? ????????? ?????????.
        astar.FindDangerousNodeNum();

        // 2. ??????/???????????? ?????? ????????? ??????????????? ?????????.
        int start = astar.findCloseNode(jp_src);
        int end = astar.findCloseNode(jp_dst);


        // 3. ??? ?????? ????????? ???????????? A* ???????????? ??????.
        astar.AstarSearch(start, end);
        astar_t.AstarSearch(start, end);

        // 4. closeList??? ???????????? ?????? ??????
        astar.FindPath(start, end);
        astar_t.FindPath(start, end);


        // 5. ?????? ???????????? ????????? ???????????? ( ?????? + ?????? + ?????? ) ?????? ????????? ??????.
        astar.GetCoordPath(jp_src.GetLat(), jp_src.GetLng(), jp_dst.GetLat(), jp_dst.GetLng());
        astar_t.GetCoordPath(jp_src.GetLat(), jp_src.GetLng(), jp_dst.GetLat(), jp_dst.GetLng());

        // 6. ?????? ?????? ??????.
        astar.GetPathInfo();

    }

    private void GetErrandData() {
        // 1. ?????? ???????????? ???????????? ????????? ????????? ????????????.
        String url = CommonMethod.ipConfig + "/api/fetchRecentErrand";
        String rtnStr = "";

        try {
            String jsonString = new JSONObject()
                    .put("userId", parent_id)
                    .toString();

            //REST API
            RequestHttpURLConnection.NetworkAsyncTask networkTask = new RequestHttpURLConnection.NetworkAsyncTask(url, jsonString);
            rtnStr = networkTask.execute().get();

            Log.d("ChildMap123", "/api/fetchRecentErrand : " + rtnStr);

            JSONObject Alldata = new JSONObject(rtnStr);

            // 2. ?????? ??????
            src_lat = Double.parseDouble(Alldata.getString("start_latitude"));
            src_lon = Double.parseDouble(Alldata.getString("start_longitude"));
            dst_lat = Double.parseDouble(Alldata.getString("target_latitude"));
            dst_lon = Double.parseDouble(Alldata.getString("target_longitude"));

            src_name = Alldata.getString("start_name");
            dst_name = Alldata.getString("target_name");

            // 3. ?????????, ???????????? ?????? ?????? ?????????
            jp_src.SetLat(src_lat);
            jp_src.SetLng(src_lon);
            jp_dst.SetLat(dst_lat);
            jp_dst.SetLng(dst_lon);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChange(Location location) {
        tMapView.setLocationPoint(location.getLongitude(), location.getLatitude());
        tMapView.setCenterPoint(location.getLongitude(), location.getLatitude());
    }

    /*public void getLocation() {
        // Get the location manager
        LocationManager locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(bestProvider);
        LocationListener loc_listener = new LocationListener() {

            public void onLocationChanged(Location l) {}

            public void onProviderEnabled(String p) {}

            public void onProviderDisabled(String p) {}

            public void onStatusChanged(String p, int status, Bundle extras) {}
        };
        locationManager
                .requestLocationUpdates(bestProvider, 5000, 0, loc_listener);
        location = locationManager.getLastKnownLocation(bestProvider);
        try {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

        } catch (NullPointerException e) {
            latitude = -1.0;
            longitude = -1.0;
        }

    }*/

    /**
     * ????????? ????????? ??????
     */
    private void sendLocation() {
        final Location[] currentLocation = new Location[1];

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("////////////??????????????? ????????? ???????????????");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, this.REQUEST_CODE_LOCATION);
            sendLocation();
        } else {
            System.out.println("////////////???????????? ????????????");
            // ???????????? ?????? ?????????
            Timer scheduler = new Timer();
            task = new TimerTask() {
                private static final int REQUEST_CODE_LOCATION = 2;

                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    currentLocation[0] = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // ?????? GPS_PROVIDER, ?????? NETWOR_PROVIDER
                    latitude = currentLocation[0].getLatitude();
                    longitude = currentLocation[0].getLongitude();
                    System.out.println("?????? ?????? ????????? : " + latitude + "," + longitude);
                    registerChildLocation(UUID, latitude, longitude);

                    /*Location loc = getMyLocation();
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                    System.out.println("?????? ?????? ????????? : " + latitude + "," + longitude);
                    */
                    //registerChildLocation(ChildData.getChildId(), latitude, longitude);

                    // ?????? ???????????? ???????????? ????????? ??????
                    double middle_lat = getMiddleLat();
                    double middle_lon = getMiddleLon();

                    float distance = getDistance(latitude, longitude, middle_lat, middle_lon);
                    Log.i("????????????????????? ??????", String.valueOf(distance));
                    if (distance < 7.0) { //3m

                        if (ChildData.getcheckSMS() == false){
                            Log.i("?????????", "False");
                            ChildData.setCheckSMS(true);
//                            sendSMS();
                            showNoti();
                        } else {
                            Log.i("?????????", "TRUE");
                        }
                    }
                }
            };

            scheduler.scheduleAtFixedRate(task, 0, 1000); // 1????????? ????????????*/
        }


    }

    public void onBackPressed() {
        if (task != null) {
            task.cancel();
            Log.i("timer", "??????");
        }
        finish();
    }

    private Location getMyLocation() {
        Location currentLocation = null;
        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("////////////??????????????? ????????? ???????????????");
            ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, this.REQUEST_CODE_LOCATION);
            getMyLocation();
        } else {
            System.out.println("////////////???????????? ????????????");
            // ???????????? ?????? ?????????
            String locationProvider = LocationManager.GPS_PROVIDER;
            currentLocation = manager.getLastKnownLocation(locationProvider);
            /*if (currentLocation != null) {
                longitude = currentLocation.getLongitude();
                latitude = currentLocation.getLatitude();
            }*/
        }
        return currentLocation;
    }


    private float getDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] distance = new float[2];
        Location.distanceBetween(lat1, lon1, lat2, lon2, distance);
        return distance[0];
    }


    // ?????? ?????? ?????? ??????
    public static void registerChildLocation(String UUID, double current_latitude, double current_longitude) {
        String url = CommonMethod.ipConfig + "/api/savePositionChild";

        try {
            String jsonString = new JSONObject()
                    .put("UUID", UUID)
                    .put("current_latitude", current_latitude)
                    .put("current_longitude", current_longitude)
                    .toString();

            //REST API
            RequestHttpURLConnection.NetworkAsyncTask networkTask = new RequestHttpURLConnection.NetworkAsyncTask(url, jsonString);
            networkTask.execute().get();
            //Toast.makeText(getContext().getApplicationContext(), "??????????????? ?????????????????????", Toast.LENGTH_LONG).show();
            Log.i("???????????? ??????", String.format("????????? ?????? ?????? : lat " + current_latitude + ", long " + current_longitude));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void sendSMS () {
//        try {
//            if (ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.SEND_SMS)) {
//                    Log.i("kkk", "no");
//                }
//                else { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_RQST_SEND);
//                    Log.i("ddd", "n");
//                }
//            } else {
//                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_RQST_SEND);
//
//            }
//            //Toast.makeText(getApplicationContext(), "???????????? ????????? ?????? ??????!", Toast.LENGTH_LONG).show();
//
//        } catch (Exception e) {
//            //Toast.makeText(getApplicationContext(), "????????? ?????? ??????", Toast.LENGTH_LONG).show();
//            e.printStackTrace();
//        }
//
//    }

    //Now once the permission is there or not would be checked
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_RQST_SEND: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //??????
                    SmsManager smsManager = SmsManager.getDefault();
                    String phoneNo = fetchPhone(ProfileData.getUserId());
                    smsManager.sendTextMessage(phoneNo, null, "?????? ????????? ????????? ????????? ?????? ????????? ??? ???????????????", null, null);
                    Log.i("sms", "??????");
//                    Toast.makeText(getApplicationContext(), "SMS sent.",Toast.LENGTH_LONG).show();
                } else {
                    Log.i("sms", "??????");
//                    Toast.makeText(getApplicationContext(), "SMS failed, you may try again later.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    public void showNoti(){
        builder = null;
        Nmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //?????? ????????? ????????? ??????
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Nmanager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, CHANEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            );

            builder = new NotificationCompat.Builder(this,CHANNEL_ID);

            //?????? ????????? ??????
        }else{
            builder = new NotificationCompat.Builder(this);
        }

        //????????? ??????
        builder.setContentTitle("?????? ?????? ???????????? ?????????");

        //????????? ?????????
        builder.setContentText("????????? ????????? ?????? ????????? ????????????");

        //????????? ?????????
        builder.setSmallIcon(R.drawable.simbu_logo);


        Notification notification = builder.build();

        //????????? ??????
        Nmanager.notify(1,notification);
    }

    public String fetchPhone(String userId) {
        String url = CommonMethod.ipConfig + "/api/fetchTelNum";
        String rtnStr = "";

        try {
            String jsonString = new JSONObject()
                    .put("userId", userId)
                    .toString();

            //REST API
            RequestHttpURLConnection.NetworkAsyncTask networkTask = new RequestHttpURLConnection.NetworkAsyncTask(url, jsonString);
            rtnStr = networkTask.execute().get();

//          Toast.makeText(getActivity(), "?????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
            //Log.i(TAG, String.format("????????? Phonenum: (%s)", rtnStr));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rtnStr;
    }


    //
    // ??????, ??????, ?????? ?????? ????????? ?????????
    void ShowSrcMidDstOnMap() {

        int size = astar.jp_path.size();


        // safe_path.get(0) = ?????? ??????     >> ????????????, ???????????? ??? ??? ????????? ??????????????? ?????????.
        TMapMarkerItem markerItem1 = new TMapMarkerItem();
        TMapPoint mark_point1 = new TMapPoint(astar.jp_path.get(0).GetLat(), astar.jp_path.get(0).GetLng());
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jhouse);
        markerItem1.setIcon(bitmap); // ?????? ????????? ??????
        markerItem1.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
        markerItem1.setTMapPoint(mark_point1); // ????????? ?????? ??????
        markerItem1.setCanShowCallout(true);
        markerItem1.setCalloutTitle("??????!");
        markerItem1.setCalloutSubTitle("?????? ???????????????!");
        tMapView.addMarkerItem("markerItem1", markerItem1); // ????????? ?????? ??????


        TMapMarkerItem markerItem3 = new TMapMarkerItem();

       // getMiddleLat
       // TMapPoint mark_point3 = new TMapPoint(astar.jp_path.get(size/2 -1).GetLat(), astar.jp_path.get(size/2 -1).GetLng());
       TMapPoint mark_point3 = new TMapPoint(getMiddleLat(), getMiddleLon());

       Log.d("childmap123"," lat : "+getMiddleLat() + " lon : "+ getMiddleLon());

        Bitmap bitmap3 = BitmapFactory.decodeResource(getResources(), R.drawable.jmiddle);
        markerItem3.setIcon(bitmap3); // ?????? ????????? ??????
        markerItem3.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
        markerItem3.setTMapPoint(mark_point3); // ????????? ?????? ??????
        markerItem3.setName("??????"); // ????????? ????????? ??????

        // ?????????
        markerItem3.setCanShowCallout(true);
        markerItem3.setCalloutTitle("?????? ??????");
        markerItem3.setCalloutSubTitle("???????????? ??? ???????????? ??????????");
        //markerItem3.setCalloutRightButtonImage(bitmap3);

        tMapView.addMarkerItem("markerItem3", markerItem3); // ????????? ?????? ??????


        // safe_path.get(size-1) = ?????? ??????  >> ????????????, ???????????? ??? ??? ????????? ??????????????? ?????????.
        TMapMarkerItem markerItem2 = new TMapMarkerItem();
        TMapPoint mark_point2 = new TMapPoint(astar.jp_path.get(size - 1).GetLat(), astar.jp_path.get(size - 1).GetLng());
        Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.jend);
        markerItem2.setIcon(bitmap2); // ?????? ????????? ??????
        markerItem2.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
        markerItem2.setTMapPoint(mark_point2); // ????????? ?????? ??????
        markerItem2.setName("??????"); // ????????? ????????? ??????
        markerItem2.setCanShowCallout(true);
        markerItem2.setCalloutTitle("??????!");
        markerItem2.setCalloutSubTitle("?????? ???????????????!");
        tMapView.addMarkerItem("markerItem2", markerItem2); // ????????? ?????? ??????

        // ?????? ?????? ?????? - gps ?????? ????????? ??? ??????
        //tMapView.setCenterPoint(astar.jp_path.get(size / 2).GetLat(), astar.jp_path.get(size / 2).GetLng());


    }

    // ?????? ????????? ????????? ????????? ?????? ????????????.
    private void ShowDangerZoneOnMap() {

        int RED = 0;
        int GREEN = 0;
        int BLUE = 0;
        String TAG = "";
        Bitmap bitmap;


        for (int o = 0; o < astar.DangerZone.size(); o++) {

            TMapPoint cmid = new TMapPoint(astar.DangerZone.get(o).GetLat(), astar.DangerZone.get(o).GetLng());
            TMapCircle tMapCircle = new TMapCircle();
            tMapCircle.setCenterPoint( cmid );
            tMapCircle.setRadius(20);
            tMapCircle.setCircleWidth(2);
            tMapCircle.setLineColor(Color.argb(0,255,255,255));
            tMapCircle.setAreaColor(Color.RED);
            tMapCircle.setAreaAlpha(20);
            tMapView.addTMapCircle("circle1"+o, tMapCircle);

            TMapCircle tMapCircle2 = new TMapCircle();
            tMapCircle2.setCenterPoint( cmid );
            tMapCircle2.setRadius(15);
            tMapCircle2.setCircleWidth(2);
            tMapCircle2.setLineColor(Color.argb(0,255,255,255));
            tMapCircle2.setAreaColor(Color.RED);
            tMapCircle2.setAreaAlpha(75);
            tMapView.addTMapCircle("circle2"+o, tMapCircle2);

            TMapCircle tMapCircle3 = new TMapCircle();
            tMapCircle3.setCenterPoint( cmid );
            tMapCircle3.setRadius(10);
            tMapCircle3.setCircleWidth(2);
            tMapCircle3.setLineColor(Color.argb(0,255,255,255));
            tMapCircle3.setAreaColor(Color.RED);
            tMapCircle3.setAreaAlpha(125);
            tMapView.addTMapCircle("circle3"+o, tMapCircle3);

            TMapCircle tMapCircle4 = new TMapCircle();
            tMapCircle4.setCenterPoint( cmid );
            tMapCircle4.setRadius(5);
            tMapCircle4.setCircleWidth(2);
            tMapCircle4.setLineColor(Color.argb(0,255,255,255));
            tMapCircle4.setAreaColor(Color.RED);
            tMapCircle4.setAreaAlpha(255);
            tMapView.addTMapCircle("circle4"+o, tMapCircle4);



            // ?????? ?????? ????????? ???????????? ?????? ???????????????
            if (astar.DangerZone.get(o).GetType() == 1.0) {

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jdevil);

                TMapMarkerItem markerItem2 = new TMapMarkerItem();
                TMapPoint mark_point2 = new TMapPoint(astar.DangerZone.get(o).GetLat(), astar.DangerZone.get(o).GetLng());
                markerItem2.setIcon(bitmap); // ?????? ????????? ??????
                markerItem2.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
                markerItem2.setTMapPoint(mark_point2); // ????????? ?????? ??????
                markerItem2.setCanShowCallout(true);
                markerItem2.setCalloutTitle("?????? ??????");
                markerItem2.setCalloutSubTitle("???????????? ?????? ??????");

                tMapView.addMarkerItem("danger" + o, markerItem2); // ????????? ?????? ??????
            }
            // ????????? ?????? ?????? ????????? ??????
            else if (astar.DangerZone.get(o).GetType() == 2.0) {

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jaccident);

                TMapMarkerItem markerItem2 = new TMapMarkerItem();
                TMapPoint mark_point2 = new TMapPoint(astar.DangerZone.get(o).GetLat(), astar.DangerZone.get(o).GetLng());
                markerItem2.setIcon(bitmap); // ?????? ????????? ??????
                markerItem2.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
                markerItem2.setTMapPoint(mark_point2); // ????????? ?????? ??????
                markerItem2.setCanShowCallout(true);
                markerItem2.setCalloutTitle("?????? ?????? ??????");
                markerItem2.setCalloutSubTitle("????????? ?????? ??????");

                tMapView.addMarkerItem("danger" + o, markerItem2); // ????????? ?????? ??????

            }
            // ????????? ?????? ?????? ????????? ??????
            else if (astar.DangerZone.get(o).GetType() == 3.0) {

                TAG = "????????? ?????? ?????? ??????";

                // ????????? ?????? ????????? ?????? ???
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jcyclist);

                TMapMarkerItem markerItem2 = new TMapMarkerItem();
                TMapPoint mark_point2 = new TMapPoint(astar.DangerZone.get(o).GetLat(), astar.DangerZone.get(o).GetLng());
                markerItem2.setIcon(bitmap); // ?????? ????????? ??????
                markerItem2.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
                markerItem2.setTMapPoint(mark_point2); // ????????? ?????? ??????
                markerItem2.setCanShowCallout(true);
                markerItem2.setCalloutTitle("?????? ?????? ??????");
                markerItem2.setCalloutSubTitle("????????? ?????? ??????");

               tMapView.addMarkerItem("danger" + o, markerItem2); // ????????? ?????? ??????
            }
            // ???????????? ?????? ????????? ??????
            else if (astar.DangerZone.get(o).GetType() == 4.0) {

                TAG = "???????????? ?????? ??????";
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jaccident_car);

                TMapMarkerItem markerItem2 = new TMapMarkerItem();
                TMapPoint mark_point2 = new TMapPoint(astar.DangerZone.get(o).GetLat(), astar.DangerZone.get(o).GetLng());
                markerItem2.setIcon(bitmap); // ?????? ????????? ??????
                markerItem2.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
                markerItem2.setTMapPoint(mark_point2); // ????????? ?????? ??????
                markerItem2.setCanShowCallout(true);
                markerItem2.setCalloutTitle("?????? ?????? ??????");
                markerItem2.setCalloutSubTitle("???????????? ??????");

                tMapView.addMarkerItem("danger" + o, markerItem2); // ????????? ?????? ??????
            } else {

                TAG = "?????? ?????? ??????";
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jsirenback);

                TMapMarkerItem markerItem2 = new TMapMarkerItem();
                TMapPoint mark_point2 = new TMapPoint(astar.DangerZone.get(o).GetLat(), astar.DangerZone.get(o).GetLng());
                markerItem2.setIcon(bitmap); // ?????? ????????? ??????
                markerItem2.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
                markerItem2.setTMapPoint(mark_point2); // ????????? ?????? ??????
                markerItem2.setCanShowCallout(true);
                markerItem2.setCalloutTitle("?????? ?????? ??????");
               // markerItem2.setCalloutSubTitle("???????????? ??????");

                tMapView.addMarkerItem("danger" + o, markerItem2); // ????????? ?????? ??????

            }

        }
    }

    // ?????? ????????? ??????(?????????, ??????, ???????????? ???)??? ????????? ????????? ?????? ????????????.
    private void ShowPathInfoOnMap() {
        int start = 0;
        int size = 1;
        int tmp = astar.link_info.get(0);
        int i;


        for (i = 1; i < astar.link_info.size(); i++) {
            if (tmp == astar.link_info.get(i)) {
                // ????????? ????????? ????????? continue
                size += 1;
                continue;
            }
            else {
                // ????????? ????????? ?????????.
                if (size > 1) {
                    addmarker(start + size / 2, tmp);
                } else {
                    addmarker(start, tmp); // ????????? ?????? ??????
                }
                addpath(start, i, tmp);

                tmp = astar.link_info.get(i);
                start = i;
                size = 1;
            }
        }
        addmarker(start + size / 2, tmp);
        addpath(start,i, tmp);

    }


    // ?????? ?????? ????????? ????????? ?????? ?????????
    private void addmarker(int start, int type) {


        if(type == onfoot){

        }
        else if(type == alley){
            Bitmap bitmap2 = null;
            TMapMarkerItem markerItem4 = new TMapMarkerItem();
            markerItem4.setCanShowCallout(true);
            bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.jalley);
            markerItem4.setCalloutTitle("?????????");
            markerItem4.setCalloutSubTitle("????????? ????????? ?????????!");

            double mid_lat = (astar.jp_path.get(start+1).GetLat() + astar.jp_path.get(start + 2).GetLat())/2.0;
            double mid_lon = (astar.jp_path.get(start+1).GetLng() + astar.jp_path.get(start + 2).GetLng())/2.0;

            TMapPoint mark_point2 = new TMapPoint(mid_lat, mid_lon);
            markerItem4.setIcon(bitmap2); // ?????? ????????? ??????
            markerItem4.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
            markerItem4.setTMapPoint(mark_point2); // ????????? ?????? ??????
            tMapView.addMarkerItem("markerItem4" + start, markerItem4); // ????????? ?????? ??????
        }
        else if(type == traffic){
            Bitmap bitmap2 = null;
            TMapMarkerItem markerItem4 = new TMapMarkerItem();
            markerItem4.setCanShowCallout(true);
            bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.jtraffic_lights);
            markerItem4.setCalloutTitle("????????? : ???????????? ???????????????");
            markerItem4.setCalloutSubTitle("?????? ????????? ????????????!");

            double mid_lat = (astar.jp_path.get(start+1).GetLat() + astar.jp_path.get(start + 2).GetLat())/2.0;
            double mid_lon = (astar.jp_path.get(start+1).GetLng() + astar.jp_path.get(start + 2).GetLng())/2.0;

            TMapPoint mark_point2 = new TMapPoint(mid_lat, mid_lon);
            markerItem4.setIcon(bitmap2); // ?????? ????????? ??????
            markerItem4.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
            markerItem4.setTMapPoint(mark_point2); // ????????? ?????? ??????
            tMapView.addMarkerItem("markerItem4" + start, markerItem4); // ????????? ?????? ??????
        }
        else if(type == crosswalk){
            Bitmap bitmap2 = null;
            TMapMarkerItem markerItem4 = new TMapMarkerItem();
            markerItem4.setCanShowCallout(true);
            bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.jcross);
            markerItem4.setCalloutTitle("????????????");
            markerItem4.setCalloutSubTitle("???????????? ?????? ?????????!");
            markerItem4.setCalloutLeftImage(bitmap2);

            double mid_lat = (astar.jp_path.get(start+1).GetLat() + astar.jp_path.get(start + 2).GetLat())/2.0;
            double mid_lon = (astar.jp_path.get(start+1).GetLng() + astar.jp_path.get(start + 2).GetLng())/2.0;

            TMapPoint mark_point2 = new TMapPoint(mid_lat, mid_lon);
            markerItem4.setIcon(bitmap2); // ?????? ????????? ??????
            markerItem4.setPosition(0.5f, 1.0f); // ????????? ???????????? ??????, ???????????? ??????
            markerItem4.setTMapPoint(mark_point2); // ????????? ?????? ??????
            tMapView.addMarkerItem("markerItem4" + start, markerItem4); // ????????? ?????? ??????
        }
        else{

        }


       // Log.d("ChildMap123","addTmarker--");
    }

    // ????????? ????????? ?????????
    void addpath(int start,int end, int type){


        Log.d("cm123","??? ??????  start : "+ start + " end : "+ end + " type : "+ type);

        TMapPolyLine tpolyline = new TMapPolyLine();
        tpolyline.setLineWidth(10);

    //    /*
        if(type == onfoot){
            tpolyline.setLineColor(Color.argb(255, 0, 0, 0));
        }
        else if(type == alley){
            tpolyline.setLineColor(Color.argb(255, 204, 92, 37));
        }
        else if(type == traffic){
            tpolyline.setLineColor(Color.argb(255, 0,255,0));
        }
        else if(type == crosswalk){
            tpolyline.setLineColor(Color.argb(255, 0, 255, 255));
        }
        else{

        }
//*/
        //tpolyline.setLineColor(Color.argb(255, 0, 0, 0));

        for(int y = start ; y <= end ; y++) {
            TMapPoint tp = new TMapPoint(astar.jp_path.get(y+1).GetLat(),astar.jp_path.get(y+1).GetLng());
            tpolyline.addLinePoint(tp);
        }

        tMapView.addTMapPolyLine("path"+start,tpolyline);


}


    double getMiddleLat(){

        double total = 0.0;
        double middle = 0.0;
        double lat = 0.0;

        int j = 0;

        for(int i = 1 ; i < astar.jp_path.size() - 2 ; i++){
            total += GetDistance(astar.jp_path.get(i),astar.jp_path.get(i+1));
        }

        middle = total/2.0;

        for( ; j < astar.jp_path.size() -2 ; j++){
            total -= GetDistance(astar.jp_path.get(j),astar.jp_path.get(j+1));

            if( middle - total > 0){
                break;
            }

        }

        lat = (astar.jp_path.get(j).GetLat())/3.0 * 2.0 + (astar.jp_path.get(j+1).GetLat() )/3.0 * 1.0;

        return lat;
    }

    double getMiddleLon(){

        double total = 0.0;
        double middle = 0.0;
        double lon = 0.0;

        int j = 0;

        for(int i = 1 ; i < astar.jp_path.size() -2 ; i++){
            total += GetDistance(astar.jp_path.get(i),astar.jp_path.get(i+1));
        }

        middle = total/2.0;

        for( ; j < astar.jp_path.size() -2 ; j++){
            total -= GetDistance(astar.jp_path.get(j),astar.jp_path.get(j+1));

            if( middle - total > 0){
                break;
            }

        }

        lon =  (astar.jp_path.get(j).GetLng())/3.0 * 2.0 + (astar.jp_path.get(j+1).GetLng() )/3.0 * 1.0;

        return lon;
    }


    public double GetDistance(jPoint src, jPoint dst) {
        Location start = new Location("src");
        start.setLatitude(src.GetLat());
        start.setLongitude(src.GetLng());

        Location end = new Location("dst");
        end.setLatitude(dst.GetLat());
        end.setLongitude(dst.GetLng());

        double distance = start.distanceTo(end);

        return distance;
    }
}