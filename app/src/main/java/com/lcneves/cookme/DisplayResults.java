package com.lcneves.cookme;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
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

import java.util.HashMap;
import java.util.Locale;


public class DisplayResults extends ListActivity {

    private ListActivity activity = DisplayResults.this;
    ListView lv;
    static final String resMatches="Matches";
    static final String resMismatches="Mismatches";
    int rowCount;
    int displayRows;
    final int DISPLAY_ROWS_INCREASE = 20;
    final String resultsView = DatabaseHelper.resultsTable;
    String[] selIngredients;
    String[] selIngredientsLower;
    DatabaseHelper databaseHelper = new DatabaseHelper(this);
    Cursor cursor;
    ComplexCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_results);
        Intent intent = getIntent();
        rowCount = intent.getIntExtra("com.lcneves.cookme.ROW", 0);
        selIngredients = intent.getStringArrayExtra("com.lcneves.cookme.INGREDIENTS");
        selIngredientsLower = new String[selIngredients.length];
        for (int i = 0; i < selIngredients.length; ++i) selIngredientsLower[i] = selIngredients[i].toLowerCase(Locale.ENGLISH);
        displayRows = rowCount + DISPLAY_ROWS_INCREASE;
        Log.d("com.lcneves.cookme.DisplayResults", "About to get the cursor...");
        cursor = databaseHelper.getResultsViewCursor(displayRows);
        Log.d("com.lcneves.cookme.DisplayResults", "Got the cursor! It says: "+cursor.toString());
        adapter = new ComplexCursorAdapter(
                activity,
                R.layout.list_item_simple,
                cursor,
                new String[] {DatabaseHelper.recName, DatabaseHelper.recIngredients,
                        DatabaseHelper.recURL},
                new int[] { R.id.name, R.id.ingredients, R.id.url},
                0
        );
        Log.d("com.lcneves.cookme.DisplayResults", "Instantiated the adapter...");
        setListAdapter(adapter);
        Log.d("com.lcneves.cookme.DisplayResults", "Set list adapter...");
        lv = getListView();
        Log.d("com.lcneves.cookme.DisplayResults", "Got list view...");
        registerForContextMenu(lv);
        Log.d("com.lcneves.cookme.DisplayResults", "rowCount = "+ rowCount);

        View footerView = ((LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.simple_footer, null, false);
        lv.addFooterView(footerView);
        lv.setSelection(rowCount);
    }

    public void clickShowMore(View v) {
        displayRows = displayRows + DISPLAY_ROWS_INCREASE;
        cursor = databaseHelper.getResultsViewCursor(displayRows);
        adapter.changeCursor(cursor);
        adapter.notifyDataSetChanged();
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
