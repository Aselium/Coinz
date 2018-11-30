package aselia.com.coinz;

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
import android.widget.Toast;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public class map extends Fragment implements OnMapReadyCallback, LocationEngineListener, PermissionsListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
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

    private List<String> currentCollected;
    private String jsonString;
    private HashMap<String, LatLng> mapping;
    private FeatureCollection featureCollection;

    private List<Feature> features;

    public map() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment map.
     */
    // TODO: Rename and change types and number of parameters
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Apply key to mapbox
        Mapbox.getInstance(getContext(),getString(R.string.access_token));
        v = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = (MapView) v.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return v;

    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);

        enableLocation();

        jsonString = loadFile(getContext(),"data.geojson");
        featureCollection = FeatureCollection.fromJson(jsonString);
        features = featureCollection.features();

        checkCollected(getContext(), features, jsonString);
        mapping = generateHmap(featureCollection);
        setMarkers(features);
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
            originLocation = location;
            setCameraLocation(location);
            LatLng coordinates = new LatLng(location.getLatitude(),location.getLongitude());

            Set set = mapping.entrySet();
            Iterator it = set.iterator();
            while(it.hasNext()){
                HashMap.Entry<String,LatLng> pair = (HashMap.Entry<String,LatLng>) it.next();
                double distance = coordinates.distanceTo(pair.getValue());
                if (distance < 25){
                    currentCollected.remove(pair.getKey());
                    Toast.makeText(getContext(), "Coin Collected", Toast.LENGTH_SHORT).show();
                }
            }
            saveFile(getContext(),"Collected",rebuildCollected(currentCollected,jsonString));
            checkCollected(getContext(), features,jsonString);
            mapping = generateHmap(featureCollection);
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
        if (locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveFile(getContext(),"Collected",rebuildCollected(currentCollected,jsonString));
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        saveFile(getContext(),"Collected",rebuildCollected(currentCollected,jsonString));
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        saveFile(getContext(),"Collected",rebuildCollected(currentCollected,jsonString));
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationEngine != null){
            locationEngine.deactivate();
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        saveFile(getContext(),"Collected",rebuildCollected(currentCollected,jsonString));
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveFile(getContext(),"Collected",rebuildCollected(currentCollected,jsonString));
        mapView.onSaveInstanceState(outState);
    }

    /*
    // ??
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
    */

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

    private String loadFile(Context context, String filename) {

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

    private void saveFile(Context context, String filename, String data){
        try{
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String rebuildCollected(List<String> current, String json){
        String result = "";
        result += getDate(json);
        result += "\n";
        for (String element : current){
            result += element;
            result += "\n";
        }
        return result;
    }

    private void checkCollected(Context context, List<Feature> features, String jsonString){
        File file = context.getFileStreamPath("Collected");
        Log.i("checkexists","checkexists");
        if(file != null || file.exists()){
            currentCollected = new LinkedList<String>(Arrays.asList(loadFile(getContext(),"Collected").split("\n")));
            if (!currentCollected.contains(getDate(jsonString))){
                String newCollected = "";
                newCollected += getDate(jsonString);
                newCollected += "\n";
                for (Feature f : features){
                    if (f.geometry() instanceof Point){
                        newCollected += f.getStringProperty("id");
                        newCollected += "\n";
                    }
                }
                saveFile(getContext(),"Collected",newCollected);
                currentCollected = new LinkedList<String>(Arrays.asList(newCollected.split("\n")));
            }
        } else {
            String newCollected = "";
            newCollected += getDate(jsonString);
            newCollected += "\n";
            for (Feature f : features){
                if (f.geometry() instanceof Point){
                    newCollected += f.getStringProperty("id");
                    newCollected += "\n";
                }
            }
            saveFile(getContext(),"Collected",newCollected);
            currentCollected = new LinkedList<String>(Arrays.asList(newCollected.split("\n")));
        }
    }

    private String getDate(String jsonString){
        String date = "";
        int dateIndex = jsonString.indexOf("date-generated") + 18;
        boolean check = false;
        int counter = 0;
        while (check == false){
            date += jsonString.charAt(dateIndex+counter);
            counter += 1;
            if (jsonString.charAt(dateIndex+counter) == '"'){
                check = true;
            }
        }
        return date;
    }

    private HashMap<String, LatLng> generateHmap(FeatureCollection featureCollection){
        HashMap<String, LatLng> hmap = new HashMap<String, LatLng>();

        List<Feature> features = featureCollection.features();
        for (Feature f : features){
            if (f.geometry() instanceof Point && currentCollected.contains(f.getStringProperty("id"))){
                Point p = (Point) f.geometry();
                hmap.put(f.getStringProperty("id"),new LatLng(p.latitude(),p.longitude()));
            }
        }

        return hmap;
    }

    private void setMarkers(List<Feature> features){
        for (Feature f : features){
            if(f.geometry() instanceof Point &&
                    currentCollected.contains(f.getStringProperty("id"))){
                Point p = (Point) f.geometry();

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(new LatLng(p.latitude(),p.longitude()))
                        .title(f.getStringProperty("value"))
                        .snippet(f.getStringProperty("currency"));
                map.addMarker(markerOptions);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        //Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
