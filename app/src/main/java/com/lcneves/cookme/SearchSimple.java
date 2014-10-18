package com.lcneves.cookme;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;


public class SearchSimple extends ListActivity {

    private ListActivity activity = SearchSimple.this;
    ListView lv;
    ProgressDialog mProgressDialog;
    static String[] selIngredients;
    static String recipeName;
    Cursor cursor;
    int displayRows;
    final int DISPLAY_ROWS_INCREASE = 20;
    boolean results;
    boolean complex;
    boolean filter;
    DatabaseHelper database = new DatabaseHelper(this);
    ComplexCursorAdapter adapter;
    static String query;
    static boolean newQuery;
    View footerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_simple);
        getActionBar().setDisplayShowTitleEnabled(false);
        Intent intent = getIntent();
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        recipeName = intent.getStringExtra("com.lcneves.cookme.RECIPENAME");
        displayRows = 0;
        results = false;
        complex = false;
        filter = false;
        newQuery = false;
        searchResults();
    }

    void searchResults() {
        mProgressDialog = new ProgressDialog(SearchSimple.this);
        mProgressDialog.setMessage("Searching recipes...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_dialog_anim));
        mProgressDialog.setCancelable(false);

        final SearchTask searchTask = new SearchTask(SearchSimple.this);
        searchTask.execute();
    }

    private class SearchTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public SearchTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... args) {
            long startTime = System.nanoTime();
            DatabaseHelper database = new DatabaseHelper(getApplicationContext());
            SQLiteDatabase db = database.getWritableDatabase();

            cursor = db.query(DatabaseHelper.recipesTable,
                    new String[] {DatabaseHelper.recID, DatabaseHelper.recName, DatabaseHelper.recIngredients, DatabaseHelper.recURL},
                    DatabaseHelper.createWhereClause(recipeName, selIngredients), null, null, null,
                    "LENGTH("+DatabaseHelper.recIngredients+")");
//            selIngredients.length > 0 ? "LENGTH("+DatabaseHelper.recIngredients+")" : null

            if(!(results = cursor.moveToFirst()) && selIngredients.length > 0) searchComplex();
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
            if(!complex) displayRows = cursor.getCount();
            if(results) {
                adapter = new ComplexCursorAdapter(activity,
                        R.layout.list_item_simple,
                        cursor,
                        new String[]{DatabaseHelper.recName, DatabaseHelper.recIngredients, DatabaseHelper.recURL},
                        new int[]{R.id.name, R.id.ingredients, R.id.url},
                        0);
                setListAdapter(adapter);
                lv = getListView();
                registerForContextMenu(lv);
                if(complex || selIngredients.length > 2) {
                    footerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.simple_footer, null, false);
                    lv.addFooterView(footerView);
                }
            } else {
                switch (selIngredients.length) {
                    case 0:
                        Toast.makeText(SearchSimple.this, "No recipes found with the name \"" + recipeName + "\"", Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                    case 2:
                        StringBuilder sb = new StringBuilder("No recipes");
                        if (!recipeName.isEmpty()) sb.append(" for \"" + recipeName + "\"");
                        sb.append(" found using \"" + selIngredients[0] + "\"");
                        if (selIngredients.length == 2) sb.append(" and \"" + selIngredients[1] + "\"");

                        Toast.makeText(SearchSimple.this, sb.toString(), Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(SearchSimple.this, "No recipes found!", Toast.LENGTH_LONG).show();
                }
                startActivity(new Intent(SearchSimple.this, MainActivity.class));
            }
            mProgressDialog.dismiss();
        }
    }

    private void searchComplex() {
        complex = true;
        StringBuilder sb = new StringBuilder("CREATE VIEW ");
        sb.append(DatabaseHelper.RESULTS_VIEW);
        sb.append(" AS SELECT ");
        sb.append(DatabaseHelper.recipesTable);
        sb.append(".*,Count(r._id) as CountMatches FROM (");

        final String QUERY_LIKE_STUB = "SELECT " + DatabaseHelper.recID + " FROM " + DatabaseHelper.recipesTable + " WHERE ";
        sb.append(QUERY_LIKE_STUB);
        sb.append(DatabaseHelper.createWhereClause(recipeName, new String[] { selIngredients[0] } ));

        for (int i = 1; i < selIngredients.length; ++i) {
            sb.append(" UNION ALL ");
            sb.append(QUERY_LIKE_STUB);
            sb.append(DatabaseHelper.createWhereClause(recipeName, new String[]{selIngredients[i]}));
        }

        sb.append(") AS r INNER JOIN ");
        sb.append(DatabaseHelper.recipesTable);
        sb.append(" ON r._id = ");
        sb.append(DatabaseHelper.recipesTable);
        sb.append("._id GROUP BY r._id ORDER BY CountMatches DESC, LENGTH(");
        sb.append(DatabaseHelper.recIngredients);
        sb.append(")");

        SQLiteDatabase db = database.getWritableDatabase();

        db.execSQL("DROP VIEW IF EXISTS " + DatabaseHelper.RESULTS_VIEW);
        db.execSQL(sb.toString());
        db.close();
        displayRows = displayRows + DISPLAY_ROWS_INCREASE;
        cursor = database.getResultsViewCursor(displayRows);
        results = cursor.moveToFirst();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        String[] recipe = new String[3];
        cursor.moveToPosition(pos);
        recipe[0]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recName));
        recipe[1]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recIngredients));
        recipe[2]=cursor.getString(cursor.getColumnIndex(DatabaseHelper.recURL));
        Intent intent = new Intent(this, RecipeViewer.class);
        intent.putExtra("com.lcneves.cookme.RECIPE", recipe);
        startActivity(intent);
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
        displayRows = displayRows + DISPLAY_ROWS_INCREASE;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Fetching more recipes...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_dialog_anim));
        mProgressDialog.setCancelable(false);

        if(filter) {
            final FilterResults filterResults = new FilterResults(this);
            filterResults.execute(query);
        } else {
            final ShowMore showMore = new ShowMore(this);
            showMore.execute();
        }
    }

    private class ShowMore extends AsyncTask<String, Integer, String> {

        private Context context;

        public ShowMore(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... args) {
            if(filter) cursor = database.getFilterViewCursor(displayRows, SearchSimple.query);
            else cursor = database.getResultsViewCursor(displayRows);
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

    public void filterResultsAction(String query) {
        displayRows = newQuery ? DISPLAY_ROWS_INCREASE : displayRows + DISPLAY_ROWS_INCREASE;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Filtering results for \""+query+"\"...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_dialog_anim));
        mProgressDialog.setCancelable(false);

        final FilterResults filterResults = new FilterResults(this);
        filterResults.execute(query);
    }

    private class FilterResults extends AsyncTask<String, Integer, String> {

        private Context context;

        public FilterResults(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... args) {
            StringBuilder sb = new StringBuilder("CREATE VIEW ");
            sb.append(DatabaseHelper.RESULTS_VIEW);
            sb.append(" AS SELECT ");
            sb.append(DatabaseHelper.recipesTable);
            sb.append(".*,Count(r._id) as CountMatches FROM (");

            final String QUERY_LIKE_STUB = "SELECT " + DatabaseHelper.recID + " FROM " + DatabaseHelper.recipesTable + " WHERE ";
            sb.append(QUERY_LIKE_STUB);
            sb.append(DatabaseHelper.createWhereClause(recipeName, new String[] {selIngredients[0]}, args[0]));

            for (int i = 1; i < selIngredients.length; ++i) {
                sb.append(" UNION ALL ");
                sb.append(QUERY_LIKE_STUB);
                sb.append(DatabaseHelper.createWhereClause(recipeName, new String[]{selIngredients[i]}, args[0]));
            }

            sb.append(") AS r INNER JOIN ");
            sb.append(DatabaseHelper.recipesTable);
            sb.append(" ON r._id = ");
            sb.append(DatabaseHelper.recipesTable);
            sb.append("._id GROUP BY r._id ORDER BY CountMatches DESC, LENGTH(");
            sb.append(DatabaseHelper.recIngredients);
            sb.append(")");

            SQLiteDatabase db = database.getWritableDatabase();

            db.execSQL("DROP VIEW IF EXISTS " + DatabaseHelper.RESULTS_VIEW);
            db.execSQL(sb.toString());
            db.close();
            cursor = database.getResultsViewCursor(displayRows);
            results = cursor.moveToFirst();
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
            if(newQuery) lv.setSelection(0);
            newQuery = false;
            filter = true;
            mProgressDialog.dismiss();
        }
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
            for(String s : selIngredients) {
                for(int j = -1; (j = ingredientsSpanString.indexOf(s.toLowerCase(Locale.ENGLISH), j + 1)) != -1;) {
                    ingredientsSpan.setSpan(new ForegroundColorSpan(Color.argb(208, 0, 127, 0)), j, j + s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            ingredients.setText(ingredientsSpan, TextView.BufferType.SPANNABLE);
            return view;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_simple, menu);
        MenuItem item = menu.findItem(R.id.action_filter);
        final SearchView filter = (SearchView)item.getActionView();
        filter.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                SearchSimple.query = query.trim();
                newQuery = true;
                final InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(filter.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                filterResultsAction(query.trim());
                return true;
            }
        });
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
        MainActivity.AboutDialogFragment aboutDialog = new MainActivity.AboutDialogFragment();
        aboutDialog.show(getFragmentManager(), "tag");
    }
}
