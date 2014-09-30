package com.lcneves.cookme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;


public class DisplayResults extends ListActivity {

    private ListActivity activity = DisplayResults.this;
    ListView lv;
    static final String resMatches="Matches";
    static final String resMismatches="Mismatches";
    int cursorCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_results);
        Intent intent = getIntent();
        cursorCount = intent.getIntExtra("com.lcneves.cookme.ROW", 0);

        SimpleAdapter adapter = new SimpleAdapter(
                activity,
                SearchResults.list,
                R.layout.list_item,
                new String[] {DatabaseHelper.recName, DatabaseHelper.recIngredients,
                        DatabaseHelper.recURL, resMatches,
                        resMismatches },
                new int[] { R.id.name, R.id.ingredients, R.id.url, R.id.matches, R.id.mismatches }
        );
        setListAdapter(adapter);
        lv = getListView();
        Log.d("com.lcneves.cookme.DisplayResults", "cursorCount = "+cursorCount);
        getListView().post(new Runnable() {
            @Override
            public void run() {
                getListView().setSelection(cursorCount);
            }
        });
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

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        HashMap<String, String> map = SearchResults.list.get(pos);
        String url=map.get(SearchResults.recURL);
        Intent intent = new Intent(this, RecipeViewer.class);
        intent.putExtra("com.lcneves.cookme.URL", url);
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
}
