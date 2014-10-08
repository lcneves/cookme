package com.lcneves.cookme;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends Activity implements GestureDetector.OnGestureListener {

    DatabaseHelper db = new DatabaseHelper(this);
    static boolean databaseCheck = false;
    SimpleCursorAdapter adapter;
    SimpleCursorAdapter adapter2;
    ListView lv;
    ListView lv2;
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
        adapter = new SimpleCursorAdapter(MainActivity.this, R.layout.ingredients_item, cursor, new String[] { DatabaseHelper.ingID, DatabaseHelper.ingName}, new int[] { R.id.item0, R.id.grocery }, 0);
        lv.setAdapter(adapter);

        lv2 = (ListView) findViewById(R.id.listview2);
        cursor2 = db.displayShopping();
        adapter2 = new SimpleCursorAdapter(MainActivity.this, R.layout.shopping_item, cursor2, new String[] { DatabaseHelper.shoID, DatabaseHelper.shoName}, new int[] { R.id.item0, R.id.grocery }, 0);
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
                                    String rowId = String.valueOf(adapter.getItemId(position));
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
                                    String rowId = String.valueOf(adapter2.getItemId(position));
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

        if(!db.verifyRecipesTable() && !databaseCheck) {
            DownloadParseDialogFragment dialogDownloadParse = new DownloadParseDialogFragment();
            dialogDownloadParse.show(getFragmentManager(), "tag");
        }
    }

    public void checkBoxClick(View v) {
        SearchDialogFragment.checkBoxClick(v);
    }

    public static class SearchDialogFragment extends DialogFragment {

        Activity context;
        static List<String> checkedList;

        public static void checkBoxClick(View v) {
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

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            context=activity;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            checkedList = new LinkedList<String>();
            setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.search_dialog, container, false);
            DatabaseHelper dbSearch = new DatabaseHelper(context);
            ListView lvSearch = (ListView) v.findViewById(R.id.listview_fridge);
            final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            final EditText input = (EditText) v.findViewById(R.id.editTextSearch);
            input.clearFocus();
            Cursor cursorSearch = dbSearch.displayIngredients();
            MyCursorAdapter adapterSearch = new MyCursorAdapter(context, R.layout.search_item, cursorSearch, new String[] { DatabaseHelper.ingID, DatabaseHelper.ingName}, new int[] { R.id.item0, R.id.grocery }, 0);
            lvSearch.setAdapter(adapterSearch);
            ListView lvSearch2 = (ListView) v.findViewById(R.id.listview_shopping);
            Cursor cursorSearch2 = dbSearch.displayShopping();
            MyCursorAdapter adapterSearch2 = new MyCursorAdapter(context, R.layout.search_item, cursorSearch2, new String[] { DatabaseHelper.shoID, DatabaseHelper.shoName}, new int[] { R.id.item0, R.id.grocery }, 0);
            lvSearch2.setAdapter(adapterSearch2);
            Button buttonSearch = (Button)v.findViewById(R.id.search_button);
            buttonSearch.setOnClickListener(new View.OnClickListener() {


                public void onClick(View v) {
                    boolean ingredients = false;
                    boolean name = false;
                    Intent intent = new Intent(context, SearchSimple.class);
                    imm.hideSoftInputFromWindow(input.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    if(!checkedList.isEmpty()) {
                        ingredients = true;
                        String[] arrayList = checkedList.toArray(new String[(checkedList.size())]);
                        intent.putExtra("com.lcneves.cookme.INGREDIENTS", arrayList);
                    }
                    if (!input.getText().toString().trim().isEmpty()) {
                        name = true;
                        intent.putExtra("com.lcneves.cookme.RECIPENAME", input.getText().toString().trim());
                    }
                    if (ingredients || name) {
                        startActivity(intent);
                        dismiss();
                    }
                    else {
                        Toast toast = Toast.makeText(context, "Please fill in recipe name or choose ingredients", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            });

            Button buttonCancel = (Button)v.findViewById(R.id.search_cancel);
            buttonCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    imm.hideSoftInputFromWindow(input.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    dismiss();
                }
            });
            return v;
        }
    }

    public static class AboutDialogFragment extends DialogFragment {

        Activity context;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            context=activity;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View messageView = inflater.inflate(R.layout.about, container, false);
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
            /*AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(messageView);
            builder.create();
            builder.show();*/
            Button buttonRate = (Button)messageView.findViewById(R.id.rate_button);
            buttonRate.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        context.startActivity(goToMarket);
                    } catch (Exception e) {
                        Log.e("com.lcneves.cookme.MainActivity",Log.getStackTraceString(e));
                    }
                }
            });
            Button buttonCancel = (Button)messageView.findViewById(R.id.close_button);
            buttonCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dismiss();
                }
            });
            return messageView;
        }
    }

    public void clickSearch(MenuItem menuitem) {
        SearchDialogFragment searchDialog = new SearchDialogFragment();
        searchDialog.show(getFragmentManager(), "tag");
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
        TextView item=(TextView)llMain.getChildAt(1);
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
        TextView item=(TextView)llMain.getChildAt(1);
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

    public static class DownloadParseDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
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
            return builder.create();
        }
    }

    public static class UpdateDBDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
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
            return builder.create();
        }
    }

    private static class MyCursorAdapter extends SimpleCursorAdapter {

        public MyCursorAdapter(Context context, int layout, Cursor c,
                               String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
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
        if (id == R.id.action_clearDb) {
            UpdateDBDialogFragment updateDBDialog = new UpdateDBDialogFragment();
            updateDBDialog.show(getFragmentManager(), "tag");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clickAboutMenuMain(MenuItem menu) {
        AboutDialogFragment aboutDialog = new AboutDialogFragment();
        aboutDialog.show(getFragmentManager(), "tag");
    }

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
