package com.lcneves.cookme;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class SearchResults extends Activity {

    ProgressDialog mProgressDialog;
    String[] selIngredients;
    String recipeName;
    String selIngredientsDummy = null;
    int cursorCount;
    static final String recipesTable="Recipes";
    static final String recName="Name";
    static final String recIngredients="Ingredients";
    static final String recURL="URL";
    static final String resMatches="Matches";
    static final String resMismatches="Mismatches";
    static final String resMismatchCount="MismatchCount";
    int rowCount = 0;
    static ArrayList<HashMap<String, String>> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        Intent intent = getIntent();
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        recipeName = intent.getStringExtra("com.lcneves.cookme.RECIPENAME");
        cursorCount = intent.getIntExtra("com.lcneves.cookme.ROW", 0);
        list = null;
        Log.d("com.lcneves.cookme.SearchResults", "selIngredients= "+selIngredients+", recipeName= "+recipeName);

        searchResults();
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_results, menu);
        return true;
    }*/
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        *//*
        if (id == R.id.action_settings) {
            return true;
        }*//*
        return super.onOptionsItemSelected(item);
    }*/

    void searchResults() {
        mProgressDialog = new ProgressDialog(SearchResults.this);
        mProgressDialog.setMessage("Searching recipes...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_dialog_anim));
        mProgressDialog.setCancelable(false);

        final SearchTask searchTask = new SearchTask(SearchResults.this);
        searchTask.execute(selIngredientsDummy);
    }

    private class SearchTask extends AsyncTask<String, Integer, String> {

        private String progressMessage;
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public SearchTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... selIngredientsDummy) {
            DatabaseHelper database = new DatabaseHelper(getApplicationContext());
            SQLiteDatabase db = database.getWritableDatabase();
            String whereCondition = "";

            if (recipeName != null) {
                whereCondition = recName+" LIKE \'%" + recipeName + "%\'";
                if (selIngredients != null) {
                    whereCondition = whereCondition + " AND ";
                }
            }
            if (selIngredients != null) {
                whereCondition = whereCondition + recIngredients+" LIKE \'%" + selIngredients[0] + "%\'";
                for (int i = 1; i < selIngredients.length; i++) {
                    whereCondition = whereCondition + " OR " + recIngredients + " LIKE \'%" + selIngredients[i] + "%\'";
                    if (recipeName != null) {
                        whereCondition = whereCondition +" AND "+recName+" LIKE \'%" + recipeName + "%\'";
                    }
                }
            }
            Log.d("com.lcneves.cookme.SearchResults", "whereCondition is: "+whereCondition);
            Cursor cursor = db.query(recipesTable, new String[] {recName, recIngredients, recURL}, whereCondition, null, null, null, null);
            if(cursor.moveToFirst()) {
                rowCount = cursor.getCount();
                progressMessage = "Found "+rowCount+" recipes matching your ingredients. Processing...";
                list = new ArrayList<HashMap<String, String>>(rowCount);
                int nameIndex = cursor.getColumnIndexOrThrow(recName);
                int ingredientsIndex = cursor.getColumnIndexOrThrow(recIngredients);
                int urlIndex = cursor.getColumnIndexOrThrow(recURL);
                long startTime = System.nanoTime();
                int selLength;
                if (selIngredients != null) {
                    selLength = selIngredients.length;
                } else {
                    selLength = 0;
                }
                HashMap<String, String> map;
                String name;
                String ingredients;
                String url;
                StringBuilder matchesBuilder = null;
                StringBuilder misMatchesBuilder = null;
                String comma = ", ";
                String matches = null;
                String mismatches = null;
                for (int i = 0; i < rowCount; i++) {
                    if(i % 1000 == 0)
                        publishProgress((int) (i));
                    name = cursor.getString(nameIndex);
                    ingredients = cursor.getString(ingredientsIndex);
                    url = cursor.getString(urlIndex);
                    if (selIngredients != null) {
                        matchesBuilder = new StringBuilder("Uses: ");
                        misMatchesBuilder = new StringBuilder("Doesn't use: ");
                    } else {
                        matches = "";
                        mismatches = "";
                    }
                    int misCount = 0;
                    map = new HashMap<String, String>(7, 1);
                    for (int j = 0; j < selLength; j++) {
                        if (ingredients.toLowerCase(Locale.ENGLISH).contains(selIngredients[j].toLowerCase(Locale.ENGLISH))) {
                            if(matchesBuilder.length() > 6)
                                matchesBuilder.append(", ");
                            matchesBuilder.append(selIngredients[j]);

                        } else {
                            if(misMatchesBuilder.length() > 13)
                                misMatchesBuilder.append(", ");
                            misMatchesBuilder.append(selIngredients[j]);
                            misCount++;
                        }
                    }
                    if(misCount == 0 && selIngredients != null)
                        misMatchesBuilder.setLength(0);
                    if (selIngredients != null) {
                        matches = matchesBuilder.toString();
                        mismatches = misMatchesBuilder.toString();
                    }
                    map.put("_id", Integer.toString(i));
                    map.put(recName, name);
                    map.put(recIngredients, ingredients);
                    map.put(recURL, url);
                    map.put(resMatches, matches);
                    map.put(resMismatches, mismatches);
                    map.put(resMismatchCount, Integer.toString(misCount));
                    list.add(map);
                    cursor.moveToNext();
                }
                Log.d("com.lcneves.cookme.SearchResults", "Processing took " + ((System.nanoTime() - startTime)) / 1000000);
                cursor.close();
                db.close();
                if (selIngredients != null) {
                    Collections.sort(list, new LengthComparator());
                    Collections.sort(list, new MiscountComparator());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setMessage("Found "+rowCount+" recipes matching some of your ingredients. Processing...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(rowCount);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if(list == null) {
                Toast toast = Toast.makeText(SearchResults.this, "No recipes found!", Toast.LENGTH_LONG);
                toast.show();
                Intent intent = new Intent(SearchResults.this, MainActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(SearchResults.this, DisplayResults.class);
                intent.putExtra("com.lcneves.cookme.ROW", cursorCount);
                startActivity(intent);
            }
        }
    }

    public class MiscountComparator implements Comparator<HashMap<String, String>> {
        @Override
        public int compare(HashMap<String, String> map1, HashMap<String, String> map2) {
            return Double.compare(Integer.parseInt(map1.get(resMismatchCount)), Integer.parseInt(map2.get(resMismatchCount)));
        }
    }
    public class LengthComparator implements Comparator<HashMap<String, String>> {
        @Override
        public int compare(HashMap<String, String> map1, HashMap<String, String> map2) {
            return Double.compare(map1.get(recIngredients).length(), map2.get(recIngredients).length());
        }
    }
}
