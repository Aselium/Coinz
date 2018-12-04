package aselia.com.coinz;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;


public class statistics extends Fragment {

    private OnFragmentInteractionListener mListener;

    public statistics() {
        // Required empty public constructor
    }

    //Default Method
    public static statistics newInstance(String param1, String param2) {
        statistics fragment = new statistics();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);
        String jsonString = loadFile();
        loadUserData();
        TextView dateText = view.findViewById(R.id.textDate);
        dateText.setText(getDate(jsonString));

        ImageView image = view.findViewById(R.id.pics);
        image.setImageAlpha(50);

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

    @SuppressWarnings("ConstantConditions")
    private void loadUserData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String currentUser = mAuth.getCurrentUser().getUid();

        DocumentReference docRef = db.collection("userData").document(currentUser);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if (document.exists()){
                    String cointot = (String) document.getData().get("totalCoins");
                    double dist = (double) document.getData().get("totalDistance");
                    double money = (double) document.getData().get("Money");

                    TextView textMoney = getView().findViewById(R.id.textMoney);
                    TextView textDist = getView().findViewById(R.id.textDistance);
                    TextView textCoin = getView().findViewById(R.id.textCoins);

                    String moneyText = "Money: " + String.format("%.2f", money);
                    String distText = "Total Distance: " + String.format("%.1f", dist) + " meters";
                    String coinText = "Total Coins Collected: " + String.valueOf(cointot) + " coins";

                    textMoney.setText(moneyText);
                    textDist.setText(distText);
                    textCoin.setText(coinText);
                }
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
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

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }
}
