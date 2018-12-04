package aselia.com.coinz;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
public class bank extends Fragment{

    private double money;
    private int todaysCoins;
    private double dolrRate;
    private double penyRate;
    private double quidRate;
    private double shilRate;
    private String jsonString;
    private String currentUser;
    private Map<String,Object> collectedCoins;

    @Nullable
    List<String> dolrCollected= new ArrayList<>();
    List<String> penyCollected= new ArrayList<>();
    List<String> shilCollected= new ArrayList<>();
    List<String> quidCollected= new ArrayList<>();

    ArrayAdapter<String> arrayAdapterDOLR;
    ArrayAdapter<String> arrayAdapterPENY;
    ArrayAdapter<String> arrayAdapterSHIL;
    ArrayAdapter<String> arrayAdapterQUID;

    Button switchbtn;

    private OnFragmentInteractionListener mListener;

    public bank() {
        // Required empty public constructor
    }

    //Default Method
    public static bank newInstance(String param1, String param2) {
        bank fragment = new bank();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        jsonString = loadFile();
        setRates(jsonString);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser().getUid();

        DocumentReference docRef = db.collection("userData").document(currentUser);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                String moneyText = "Your Money: " + Double.toString(money).substring(0, Math.min(Double.toString(money).length(), 8));
                String tradedText = "Coins Traded Today: " + Integer.toString(todaysCoins) + "/25";
                if (document.exists()){
                    money = (double) document.getData().get("Money");
                    if (document.getData().get("date2").equals(getDate(jsonString))){
                        Map<String, Object> user = document.getData();
                        todaysCoins = Integer.valueOf(user.get("Traded").toString());
                        TextView textMoney = getView().findViewById(R.id.textMoney);
                        TextView textTraded = getView().findViewById(R.id.textTraded);
                        textMoney.setText(moneyText);
                        textTraded.setText(tradedText);
                    } else {
                        todaysCoins = 0;
                        TextView textMoney = getView().findViewById(R.id.textMoney);
                        TextView textTraded = getView().findViewById(R.id.textTraded);
                        textMoney.setText(moneyText);
                        textTraded.setText(tradedText);
                    }
                }
            } else {
                Log.d("LoadUserData", "get failed with ", task.getException());
            }
        });
    }

    @Override
    @SuppressWarnings("ConstantConditions")
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
        textDOLR.setText("DOLR: " + Double.toString(dolrRate).substring(0, Math.min(Double.toString(dolrRate).length(), 8)));
        textPENY.setText("PENY: " + Double.toString(penyRate).substring(0, Math.min(Double.toString(penyRate).length(), 8)));
        textSHIL.setText("SHIL: " + Double.toString(shilRate).substring(0, Math.min(Double.toString(shilRate).length(), 8)));
        textQUID.setText("QUID: " + Double.toString(quidRate).substring(0, Math.min(Double.toString(quidRate).length(), 8)));

        loadCollectData();

        collectedListDOLR.setOnItemClickListener((parent, view1, position, id) -> {
            List<List<String>> temp = getCollected();
            dolrCollected = temp.get(0);
            penyCollected = temp.get(1);
            shilCollected = temp.get(2);
            quidCollected = temp.get(3);
            String selected = parent.getItemAtPosition(position).toString().trim();
            if (selected.equals("") || selected.equals("None") || !(isNumeric(selected))){
                Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
            } else if (todaysCoins < 25){
                money += Double.valueOf(selected) * dolrRate;
                dolrCollected.remove(position);
                if (dolrCollected.isEmpty()){
                    dolrCollected = Arrays.asList("None");
                }
                ArrayAdapter<String> arrayAdapterDOLR = new ArrayAdapter<>(getContext(), R.layout.simplerow, dolrCollected);
                collectedListDOLR.setAdapter(arrayAdapterDOLR);
                arrayAdapterDOLR.notifyDataSetChanged();
                textMoney.setText(String.valueOf(money));
                todaysCoins += 1;
                textTraded.setText("Coins Traded Today: " + Integer.toString(todaysCoins) + "/25");
                Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                updateCollectedCoins();
            } else {
                Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
            }
        });

        collectedListPENY.setOnItemClickListener((parent, view12, position, id) -> {
            List<List<String>> temp = getCollected();
            dolrCollected = temp.get(0);
            penyCollected = temp.get(1);
            shilCollected = temp.get(2);
            quidCollected = temp.get(3);
            String selected = parent.getItemAtPosition(position).toString().trim();
            if (selected.equals("") || selected.equals("None") || !(isNumeric(selected))){
                Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
            } else if (todaysCoins < 25){
                money += Double.valueOf(selected) * penyRate;
                penyCollected.remove(position);
                if (penyCollected.isEmpty()){
                    penyCollected = Arrays.asList("None");
                }
                ArrayAdapter<String> arrayAdapterPENY = new ArrayAdapter<>(getContext(), R.layout.simplerow, penyCollected);
                collectedListPENY.setAdapter(arrayAdapterPENY);
                arrayAdapterPENY.notifyDataSetChanged();
                textMoney.setText(String.valueOf(money));
                todaysCoins += 1;
                textTraded.setText("Coins Traded Today: " + Integer.toString(todaysCoins) + "/25");
                Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                updateCollectedCoins();
            } else {
                Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
            }
        });

        collectedListSHIL.setOnItemClickListener((parent, view13, position, id) -> {

            List<List<String>> temp = getCollected();
            dolrCollected = temp.get(0);
            penyCollected = temp.get(1);
            shilCollected = temp.get(2);
            quidCollected = temp.get(3);

            String selected = parent.getItemAtPosition(position).toString().trim();
            if (selected.equals("") || selected.equals("None") || !(isNumeric(selected))){
                Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
            } else if (todaysCoins < 25){
                money += Double.valueOf(selected) * shilRate;
                shilCollected.remove(position);
                if (shilCollected.isEmpty()){
                    shilCollected = Arrays.asList("None");
                }
                ArrayAdapter<String> arrayAdapterSHIL = new ArrayAdapter<>(getContext(), R.layout.simplerow, shilCollected);
                collectedListSHIL.setAdapter(arrayAdapterSHIL);
                arrayAdapterSHIL.notifyDataSetChanged();
                textMoney.setText(String.valueOf(money));
                todaysCoins += 1;
                textTraded.setText("Coins Traded Today: " + Integer.toString(todaysCoins) + "/25");
                Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                updateCollectedCoins();
            } else {
                Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
            }
        });

        collectedListQUID.setOnItemClickListener((parent, view14, position, id) -> {
            List<List<String>> temp = getCollected();
            dolrCollected = temp.get(0);
            penyCollected = temp.get(1);
            shilCollected = temp.get(2);
            quidCollected = temp.get(3);
            String selected = parent.getItemAtPosition(position).toString().trim();
            if (selected.equals("") || selected.equals("None") || !(isNumeric(selected))){
                Toast.makeText(getContext(), "No coin of this currency exists", Toast.LENGTH_SHORT).show();
            } else if (todaysCoins < 25){
                money += Double.valueOf(selected) * quidRate;
                quidCollected.remove(position);
                if (quidCollected.isEmpty()){
                    quidCollected = Arrays.asList("None");
                }
                ArrayAdapter<String> arrayAdapterQUID = new ArrayAdapter<String>(getContext(),R.layout.simplerow,quidCollected);
                collectedListQUID.setAdapter(arrayAdapterQUID);
                arrayAdapterQUID.notifyDataSetChanged();
                textMoney.setText(String.valueOf(money));
                todaysCoins += 1;
                textTraded.setText("Coins Traded Today: " + Integer.toString(todaysCoins) + "/25");
                Toast.makeText(getContext(), "Coin has been cashed out", Toast.LENGTH_SHORT).show();
                updateCollectedCoins();
            } else {
                Toast.makeText(getContext(), "Already traded 25 today", Toast.LENGTH_SHORT).show();
            }
        });

        textMoney.setText("Your Money: " + Double.toString(money).substring(0, Math.min(Double.toString(money).length(), 8)));

        Button switchbtn = view.findViewById(R.id.switchtype);
        switchbtn.setVisibility(View.INVISIBLE);

        return view;
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

    @Override
    public void onStop() {
        super.onStop();
        saveData();
    }

    private String loadFile() {

        String output = "";

        try {
            InputStream inputStream = getContext().openFileInput("data.geojson");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            output = new String(buffer);
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
            while (!check){
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
            } else {
                shilRate = Double.valueOf(rateS);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void populateList(){
        ListView collectedListDOLR = getView().findViewById(R.id.coinboxDOLR);
        ListView collectedListPENY = getView().findViewById(R.id.coinboxPENY);
        ListView collectedListSHIL = getView().findViewById(R.id.coinboxSHIL);
        ListView collectedListQUID = getView().findViewById(R.id.coinboxQUID);

        List<String> dolrCollected;
        List<String> penyCollected;
        List<String> shilCollected;
        List<String> quidCollected;

        String[] currencies = new String[]{"DOLR","PENY","SHIL","QUID"};

        for (int i = 0; i < 4; i++){
            String x;
            String[] split;
            if (collectedCoins.get(currencies[i]) == null){
                split = new String[]{"None"};
            } else {
                x = collectedCoins.get(currencies[i]).toString();
                split = x.split(",");
            }

            if (i == 0){
                dolrCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterDOLR = new ArrayAdapter<>(getContext(), R.layout.simplerow, dolrCollected);
                collectedListDOLR.setAdapter(arrayAdapterDOLR);
            } else if (i == 1){
                penyCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterPENY = new ArrayAdapter<>(getContext(), R.layout.simplerow, penyCollected);
                collectedListPENY.setAdapter(arrayAdapterPENY);
            } else if (i == 2){
                shilCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterSHIL = new ArrayAdapter<>(getContext(), R.layout.simplerow, shilCollected);
                collectedListSHIL.setAdapter(arrayAdapterSHIL);
            } else if (i == 3){
                quidCollected = new ArrayList<>(Arrays.asList(split));
                ArrayAdapter<String> arrayAdapterQUID = new ArrayAdapter<>(getContext(), R.layout.simplerow, quidCollected);
                collectedListQUID.setAdapter(arrayAdapterQUID);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private List<List<String>> getCollected(){
        List<String> dolrCollectedt = new ArrayList<>();
        List<String> penyCollectedt = new ArrayList<>();
        List<String> shilCollectedt = new ArrayList<>();
        List<String> quidCollectedt = new ArrayList<>();

        String[] currencies = new String[]{"DOLR","PENY","SHIL","QUID"};

        for (int i = 0; i < 4; i++){
            String x;
            String[] split;
            if (collectedCoins.get(currencies[i]) == null){
                split = new String[]{"None"};
            } else {
                x = collectedCoins.get(currencies[i]).toString();
                split = x.split(",");
            }

            if (i == 0){
                dolrCollectedt = new ArrayList<>(Arrays.asList(split));
            } else if (i == 1){
                penyCollectedt = new ArrayList<>(Arrays.asList(split));
            } else if (i == 2){
                shilCollectedt = new ArrayList<>(Arrays.asList(split));
            } else if (i == 3){
                quidCollectedt = new ArrayList<>(Arrays.asList(split));
            }
        }

        List<List<String>> result = new ArrayList<>();
        result.add(dolrCollectedt);
        result.add(penyCollectedt);
        result.add(shilCollectedt);
        result.add(quidCollectedt);

        return result;
    }

    @SuppressWarnings("ConstantConditions")
    private void loadCollectData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("collectData").document(currentUser);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if (document.exists()){
                    collectedCoins = new HashMap<>();
                    collectedCoins = document.getData();
                    TextView textMoney = getView().findViewById(R.id.textMoney);
                    TextView textTraded = getView().findViewById(R.id.textTraded);
                    textMoney.setText("Your Money: " + Double.toString(money).substring(0, Math.min(Double.toString(money).length(), 8)));
                    textTraded.setText("Coins Traded Today: " + Integer.toString(todaysCoins) + "/25");
                }
            }
            populateList();
            List<List<String>> temp = getCollected();
            dolrCollected = temp.get(0);
            penyCollected = temp.get(1);
            shilCollected = temp.get(2);
            quidCollected = temp.get(3);
            Log.i("popinfo" , "info: " + temp);
        });
        db = null;
    }

    private Map<String, Object> packUserdata(){
        Map<String, Object> result = new HashMap<>();
        result.put("Money", money);
        result.put("Traded", todaysCoins);
        result.put("date2", getDate(jsonString));

        return result;
    }

    @SuppressWarnings("ConstantConditions")
    private Map<String, Object> packCollecteddata(){
        Map<String, Object> result = new HashMap<>();
        Log.i("data", "info: " + dolrCollected);
        Log.i("data", "info: " + quidCollected);
        Log.i("data", "info: " + shilCollected);
        Log.i("data", "info: " + penyCollected);

        if (dolrCollected.contains("") || dolrCollected.isEmpty() || dolrCollected.contains("None")){
            result.put("DOLR",null);
        } else {
            String temp = "";
            for (String str : dolrCollected){
                temp += str + ",";
            }
            temp = temp.substring(0,temp.length() -1);
            result.put("DOLR",temp);
        }

        if (penyCollected.contains("") || penyCollected.isEmpty() || dolrCollected.contains("None")){
            result.put("PENY",null);
        } else {
            String temp = "";
            for (String str : penyCollected){
                temp += str + ",";
            }
            temp = temp.substring(0,temp.length() -1);
            result.put("PENY",temp);
        }

        if (shilCollected.contains("") || shilCollected.isEmpty() || dolrCollected.contains("None")){
            result.put("SHIL",null);
        } else {
            String temp = "";
            for (String str : shilCollected){
                temp += str + ",";
            }
            temp = temp.substring(0,temp.length() -1);
            result.put("SHIL",temp);
        }

        if (quidCollected.contains("") || quidCollected.isEmpty() || dolrCollected.contains("None")){
            result.put("QUID",null);
        } else {
            String temp = "";
            for (String str : quidCollected){
                temp += str + ",";
            }
            temp = temp.substring(0,temp.length() -1);
            result.put("QUID",temp);
        }
        return result;
    }

    private void saveData(){
        Map<String,Object> userData = packUserdata();
        Map<String,Object> collectData = packCollecteddata();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("collectData").document(currentUser);
        docRef.update(collectData);
        docRef = db.collection("userData").document(currentUser);
        docRef.update(userData);
        db = null;
    }

    private void updateCollectedCoins(){
        collectedCoins = packCollecteddata();
    }

    public static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
