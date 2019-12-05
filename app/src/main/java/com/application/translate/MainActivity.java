package com.application.translate;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private final String API_KEY = "trnsl.1.1.20191205T035205Z.98590ce6564242d2.f49af4a356ef9de668dbe5f7ec5a770a135ae7f6";

    private Spinner firstSpinner, secondSpinner;
    private EditText firstEditText, secondEditText;

    private HashMap<String, List<String>> directions = new HashMap<>();

    private List<String> langs = new ArrayList<>();

    private List<String> secondLangs = new ArrayList<>();

    private String from;

    private String to;

    private JSONObject langsApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firstSpinner = findViewById(R.id.firstSpinner);
        secondSpinner = findViewById(R.id.secondSpinner);
        firstEditText = findViewById(R.id.firstEditText);
        secondEditText = findViewById(R.id.secondEditText);

        firstSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                from = langs.get(position);

                if (directions.containsKey(from)) {
//                    Toast.makeText(MainActivity.this, "true", Toast.LENGTH_SHORT).show();
                    // Если доступные языки существуют.

                    secondLangs = new ArrayList<>();
                    secondLangs.addAll(directions.get(from));

                    ArrayList<String> normalLangs = new ArrayList<>();

                    // Заменяем ключи на нормальные названия.
                    for (int i = 0; i < directions.get(from).size(); i++) {
                        if (langsApi.has(directions.get(from).get(i))) {
                            try {
                                normalLangs.add(langsApi.getString(directions.get(from).get(i)));
                            } catch (JSONException e) {
                                normalLangs.add("Unknown language");
                            }
                        } else {
                            normalLangs.add("Unknown language");
                        }
                    }

//                    Toast.makeText(MainActivity.this, Arrays.toString(normalLangs.toArray()), Toast.LENGTH_SHORT).show();

                    ArrayAdapter<?> adapter2 = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, normalLangs.toArray());
                    secondSpinner.setAdapter(adapter2);
                } else {
                    Toast.makeText(MainActivity.this, "false", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        secondSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                to = secondLangs.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Грузим возможные языки.
        new APIGetLangsTask().execute();
    }

    public void translate(View view) {
        // Нажали кнопку "Перевод".
        new APIRequestTask().execute(firstEditText.getText().toString());
    }

    class APIGetLangsTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            final String API_KEY = "trnsl.1.1.20191205T035205Z.98590ce6564242d2.f49af4a356ef9de668dbe5f7ec5a770a135ae7f6";
            String set_server_url = "https://translate.yandex.net/api/v1.5/tr.json/getLangs";
            String result = "";

            // need to URL encode form fields
            // https://docs.oracle.com/javase/8/docs/api/java/net/URLEncoder.html

            set_server_url += "?key=" + API_KEY;
            set_server_url += "&ui=ru";
            try {
                URL url = new URL(set_server_url);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                // reading response
                Scanner in = new Scanner(urlConnection.getInputStream());
                while (in.hasNext()) {
                    result += in.nextLine();
                }
                urlConnection.disconnect();
            } catch (IOException e) {
                Log.d("mytag", e.getLocalizedMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject object = new JSONObject(s);
                if (object.has("dirs")) {
                    // Заполняем массив языков, если он пустой.
                    JSONArray dirs = object.getJSONArray("dirs");
                    for (int i = 0; i < dirs.length(); i++) {
                        String currentLang = dirs.getString(i);
                        String toLang = currentLang.substring(3);
                        currentLang = currentLang.substring(0, 2);

                        if (directions.containsKey(currentLang)) {
                            // Если перечень направлений содержит уже этот язык.
                            directions.get(currentLang).add(toLang);
                        } else {
                            directions.put(currentLang, new ArrayList<String>());
                            directions.get(currentLang).add(toLang);
                        }

                        if (!langs.contains(currentLang)) {
                            langs.add(currentLang);
                        }
                    }

                    for (String key : directions.keySet()) {
                        Log.i("mytag", "onPostExecute: " + key + " : " + Arrays.toString(directions.get(key).toArray()));
                    }

                    List<String> normalLangs = new ArrayList<>();

                    // Заменяем ключи на нормальные названия.
                    if (object.has("langs")) {
                        langsApi = object.getJSONObject("langs");
                        for (int i = 0; i < langs.size(); i++) {
                            if (langsApi.has(langs.get(i))) {
                                normalLangs.add(langsApi.getString(langs.get(i)));
                            } else {
                                normalLangs.add("Unknown language");
                            }
                        }
                    }

                    ArrayAdapter<?> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, normalLangs.toArray());
                    firstSpinner.setAdapter(adapter);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class APIRequestTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            final String API_KEY = "trnsl.1.1.20191205T035205Z.98590ce6564242d2.f49af4a356ef9de668dbe5f7ec5a770a135ae7f6";
            String set_server_url = "https://translate.yandex.net/api/v1.5/tr.json/translate";
            String result = "";

            // need to URL encode form fields
            // https://docs.oracle.com/javase/8/docs/api/java/net/URLEncoder.html

            set_server_url += "?key=" + API_KEY;
            set_server_url += "&lang=" + from + "-" + to;
            set_server_url += "&text=" + params[0];
            try {
                URL url = new URL(set_server_url);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                // reading response
                Scanner in = new Scanner(urlConnection.getInputStream());
                while (in.hasNext()) {
                    result += in.nextLine();
                }
                urlConnection.disconnect();
            } catch (IOException e) {
                Log.d("mytag", e.getLocalizedMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            // got request result returned from doInBackground
            Log.d("mytag", s);
            try {
                JSONObject object = new JSONObject(s);

                if (object.has("text")) {
                    JSONArray jsonArray = object.getJSONArray("text");
                    secondEditText.setText(jsonArray.getString(0));
                } else {
                    Log.i("mytag", "onPostExecute: " + s);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
