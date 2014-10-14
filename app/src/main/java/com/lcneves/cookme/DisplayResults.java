package com.lcneves.cookme;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.HashMap;


public class DisplayResults extends ListActivity {

    private ListActivity activity = DisplayResults.this;
    ListView lv;
    static final String resMatches="Matches";
    static final String resMismatches="Mismatches";
    int rowCount;
    final String resultsView = "resultsView";
    final String recID = DatabaseHelper.recID;
    DatabaseHelper databaseHelper = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_results);
        Intent intent = getIntent();
        rowCount = intent.getIntExtra("com.lcneves.cookme.ROW", 0);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        String[] IDs = new String[rowCount];
        for(int i = 0; i < rowCount; i++) {
            IDs[i] = SearchResults.list.get(i).get(recID);
        }
        database.execSQL("DROP VIEW IF EXISTS "+resultsView);
        database.execSQL("CREATE VIEW "+resultsView+" AS SELECT "+DatabaseHelper.recID+","+DatabaseHelper.recName+","+DatabaseHelper.recIngredients+","+DatabaseHelper.recURL+" FROM "+DatabaseHelper.recipesTable+" WHERE "+DatabaseHelper.recID+" IN "+makePlaceholders(rowCount), IDs);
        Cursor cursor = database.query(resultsView,
                new String[] {DatabaseHelper.recID,DatabaseHelper.recName,DatabaseHelper.recIngredients,DatabaseHelper.recURL},
                null, null, null, null, null);
        ComplexCursorAdapter adapter = new ComplexCursorAdapter(
                activity,
                R.layout.list_item_simple,
                cursor,
                new String[] {DatabaseHelper.recName, DatabaseHelper.recIngredients,
                        DatabaseHelper.recURL},
                new int[] { R.id.name, R.id.ingredients, R.id.url},
                0
        );
        setListAdapter(adapter);
        lv = getListView();
        registerForContextMenu(lv);
        Log.d("com.lcneves.cookme.DisplayResults", "rowCount = "+ rowCount);
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
        recipe[0]=map.get(DatabaseHelper.recName);
        recipe[1]=map.get(DatabaseHelper.recIngredients);
        recipe[2]=map.get(DatabaseHelper.recURL);
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
        HashMap<String, String> map = SearchResults.list.get(pos);
        recipe[0]=map.get(DatabaseHelper.recName);
        recipe[1]=map.get(DatabaseHelper.recIngredients);
        recipe[2]=map.get(DatabaseHelper.recURL);
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

    String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    private static class ComplexCursorAdapter extends SimpleCursorAdapter {

        public ComplexCursorAdapter(Context context, int layout, Cursor c,
                               String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

       /* @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = super.getView(position, convertView, parent);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            TextView item = (TextView) view.findViewById(R.id.grocery);
            String row_item = item.getText().toString();
            if (SearchDialogFragment.checkedList.contains(row_item)) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
            return view;
        }*/
    }
}
