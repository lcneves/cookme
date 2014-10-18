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

    static final String dbName="RecipesDB";
    static final String recipesTable="Recipes";
    static final String recID="_id";
    static final String recName="Name";
    static final String recIngredients="Ingredients";
    static final String recURL="URL";

    static final String ingredientsTable="IngredientsTable";
    static final String ingID="_id";
    static final String ingName="IngName";

    static final String shoppingTable="ShoppingTable";
    static final String shoID="_id";
    static final String shoName="ShoName";

    static final String RESULTS_VIEW = "ResultsView";

    public static String createWhereClause(String name, String[] ingredients) {
        StringBuilder sb = new StringBuilder(recName + " LIKE '%" + name + "%'");

        if (ingredients.length > 0) {
            sb.append(" AND (" + recIngredients + " LIKE '%" + ingredients[0] + "%'");
            for (int i = 1; i < ingredients.length; ++i) sb.append(" AND " + recIngredients + " LIKE '%" + ingredients[i] + "%'");
            sb.append(")");
        }

        return sb.toString();
    }

    public static String createWhereClause(String name, String[] ingredients, String query) {
        StringBuilder sb = new StringBuilder(recName + " LIKE '%" + name + "%'");

        if (ingredients.length > 0) {
            sb.append(" AND (" + recIngredients + " LIKE '%" + ingredients[0] + "%'");
            for (int i = 1; i < ingredients.length; ++i) sb.append(" AND " + recIngredients + " LIKE '%" + ingredients[i] + "%'");
            sb.append(")");
        }
        sb.append(" AND (" + recName + " LIKE '%" + query + "%' OR " + recIngredients + " LIKE '%" + query + "%')");

        return sb.toString();
    }

    public DatabaseHelper(Context context) {
        super(context, dbName, null, 33);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}


    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {}

    public void recreateDatabase() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + recipesTable);
        db.execSQL("CREATE TABLE "+recipesTable+" ("+recID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+recName+" TEXT, "+recIngredients+" TEXT, "+recURL+" TEXT)");
        db.close();
    }

    public Cursor getResultsViewCursor(int displayRows) {
        SQLiteDatabase db=this.getReadableDatabase();
        return db.rawQuery("SELECT "+recID+","+recName+","+recIngredients+","+recURL+" FROM "+RESULTS_VIEW+" LIMIT "+Integer.toString(displayRows), null);
    }

    public Cursor getFilterViewCursor(int displayRows, String query) {
        SQLiteDatabase db=this.getReadableDatabase();
        return db.rawQuery("SELECT "+recID+","+recName+","+recIngredients+","+recURL+" FROM "+RESULTS_VIEW+" WHERE "+recName+" LIKE '%"+query+"%' OR "+recIngredients+" LIKE '%"+query+"%' LIMIT "+Integer.toString(displayRows), null);
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
        Cursor cursor;
        try {
            db=this.getWritableDatabase();
            try {
                cursor = db.query(recipesTable, new String[] {recID}, null, null, null, null, null, "1");
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