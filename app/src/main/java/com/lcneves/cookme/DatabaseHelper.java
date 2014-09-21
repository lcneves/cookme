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

    static final String resultsTable="Results";
    static final String resID="_id";
    static final String resName="ResName";
    static final String resIngredients="ResIngredients";
    static final String resURL="ResURL";
    static final String resMatches="Matches";
    static final String resMismatches="Mismatches";
    static final String resMatchCount="MatchCount";
    static final String resMismatchCount="MismatchCount";

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
        // TODO Auto-generated method stub

    }

    public void recreateDatabase() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS "+recipesTable);
        db.execSQL("DROP TABLE IF EXISTS "+resultsTable);
        db.execSQL("CREATE TABLE "+recipesTable+" ("+recID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+recName+" TEXT, "+recIngredients+" TEXT, "+recURL+" TEXT)");
        db.execSQL("CREATE TABLE "+resultsTable+" ("+resID+" INTEGER PRIMARY KEY, "+resName+" TEXT, "+resIngredients+" TEXT, "+resURL+" TEXT, "+resMatches+" TEXT, "+resMismatches+" TEXT, "+resMatchCount+" INTEGER, "+resMismatchCount+" INTEGER)");
        db.close();
    }

    public void createGroceryList() {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ingredientsTable+" ("+ingID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+ingName+" TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+shoppingTable+" ("+shoID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+shoName+" TEXT)");
        db.close();
    }

    public SQLiteDatabase getDB() {
        return this.getWritableDatabase();
    }


    public void addRecipes(SQLiteDatabase db, String name, String ingredients, String url) {
        ContentValues cv=new ContentValues();
        cv.put(recName, name);
        cv.put(recIngredients, ingredients);
        cv.put(recURL, url);
        db.insert(recipesTable, null, cv);
    }

    public void searchRecipes(String[] selIngredients) {
        if (selIngredients.length == 0)
            return;

        SQLiteDatabase db=this.getWritableDatabase();
        String whereCondition = recIngredients+" LIKE \'%" + selIngredients[0] + "%\'";
        for (int i = 1; i < selIngredients.length; i++) {
            whereCondition = whereCondition + " OR " + recIngredients + " LIKE \'%" + selIngredients[i] + "%\'";
        }
        String insertCommand = "INSERT INTO "+resultsTable+" ("+resName+","+resIngredients+","+resURL+") SELECT "+recName+","+recIngredients+","+recURL+" FROM "+recipesTable+" WHERE "+whereCondition;
        db.execSQL("DROP TABLE IF EXISTS "+resultsTable);
        db.execSQL("CREATE TABLE "+resultsTable+" ("+resID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+resName+" TEXT, "+resIngredients+" TEXT, "+resURL+" TEXT, "+resMatches+" TEXT, "+resMismatches+" TEXT, "+resMatchCount+" INTEGER, "+resMismatchCount+" INTEGER)");
        db.execSQL(insertCommand);

        String[] ingredientsArray = {resIngredients};
        Cursor cursor = null;
        try {
            cursor = db.query(resultsTable, ingredientsArray, null, null, null, null, null);
            db.beginTransaction();
            int rowCount = cursor.getCount();
            int columnIndex = cursor.getColumnIndexOrThrow(resIngredients);
            cursor.moveToFirst();
            for (int i = 1; i <= rowCount; i++) {
                String matches = "Matches: ";
                String misMatches = "Does not match: ";
                int count = 0;
                int misCount = 0;
                for (int j = 0; j < selIngredients.length; j++) {
                    String content = cursor.getString(columnIndex);
                    if (content.toLowerCase(Locale.ENGLISH).contains(selIngredients[j].toLowerCase(Locale.ENGLISH))) {
                        matches = matches + selIngredients[j] + " ";
                        count++;
                    } else {
                        misMatches = misMatches + selIngredients[j] + " ";
                        misCount++;
                    }
                }
                ContentValues cv=new ContentValues();
                cv.put(resMatches, matches);
                cv.put(resMismatches, misMatches);
                cv.put(resMatchCount, count);
                cv.put(resMismatchCount, misCount);
                db.update(resultsTable, cv, resID + " = " + i, null);
                cv.clear();
                cursor.moveToNext();
            }
            db.setTransactionSuccessful();
        } finally {
            cursor.close();
            db.endTransaction();
        }
        db.close();
    }

    public Cursor displayResults() {
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor resultsCursor = db.query(resultsTable, new String[] {resID, resName, resIngredients, resURL, resMatches, resMismatches, resMismatchCount}, null, null, null, null, resMismatchCount);

        if (resultsCursor != null) {
            resultsCursor.moveToFirst();
        }
        db.close();
        return resultsCursor;
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

    public Cursor displayRecipes() {
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor resultsCursor = db.query(recipesTable, new String[] {recID, recName, recIngredients, recURL}, null, null, null, null, null);

            resultsCursor.moveToFirst();

        db.close();
        return resultsCursor;
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