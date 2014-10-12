package com.lcneves.cookme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
        registerForContextMenu(lv);
        Log.d("com.lcneves.cookme.DisplayResults", "cursorCount = "+cursorCount);
        getListView().post(new Runnable() {
            @Override
            public void run() {
                getListView().setSelection(cursorCount);
            }
        });
        if(SearchResults.selIngredients != null) {
            if((SearchResults.selIngredients.length - SearchResults.selMaxMismatches) > 1) {
                View footerView = ((LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.simple_footer, null, false);
                lv.addFooterView(footerView);
            }
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
        HashMap<String, String> map = SearchResults.list.get(info.position);
        recipe[0]=map.get(SearchResults.recName);
        recipe[1]=map.get(SearchResults.recIngredients);
        recipe[2]=map.get(SearchResults.recURL);
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

    public void clickShowMore(View v) {
        Intent intent = new Intent(DisplayResults.this, SearchResults.class);
        intent.putExtra("com.lcneves.cookme.RECIPENAME", SearchResults.recipeName);
        intent.putExtra("com.lcneves.cookme.INGREDIENTS", SearchResults.selIngredients);
        intent.putExtra("com.lcneves.cookme.ROW", SearchResults.list.size());
        intent.putExtra("com.lcneves.cookme.MAX_MISMATCHES", (SearchResults.selMaxMismatches + 1));
        startActivity(intent);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        String[] recipe = new String[3];
        HashMap<String, String> map = SearchResults.list.get(pos);
        recipe[0]=map.get(SearchResults.recName);
        recipe[1]=map.get(SearchResults.recIngredients);
        recipe[2]=map.get(SearchResults.recURL);
        Log.d("com.lcneves.cookme.DisplayResults", "Clicked! Recipe name is "+recipe[0]);
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
}
