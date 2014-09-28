package com.lcneves.cookme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends Activity implements View.OnClickListener {

    static String fileNameOld = "recipeitems-latest.json";
    static File fileOld = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileNameOld);
    DatabaseHelper db = new DatabaseHelper(this);
    static boolean databaseCheck = false;
    MyCursorAdapter adapter;
    MyCursorAdapter adapter2;
    ListView lv;
    ListView lv2;
    static List<String> checkedList = new LinkedList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db.createGroceryList();
        setContentView(R.layout.grocery_list);

        lv = (ListView) findViewById(R.id.listview);
        Cursor cursor = db.displayIngredients();
        adapter = new MyCursorAdapter(MainActivity.this, R.layout.ingredients_item, cursor, new String[] { DatabaseHelper.ingID, DatabaseHelper.ingName}, new int[] { R.id.item0, R.id.grocery }, 0);
        lv.setAdapter(adapter);

        lv2 = (ListView) findViewById(R.id.listview2);
        Cursor cursor2 = db.displayShopping();
        adapter2 = new MyCursorAdapter(MainActivity.this, R.layout.shopping_item, cursor2, new String[] { DatabaseHelper.shoID, DatabaseHelper.shoName}, new int[] { R.id.item0, R.id.grocery }, 0);
        lv2.setAdapter(adapter2);

        if(db.verifyRecipesTable() == false && databaseCheck == false) {
            DownloadParseDialogFragment dialogDownloadParse = new DownloadParseDialogFragment();
            dialogDownloadParse.show(getFragmentManager(), "tag");
        }
    }

    @Override
    public void onClick(View view) {

    }

    public void searchRecipes(MenuItem menuItem) {

        if(checkedList.isEmpty()) {
            Toast toast = Toast.makeText(this, "Use checkboxes to select ingredients", Toast.LENGTH_LONG);
            toast.show();
        } else {
            String[] arrayList = new String[checkedList.size()];
            arrayList = checkedList.toArray(arrayList);
            Intent intent = new Intent(this, SearchResults.class);
            intent.putExtra("com.lcneves.cookme.INGREDIENTS", arrayList);
            startActivity(intent);
        }
    }

    public void clickSearchByName(MenuItem menuitem) {
        RecipeNameDialogFragment recipeNameDialog = new RecipeNameDialogFragment();
        recipeNameDialog.show(getFragmentManager(), "tag");
    }

    public void clickSearchComposite(MenuItem menuitem) {
        CompositeDialogFragment compositeDialog = new CompositeDialogFragment();
        compositeDialog.show(getFragmentManager(), "tag");
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

    public static class RecipeNameDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final EditText input = new EditText(context);
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("What recipe are you looking for?")
                    .setView(input)
                    .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if(input.getText().toString().isEmpty()) {
                                Toast toast = Toast.makeText(context, "Recipe name cannot be blank", Toast.LENGTH_LONG);
                                toast.show();
                            } else {
                                Intent intent = new Intent(context, SearchResults.class);
                                intent.putExtra("com.lcneves.cookme.RECIPENAME", input.getText().toString());
                                startActivity(intent);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    public static class CompositeDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final Context context = getActivity();
            final EditText input = new EditText(context);

            if(checkedList.isEmpty()) {
                Toast toast = Toast.makeText(context, "Use checkboxes to select ingredients", Toast.LENGTH_LONG);
                toast.show();
                this.dismiss();
            }
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("What recipe are you looking for?")
                    .setView(input)
                    .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if(input.getText().toString().isEmpty()) {
                                Toast toast = Toast.makeText(context, "Recipe name cannot be blank", Toast.LENGTH_LONG);
                                toast.show();
                            } else {
                                String[] arrayList = new String[checkedList.size()];
                                arrayList = checkedList.toArray(arrayList);
                                Intent intent = new Intent(context, SearchResults.class);
                                intent.putExtra("com.lcneves.cookme.RECIPENAME", input.getText().toString());
                                intent.putExtra("com.lcneves.cookme.INGREDIENTS", arrayList);
                                startActivity(intent);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
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
                            databaseCheck = true;
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private class MyCursorAdapter extends SimpleCursorAdapter {

        public MyCursorAdapter(Context context, int layout, Cursor c,
                               String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //get reference to the row
            View view = super.getView(position, convertView, parent);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            TextView item = (TextView) view.findViewById(R.id.grocery);
            String row_item = item.getText().toString();
            if (checkedList.contains(row_item)) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
            return view;
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
        /*if (id == R.id.action_search) {
            searchRecipes(checkedList);
            return true;
        }*/
        if (id == R.id.action_clearDb) {
            db.dropRecipes();
            Intent intent = new Intent(MainActivity.this, JSONHelper.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*public void showPopup(MenuItem v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.search_menu);
        popup.show();
    }*/

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
}
