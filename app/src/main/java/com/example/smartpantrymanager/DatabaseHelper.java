package com.example.smartpantrymanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "SmartPantry.db";
    private static final int DB_VERSION = 1;

    public static final String T_PANTRY = "pantry_items";
    public static final String C_ID = "id";
    public static final String C_NAME = "name";
    public static final String C_QTY = "quantity";
    public static final String C_UNIT = "unit";
    public static final String C_EXPIRY = "expiry_date";

    public static final String T_RECIPES = "recipes";
    public static final String C_RECIPE_ID = "id";
    public static final String C_RECIPE_NAME = "name";
    public static final String C_RECIPE_STEPS = "steps";

    public static final String T_RI = "recipe_ingredients";
    public static final String C_RI_ID = "id";
    public static final String C_RI_RECIPE_ID = "recipe_id";
    public static final String C_RI_ING = "ingredient_name";
    public static final String C_RI_QTY = "quantity";
    public static final String C_RI_UNIT = "unit";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_PANTRY + " (" +
                C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_NAME + " TEXT NOT NULL, " +
                C_QTY + " REAL NOT NULL, " +
                C_UNIT + " TEXT, " +
                C_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + T_RECIPES + " (" +
                C_RECIPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_RECIPE_NAME + " TEXT NOT NULL, " +
                C_RECIPE_STEPS + " TEXT)");

        db.execSQL("CREATE TABLE " + T_RI + " (" +
                C_RI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_RI_RECIPE_ID + " INTEGER NOT NULL, " +
                C_RI_ING + " TEXT NOT NULL, " +
                C_RI_QTY + " REAL, " +
                C_RI_UNIT + " TEXT)");

        seedRecipes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_PANTRY);
        db.execSQL("DROP TABLE IF EXISTS " + T_RI);
        db.execSQL("DROP TABLE IF EXISTS " + T_RECIPES);
        onCreate(db);
    }

    /** Lowercase + trim trailing 's'/'es' so "Tomatoes" matches "tomato". */
    public static String normalize(String name) {
        if (name == null) return "";
        String n = name.trim().toLowerCase();
        if (n.endsWith("es") && n.length() > 3) n = n.substring(0, n.length() - 2);
        else if (n.endsWith("s") && n.length() > 2) n = n.substring(0, n.length() - 1);
        return n;
    }

    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_NAME, normalize(item.getName()));
        cv.put(C_QTY, item.getQuantity());
        cv.put(C_UNIT, item.getUnit());
        cv.put(C_EXPIRY, item.getExpiryDate());
        long id = db.insert(T_PANTRY, null, cv);
        db.close();
        return id;
    }

    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_NAME, normalize(item.getName()));
        cv.put(C_QTY, item.getQuantity());
        cv.put(C_UNIT, item.getUnit());
        cv.put(C_EXPIRY, item.getExpiryDate());
        int rows = db.update(T_PANTRY, cv, C_ID + "=?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        return rows;
    }

    public void deletePantryItem(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_PANTRY, C_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_PANTRY, null, null, null, null, null, C_NAME + " ASC");
        while (c.moveToNext()) {
            list.add(new PantryItem(
                    c.getInt(c.getColumnIndexOrThrow(C_ID)),
                    c.getString(c.getColumnIndexOrThrow(C_NAME)),
                    c.getDouble(c.getColumnIndexOrThrow(C_QTY)),
                    c.getString(c.getColumnIndexOrThrow(C_UNIT)),
                    c.getString(c.getColumnIndexOrThrow(C_EXPIRY))));
        }
        c.close();
        db.close();
        return list;
    }

    public List<Recipe> getAllRecipes() {
        List<Recipe> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_RECIPES, null, null, null, null, null, C_RECIPE_NAME + " ASC");
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow(C_RECIPE_ID));
            Recipe r = new Recipe(id,
                    c.getString(c.getColumnIndexOrThrow(C_RECIPE_NAME)),
                    c.getString(c.getColumnIndexOrThrow(C_RECIPE_STEPS)));
            r.setIngredients(getIngredientsForRecipe(id));
            list.add(r);
        }
        c.close();
        db.close();
        return list;
    }

    public Recipe getRecipeById(int recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_RECIPES, null, C_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)}, null, null, null);
        Recipe r = null;
        if (c.moveToFirst()) {
            r = new Recipe(recipeId,
                    c.getString(c.getColumnIndexOrThrow(C_RECIPE_NAME)),
                    c.getString(c.getColumnIndexOrThrow(C_RECIPE_STEPS)));
            r.setIngredients(getIngredientsForRecipe(recipeId));
        }
        c.close();
        db.close();
        return r;
    }

    public List<RecipeIngredient> getIngredientsForRecipe(int recipeId) {
        List<RecipeIngredient> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_RI, null, C_RI_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)}, null, null, null);
        while (c.moveToNext()) {
            list.add(new RecipeIngredient(
                    c.getString(c.getColumnIndexOrThrow(C_RI_ING)),
                    c.getDouble(c.getColumnIndexOrThrow(C_RI_QTY)),
                    c.getString(c.getColumnIndexOrThrow(C_RI_UNIT))));
        }
        c.close();
        return list;
    }

    private void seedRecipes(SQLiteDatabase db) {
        addSeedRecipe(db, "Tomato Pasta",
                "Boil pasta. Fry garlic in oil. Add tomatoes, simmer 10 min. Mix with pasta.",
                new Object[][]{
                        {"pasta", 200.0, "g"},
                        {"tomato", 3.0, "piece"},
                        {"garlic", 2.0, "clove"},
                        {"olive oil", 2.0, "tbsp"}
                });
        addSeedRecipe(db, "Omelette",
                "Beat eggs, add milk and salt. Cook in buttered pan until set.",
                new Object[][]{
                        {"egg", 3.0, "piece"},
                        {"milk", 50.0, "ml"},
                        {"butter", 1.0, "tbsp"},
                        {"salt", 1.0, "pinch"}
                });
        addSeedRecipe(db, "Garlic Butter Rice",
                "Cook rice. Melt butter with garlic. Toss together and season.",
                new Object[][]{
                        {"rice", 200.0, "g"},
                        {"butter", 2.0, "tbsp"},
                        {"garlic", 3.0, "clove"},
                        {"salt", 1.0, "pinch"}
                });
        addSeedRecipe(db, "Vegetable Stir Fry",
                "Chop vegetables. Stir fry in oil on high heat. Add soy sauce.",
                new Object[][]{
                        {"carrot", 2.0, "piece"},
                        {"onion", 1.0, "piece"},
                        {"bell pepper", 1.0, "piece"},
                        {"soy sauce", 2.0, "tbsp"},
                        {"oil", 1.0, "tbsp"}
                });
        addSeedRecipe(db, "Pancakes",
                "Whisk flour, egg, milk. Cook on greased pan until golden.",
                new Object[][]{
                        {"flour", 150.0, "g"},
                        {"egg", 1.0, "piece"},
                        {"milk", 200.0, "ml"},
                        {"butter", 1.0, "tbsp"}
                });
        addSeedRecipe(db, "Scrambled Eggs",
                "Beat eggs, cook in butter, stir constantly until creamy.",
                new Object[][]{
                        {"egg", 2.0, "piece"},
                        {"butter", 1.0, "tbsp"},
                        {"salt", 1.0, "pinch"}
                });
        addSeedRecipe(db, "Cheese Toast",
                "Toast bread, top with cheese, grill until melted.",
                new Object[][]{
                        {"bread", 2.0, "slice"},
                        {"cheese", 50.0, "g"},
                        {"butter", 1.0, "tbsp"}
                });
        addSeedRecipe(db, "Onion Soup",
                "Caramelize onions in butter. Add water, simmer 20 min. Season.",
                new Object[][]{
                        {"onion", 3.0, "piece"},
                        {"butter", 2.0, "tbsp"},
                        {"salt", 1.0, "pinch"},
                        {"water", 500.0, "ml"}
                });
        addSeedRecipe(db, "Tomato Soup",
                "Cook tomatoes with onion and garlic, blend, season.",
                new Object[][]{
                        {"tomato", 4.0, "piece"},
                        {"onion", 1.0, "piece"},
                        {"garlic", 2.0, "clove"},
                        {"salt", 1.0, "pinch"}
                });
        addSeedRecipe(db, "Fried Rice",
                "Cook rice. Fry egg, add rice and soy sauce. Toss well.",
                new Object[][]{
                        {"rice", 250.0, "g"},
                        {"egg", 2.0, "piece"},
                        {"soy sauce", 2.0, "tbsp"},
                        {"oil", 1.0, "tbsp"}
                });
        addSeedRecipe(db, "Garlic Bread",
                "Mix butter and garlic. Spread on bread. Bake 10 min.",
                new Object[][]{
                        {"bread", 4.0, "slice"},
                        {"butter", 3.0, "tbsp"},
                        {"garlic", 2.0, "clove"}
                });
        addSeedRecipe(db, "Creamy Pasta",
                "Cook pasta. Mix milk, butter, cheese into sauce. Combine.",
                new Object[][]{
                        {"pasta", 200.0, "g"},
                        {"milk", 150.0, "ml"},
                        {"butter", 2.0, "tbsp"},
                        {"cheese", 50.0, "g"}
                });
        addSeedRecipe(db, "Tomato Rice",
                "Cook rice. Fry tomato and onion. Mix in rice with salt.",
                new Object[][]{
                        {"rice", 200.0, "g"},
                        {"tomato", 2.0, "piece"},
                        {"onion", 1.0, "piece"},
                        {"salt", 1.0, "pinch"}
                });
        addSeedRecipe(db, "Cheese Omelette",
                "Beat eggs. Cook in butter, add cheese, fold.",
                new Object[][]{
                        {"egg", 2.0, "piece"},
                        {"cheese", 30.0, "g"},
                        {"butter", 1.0, "tbsp"},
                        {"salt", 1.0, "pinch"}
                });
        addSeedRecipe(db, "Vegetable Soup",
                "Chop vegetables. Boil in water 25 min. Season and serve.",
                new Object[][]{
                        {"carrot", 2.0, "piece"},
                        {"onion", 1.0, "piece"},
                        {"potato", 2.0, "piece"},
                        {"water", 700.0, "ml"},
                        {"salt", 1.0, "pinch"}
                });
        addSeedRecipe(db, "Butter Pasta",
                "Boil pasta. Toss with butter, garlic and salt.",
                new Object[][]{
                        {"pasta", 200.0, "g"},
                        {"butter", 2.0, "tbsp"},
                        {"garlic", 2.0, "clove"},
                        {"salt", 1.0, "pinch"}
                });
    }

    private void addSeedRecipe(SQLiteDatabase db, String name, String steps, Object[][] ings) {
        ContentValues rv = new ContentValues();
        rv.put(C_RECIPE_NAME, name);
        rv.put(C_RECIPE_STEPS, steps);
        long recipeId = db.insert(T_RECIPES, null, rv);
        for (Object[] ing : ings) {
            ContentValues iv = new ContentValues();
            iv.put(C_RI_RECIPE_ID, recipeId);
            iv.put(C_RI_ING, normalize((String) ing[0]));
            iv.put(C_RI_QTY, (Double) ing[1]);
            iv.put(C_RI_UNIT, (String) ing[2]);
            db.insert(T_RI, null, iv);
        }
    }
}