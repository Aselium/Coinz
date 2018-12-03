package aselia.com.coinz;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link bank.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link bank#newInstance} factory method to
 * create an instance of this fragment.
 */
public class bank extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private double money;
    private int todaysCoins;
    private double dolrRate;
    private double penyRate;
    private double quidRate;
    private double shilRate;
    private String jsonString;
    private String currentUser;
    private Map<String,Object> collectedCoins;

    List<String> dolrCollected= new ArrayList<>();
    List<String> penyCollected= new ArrayList<>();
    List<String> shilCollected= new ArrayList<>();
    List<String> quidCollected= new ArrayList<>();

    ArrayAdapter<String> arrayAdapterDOLR;
    ArrayAdapter<String> arrayAdapterPENY;
    ArrayAdapter<String> arrayAdapterSHIL;
    ArrayAdapter<String> arrayAdapterQUID;

    private OnFragmentInteractionListener mListener;

    public bank() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment bank.
     */
    // TODO: Rename and change types and number of parameters
    public static bank newInstance(String param1, String param2) {
        bank fragment = new bank();
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

        jsonString = loadFile(getContext(),"data.geojson");
        setRates(jsonString);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser().getUid();

        DocumentReference docRef = db.collection("userData").document(currentUser);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        money = (double) document.getData().get("Money");
                        if (document.getData().containsValue(getDate(jsonString))){
                            Map<String, Object> user = document.getData();
                            todaysCoins = Integer.valueOf(user.get("Traded").toString());
                        } else {
                            todaysCoins = 0;
                        }
                    }
                } else {
                    Log.d("LoadUserData", "get failed with ", task.getException());
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bank, container, false);
        ListView collectedListDOLR = view.findViewById(R.id.coinboxDOLR);
        ListView collectedListPENY = view.findViewById(R.id.coinboxPENY);
        ListView collectedListSHIL = view.findViewById(R.id.coinboxSHIL);
        ListView collectedListQUID = view.findViewById(R.id.coinboxQUID);

        TextView textDate = view.findViewById(R.id.textDate);
        TextView textMoney = view.findViewById(R.id.textMoney);
        TextView textTraded = view.findViewById(R.id.textTraded);
        TextView textDOLR = view.findViewById(R.id.textDOLRrate);
        TextView textPENY = view.findViewById(R.id.textPENYrate);
        TextView textSHIL = view.findViewById(R.id.textSHILrate);
        TextView textQUID = view.findViewById(R.id.textQUIDrate);

        textDate.setText(getDate(jsonString));
        textMoney.setText("Your Money: " + Double.toString(money).substring(0, Math.min(Double.toString(money).length(), 8)));
        textTraded.setText("Coins Traded Today: " + Integer.toString(todaysCoins) + "/25");
        textDOLR.setText("DOLR: " + Double.toString(dolrRate).substring(0, Math.min(Double.toString(dolrRate).length(), 8)));
        textPENY.setText("PENY: " + Double.toString(penyRate).substring(0, Math.min(Double.toString(penyRate).length(), 8)));
        textSHIL.setText("SHIL: " + Double.toString(shilRate).substring(0, Math.min(Double.toString(shilRate).length(), 8)));
        textQUID.setText("QUID: " + Double.toString(quidRate).substring(0, Math.min(Double.toString(quidRate).length(), 8)));

        loadCollectData();

        collectedListDOLR.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString().trim();
                if (selected == ""){
                    Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
                } else if (todaysCoins < 25){
                    ArrayAdapter<String> arrayAdapterDOLR = new ArrayAdapter<String>(getContext(),R.layout.simplerow,dolrCollected);
                    money += Double.valueOf(selected) * dolrRate;
                    dolrCollected.remove(selected);
                    if (dolrCollected.isEmpty()){
                        dolrCollected = Arrays.asList(new String[]{"None"});
                    }
                    collectedListDOLR.setAdapter(arrayAdapterDOLR);
                    arrayAdapterDOLR.notifyDataSetChanged();
                    textMoney.setText(String.valueOf(money));
                    todaysCoins += 1;
                    textTraded.setText(String.valueOf(todaysCoins));
                    Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
                }
            }
        });

        collectedListPENY.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString().trim();
                if (selected == ""){
                    Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
                } else if (todaysCoins < 25){
                    ArrayAdapter<String> arrayAdapterPENY = new ArrayAdapter<String>(getContext(),R.layout.simplerow,penyCollected);
                    money += Double.valueOf(selected) * penyRate;
                    penyCollected.remove(selected);
                    if ( penyCollected.isEmpty()){
                        penyCollected = Arrays.asList(new String[]{"None"});
                    }
                    collectedListPENY.setAdapter(arrayAdapterPENY);
                    arrayAdapterPENY.notifyDataSetChanged();
                    textMoney.setText(String.valueOf(money));
                    todaysCoins += 1;
                    textTraded.setText(String.valueOf(todaysCoins));
                    Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
                }
            }
        });

        collectedListSHIL.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString().trim();
                if (selected == ""){
                    Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
                } else if (todaysCoins < 25){
                    ArrayAdapter<String> arrayAdapterSHIL = new ArrayAdapter<String>(getContext(),R.layout.simplerow,shilCollected);
                    money += Double.valueOf(selected) * shilRate;
                    shilCollected.remove(selected);
                    if (shilCollected.isEmpty()){
                        shilCollected = Arrays.asList(new String[]{"None"});
                    }
                    collectedListSHIL.setAdapter(arrayAdapterSHIL);
                    arrayAdapterSHIL.notifyDataSetChanged();
                    textMoney.setText(String.valueOf(money));
                    todaysCoins += 1;
                    textTraded.setText(String.valueOf(todaysCoins));
                    Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
                }
            }
        });

        collectedListQUID.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString().trim();
                if (selected == ""){
                    Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
                } else if (todaysCoins < 25){
                    ArrayAdapter<String> arrayAdapterQUID = new ArrayAdapter<String>(getContext(),R.layout.simplerow,quidCollected);
                    money += Double.valueOf(selected) * quidRate;
                    quidCollected.remove(selected);
                    if (quidCollected.isEmpty()){
                        quidCollected = Arrays.asList(new String[]{"None"});
                    }
                    collectedListQUID.setAdapter(arrayAdapterQUID);
                    arrayAdapterQUID.notifyDataSetChanged();
                    textMoney.setText(String.valueOf(money));
                    todaysCoins += 1;
                    textTraded.setText(String.valueOf(todaysCoins));
                    Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    /*
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }*/

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

    private void setRates(String jsonString){
        int dolrIndex = jsonString.indexOf("DOLR") + 7;
        int penyIndex = jsonString.indexOf("PENY") + 7;
        int quidIndex = jsonString.indexOf("QUID") + 7;
        int shilIndex = jsonString.indexOf("SHIL") + 7;
        int[] Indexs = {dolrIndex,penyIndex,quidIndex,shilIndex};

        for (int i = 0; i < 4; i++){
            boolean check = false;
            int counter = 0;
            String rateS = "";
            while (check == false){
                rateS += jsonString.charAt(Indexs[i] + counter);
                counter += 1;
                if (jsonString.charAt(Indexs[i] + counter) == '"'){
                    check = true;
                }
            }
            rateS = rateS.replaceAll("[^\\d.]", "");
            if (i == 0){
                dolrRate = Double.valueOf(rateS);
            } else if (i == 1){
                penyRate = Double.valueOf(rateS);
            } else if (i == 2){
                quidRate = Double.valueOf(rateS);
            } else if (i == 3){
                shilRate = Double.valueOf(rateS);
            }
        }
    }

    private void populateList(){
        ListView collectedListDOLR = getView().findViewById(R.id.coinboxDOLR);
        ListView collectedListPENY = getView().findViewById(R.id.coinboxPENY);
        ListView collectedListSHIL = getView().findViewById(R.id.coinboxSHIL);
        ListView collectedListQUID = getView().findViewById(R.id.coinboxQUID);

        List<String> dolrCollected= new ArrayList<>();
        List<String> penyCollected= new ArrayList<>();
        List<String> shilCollected= new ArrayList<>();
        List<String> quidCollected= new ArrayList<>();

        String[] currencies = new String[]{"DOLR","PENY","SHIL","QUID"};

        for (int i = 0; i < 4; i++){
            String x;
            String[] split;
            if (collectedCoins.get(currencies[i]) == null){
                split = new String[]{""};
            } else {
                x = collectedCoins.get(currencies[i]).toString();
                split = x.split(",");
            }

            if (i == 0){
                dolrCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterDOLR = new ArrayAdapter<String>(getContext(),R.layout.simplerow,dolrCollected);
                collectedListDOLR.setAdapter(arrayAdapterDOLR);
            } else if (i == 1){
                penyCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterPENY = new ArrayAdapter<String>(getContext(),R.layout.simplerow, penyCollected);
                collectedListPENY.setAdapter(arrayAdapterPENY);
            } else if (i == 2){
                shilCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterSHIL = new ArrayAdapter<String>(getContext(),R.layout.simplerow, shilCollected);
                collectedListSHIL.setAdapter(arrayAdapterSHIL);
            } else if (i == 3){
                quidCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterQUID = new ArrayAdapter<String>(getContext(),R.layout.simplerow, quidCollected);
                collectedListQUID.setAdapter(arrayAdapterQUID);
            }
        }


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
                        collectedCoins = new HashMap<>();
                        collectedCoins = document.getData();
                    }
                }
                populateList();
            }
        });
        db = null;
    }

    private Map<String, Object> packUserdata(){
        Map<String, Object> result = new HashMap<>();
        result.put("Money", Double.valueOf(money));
        result.put("Traded", todaysCoins);

        return result;
    }

    private Map<String, Object> packCollecteddata(){
        Map<String, Object> result = new HashMap<>();
        String[] currencies = new String[]{"DOLR","PENY","SHIL","QUID"};

        return result;
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
