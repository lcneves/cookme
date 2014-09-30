package com.lcneves.cookme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;


public class SearchSimple extends ListActivity {

    private ListActivity activity = SearchSimple.this;
    ListView lv;
    ProgressDialog mProgressDialog;
    String[] selIngredients;
    String recipeName;
    String selIngredientsDummy = null;
    static final String recipesTable="Recipes";
    static final String recID="_id";
    static final String recName="Name";
    static final String recIngredients="Ingredients";
    static final String recURL="URL";
    static final String recLength="Length";
    Cursor cursor;
    int cursorCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_simple);
        Intent intent = getIntent();
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        recipeName = intent.getStringExtra("com.lcneves.cookme.RECIPENAME");
        searchResults();
    }

    void searchResults() {
        mProgressDialog = new ProgressDialog(SearchSimple.this);
        mProgressDialog.setMessage("Searching recipes...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        final SearchTask searchTask = new SearchTask(SearchSimple.this);
        searchTask.execute(selIngredientsDummy);
    }

    private class SearchTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public SearchTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... selIngredientsDummy) {
            long startTime = System.nanoTime();
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
                    whereCondition = whereCondition + " AND " + recIngredients + " LIKE \'%" + selIngredients[i] + "%\'";
                }
            }
            Log.d("com.lcneves.cookme.SearchSimple", "whereCondition is: " + whereCondition);
            cursor = db.query(recipesTable, new String[] {recID, recName, recIngredients, recURL, recLength}, whereCondition, null, null, null, recLength);
            Log.d("com.lcneves.cookme.SearchResults", "Simple search took " + (((System.nanoTime() - startTime)) / 1000000)+" ms");
            return null;
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
            if(cursor.moveToFirst()) {
                cursorCount = cursor.getCount();
                SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity,
                        R.layout.list_item_simple,
                        cursor,
                        new String[]{recName, recIngredients, recURL},
                        new int[]{R.id.name, R.id.ingredients, R.id.url},
                        0);
                setListAdapter(adapter);
                lv = getListView();
                if(selIngredients != null) {
                    if(selIngredients.length > 2) {
                        View footerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.simple_footer, null, false);
                        lv.addFooterView(footerView);
                    }
                }
            } else {
                Toast toast = Toast.makeText(SearchSimple.this, "No recipe uses all the selected ingredients...", Toast.LENGTH_LONG);
                toast.show();
                Intent intent = new Intent(SearchSimple.this, SearchResults.class);
                intent.putExtra("com.lcneves.cookme.RECIPENAME", recipeName);
                intent.putExtra("com.lcneves.cookme.INGREDIENTS", selIngredients);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        DatabaseHelper db = new DatabaseHelper(this);
        String url = db.getUrl(id);
        Intent intent = new Intent(this, RecipeViewer.class);
        intent.putExtra("com.lcneves.cookme.URL", url);
        intent.putExtra("com.lcneves.cookme.ACTIVITY", "simple");
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void clickShowMore(View v) {
        Intent intent = new Intent(SearchSimple.this, SearchResults.class);
        intent.putExtra("com.lcneves.cookme.RECIPENAME", recipeName);
        intent.putExtra("com.lcneves.cookme.INGREDIENTS", selIngredients);
        intent.putExtra("com.lcneves.cookme.ROW", cursorCount);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_simple, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        /*
        if (id == R.id.action_settings) {
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

    public void clickAboutMenuSimple(MenuItem menu) {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
        TextView par1 = (TextView) messageView.findViewById(R.id.aboutPar1);
        TextView par4 = (TextView) messageView.findViewById(R.id.aboutPar4);
        Log.d("com.lcneves.cookme.MainActivity", "Par4 = "+par4.toString());
        Linkify.addLinks(par1, Linkify.WEB_URLS);
        Linkify.addLinks(par4, Linkify.WEB_URLS);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }
}
