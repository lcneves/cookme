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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

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


    Context context = SearchResults.this;
    ProgressDialog mProgressDialog;
    DatabaseHelper database = new DatabaseHelper(this);
    ListView lv;
    String[] selIngredients;
    static final String dbName="RecipesDB";
    static final String recipesTable="Recipes";
    static final String recID="_id";
    static final String recName="Name";
    static final String recIngredients="Ingredients";
    static final String resultsTable="Results";
    static final String recURL="URL";
    static final String resID="_id";
    static final String resName="ResName";
    static final String resIngredients="ResIngredients";
    static final String resURL="ResURL";
    static final String resMatches="Matches";
    static final String resMismatches="Mismatches";
    static final String resMatchCount="MatchCount";
    static final String resMismatchCount="MismatchCount";
    int rowCount = 0;
    static ArrayList<HashMap<String, String>> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        Intent intent = getIntent();
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        displayResults();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void displayResults() {
        mProgressDialog = new ProgressDialog(SearchResults.this);
        mProgressDialog.setMessage("Searching recipes...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        final SearchTask searchTask = new SearchTask(SearchResults.this);
        searchTask.execute(selIngredients);
    }

    private class SearchTask extends AsyncTask<String, Integer, String> {

        private String progressMessage;
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public SearchTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... selIngredients) {
            DatabaseHelper database = new DatabaseHelper(getApplicationContext());
            SQLiteDatabase db = database.getWritableDatabase();

            if (selIngredients.length == 0)
                return "selIngredients is zero";

            String whereCondition = recIngredients+" LIKE \'%" + selIngredients[0] + "%\'";
            for (int i = 1; i < selIngredients.length; i++) {
                whereCondition = whereCondition + " OR " + recIngredients + " LIKE \'%" + selIngredients[i] + "%\'";
            }
/*            String insertCommand = "INSERT INTO "+resultsTable+" ("+resName+","+resIngredients+","+resURL+") SELECT "+recName+","+recIngredients+","+recURL+" FROM "+recipesTable+" WHERE "+whereCondition;
            db.execSQL("DROP TABLE IF EXISTS "+resultsTable);
            db.execSQL("CREATE TABLE "+resultsTable+" ("+resID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+resName+" TEXT, "+resIngredients+" TEXT, "+resURL+" TEXT, "+resMatches+" TEXT, "+resMismatches+" TEXT, "+resMatchCount+" INTEGER, "+resMismatchCount+" INTEGER)");
            db.execSQL(insertCommand);*/

            Cursor cursor = db.query(recipesTable, new String[] {recName, recIngredients, recURL}, whereCondition, null, null, null, null);
            if(cursor.moveToFirst()) {
                rowCount = cursor.getCount();
                progressMessage = "Found "+rowCount+" recipes matching your ingredients. Processing...";
                list = new ArrayList<HashMap<String, String>>();
                int nameIndex = cursor.getColumnIndexOrThrow(recName);
                int ingredientsIndex = cursor.getColumnIndexOrThrow(recIngredients);
                int urlIndex = cursor.getColumnIndexOrThrow(recURL);

                for (int i = 0; i < rowCount; i++) {
                    publishProgress((int) (i+1));
                    HashMap<String, String> map = new HashMap<String, String>();
                    String name = cursor.getString(nameIndex);
                    String ingredients = cursor.getString(ingredientsIndex);
                    String url = cursor.getString(urlIndex);
                    String matches = "Uses: ";
                    String misMatches = "Doesn't use: ";
                    int misCount = 0;
                    for (int j = 0; j < selIngredients.length; j++) {
                        if (ingredients.toLowerCase(Locale.ENGLISH).contains(selIngredients[j].toLowerCase(Locale.ENGLISH))) {
                            matches = matches + selIngredients[j] + ", ";
                        } else {
                            misMatches = misMatches + selIngredients[j] + ", ";
                            misCount++;
                        }
                    }
                    matches = matches.substring(0, matches.length() - 2);
                    if (misMatches.length() == 13) {
                        misMatches = "";
                    } else {
                        misMatches = misMatches.substring(0, misMatches.length() - 2);
                    }
                    map.put("_id", Integer.toString(i));
                    map.put(recName, name);
                    map.put(recIngredients, ingredients);
                    map.put(recURL, url);
                    map.put(resMatches, matches);
                    map.put(resMismatches, misMatches);
                    map.put(resMismatchCount, Integer.toString(misCount));
                    list.add(map);
                    cursor.moveToNext();
                }
                cursor.close();
                progressMessage = "Sorting...";
                publishProgress((int) (rowCount));
                Collections.sort(list, new CustomComparator());
            }



 /*           try {
                cursor = db.query(resultsTable, ingredientsArray, null, null, null, null, null);
                db.beginTransaction();
                rowCount = cursor.getCount();

                int columnIndex = cursor.getColumnIndexOrThrow(resIngredients);
                cursor.moveToFirst();
                for (int i = 1; i <= rowCount; i++) {
                    publishProgress((int) (i));
                    String matches = "Uses: ";
                    String misMatches = "Doesn't use: ";
                    int count = 0;
                    int misCount = 0;
                    for (int j = 0; j < selIngredients.length; j++) {
                        String content = cursor.getString(columnIndex);
                        if (content.toLowerCase(Locale.ENGLISH).contains(selIngredients[j].toLowerCase(Locale.ENGLISH))) {
                            matches = matches + selIngredients[j] + ", ";
                            count++;
                        } else {
                            misMatches = misMatches + selIngredients[j] + ", ";
                            misCount++;
                        }
                    }
                    matches = matches.substring(0, matches.length()-2);
                    if(misMatches.length() == 13) {
                        misMatches = "";
                    } else {
                        misMatches = misMatches.substring(0, misMatches.length()-2);
                    }
                    ContentValues cv=new ContentValues();
                    cv.put(resMatches, matches);
                    cv.put(resMismatches, misMatches);
                    cv.put(resMatchCount, count);
                    cv.put(resMismatchCount, misCount);
                    db.update(resultsTable, cv, resID + " = " + i, null);
                    cv.clear();
                    cursor.moveToNext();
                }
                db.setTransactionSuccessful();
            } finally {
                cursor.close();
                db.endTransaction();
            }*/
            db.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setMessage("Found "+rowCount+" recipes matching your ingredients. Processing...");
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
            Intent intent = new Intent(SearchResults.this, DisplayResults.class);
            startActivity(intent);
        }
    }

    public class CustomComparator implements Comparator<HashMap<String, String>> {
        @Override
        public int compare(HashMap<String, String> map1, HashMap<String, String> map2) {
            return map1.get(resMismatchCount).compareTo(map2.get(resMismatchCount));
        }
    }
}
