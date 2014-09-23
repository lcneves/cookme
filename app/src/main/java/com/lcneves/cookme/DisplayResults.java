package com.lcneves.cookme;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class DisplayResults extends ListActivity {

    private ListActivity activity = DisplayResults.this;
    ListView lv;
    DatabaseHelper database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_results);
        database = new DatabaseHelper(DisplayResults.this);
        Cursor cursor = database.displayResults();

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity, R.layout.list_item, cursor, new String[] { DatabaseHelper.resName, DatabaseHelper.resIngredients, DatabaseHelper.resURL, DatabaseHelper.resMatches, DatabaseHelper.resMismatches }, new int[] { R.id.name, R.id.ingredients, R.id.url, R.id.matches, R.id.mismatches }, 0);
        setListAdapter(adapter);
        lv = getListView();
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        Intent intent = new Intent(this, RecipeViewer.class);
        intent.putExtra("com.lcneves.cookme.URL", database.getUrl(id));
        startActivity(intent);
    }
}
