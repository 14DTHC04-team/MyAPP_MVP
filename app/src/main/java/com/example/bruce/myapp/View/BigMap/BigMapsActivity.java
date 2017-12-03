package com.example.bruce.myapp.View.BigMap;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bruce.myapp.Adapter.MenumapAdapter;
import com.example.bruce.myapp.Data.Tourist_Location;
import com.example.bruce.myapp.Direction.DirectionFinder;
import com.example.bruce.myapp.Direction.DirectionFinderListener;
import com.example.bruce.myapp.Direction.Route;
import com.example.bruce.myapp.GPSTracker;
import com.example.bruce.myapp.Model.MBigMap;
import com.example.bruce.myapp.Presenter.BigMap.PBigMap;
import com.example.bruce.myapp.R;
import com.example.bruce.myapp.View.HistoryAndHobby.HistoryAndHobbyActivity;
import com.example.bruce.myapp.View.Information_And_Comments.InformationAndCommentsActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BigMapsActivity extends FragmentActivity implements IViewBigMap,OnMapReadyCallback,DirectionFinderListener,MenumapAdapter.RecyclerViewClicklistener {

    //biến chứ dữ liêu intent
    ArrayList<Tourist_Location> tourist_locations = new ArrayList<>();
    //googleMaps
    private GoogleMap mMap;
    private boolean count = false;
    GPSTracker gps;
    private LatLng mLocation;
    //direction
    //lấy tọa độ gps để tìm đường từ mình đến địa điểm du lịch
    private String origin;
    private String destination;
    //component
    private RecyclerView recyclerView_ListLocation;
    private MenumapAdapter adapter;

    private TextView txtToogle;
    private DrawerLayout drawer;
    //model
    private MBigMap modelBigMap = new MBigMap();
    private PBigMap pBigMap = new PBigMap(this);
    //direction
    private List<Marker> originMarkers;
    private List<Marker> destinationMarkers;
    private List<Polyline> polylinePaths;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_big_maps);

        //lấy dữ liệu từ intent
        tourist_locations = getIntent().getParcelableArrayListExtra("allLocation");



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initialize();
        textViewToogle(txtToogle,drawer);
    }


    private void initialize(){

        recyclerView_ListLocation = findViewById(R.id.recycleView_BigMap);
        txtToogle = findViewById(R.id.txtToogle);
        drawer = findViewById(R.id.drawer_map);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //lấy vi trí người dùng
        mMap.setOnMyLocationChangeListener((location) -> {
            origin = location.getLatitude() + ", " + location.getLongitude();
            mLocation = new LatLng(location.getLatitude(), location.getLongitude());

            if (count != true) {

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLocation, 14));
                count = true;
            }
        });

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

        //Google maps controller
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        gps = new GPSTracker(this);
        mLocation= new LatLng(gps.getLatitude(),gps.getLongtitude());
        origin = mLocation.latitude+", "+mLocation.longitude;
        markAllLocation(tourist_locations,mMap,recyclerView_ListLocation,adapter,mLocation);

        //tìm đường của địa điểm truyền từ HistoryAndHobby
        findDirectionFromHistoryAndHobby(origin);
        //set up bottmo sheet
        setupBottomSheet(mMap,tourist_locations);
    }

    private void markAllLocation(ArrayList<Tourist_Location> tourist_locations,GoogleMap mMap,RecyclerView recyclerView_ListLocation, MenumapAdapter adapter, LatLng mLocation)
    {

        for(Tourist_Location tl : tourist_locations){

            final  LatLng latLgData = new LatLng(tl.Latitude,tl.Longtitude);
            tl.setDistance(modelBigMap.Radius(mLocation,latLgData));
            mMap.addMarker(new MarkerOptions().position(latLgData).title(tl.LocationName).snippet(tl.Address).icon(null));
        }
        //sắp xếp lại array
        Collections.sort(tourist_locations);
        //set up cho recyclerView
        setUpRecyclerView(recyclerView_ListLocation,adapter,tourist_locations);
    }

    private void findDirectionFromHistoryAndHobby(String origin){
        if(getIntent().getStringExtra("destination") != null){
            sendRequest(origin,getIntent().getStringExtra("destination"));
        }
    }

    private void setupBottomSheet(GoogleMap mMap, ArrayList<Tourist_Location> tourist_locations){
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(BigMapsActivity.this);
                View parentView =  getLayoutInflater().inflate(R.layout.adapter_bottom_sheet,null);
                bottomSheetDialog.setContentView(parentView);
                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) parentView.getParent());
                bottomSheetBehavior.setPeekHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,400, getResources().getDisplayMetrics()));

                ImageView img_BtmSheet = parentView.findViewById(R.id.img_BtmSheet);
                TextView txt_BtmSheet = parentView.findViewById(R.id.txt_BtmSheet);
                TextView txt_BtmSheet_LocationName = parentView.findViewById(R.id.txt_BtmSheet_LocationName);
                RatingBar rB_BtmSheet_Star = parentView.findViewById(R.id.rB_BtmSheet_Star);
                for(Tourist_Location tl : tourist_locations){
                    if(tl.getLocationName().equals(marker.getTitle()) && tl.getAddress().equals(marker.getSnippet())){
                        Picasso.with(BigMapsActivity.this).load(tl.getLocationImg()).into(img_BtmSheet);
                        txt_BtmSheet.setText(tl.getBasicInfo());
                        txt_BtmSheet_LocationName.setText(tl.getLocationName());
                        rB_BtmSheet_Star.setRating(tl.getStar());
                        bottomSheetDialog.show();
                        break;
                    }
                }
                return false;
            }
        });
    }

    private void setUpRecyclerView(RecyclerView recyclerView, MenumapAdapter adapter, ArrayList<Tourist_Location> tourist_locations) {
        this.adapter = adapter;
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new MenumapAdapter(tourist_locations, this);
        recyclerView.setAdapter(adapter);
        adapter.setClickListener(this);
        adapter.notifyDataSetChanged();
    }

    private void textViewToogle(TextView txtToogle, DrawerLayout drawer){
        txtToogle.setOnClickListener(v ->{
            drawer.openDrawer(Gravity.START);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        Intent intent = new Intent(BigMapsActivity.this, HistoryAndHobbyActivity.class);
        startActivity(intent);
    }

    @Override
    public void itemClick(View view, int position) {
        gps.canGetLocation();
        Tourist_Location tl = tourist_locations.get(position);
        final Dialog info = new Dialog(BigMapsActivity.this);

        //info.requestWindowFeature(Window.FEATURE_NO_TITLE); -- bo title cua dialog
        info.setContentView(R.layout.dialog_bigmap);
        info.setTitle("Choose what you want !");
        info.show();
        Button btnDirection = info.findViewById(R.id.btnDirection);
        Button btnInformation = info.findViewById(R.id.btnInformation);
        btnDirection.setOnClickListener(v -> {
            if(!gps.canGetLocation()){
                gps.showSettingAlert();
            }
            else{

                destination = tl.Latitude + ", " + tl.Longtitude;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng (tl.Latitude, tl.Longtitude), 16));
                sendRequest(origin,destination);

                info.dismiss();
                drawer.closeDrawer(Gravity.START);
            }
        });

        btnInformation.setOnClickListener(v ->{
            info.dismiss();
            //lưu lại lịch sử xem của user
            pBigMap.receivedSaveHistoryAndBehavior(tl.location_ID,tl.getKindOfLocation(), FirebaseAuth.getInstance().getCurrentUser().getUid());
            ArrayList<Tourist_Location> tls = new ArrayList<>();
            tls.add(tl);
            Intent infor = new Intent(this, InformationAndCommentsActivity.class);
            infor.putParcelableArrayListExtra("tourist_location",tls);
            startActivity(infor);
        });
    }

    //ham tim duong
    private void sendRequest(String origin,String destination) {
        if (origin == "") {
            Toast.makeText(this, "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination == "") {
            Toast.makeText(this, "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDirectionFinderStart() {

        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));

            Toast.makeText(BigMapsActivity.this,"Distance: "+route.distance.text+"\n"+"Duration: "+route.duration.text,Toast.LENGTH_LONG).show();

            //((TextView) findViewById(R.id.tvDuration)).setText(route.duration.text);
            //((TextView) findViewById(R.id.tvDistance)).setText(route.distance.text);

//            originMarkers.add(mMap.addMarker(new MarkerOptions()
//                   // .icon(BitmapDescriptorFactory.fromResource(R.drawable.imagemenu))
//                    .title(route.startAddress)
//                    .position(myLocation)));
//            LatLng LatLgData = new LatLng(Double.parseDouble(tourist_location.Latitude),Double.parseDouble(tourist_location.Longtitude));
//            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
//                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.imagemenu))
//                    .title(route.endAddress)
//                    .position(LatLgData)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.GRAY).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    @Override
    public void saveHistory() {

    }
}
