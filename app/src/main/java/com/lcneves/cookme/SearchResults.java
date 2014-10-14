package com.lcneves.cookme;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;


public class SearchResults extends Activity {

    ProgressDialog mProgressDialog;
    String[] selIngredients;
    static String recipeName;
    String selIngredientsDummy = null;
    int rowCount = 0;
    static ArrayList<HashMap<String, String>> list;
    final String resMismatches = DatabaseHelper.resMismatches;
    final String recSize = "size";
    String[] selIngredientsLower;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        Intent intent = getIntent();
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        recipeName = intent.getStringExtra("com.lcneves.cookme.RECIPENAME");
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
                whereCondition = DatabaseHelper.recName+" LIKE \'%" + recipeName + "%\'";
                if (selIngredients != null) {
                    whereCondition = whereCondition + " AND ";
                }
            }
            if (selIngredients != null) {
                whereCondition = whereCondition + DatabaseHelper.recIngredients+" LIKE \'%" + selIngredients[0] + "%\'";
                for (int i = 1; i < selIngredients.length; i++) {
                    whereCondition = whereCondition + " OR " + DatabaseHelper.recIngredients + " LIKE \'%" + selIngredients[i] + "%\'";
                    if (recipeName != null) {
                        whereCondition = whereCondition +" AND "+DatabaseHelper.recName+" LIKE \'%" + recipeName + "%\'";
                    }
                }
            }
            Log.d("com.lcneves.cookme.SearchResults", "whereCondition is: "+whereCondition);
            Cursor cursor = db.query(DatabaseHelper.recipesTable, new String[] {DatabaseHelper.recID, DatabaseHelper.recIngredientsLower}, whereCondition, null, null, null, null);
            if(cursor.moveToFirst()) {
                final String recID = DatabaseHelper.recID;

                rowCount = cursor.getCount();
                progressMessage = "Found "+rowCount+" recipes matching your ingredients. Processing...";
                list = new ArrayList<HashMap<String, String>>(rowCount);
                int IDIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recID);
                int ingredientsLowerIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recIngredientsLower);
                long startTime = System.nanoTime();
                int selLength;
                if (selIngredients != null) {
                    selLength = selIngredients.length;
                } else {
                    selLength = 0;
                }
                HashMap<String, String> map;
                String id;
                String ingredientsLower;

                selIngredientsLower = new String[selIngredients.length];
                for (int i = 0; i < selIngredients.length; ++i) selIngredientsLower[i] = selIngredients[i].toLowerCase(Locale.ENGLISH);

                long oldTime = System.nanoTime();

                parse: for (int i = 0; i < rowCount; i++) {
                    if(System.nanoTime() - oldTime > 1e9) { // update every second
                        oldTime = System.nanoTime();
                        publishProgress((int) (i));
                    }
                    id = cursor.getString(IDIndex);
                    ingredientsLower = cursor.getString(ingredientsLowerIndex);
                    int misCount = 0;
                    map = new HashMap<String, String>(3, 1);
                    for (int j = 0; j < selLength; j++) {
                        if (!ingredientsLower.contains(selIngredientsLower[j])) ++misCount;
                    }
                    map.put(recID, id);
                    map.put(resMismatches, Integer.toString(misCount));
                    map.put(recSize, Integer.toString(ingredientsLower.length()));
                    list.add(map);
                    cursor.moveToNext();
                }
                Log.d("com.lcneves.cookme.SearchResults", "Processing took " + ((System.nanoTime() - startTime)) / 1000000);
                cursor.close();
                db.close();
                if (selIngredients != null) {
                    Collections.sort(list, new LengthComparator());
                    Collections.sort(list, new MiscountComparator());
                    String[] listArray = new String[list.size()];
                    for(int i = 0; i < list.size(); i++) {
                        listArray[i] = list.get(i).get(recID);
                    }
                    database.createResultsView(listArray);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setMessage("Processing "+rowCount+" recipes... "+(progress[0] * 100 / rowCount)+"%");
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
                intent.putExtra("com.lcneves.cookme.ROW", rowCount);
                intent.putExtra("com.lcneves.cookme.INGREDIENTS_LOWER", selIngredientsLower);
                startActivity(intent);
            }
        }
    }

    public class MiscountComparator implements Comparator<HashMap<String, String>> {
        @Override
        public int compare(HashMap<String, String> map1, HashMap<String, String> map2) {
            return Double.compare(Integer.parseInt(map1.get(resMismatches)), Integer.parseInt(map2.get(resMismatches)));
        }
    }
    public class LengthComparator implements Comparator<HashMap<String, String>> {
        @Override
        public int compare(HashMap<String, String> map1, HashMap<String, String> map2) {
            return Double.compare(Integer.parseInt(map1.get(recSize)), Integer.parseInt(map2.get(recSize)));
        }
    }
}