package aselia.com.coinz;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
        return stream.toString();
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
        DownloadCompleteRunner.downloadComplete(result);
    }

}