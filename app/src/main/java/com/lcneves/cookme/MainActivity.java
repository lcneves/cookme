package com.lcneves.cookme;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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


public class MainActivity extends Activity implements GestureDetector.OnGestureListener {

    static String fileNameOld = "recipeitems-latest.json";
    static File fileOld = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileNameOld);
    DatabaseHelper db = new DatabaseHelper(this);
    static boolean databaseCheck = false;
    MyCursorAdapter adapter;
    MyCursorAdapter adapter2;
    ListView lv;
    ListView lv2;
    static List<String> checkedList = new LinkedList<String>();
    LinearLayout ingredientsLayout;
    static Cursor cursor;
    static Cursor cursor2;

    GestureDetector gestureDetector;
    private boolean isIngredients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db.createGroceryList();
        setContentView(R.layout.grocery_list_vertical);

        gestureDetector = new GestureDetector(this, this);
        View shoppingHeader = findViewById(R.id.shoppingHeader);
        shoppingHeader.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                isIngredients = false;
                return gestureDetector.onTouchEvent(event);
            }
        });
        View ingredientsHeader = findViewById(R.id.ingredientsHeader);
        ingredientsHeader.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                isIngredients = true;
                return gestureDetector.onTouchEvent(event);
            }
        });

        ingredientsLayout = (LinearLayout) this.findViewById(R.id.ingredientsLayout);

        lv = (ListView) findViewById(R.id.listview);
        cursor = db.displayIngredients();
        adapter = new MyCursorAdapter(MainActivity.this, R.layout.ingredients_item, cursor, new String[] { DatabaseHelper.ingID, DatabaseHelper.ingName}, new int[] { R.id.item0, R.id.grocery }, 0);
        lv.setAdapter(adapter);

        lv2 = (ListView) findViewById(R.id.listview2);
        cursor2 = db.displayShopping();
        adapter2 = new MyCursorAdapter(MainActivity.this, R.layout.shopping_item, cursor2, new String[] { DatabaseHelper.shoID, DatabaseHelper.shoName}, new int[] { R.id.item0, R.id.grocery }, 0);
        lv2.setAdapter(adapter2);

        // Create a ListView-specific touch listener. ListViews are given special treatment because
        // by default they handle touches for their list items... i.e. they're in charge of drawing
        // the pressed state (the list selector), handling list item clicks, etc.
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        lv,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    cursor = db.displayIngredients();
                                    String rowId = String.valueOf(adapter.getItemId(position));
                                    cursor.moveToPosition(position);
                                    String row_item = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ingName));
                                    if (checkedList.contains(row_item))
                                        checkedList.remove(row_item);
                                    db.deleteIngredient(rowId);
                                }
                                cursor = db.displayIngredients();
                                adapter.changeCursor(cursor);
                                adapter.notifyDataSetChanged();
                            }
                        });
        lv.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        lv.setOnScrollListener(touchListener.makeScrollListener());

        // Create a ListView-specific touch listener. ListViews are given special treatment because
        // by default they handle touches for their list items... i.e. they're in charge of drawing
        // the pressed state (the list selector), handling list item clicks, etc.
        SwipeDismissListViewTouchListener touchListener2 =
                new SwipeDismissListViewTouchListener(
                        lv2,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    cursor2 = db.displayShopping();
                                    String rowId = String.valueOf(adapter2.getItemId(position));
                                    cursor2.moveToPosition(position);
                                    String row_item = cursor2.getString(cursor2.getColumnIndex(DatabaseHelper.shoName));
                                    if (checkedList.contains(row_item))
                                        checkedList.remove(row_item);
                                    db.deleteShopping(rowId);
                                }
                                cursor2 = db.displayShopping();
                                adapter2.changeCursor(cursor2);
                                adapter2.notifyDataSetChanged();
                            }
                        });
        lv2.setOnTouchListener(touchListener2);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        lv2.setOnScrollListener(touchListener2.makeScrollListener());

        if(db.verifyRecipesTable() == false && databaseCheck == false) {
            DownloadParseDialogFragment dialogDownloadParse = new DownloadParseDialogFragment();
            dialogDownloadParse.show(getFragmentManager(), "tag");
        }
    }

    public void searchRecipes(MenuItem menuItem) {

        if(checkedList.isEmpty()) {
            Toast toast = Toast.makeText(this, "Use checkboxes to select ingredients", Toast.LENGTH_LONG);
            toast.show();
        } else {
            String[] arrayList = new String[checkedList.size()];
            arrayList = checkedList.toArray(arrayList);
            Intent intent = new Intent(this, SearchSimple.class);
            intent.putExtra("com.lcneves.cookme.INGREDIENTS", arrayList);
            startActivity(intent);
        }
    }

    public static class SearchDialogFragment extends DialogFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.search_dialog, container, false);
            Activity activity = (Activity)v.getContext();
            DatabaseHelper dbSearch = new DatabaseHelper(activity);
            ListView lvSearch = (ListView) v.findViewById(R.id.listview_fridge);
            Cursor cursorSearch = dbSearch.displayIngredients();
            MyCursorAdapter adapterSearch = new MyCursorAdapter(activity, R.layout.search_item, cursorSearch, new String[] { DatabaseHelper.ingID, DatabaseHelper.ingName}, new int[] { R.id.item0, R.id.search_ingredient }, 0);
            lvSearch.setAdapter(adapterSearch);
            ListView lvSearch2 = (ListView) v.findViewById(R.id.listview_shopping);
            Cursor cursorSearch2 = dbSearch.displayShopping();
            MyCursorAdapter adapterSearch2 = new MyCursorAdapter(activity, R.layout.search_item, cursorSearch2, new String[] { DatabaseHelper.shoID, DatabaseHelper.shoName}, new int[] { R.id.item0, R.id.search_ingredient }, 0);
            lvSearch2.setAdapter(adapterSearch2);
            /*View tv = v.findViewById(R.id.text);
            ((TextView)tv).setText("Dialog #" + mNum + ": using style "
                    + getNameForNum(mNum));*/

            // Watch for button clicks.
            Button buttonSearch = (Button)v.findViewById(R.id.search_button);
            buttonSearch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // When button is clicked, call up to owning activity.
                }
            });

            Button buttonCancel = (Button)v.findViewById(R.id.search_cancel);
            buttonCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                }
            });
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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
        String newIngredient = editText.getText().toString().trim();
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
        String newShopping = editText.getText().toString().trim();
        if(newShopping.length() > 1) {
            db.addShopping(newShopping);
            editText.setText("");
            Cursor newCursor2 = db.displayShopping();
            adapter2.changeCursor(newCursor2);
            adapter2.notifyDataSetChanged();
        }
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
        Toast toast = Toast.makeText(this, "\'"+row_item+"\' moved to shopping list", Toast.LENGTH_SHORT);
        toast.show();
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
        Toast toast = Toast.makeText(this, "\'"+row_item+"\' moved to fridge", Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if(isIngredients) expandIngredients(null);
        else expandShopping(null);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
        float verticalDiff = motionEvent2.getY() - motionEvent.getY();
        if(verticalDiff<0)
            expandShopping(null);
        else expandIngredients(null);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
        float verticalDiff = motionEvent2.getY() - motionEvent.getY();
        if(verticalDiff<0)
            expandShopping(null);
        else expandIngredients(null);
        return true;
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
                            if (input.getText().toString().isEmpty()) {
                                Toast toast = Toast.makeText(context, "Recipe name cannot be blank", Toast.LENGTH_LONG);
                                toast.show();
                            } else {
                                Intent intent = new Intent(context, SearchSimple.class);
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

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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
                                Intent intent = new Intent(context, SearchSimple.class);
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

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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

    public static class UpdateDBDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Do you want to download and import the newest recipes database?")
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
        /*
        if (id == R.id.action_settings) {
            return true;
        }*/
        /*if (id == R.id.action_search) {
            searchRecipes(checkedList);
            return true;
        }*/
        if (id == R.id.action_clearDb) {
            UpdateDBDialogFragment updateDBDialog = new UpdateDBDialogFragment();
            updateDBDialog.show(getFragmentManager(), "tag");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clickAboutMenuMain(MenuItem menu) {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
        TextView par1 = (TextView) messageView.findViewById(R.id.aboutPar1);
        TextView par4 = (TextView) messageView.findViewById(R.id.aboutPar4);
        TextView par5 = (TextView) messageView.findViewById(R.id.aboutPar5);
        TextView par6 = (TextView) messageView.findViewById(R.id.aboutPar6);
        TextView par7 = (TextView) messageView.findViewById(R.id.aboutPar7);
        Linkify.addLinks(par1, Linkify.WEB_URLS);
        Linkify.addLinks(par4, Linkify.WEB_URLS);
        Linkify.addLinks(par5, Linkify.WEB_URLS);
        Linkify.addLinks(par6, Linkify.WEB_URLS);
        Linkify.addLinks(par7, Linkify.WEB_URLS);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    /*public void showPopup(MenuItem v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.search_menu);
        popup.show();
    }*/

    public void expandIngredients (View v) {
        float ingredientsWeight = ((LinearLayout.LayoutParams) ingredientsLayout.getLayoutParams()).weight;
        if(ingredientsWeight == 0) {
            ValueAnimator anim = ValueAnimator.ofFloat(ingredientsWeight, 1);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float val = (Float) valueAnimator.getAnimatedValue();
                    ingredientsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0,
                            val));
                }
            });
            anim.setDuration(500);
            anim.start();
        }
    }

    public void expandShopping (View v) {
        float ingredientsWeight = ((LinearLayout.LayoutParams) ingredientsLayout.getLayoutParams()).weight;
        Log.d("com.lcneves.cookme.MainActivity", "ingredientsWeight ="+ingredientsWeight);
        if(ingredientsWeight == 1) {
            ValueAnimator anim = ValueAnimator.ofFloat(ingredientsWeight, 0);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float val = (Float) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = ingredientsLayout.getLayoutParams();
                    ingredientsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0,
                            val));
                }
            });
            anim.setDuration(500);
            anim.start();
        }
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
}
