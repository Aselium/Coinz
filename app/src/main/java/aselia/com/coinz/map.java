package aselia.com.coinz;

import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class map extends Fragment implements OnMapReadyCallback, LocationEngineListener, PermissionsListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private MapView mapView;
    View v;

    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;

    private Map<String,Object> currentCollected = new HashMap<>();
    private String jsonString;
    private FeatureCollection featureCollection;
    private Map<String, String> coinData = new HashMap<>();

    private List<Feature> features;
    private String currentUser;

    private double dist;
    private int cointot;

    public map() {
        // Required empty public constructor
    }


    public static map newInstance(String param1, String param2) {
        map fragment = new map();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Apply key to mapbox
        Mapbox.getInstance(getContext(),getString(R.string.access_token));
        v = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = (MapView) v.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        return v;

    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);

        enableLocation();

        jsonString = loadFile("data.geojson");
        featureCollection = FeatureCollection.fromJson(jsonString);
        features = featureCollection.features();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser().getUid();

        DocumentReference docRef = db.collection("userData").document(currentUser);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if (document.exists() && document.getData().get("date").equals(getDate(jsonString))){
                    loadCoinData();
                    cointot = Integer.valueOf(document.getData().get("totalCoins").toString());
                    dist = (double) document.getData().get("totalDistance");
                } else {
                    coinData = generateCoinData(featureCollection);
                    Map<String, Object> currentDate = new HashMap<>();
                    currentDate.put("date",getDate(jsonString));
                    docRef.update(currentDate);
                    setMarkers(features);
                    saveCoinData();
                }
            } else {
                Log.d("userData load Failed", "get failed with", task.getException());
            }
        });
        loadCollectData();
        db = null;
        mAuth = null;
    }

    private void enableLocation(){
        if (PermissionsManager.areLocationPermissionsGranted(getContext())){
            initializeLocationEngine();
            initializeLocationLayer();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){
        locationEngine = new LocationEngineProvider(getContext()).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.setInterval(5000);
        locationEngine.setFastestInterval(1000);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null){
            originLocation = lastLocation;
            setCameraLocation(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
        Lifecycle lifecycle = getLifecycle();
        lifecycle.addObserver(locationLayerPlugin);
    }

    private void setCameraLocation(Location location){
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            if (originLocation != null){
                dist += originLocation.distanceTo(location);
            }
            originLocation = location;
            setCameraLocation(location);
            LatLng coordinates = new LatLng(location.getLatitude(),location.getLongitude());
            ArrayList<String> toRemove = new ArrayList<>();
            toRemove.clear();

            Set set = coinData.entrySet();
            Iterator it = set.iterator();
            while(it.hasNext()){
                HashMap.Entry<String,String> pair = (HashMap.Entry<String,String>) it.next();
                double distance = coordinates.distanceTo(stringToLatLng(pair.getValue()));
                if (distance < 25){
                    toRemove.add(pair.getKey());
                    Toast.makeText(getContext(), "Coin Collected", Toast.LENGTH_SHORT).show();
                    cointot += 1;
                }
            }

            for (String rmv : toRemove){
                for (Feature f : features){
                    if (f.getStringProperty("id") == rmv){
                        Object values = currentCollected.get(f.getStringProperty("currency"));
                        if (values == null){
                            values = f.getStringProperty("value");
                        } else {
                            values += "," + f.getStringProperty("value");
                        }

                        currentCollected.put(f.getStringProperty("currency"), values);
                    }
                }
                coinData.remove(rmv);
            }

            map.removeAnnotations();
            setMarkers(features);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getContext(),"Location Permissions is required for this App.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onStart(){
        super.onStart();
        mapView.onStart();
        if (locationEngine != null){
            try {
                locationEngine.requestLocationUpdates();
            } catch(SecurityException ignored) {}
            locationEngine.addLocationEngineListener(this);
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onResume(){
        super.onResume();
        if (locationEngine != null){
            try {
                locationEngine.requestLocationUpdates();
            } catch(SecurityException ignored) {}
            locationEngine.addLocationEngineListener(this);
        }
        if (locationLayerPlugin != null){
            if (PermissionsManager.areLocationPermissionsGranted(getContext())){
                locationLayerPlugin.onStart();
            } else {
                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(getActivity());
            }
        }
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(locationEngine != null){
            locationEngine.removeLocationEngineListener(this);
            locationEngine.removeLocationUpdates();
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        saveCoinData();
        saveUserData();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        saveCoinData();
        saveCollectData();
        saveUserData();
        if(locationEngine != null){
            locationEngine.removeLocationEngineListener(this);
            locationEngine.removeLocationUpdates();
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        saveCoinData();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
        saveCoinData();
        saveCollectData();
        saveUserData();
        if(locationEngine != null){
            locationEngine.removeLocationEngineListener(this);
            locationEngine.removeLocationUpdates();
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        locationEngine = null;
        locationLayerPlugin = null;
        mapView = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveCoinData();
        mapView.onSaveInstanceState(outState);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private String loadFile(String filename) {

        String output = "";

        try {
            InputStream inputStream = getContext().openFileInput(filename);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            output = new String(buffer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return output;
    }


    private String getDate(String jsonString){
        String date = "";
        int dateIndex = jsonString.indexOf("date-generated") + 18;
        boolean check = false;
        int counter = 0;
        while (!check){
            date += jsonString.charAt(dateIndex+counter);
            counter += 1;
            if (jsonString.charAt(dateIndex+counter) == '"'){
                check = true;
            }
        }
        return date;
    }


    private Map<String, String> generateCoinData(FeatureCollection featureCollection){
        Map<String, String> coinData = new HashMap<String,String>();

        List<Feature> features = featureCollection.features();
        for (Feature f : features){
            if (f.geometry() instanceof Point){
                Point p = (Point) f.geometry();
                coinData.put(f.getStringProperty("id"), latLngToString(new LatLng(p.latitude(),p.longitude())));
            }
        }
        return coinData;
    }

    private String latLngToString(LatLng latLng){
        return String.valueOf(latLng.getLatitude()) + "," + String.valueOf(latLng.getLongitude());
    }

    private LatLng stringToLatLng(String string){
        String[] latLngs = string.split(",");
        return new LatLng(Double.valueOf(latLngs[0]), Double.valueOf(latLngs[1]));
    }

    private void setMarkers(List<Feature> features){
        for (Feature f : features){
            if(f.geometry() instanceof Point && coinData.containsKey(f.getStringProperty("id"))){
                Point p = (Point) f.geometry();

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(new LatLng(p.latitude(),p.longitude()))
                        .title(f.getStringProperty("value"))
                        .snippet(f.getStringProperty("currency"));
                map.addMarker(markerOptions);
            }
        }
    }

    private void loadCoinData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("coinData").document(currentUser);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        coinData = new HashMap<>();
                        for (Feature f : features){
                            if (document.get(f.getStringProperty("id")) != null){
                                Point p = (Point) f.geometry();
                                String coor = latLngToString(new LatLng(p.latitude(),p.longitude()));
                                coinData.put(f.getStringProperty("id"), coor);
                            }
                        }

                        setMarkers(features);
                    } else {
                        Log.d("Missing Document", "No such document");
                    }
                } else {
                    Log.d("LoadFailed", "get failed with " , task.getException());
                }
            }
        });
        db = null;
    }

    private void saveCoinData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("coinData").document(currentUser);
        docRef.set(coinData);
        db = null;
    }

    private void loadCollectData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("collectData").document(currentUser);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        currentCollected = new HashMap<>();
                        currentCollected = document.getData();
                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                }
            }
        });
        db = null;
    }

    private void saveCollectData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("collectData").document(currentUser);
        docRef.set(currentCollected);
        db = null;
    }

    private void saveUserData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser().getUid();

        DocumentReference docRef = db.collection("userData").document(currentUser);
        Map<String, Object> newData = new HashMap<>();
        newData.put("totalCoins", String.valueOf(cointot));
        newData.put("totalDistance", dist);

        docRef.update(newData);
    }

    public interface OnFragmentInteractionListener {
        //Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
