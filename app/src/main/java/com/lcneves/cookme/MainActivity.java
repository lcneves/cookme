package com.lcneves.cookme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends Activity implements View.OnClickListener {

    static String fileNameOld = "recipeitems-latest.json";
    static File fileOld = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileNameOld);
    DatabaseHelper db = new DatabaseHelper(this);

    SimpleCursorAdapter adapter;
    SimpleCursorAdapter adapter2;
    ListView lv;
    ListView lv2;
    List<String> checkedList = new LinkedList<String>();

    DownloadParseDialogFragment dialogDownloadParse = new DownloadParseDialogFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db.createGroceryList();
        setContentView(R.layout.grocery_list);

        lv = (ListView) findViewById (R.id.listview);
        Cursor cursor = db.displayIngredients();
        adapter = new SimpleCursorAdapter(MainActivity.this, R.layout.ingredients_item, cursor, new String[] { DatabaseHelper.ingID, DatabaseHelper.ingName}, new int[] { R.id.item0, R.id.item1 }, 0);
        lv.setAdapter(adapter);

        lv2 = (ListView) findViewById(R.id.listview2);
        Cursor cursor2 = db.displayShopping();
        adapter2 = new SimpleCursorAdapter(MainActivity.this, R.layout.shopping_item, cursor2, new String[] { DatabaseHelper.shoID, DatabaseHelper.shoName}, new int[] { R.id.item0, R.id.item1 }, 0);
        lv2.setAdapter(adapter2);

        if(db.verifyRecipesTable() == false) {
            dialogDownloadParse.show(getFragmentManager(), "tag");
        }
    }

    @Override
    public void onClick(View view) {

    }

    public void searchRecipes(List<String> list) {
        if(list.isEmpty()) return; else {
            String[] arrayList = new String[list.size()];
            arrayList = list.toArray(arrayList);
            Intent intent = new Intent(this, SearchResults.class);
            intent.putExtra("com.lcneves.cookme.INGREDIENTS", arrayList);
            startActivity(intent);
        }
    }

    public void CheckBoxClick(View v) {
        CheckBox checkBox = (CheckBox) v;
        LinearLayout llMain = (LinearLayout)v.getParent();
        TextView item=(TextView)llMain.getChildAt(2);
        String row_item=item.getText().toString();
        if(checkBox.isChecked()) {
            if(!checkedList.contains(row_item)) {
                checkedList.add(row_item);
            }
        } else {
            if(checkedList.contains(row_item)) {
                checkedList.remove(row_item);
            }
        }
    }

    public void OnAddingIngredients(View v){
        EditText editText = (EditText) findViewById(R.id.editText);
        String newIngredient = editText.getText().toString();
        if(newIngredient.length() > 1) {
            db.addIngredient(newIngredient);
            editText.setText("");
            Cursor newCursor = db.displayIngredients();
            adapter.changeCursor(newCursor);
            adapter.notifyDataSetChanged();
        }
    }

    public void OnAddingShopping(View v){
        EditText editText = (EditText) findViewById(R.id.editText2);
        String newShopping = editText.getText().toString();
        if(newShopping.length() > 1) {
            db.addShopping(newShopping);
            editText.setText("");
            Cursor newCursor2 = db.displayShopping();
            adapter2.changeCursor(newCursor2);
            adapter2.notifyDataSetChanged();
        }
    }

    public void DeleteRowIngredients(View v){
        LinearLayout llMain = (LinearLayout)v.getParent();
        TextView row=(TextView)llMain.getChildAt(0);
        String row_no=row.getText().toString();
        db.deleteIngredient(row_no);
        Cursor newCursor = db.displayIngredients();
        adapter.changeCursor(newCursor);
        adapter.notifyDataSetChanged();
    }

    public void DeleteRowShopping(View v){
        LinearLayout llMain = (LinearLayout)v.getParent();
        TextView row=(TextView)llMain.getChildAt(0);
        String row_no=row.getText().toString();
        db.deleteShopping(row_no);
        Cursor newCursor2 = db.displayShopping();
        adapter2.changeCursor(newCursor2);
        adapter2.notifyDataSetChanged();
    }

    public void MoveRowIngredients(View v){
        LinearLayout llMain = (LinearLayout)v.getParent();
        TextView row=(TextView)llMain.getChildAt(0);
        TextView item=(TextView)llMain.getChildAt(2);
        String row_no=row.getText().toString();
        String row_item=item.getText().toString();
        db.deleteIngredient(row_no);
        db.addShopping(row_item);
        Cursor newCursor = db.displayIngredients();
        adapter.changeCursor(newCursor);
        adapter.notifyDataSetChanged();
        Cursor newCursor2 = db.displayShopping();
        adapter2.changeCursor(newCursor2);
        adapter2.notifyDataSetChanged();
    }

    public void MoveRowShopping(View v){
        LinearLayout llMain = (LinearLayout)v.getParent();
        TextView row=(TextView)llMain.getChildAt(0);
        TextView item=(TextView)llMain.getChildAt(2);
        String row_no=row.getText().toString();
        String row_item=item.getText().toString();
        db.deleteShopping(row_no);
        db.addIngredient(row_item);
        Cursor newCursor = db.displayIngredients();
        adapter.changeCursor(newCursor);
        adapter.notifyDataSetChanged();
        Cursor newCursor2 = db.displayShopping();
        adapter2.changeCursor(newCursor2);
        adapter2.notifyDataSetChanged();
    }

    public static class DownloadParseDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Database empty. Download and parse recipes?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(getActivity(), JSONHelper.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        if (id == R.id.action_search) {
            searchRecipes(checkedList);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public class Debug extends Exception {
        public Debug(String message){
            super(message);
        }
    }
}
