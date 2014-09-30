package com.lcneves.cookme;

/**
 * Created by lucas on 10.09.14.
 */

        import java.util.Locale;

        import android.content.ContentValues;
        import android.content.Context;
        import android.database.Cursor;
        import android.database.sqlite.SQLiteDatabase;
        import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String dbName="RecipesDB";
    static final String recipesTable="Recipes";
    static final String recID="_id";
    static final String recName="Name";
    static final String recIngredients="Ingredients";
    static final String recURL="URL";
    static final String recLength="Length";

    static final String ingredientsTable="IngredientsTable";
    static final String ingID="_id";
    static final String ingName="IngName";

    static final String shoppingTable="ShoppingTable";
    static final String shoID="_id";
    static final String shoName="ShoName";


    @Override
    public void onCreate(SQLiteDatabase db) {
     //   db.execSQL("DROP TABLE IF EXISTS "+recipesTable);
     //   db.execSQL("DROP TABLE IF EXISTS "+resultsTable);
     //   db.execSQL("CREATE TABLE "+recipesTable+" ("+recID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+recName+" TEXT, "+recIngredients+" TEXT, "+recURL+" TEXT)");
     //   db.execSQL("CREATE TABLE "+resultsTable+" ("+resID+" INTEGER PRIMARY KEY, "+resName+" TEXT, "+resIngredients+" TEXT, "+resURL+" TEXT, "+resMatches+" TEXT, "+resMismatches+" TEXT, "+resMatchCount+" INTEGER, "+resMismatchCount+" INTEGER)");
    }

    public DatabaseHelper(Context context) {
        super(context, dbName, null,33);
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

    }

    public void recreateDatabase() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS "+recipesTable);
        db.execSQL("CREATE TABLE "+recipesTable+" ("+recID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+recName+" TEXT, "+recIngredients+" TEXT, "+recURL+" TEXT, "+recLength+" INTEGER)");
        db.close();
    }

    public void dropRecipes() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS "+recipesTable);
        db.close();
    }

    public void createGroceryList() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ingredientsTable+" ("+ingID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+ingName+" TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+shoppingTable+" ("+shoID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+shoName+" TEXT)");
        db.close();
    }

    public String getUrl(long id) {
        String url = null;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(recipesTable, new String[]{recURL}, recID+" = "+id, null, null, null, null);
        if (cursor.moveToFirst()) {
            url = cursor.getString(cursor.getColumnIndex(recURL));
        }
        cursor.close();
        db.close();
        return url;
    }

    public Cursor displayIngredients() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultsCursor = db.query(ingredientsTable, new String[]{ingID, ingName}, null, null, null, null, null);

        if (resultsCursor != null) {
            resultsCursor.moveToFirst();
        }
        db.close();
        return resultsCursor;
    }

    public Cursor displayShopping() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultsCursor = db.query(shoppingTable, new String[]{shoID, shoName}, null, null, null, null, null);

        if (resultsCursor != null) {
            resultsCursor.moveToFirst();
        }
        db.close();
        return resultsCursor;
    }

    public void addIngredient(String newIngredient) {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues cv=new ContentValues();
        cv.put(ingName, newIngredient);
        db.insert(ingredientsTable, null, cv);
        db.close();
    }

    public void addShopping(String newShopping) {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues cv=new ContentValues();
        cv.put(shoName, newShopping);
        db.insert(shoppingTable, null, cv);
        db.close();
    }

    public void deleteIngredient(String rowID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ingredientsTable, ingID+" = "+rowID, null);
        db.close();
    }

    public void deleteShopping(String rowID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(shoppingTable, shoID+" = "+rowID, null);
        db.close();
    }

    public boolean verifyRecipesTable() {
        SQLiteDatabase db = null;
        try {
            db=this.getWritableDatabase();
            Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+recipesTable+"'", null);
            if(cursor!=null) {
                if(cursor.getCount()>0) {
                    cursor.close();
                    return true;
                }
                cursor.close();
            }
            return false;
        } finally {
            db.close();
        }
    }
}