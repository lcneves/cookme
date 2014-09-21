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
import java.util.Locale;


public class SearchResults extends ListActivity {


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
    private ListActivity activity = SearchResults.this;

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
            String insertCommand = "INSERT INTO "+resultsTable+" ("+resName+","+resIngredients+","+resURL+") SELECT "+recName+","+recIngredients+","+recURL+" FROM "+recipesTable+" WHERE "+whereCondition;
            db.execSQL("DROP TABLE IF EXISTS "+resultsTable);
            db.execSQL("CREATE TABLE "+resultsTable+" ("+resID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+resName+" TEXT, "+resIngredients+" TEXT, "+resURL+" TEXT, "+resMatches+" TEXT, "+resMismatches+" TEXT, "+resMatchCount+" INTEGER, "+resMismatchCount+" INTEGER)");
            db.execSQL(insertCommand);

            String[] ingredientsArray = {resIngredients};
            Cursor cursor = null;
            try {
                cursor = db.query(resultsTable, ingredientsArray, null, null, null, null, null);
                db.beginTransaction();
                rowCount = cursor.getCount();

                int columnIndex = cursor.getColumnIndexOrThrow(resIngredients);
                cursor.moveToFirst();
                for (int i = 1; i <= rowCount; i++) {
                    publishProgress((int) (i));
                    String matches = "Matches: ";
                    String misMatches = "Does not match: ";
                    int count = 0;
                    int misCount = 0;
                    for (int j = 0; j < selIngredients.length; j++) {
                        String content = cursor.getString(columnIndex);
                        if (content.toLowerCase(Locale.ENGLISH).contains(selIngredients[j].toLowerCase(Locale.ENGLISH))) {
                            matches = matches + selIngredients[j] + " ";
                            count++;
                        } else {
                            misMatches = misMatches + selIngredients[j] + " ";
                            misCount++;
                        }
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
            }
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
//          Context context = activity;
            DatabaseHelper database = new DatabaseHelper(SearchResults.this);
            SQLiteDatabase db = database.getWritableDatabase();
            Cursor cursor = database.displayResults();
            //	Cursor cursor = database.displayRecipes();

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity, R.layout.list_item, cursor, new String[] { DatabaseHelper.resName, DatabaseHelper.resIngredients, DatabaseHelper.resURL, DatabaseHelper.resMatches, DatabaseHelper.resMismatches }, new int[] { R.id.name, R.id.ingredients, R.id.url, R.id.matches, R.id.mismatches }, 0);
            //	SimpleCursorAdapter adapter = new SimpleCursorAdapter(context, R.layout.list_item, cursor, new String[] { DatabaseHelper.recName, DatabaseHelper.recIngredients, DatabaseHelper.recURL }, new int[] { R.id.name, R.id.ingredients, R.id.url }, 0);

                setListAdapter(adapter);
            // select single ListView item
                lv = getListView();

        }
    }
}
