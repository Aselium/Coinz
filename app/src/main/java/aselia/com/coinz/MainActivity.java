package aselia.com.coinz;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, map.OnFragmentInteractionListener, bank.OnFragmentInteractionListener, coin.OnFragmentInteractionListener, bank2.OnFragmentInteractionListener, statistics.OnFragmentInteractionListener {

    Fragment fragment = null;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Date currentTime = Calendar.getInstance().getTime();

        String day = (String) DateFormat.format("dd", currentTime);
        String monthNumber  = (String) DateFormat.format("MM",   currentTime);
        String year         = (String) DateFormat.format("yyyy", currentTime);
        String pt1 = "http://homepages.inf.ed.ac.uk/stg/coinz/";
        String pt2 = "/coinzmap.geojson";
        String url = pt1 + year + "/" + monthNumber + "/" + day + pt2;

        DownloadFileTask dl = new DownloadFileTask();
        dl.execute(url);

        FirebaseApp.initializeApp(this);
        FirebaseAuth mAuth;
        mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        mAuth = FirebaseAuth.getInstance();

        Button loginButton = findViewById(R.id.buttonLogin);
        Button signUpButton = findViewById(R.id.buttonSignup);
        View colourBox = findViewById(R.id.colored_bar);
        TextView loginText = findViewById(R.id.textLogin);
        EditText emailedit = findViewById(R.id.editTextUser);
        EditText passwordedit = findViewById(R.id.editTextPass);
        //Fragment fragment = null;

        Log.i("CUSER", "user: " + mAuth.getCurrentUser());
        if (mAuth.getCurrentUser() != null){
            if (id == R.id.Map) {
                fragment = new map();
            } else if (id == R.id.Bank) {
                fragment = new bank();
            } else if (id == R.id.Coin_Transfer) {
                fragment = new coin();
            } else if (id == R.id.Bank2) {
                fragment = new bank2();
            } else if (id == R.id.Statistics){
                fragment = new statistics();
            }

            if (fragment != null) {
                hideUI(loginButton,signUpButton,colourBox,loginText,emailedit,passwordedit);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.screen_area, fragment);
                ft.commit();
            }
        } else {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
        }



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    public void hideUI(Button loginButton, Button signUpButton, View colourBox, TextView loginText, EditText emailedit, EditText passwordedit){
        loginButton.setVisibility(View.GONE);
        signUpButton.setVisibility(View.GONE);
        colourBox.setVisibility(View.GONE);
        loginText.setVisibility(View.GONE);
        emailedit.setVisibility(View.GONE);
        passwordedit.setVisibility(View.GONE);
    }

    public void showUI(Button loginButton, Button signUpButton, View colourBox, TextView loginText, EditText emailedit, EditText passwordedit){
        loginButton.setVisibility(View.VISIBLE);
        signUpButton.setVisibility(View.VISIBLE);
        colourBox.setVisibility(View.VISIBLE);
        loginText.setVisibility(View.VISIBLE);
        emailedit.setVisibility(View.VISIBLE);
        passwordedit.setVisibility(View.VISIBLE);
    }

    public void tryLogin(View view) {
        Button loginButton = findViewById(R.id.buttonLogin);
        Button signUpButton = findViewById(R.id.buttonSignup);
        View colourBox = findViewById(R.id.colored_bar);
        TextView loginText = findViewById(R.id.textLogin);
        EditText emailedit = findViewById(R.id.editTextUser);
        EditText passwordedit = findViewById(R.id.editTextPass);

        mAuth = FirebaseAuth.getInstance();

        String email = emailedit.getText().toString();
        String password = passwordedit.getText().toString();

        if (password.length() > 5 && password != null && email != null){
            mAuth.signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()){
                            Log.d("UserLoggedIn", "LogUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            fragment = new map();
                            hideUI(loginButton,signUpButton,colourBox,loginText,emailedit,passwordedit);
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            FragmentTransaction ft = fragmentManager.beginTransaction();
                            ft.replace(R.id.screen_area, fragment);
                            ft.commit();
                            View view1 = getCurrentFocus();
                            if (view1 != null) {
                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view1.getWindowToken(), 0);
                            }
                        } else {
                            Log.w("SignInFailed", "LogUserWithEmail:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                Toast.makeText(getBaseContext(),"Check your email is entered correctly", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getBaseContext(),"Authentication Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Log.i("entered11","enter");
            Toast.makeText(getBaseContext(), "Check an email is entered and the password at least 6 characters long", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void signUp(View view) {
        Button loginButton = findViewById(R.id.buttonLogin);
        Button signUpButton = findViewById(R.id.buttonSignup);
        View colourBox = findViewById(R.id.colored_bar);
        TextView loginText = findViewById(R.id.textLogin);
        EditText emailedit = findViewById(R.id.editTextUser);
        EditText passwordedit = findViewById(R.id.editTextPass);

        mAuth = FirebaseAuth.getInstance();

        String email = emailedit.getText().toString();
        String password = passwordedit.getText().toString();

        if (password.length() > 5 && password != null && email != null){
            mAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()){
                            Log.d("UserCreated", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            //New user initialization
                            DocumentReference docRef = db.collection("userData").document(mAuth.getCurrentUser().getUid());
                            Map<String,Object> userInfo = new HashMap<>();
                            userInfo.put("date","Thu Jan 01 1970");
                            userInfo.put("Money",0.0);
                            userInfo.put("Traded", 0);
                            userInfo.put("date2","Thu Jan 01 1970");
                            userInfo.put("totalCoins", "0");
                            userInfo.put("totalDistance", 0.0);
                            docRef.set(userInfo);

                            docRef = db.collection("collectData").document(mAuth.getCurrentUser().getUid());
                            Map<String, String> collectInfo = new HashMap<>();
                            collectInfo.put("PENY",null);
                            collectInfo.put("DOLR",null);
                            collectInfo.put("SHIL",null);
                            collectInfo.put("QUID",null);
                            docRef.set(collectInfo);

                            docRef = db.collection("receiveData").document(mAuth.getCurrentUser().getEmail());
                            Map<String, String> receiveInfo = new HashMap<>();
                            receiveInfo.put("PENY",null);
                            receiveInfo.put("DOLR",null);
                            receiveInfo.put("SHIL",null);
                            receiveInfo.put("QUID",null);
                            docRef.set(receiveInfo);

                            fragment = new map();
                            hideUI(loginButton,signUpButton,colourBox,loginText,emailedit,passwordedit);
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            FragmentTransaction ft = fragmentManager.beginTransaction();
                            ft.replace(R.id.screen_area, fragment);
                            ft.commit();
                            View view1 = getCurrentFocus();
                            if (view1 != null) {
                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view1.getWindowToken(), 0);
                            }
                        } else {
                            Log.w("UserCreationFailed", "createUserWithEmail:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                Toast.makeText(getBaseContext(),"Check your email is entered correctly", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getBaseContext(),"Account creation failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(getBaseContext(), "Check an email is entered and the password at least 6 characters long", Toast.LENGTH_SHORT).show();
        }

    }

    public class DownloadFileTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try{
                return loadFileFromNetwork(urls[0]);
            } catch (IOException e){
                return "Unable to load content. Check your network connection";
            }
        }

        private String loadFileFromNetwork(String urlString) throws IOException {
            return readStream(downloadUrl(new URL(urlString)));
        }

        private InputStream downloadUrl(URL url) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000); // milliseconds
            conn.setConnectTimeout(15000); // milliseconds
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            return conn.getInputStream();
        }

        @NonNull
        private String readStream(InputStream stream) throws IOException{
            BufferedReader r = new BufferedReader(new InputStreamReader(stream));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null;) {
                total.append(line).append('\n');
            }
            return total.toString();
        }

        @Override
        protected void onPostExecute(String result){
            FileOutputStream outputStream;
            super.onPostExecute(result);
            DownloadCompleteRunner.downloadComplete(result);
            try{
                outputStream = openFileOutput("data.geojson", Context.MODE_PRIVATE);
                outputStream.write(result.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}

