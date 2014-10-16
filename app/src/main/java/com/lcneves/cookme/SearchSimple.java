package com.lcneves.cookme;

import android.app.Activity;
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
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
    static String[] selIngredients;
    String[] selIngredientsLower;
    static String recipeName;
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
        selIngredientsLower = new String[selIngredients.length];
        for (int i = 0; i < selIngredients.length; ++i) selIngredientsLower[i] = selIngredients[i].toLowerCase(Locale.ENGLISH);
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
            if (selIngredients != null) {
                cursor = db.query(recipesTable, new String[] {recID, recName, recIngredients, recURL}, whereCondition, null, null, null, "LENGTH("+recIngredients+")");
            } else {
                cursor = db.query(recipesTable, new String[] {recID, recName, recIngredients, recURL}, whereCondition, null, null, null, null);
            }
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
                ComplexCursorAdapter adapter = new ComplexCursorAdapter(activity,
                        R.layout.list_item_simple,
                        cursor,
                        new String[]{recName, recIngredients, recURL},
                        new int[]{R.id.name, R.id.ingredients, R.id.url},
                        0);
                setListAdapter(adapter);
                lv = getListView();
                registerForContextMenu(lv);
                if(selIngredients != null) {
                    if(selIngredients.length > 2) {
                        View footerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.simple_footer, null, false);
                        lv.addFooterView(footerView);
                    }
                }
            } else {
                if(selIngredients != null) {
                    if(selIngredients.length > 2) {
                        SearchMoreDialogFragment searchMoreDialog = new SearchMoreDialogFragment();
                        searchMoreDialog.show(getFragmentManager(), "tag");
                    } else {
                        Toast toast;
                        if (recipeName != null) {
                            if(selIngredients.length == 1) {
                                toast = Toast.makeText(SearchSimple.this, "No recipes for \""+recipeName+"\" found using \""+selIngredients[0]+"\"", Toast.LENGTH_LONG);
                            } else {
                                toast = Toast.makeText(SearchSimple.this, "No recipes for \""+recipeName+"\" found using \""+selIngredients[0]+"\" and \""+selIngredients[1]+"\"", Toast.LENGTH_LONG);
                            }
                            toast.show();
                            Intent intent = new Intent(SearchSimple.this, MainActivity.class);
                            startActivity(intent);
                        } else {
                            if(selIngredients.length == 1) {
                                toast = Toast.makeText(SearchSimple.this, "No recipes found using \""+selIngredients[0]+"\"", Toast.LENGTH_LONG);
                            } else {
                                toast = Toast.makeText(SearchSimple.this, "No recipes found using \""+selIngredients[0]+"\" and \""+selIngredients[1]+"\"", Toast.LENGTH_LONG);
                            }
                            toast.show();
                            Intent intent = new Intent(SearchSimple.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                } else {
                    Toast toast = Toast.makeText(SearchSimple.this, "No recipes found with the name \""+recipeName+"\"", Toast.LENGTH_LONG);
                    toast.show();
                    Intent intent = new Intent(SearchSimple.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        }
    }


    public static class SearchMoreDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("No recipes found with all the selected ingredients. Do you want to search for recipes that use only some of your ingredients?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(getActivity(), SearchResults.class);
                            intent.putExtra("com.lcneves.cookme.RECIPENAME", recipeName);
                            intent.putExtra("com.lcneves.cookme.INGREDIENTS", selIngredients);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                        }
                    });
            return builder.create();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        DatabaseHelper db = new DatabaseHelper(this);
        String[] recipe = db.getRecipe(id);
        db.close();
        Intent intent = new Intent(this, RecipeViewer.class);
        intent.putExtra("com.lcneves.cookme.RECIPE", recipe);
        intent.putExtra("com.lcneves.cookme.ACTIVITY", "simple");
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
//        menu.setHeaderTitle(Countries[info.position]);
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
        DatabaseHelper db = new DatabaseHelper(this);
        String[] recipe = db.getRecipe(info.id);
        db.close();
        switch (menuItemIndex) {
            case 0:
                Intent intent = new Intent(this, RecipeViewer.class);
                intent.putExtra("com.lcneves.cookme.RECIPE", recipe);
                intent.putExtra("com.lcneves.cookme.ACTIVITY", "simple");
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
        Intent intent = new Intent(SearchSimple.this, SearchResults.class);
        intent.putExtra("com.lcneves.cookme.RECIPENAME", recipeName);
        intent.putExtra("com.lcneves.cookme.INGREDIENTS", selIngredients);
        intent.putExtra("com.lcneves.cookme.ROW", cursorCount);
        startActivity(intent);
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
        MainActivity.AboutDialogFragment aboutDialog = new MainActivity.AboutDialogFragment();
        aboutDialog.show(getFragmentManager(), "tag");
    }
}
