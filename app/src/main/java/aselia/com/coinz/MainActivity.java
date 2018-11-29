package aselia.com.coinz;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.app.Fragment;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, map.OnFragmentInteractionListener, bank.OnFragmentInteractionListener, coin.OnFragmentInteractionListener {

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

        DownloadFileTask dl = new DownloadFileTask();
        dl.execute("http://homepages.inf.ed.ac.uk/stg/coinz/2018/10/03/coinzmap.geojson");

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
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;

        if (id == R.id.Map) {
            fragment = new map();
        } else if (id == R.id.Bank) {
            fragment = new bank();
            try{
                String test = openFileInput("data").toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (id == R.id.Coin_Transfer) {
            fragment = new coin();
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.screen_area, fragment);
            ft.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
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
                outputStream = openFileOutput("data", Context.MODE_PRIVATE);
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

