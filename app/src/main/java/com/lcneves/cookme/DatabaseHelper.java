package com.lcneves.cookme;

/**
 * Created by lucas on 10.09.14.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String DB_NAME = "RecipesDB";
    static final String RECIPES_TABLE = "Recipes";
    static final String REC_ID = "_id";
    static final String REC_NAME = "Name";
    static final String REC_INGREDIENTS = "Ingredients";
    static final String REC_URL = "URL";

    static final String INGREDIENTS_TABLE = "IngredientsTable";
    static final String ING_ID = "_id";
    static final String ING_NAME = "IngName";

    static final String SHOPPING_TABLE = "ShoppingTable";
    static final String SHO_ID = "_id";
    static final String SHO_NAME = "ShoName";

    static final String RESULTS_VIEW = "ResultsView";

    public static String createWhereClause(String name, String[] ingredients) {
        StringBuilder sb = new StringBuilder(REC_NAME + " LIKE '%" + name + "%'");

        if (ingredients.length > 0) {
            sb.append(" AND (" + REC_INGREDIENTS + " LIKE '%" + ingredients[0] + "%'");
            for (int i = 1; i < ingredients.length; ++i) sb.append(" AND " + REC_INGREDIENTS + " LIKE '%" + ingredients[i] + "%'");
            sb.append(")");
        }

        return sb.toString();
    }

    public static String createWhereClause(String name, String[] ingredients, String query) {
        StringBuilder sb = new StringBuilder(REC_NAME + " LIKE '%" + name + "%'");

        if (ingredients.length > 0) {
            sb.append(" AND (" + REC_INGREDIENTS + " LIKE '%" + ingredients[0] + "%'");
            for (int i = 1; i < ingredients.length; ++i) sb.append(" AND " + REC_INGREDIENTS + " LIKE '%" + ingredients[i] + "%'");
            sb.append(")");
        }
        sb.append(" AND (" + REC_NAME + " LIKE '%" + query + "%' OR " + REC_INGREDIENTS + " LIKE '%" + query + "%')");

        return sb.toString();
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 33);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}


    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {}

    public void recreateDatabase() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + RECIPES_TABLE);
        db.execSQL("CREATE TABLE "+ RECIPES_TABLE +" ("+ REC_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "+ REC_NAME +" TEXT, "+ REC_INGREDIENTS +" TEXT, "+ REC_URL +" TEXT)");
        db.close();
    }

    public Cursor getResultsViewCursor(int displayRows) {
        SQLiteDatabase db=this.getReadableDatabase();
        return db.rawQuery("SELECT "+ REC_ID +","+ REC_NAME +","+ REC_INGREDIENTS +","+ REC_URL +" FROM "+RESULTS_VIEW+" LIMIT "+Integer.toString(displayRows), null);
    }

    public Cursor getSimpleViewCursor(String recipeName, String[] selIngredients, int displayRows) {
        SQLiteDatabase db=this.getReadableDatabase();
        return db.query(RECIPES_TABLE,
                new String[] {REC_ID, REC_NAME, REC_INGREDIENTS, REC_URL},
                createWhereClause(recipeName, selIngredients), null, null, null,
                "LENGTH("+DatabaseHelper.REC_INGREDIENTS +")", Integer.toString(displayRows));
    }

    public Cursor getFilterSimpleCursor(String recipeName, String[] selIngredients, int displayRows, String query) {
        SQLiteDatabase db=this.getReadableDatabase();
        return db.query(RECIPES_TABLE,
                new String[] {REC_ID, REC_NAME, REC_INGREDIENTS, REC_URL},
                createWhereClause(recipeName, selIngredients, query), null, null, null,
                "LENGTH("+DatabaseHelper.REC_INGREDIENTS +")", Integer.toString(displayRows));
    }

    public Cursor getFilterViewCursor(int displayRows, String query) {
        SQLiteDatabase db=this.getReadableDatabase();
        return db.rawQuery("SELECT "+ REC_ID +","+ REC_NAME +","+ REC_INGREDIENTS +","+ REC_URL +" FROM "+RESULTS_VIEW+" WHERE "+ REC_NAME +" LIKE '%"+query+"%' OR "+ REC_INGREDIENTS +" LIKE '%"+query+"%' LIMIT "+Integer.toString(displayRows), null);
    }

    public void dropRecipes() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS "+ RECIPES_TABLE);
        db.close();
    }

    public void createGroceryList() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ INGREDIENTS_TABLE +" ("+ ING_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "+ ING_NAME +" TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ SHOPPING_TABLE +" ("+ SHO_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "+ SHO_NAME +" TEXT)");
        db.close();
    }

    public Cursor displayIngredients() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultsCursor = db.query(INGREDIENTS_TABLE, new String[]{ING_ID, ING_NAME}, null, null, null, null, null);

        if (resultsCursor != null) {
            resultsCursor.moveToFirst();
        }
        db.close();
        return resultsCursor;
    }

    public Cursor displayShopping() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultsCursor = db.query(SHOPPING_TABLE, new String[]{SHO_ID, SHO_NAME}, null, null, null, null, null);

        if (resultsCursor != null) {
            resultsCursor.moveToFirst();
        }
        db.close();
        return resultsCursor;
    }

    public void addIngredient(String newIngredient) {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues cv=new ContentValues();
        cv.put(ING_NAME, newIngredient);
        db.insert(INGREDIENTS_TABLE, null, cv);
        db.close();
    }

    public void addShopping(String newShopping) {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues cv=new ContentValues();
        cv.put(SHO_NAME, newShopping);
        db.insert(SHOPPING_TABLE, null, cv);
        db.close();
    }

    public void deleteIngredient(String rowID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(INGREDIENTS_TABLE, ING_ID +" = "+rowID, null);
        db.close();
    }

    public void deleteShopping(String rowID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(SHOPPING_TABLE, SHO_ID +" = "+rowID, null);
        db.close();
    }

    public boolean verifyRecipesTable() {
        SQLiteDatabase db = null;
        Cursor cursor;
        try {
            db=this.getWritableDatabase();
            try {
                cursor = db.query(RECIPES_TABLE, new String[] {REC_ID}, null, null, null, null, null, "1");
            } catch (Exception e) {
                return false;
            }
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