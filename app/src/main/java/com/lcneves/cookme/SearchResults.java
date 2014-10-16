package com.lcneves.cookme;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
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
    String recipeName;
    String selIngredientsDummy = null;
    int rowCount = 0;
    final String resMismatches = DatabaseHelper.resMismatches;
    final String recipesTable = DatabaseHelper.recipesTable;
    final String recID = DatabaseHelper.recID;
    final String recName = DatabaseHelper.recName;
    final String recIngredients = DatabaseHelper.recIngredients;
    final String recIngredientsLower = DatabaseHelper.recIngredientsLower;
    final String recURL = DatabaseHelper.recURL;
    final String recLength = DatabaseHelper.recLength;
    final String recSize = "size";
    String[] selIngredientsLower;
    int searchSimpleRows;
    boolean results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        Intent intent = getIntent();
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        recipeName = intent.getStringExtra("com.lcneves.cookme.RECIPENAME");
        searchSimpleRows = intent.getIntExtra("com.lcneves.cookme.ROW", 0);
        boolean results = false;
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
            /*DatabaseHelper database = new DatabaseHelper(getApplicationContext());
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
            Cursor cursor = db.query(DatabaseHelper.recipesTable,
                    new String[] {recID, recIngredientsLower},
                    whereCondition, null, null, null, null);
            if(cursor.moveToFirst()) {
                results = true;
                rowCount = cursor.getCount();
                progressMessage = "Found "+rowCount+" recipes matching your ingredients. Processing...";
                *//*list = new ArrayList<HashMap<String, String>>(rowCount);
                int nameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recName);
                int ingredientsIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recIngredients);
                int urlIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recURL);
                int lengthIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recLength);*//*
                int ingredientsLowerIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recIngredientsLower);
                int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.recID);
                long startTime = System.nanoTime();
                int selLength;
                if (selIngredients != null) {
                    selLength = selIngredients.length;
                } else {
                    selLength = 0;
                }
                *//*String resultsTable = DatabaseHelper.resultsTable;
                String comma = ",";
                SQLiteStatement st = db.compileStatement("INSERT INTO "+resultsTable+" ("+recID+comma+recName+comma+recIngredients+comma+recURL+comma+recLength+comma+resMismatches+") VALUES (?,?,?,?,?,?);");

                String name;
                String ingredients;
                String url;
                int length;*//*
                String ingredientsLower;
                int id;

                selIngredientsLower = new String[selIngredients.length];
                for (int i = 0; i < selIngredients.length; ++i) selIngredientsLower[i] = selIngredients[i].toLowerCase(Locale.ENGLISH);

                long oldTime = System.nanoTime();
                db.beginTransaction();
                db.execSQL("UPDATE "+ recipesTable + " SET "+resMismatches +" = NULL WHERE "+resMismatches+" NOT NULL");
                Log.d("com.lcneves.cookme.SearchResults", "Setting resMismatches to NULL took " + ((System.nanoTime() - oldTime)) / 1000000+" ms");
                for (int i = 0; i < rowCount; i++) {
                    if(System.nanoTime() - oldTime > 1e9) { // update every second
                        oldTime = System.nanoTime();
                        publishProgress((int) (i));
                    }
                    ingredientsLower = cursor.getString(ingredientsLowerIndex);
                    *//*name = cursor.getString(nameIndex);
                    ingredients = cursor.getString(ingredientsIndex);
                    url = cursor.getString(urlIndex);
                    length = cursor.getInt(lengthIndex);*//*
                    id = cursor.getInt(idIndex);
                    int misCount = 0;
                    for (int j = 0; j < selLength; j++) {
                        if (!ingredientsLower.contains(selIngredientsLower[j])) ++misCount;
                    }
                    db.execSQL("UPDATE "+ recipesTable + " SET "+ resMismatches +" = "+misCount+" WHERE "+recID+" = "+id);
                 *//*   st.bindLong(1, i);
                    st.bindString(2, name);
                    st.bindString(3, ingredients);
                    st.bindString(4, url);
                    st.bindLong(5, length);
                    st.bindLong(6, misCount);
                    st.executeInsert();
                    st.clearBindings();*//*
                    cursor.moveToNext();
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                Log.d("com.lcneves.cookme.SearchResults", "Processing took " + ((System.nanoTime() - startTime)) / 1000000);
                cursor.close();
                db.close();
                *//*if (selIngredients != null) {
                    startTime = System.nanoTime();
                    Collections.sort(list, new LengthComparator());
                    Collections.sort(list, new MiscountComparator());
                    Log.d("com.lcneves.cookme.SearchResults", "Sorting took "+((System.nanoTime()-startTime)/1000000));
                    startTime = System.nanoTime();
                    String[] listArray = new String[list.size()];
                    for(int i = 0; i < list.size(); i++) {
                        listArray[i] = list.get(i).get(recID);
                    }
                    Log.d("com.lcneves.cookme.SearchResults", "Creating string array took "+((System.nanoTime()-startTime)/1000000));
                    startTime = System.nanoTime();
                    database.createResultsView(listArray);
                    Log.d("com.lcneves.cookme.SearchResults", "Creating view took "+((System.nanoTime()-startTime)/1000000));
                }*/
            StringBuilder sb = new StringBuilder("CREATE VIEW ");
            sb.append(DatabaseHelper.RESULTS_VIEW);
            sb.append(" AS SELECT Recipes.*,Count(r._id) as CountMatches FROM (");

            final String QUERY_LIKE = "SELECT _id FROM Recipes WHERE Ingredients LIKE '%%%s%%'";
            sb.append(String.format(QUERY_LIKE, selIngredients[0]));
            for (int i = 1; i < selIngredients.length; ++i) {
                sb.append(" UNION ALL ");
                sb.append(String.format(QUERY_LIKE, selIngredients[i]));
            }

            sb.append(") AS r INNER JOIN Recipes ON r._id = Recipes._id GROUP BY r._id");
            sb.append(" ORDER BY CountMatches DESC, LENGTH(Ingredients)");

            DatabaseHelper database = new DatabaseHelper(getApplicationContext());
            SQLiteDatabase db = database.getWritableDatabase();

            long startTime = System.nanoTime();
            db.execSQL("DROP VIEW IF EXISTS " + DatabaseHelper.RESULTS_VIEW);
            Log.d("com.lcneves.cookme.SearchResults", "Dropping took "+((System.nanoTime()-startTime)/1000000));
            startTime = System.nanoTime();
            db.execSQL(sb.toString());
            Log.d("com.lcneves.cookme.SearchResults", "Executing SQL took "+((System.nanoTime()-startTime)/1000000));

            results = 0 < db.query(DatabaseHelper.RESULTS_VIEW, new String[]{"_id"}, null, null, null, null, null, "1").getCount();

            db.close();
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
            if(!results) {
                Toast toast = Toast.makeText(SearchResults.this, "No recipes found!", Toast.LENGTH_LONG);
                toast.show();
                Intent intent = new Intent(SearchResults.this, MainActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(SearchResults.this, DisplayResults.class);
                intent.putExtra("com.lcneves.cookme.ROW", searchSimpleRows);
                intent.putExtra("com.lcneves.cookme.INGREDIENTS", selIngredients);
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