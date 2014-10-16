package com.lcneves.cookme;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;


public class SearchResults extends ListActivity {

    private ListActivity activity = this;
    ProgressDialog mProgressDialog;
    String[] selIngredients;
    String recipeName;
    String selIngredientsDummy = null;
    int rowCount;
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
    Cursor cursor;
    ComplexCursorAdapter adapter;
    ListView lv;
    DatabaseHelper database;
    int displayRows;
    final int DISPLAY_ROWS_INCREASE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        Intent intent = getIntent();
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        recipeName = intent.getStringExtra("com.lcneves.cookme.RECIPENAME");
        rowCount = intent.getIntExtra("com.lcneves.cookme.ROW", 0);
        selIngredientsLower = new String[selIngredients.length];
        for (int i = 0; i < selIngredients.length; ++i) selIngredientsLower[i] = selIngredients[i].toLowerCase(Locale.ENGLISH);
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
            String nameLike = "";
            if (recipeName != null) nameLike = " AND "+recName+" LIKE '%"+recipeName+"%\'";
            StringBuilder sb = new StringBuilder("CREATE VIEW ");
            sb.append(DatabaseHelper.RESULTS_VIEW);
            sb.append(" AS SELECT Recipes.*,Count(r._id) as CountMatches FROM (");

            final String QUERY_LIKE = "SELECT _id FROM Recipes WHERE Ingredients LIKE '%%%s%%'%s";

            sb.append(String.format(QUERY_LIKE, selIngredients[0], nameLike));

            for (int i = 1; i < selIngredients.length; ++i) {
                sb.append(" UNION ALL ");
                sb.append(String.format(QUERY_LIKE, selIngredients[i], nameLike));
            }

            sb.append(") AS r INNER JOIN Recipes ON r._id = Recipes._id GROUP BY r._id");
            sb.append(" ORDER BY CountMatches DESC, LENGTH(Ingredients)");

            database = new DatabaseHelper(getApplicationContext());
            SQLiteDatabase db = database.getWritableDatabase();

            long startTime = System.nanoTime();
            db.execSQL("DROP VIEW IF EXISTS " + DatabaseHelper.RESULTS_VIEW);
            Log.d("com.lcneves.cookme.SearchResults", "Dropping took "+((System.nanoTime()-startTime)/1000000));
            startTime = System.nanoTime();
            db.execSQL(sb.toString());
            Log.d("com.lcneves.cookme.SearchResults", "Executing SQL took "+((System.nanoTime()-startTime)/1000000));
            db.close();
            displayRows = rowCount + DISPLAY_ROWS_INCREASE;
            Log.d("com.lcneves.cookme.SearchResults", "About to get the cursor...");
            cursor = database.getResultsViewCursor(displayRows);
            Log.d("com.lcneves.cookme.SearchResults", "Got the cursor! It says: "+cursor.toString());
            if(!cursor.moveToFirst()) {
                mWakeLock.release();
                mProgressDialog.dismiss();
                Toast toast = Toast.makeText(SearchResults.this, "No recipes found!", Toast.LENGTH_LONG);
                toast.show();
                Intent intent2 = new Intent(SearchResults.this, MainActivity.class);
                startActivity(intent2);
            }
            adapter = new ComplexCursorAdapter(
                    activity,
                    R.layout.list_item_simple,
                    cursor,
                    new String[] {DatabaseHelper.recName, DatabaseHelper.recIngredients,
                            DatabaseHelper.recURL},
                    new int[] { R.id.name, R.id.ingredients, R.id.url},
                    0
            );
            Log.d("com.lcneves.cookme.SearchResults", "Instantiated the adapter...");
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
            setListAdapter(adapter);
            Log.d("com.lcneves.cookme.SearchResults", "Set list adapter...");
            lv = getListView();
            Log.d("com.lcneves.cookme.SearchResults", "Got list view...");
            registerForContextMenu(lv);
            Log.d("com.lcneves.cookme.SearchResults", "rowCount = "+ rowCount);

            View footerView = ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.simple_footer, null, false);
            lv.addFooterView(footerView);
            lv.setSelection(rowCount);
            mProgressDialog.dismiss();
        }
    }

    public void clickShowMore(View v) {
        mProgressDialog = new ProgressDialog(SearchResults.this);
        mProgressDialog.setMessage("Fetching more recipes...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_dialog_anim));
        mProgressDialog.setCancelable(false);

        final ShowMore showMore = new ShowMore(SearchResults.this);
        showMore.execute();
    }

    private class ShowMore extends AsyncTask<String, Integer, String> {

        private Context context;

        public ShowMore(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... args) {
            displayRows = displayRows + DISPLAY_ROWS_INCREASE;
            cursor = database.getResultsViewCursor(displayRows);
            cursor.moveToFirst();
            return null;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            adapter.changeCursor(cursor);
            adapter.notifyDataSetChanged();
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        String[] menuItems = new String[3];
        menuItems[0] = "Open recipe webpage";
        menuItems[1] = "Share recipe link";
        menuItems[2] = "Share recipe ingredients";
        for (int i = 0; i<menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] recipe = new String[3];
        cursor.moveToPosition(info.position);
        recipe[0]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recName));
        recipe[1]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recIngredients));
        recipe[2]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recURL));
        switch (menuItemIndex) {
            case 0:
                Intent intent = new Intent(this, RecipeViewer.class);
                intent.putExtra("com.lcneves.cookme.RECIPE", recipe);
                intent.putExtra("com.lcneves.cookme.ACTIVITY", "display");
                startActivity(intent);
                break;
            case 1:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, recipe[0]+": "+recipe[2]);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share link with..."));
                break;
            case 2:
                Intent sendIntent2 = new Intent();
                sendIntent2.setAction(Intent.ACTION_SEND);
                sendIntent2.putExtra(Intent.EXTRA_TEXT, recipe[0]+":\n\n"+recipe[1]+"\n\n"+recipe[2]);
                sendIntent2.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent2, "Share recipe with..."));
                break;
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
/*        if (id == R.id.action_settings) {
            return true;
        }*/
        if(id == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clickAboutMenuDisplay(MenuItem menu) {
        MainActivity.AboutDialogFragment aboutDialog = new MainActivity.AboutDialogFragment();
        aboutDialog.show(getFragmentManager(), "tag");
    }

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        String[] recipe = new String[3];
        cursor.moveToPosition(pos);
        recipe[0]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recName));
        recipe[1]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recIngredients));
        recipe[2]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recURL));
        Log.d("com.lcneves.cookme.SearchResults", "Clicked! Recipe name is "+recipe[0]);
        Intent intent = new Intent(this, RecipeViewer.class);
        intent.putExtra("com.lcneves.cookme.RECIPE", recipe);
        intent.putExtra("com.lcneves.cookme.ACTIVITY", "display");
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

    private class ComplexCursorAdapter extends SimpleCursorAdapter {

        public ComplexCursorAdapter(Context context, int layout, Cursor c,
                                    String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = super.getView(position, convertView, parent);
            TextView ingredients = (TextView) view.findViewById(R.id.ingredients);
            Spannable ingredientsSpan = new SpannableString(ingredients.getText());
            String ingredientsSpanString = ingredientsSpan.toString().toLowerCase(Locale.ENGLISH);
            for(String s : selIngredientsLower) {
                for(int j = -1; (j = ingredientsSpanString.indexOf(s, j + 1)) != -1;) {
                    ingredientsSpan.setSpan(new ForegroundColorSpan(Color.argb(208, 0, 127, 0)), j, j + s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            ingredients.setText(ingredientsSpan, TextView.BufferType.SPANNABLE);
            return view;
        }
    }
}