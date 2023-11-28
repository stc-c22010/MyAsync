package jp.suntechc22010.myasync;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private static final String DEBUG_TAG = "AsyncSample";
    private static final String WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?lang=ja";
    private static final String APP_ID = "cc1f0f1f92ec49db52cb56199b8d1c8c";
    private List<Map<String, String>> _list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _list = createList();

        ListView lvCityList = findViewById(R.id.lvCityList);
        String[] from = {"name"};
        int[] to = {android.R.id.text1};
        SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), _list, android.R.layout.simple_list_item_1, from, to);
        lvCityList.setAdapter(adapter);
        lvCityList.setOnItemClickListener(new ListItemClickListener());
    }

    private List<Map<String, String>> createList(){
        List<Map<String, String>> list = new ArrayList<>();

        Map<String, String> map = new HashMap<>();
        map.put("name", "大阪");
        map.put("q", "Osaka");
        list.add(map);

        map = new HashMap<>();
        map.put("name", "神戸");
        map.put("q", "Kobe");
        list.add(map);

        map = new HashMap<>();
        map.put("name", "甲府");
        map.put("q", "Kofu");
        list.add(map);

        return list;
    }

    @UiThread
    private void receiveWeatherInfo(final String urlFull){
        WeatherInfoBackgroundReceiver backgroundReceiver = new WeatherInfoBackgroundReceiver(urlFull);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = executorService.submit(backgroundReceiver);
        String result = "";
        try{
            result = future.get();
        }
        catch(ExecutionException ex){
            Log.w(DEBUG_TAG, "非同期処理結果の取得で例外発生: ", ex);
        }
        catch(InterruptedException ex){
            Log.w(DEBUG_TAG, "非同期処理結果の取得で例外発生: ", ex);
        }
        showWeatherInfo(result);
    }

    @UiThread
    private void showWeatherInfo(String result){
        String cityName = "";
        String weather = "";
        String latitude = "";
        String longitude = "";
        try{
            JSONObject rootJSON = new JSONObject(result);
            cityName = rootJSON.getString("name");
            JSONObject coordJSON = rootJSON.getJSONObject("coord");
            latitude = coordJSON.getString("lat");
            longitude = coordJSON.getString("lon");
            JSONArray weatherJSONArray = rootJSON.getJSONArray("weather");
            JSONObject weatherJSON = weatherJSONArray.getJSONObject(0);
            weather = weatherJSON.getString("description");
        }
        catch(JSONException ex){
            Log.e(DEBUG_TAG, "JSON解析失敗", ex);
        }

        String telop = cityName + "の天気";
        String desc = "現在は" + weather + "です。\n緯度は" + latitude + "度で経度は" + longitude + "度です。";

        TextView tvWeatherTelop = findViewById(R.id.tvWeatherTelop);
        TextView tvWeatherDesc = findViewById(R.id.tvWeatherDesc);

        tvWeatherTelop.setText(telop);
        tvWeatherDesc.setText(desc);
    }

    private class WeatherInfoBackgroundReceiver implements Callable<String> {
        private final String _urlFull;

        public WeatherInfoBackgroundReceiver(String urlFull){
            _urlFull = urlFull;
        }

        @WorkerThread
        @Override
        public String call() {
            String result = "";
            HttpURLConnection con = null;
            InputStream is = null;
            try{
                URL url = new URL(_urlFull);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(1000);
                con.setReadTimeout(1000);
                con.setRequestMethod("GET");
                con.connect();
                is = con.getInputStream();
                result = is2String(is);
            }
            catch(MalformedURLException ex){
                Log.e(DEBUG_TAG, "URL変換失敗", ex);
            }
            catch(SocketTimeoutException ex){
                Log.w(DEBUG_TAG, "通信タイムアウト", ex);
            }
            catch(IOException ex){
                Log.e(DEBUG_TAG, "通信失敗", ex);
            }
            finally{
                if(con != null){
                    con.disconnect();
                }
                if(is != null){
                    try{
                        is.close();
                    }
                    catch(IOException ex){
                        Log.e(DEBUG_TAG, "InputStream解放失敗", ex);
                    }
                }
            }

            return result;
        }

        private String is2String(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while(0 <= (line = reader.read(b))){
                sb.append(b, 0, line);
            }
            return sb.toString();
        }
    }

    private class ListItemClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Map<String, String> item = _list.get(position);
            String q = item.get("q");
            String urlFull = WEATHERINFO_URL + "&q=" + q + "&appid=" + APP_ID;

            receiveWeatherInfo(urlFull);
        }
    }
}