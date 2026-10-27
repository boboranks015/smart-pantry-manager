package com.example.smartpantrymanager.utils;

import com.example.smartpantrymanager.database.DatabaseHelper;
import com.example.smartpantrymanager.database.PantryItem;
import com.example.smartpantrymanager.database.Recipe;
import com.example.smartpantrymanager.database.RecipeIngredient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IngredientMatcher {

    private static final Map<String, Double> UNIT_TO_BASE = new HashMap<>();
    static {
        UNIT_TO_BASE.put("g", 1.0);
        UNIT_TO_BASE.put("kg", 1000.0);
        UNIT_TO_BASE.put("ml", 1.0);
        UNIT_TO_BASE.put("l", 1000.0);
        UNIT_TO_BASE.put("tbsp", 15.0);
        UNIT_TO_BASE.put("tsp", 5.0);
        UNIT_TO_BASE.put("cup", 240.0);
        UNIT_TO_BASE.put("piece", 1.0);
        UNIT_TO_BASE.put("slice", 1.0);
        UNIT_TO_BASE.put("clove", 1.0);
        UNIT_TO_BASE.put("pinch", 1.0);
    }

    public static boolean canMake(Recipe recipe, List<PantryItem> pantry) {
        Map<String, Double> available = new HashMap<>();
        for (PantryItem p : pantry) {
            String key = DatabaseHelper.normalize(p.getName());
            double base = toBase(p.getQuantity(), p.getUnit());
            available.put(key, available.getOrDefault(key, 0.0) + base);
        }
        for (RecipeIngredient ri : recipe.getIngredients()) {
            String key = DatabaseHelper.normalize(ri.getName());
            Double have = available.get(key);
            if (have == null) return false;
            double need = toBase(ri.getQuantity(), ri.getUnit());
            if (need > 0 && have + 0.0001 < need) return false;
        }
        return true;
    }

    public static int missingCount(Recipe recipe, List<PantryItem> pantry) {
        Map<String, Double> available = new HashMap<>();
        for (PantryItem p : pantry) {
            available.merge(DatabaseHelper.normalize(p.getName()),
                    toBase(p.getQuantity(), p.getUnit()), Double::sum);
        }
        int missing = 0;
        for (RecipeIngredient ri : recipe.getIngredients()) {
            String key = DatabaseHelper.normalize(ri.getName());
            Double have = available.get(key);
            if (have == null || have + 0.0001 < toBase(ri.getQuantity(), ri.getUnit())) {
                missing++;
            }
        }
        return missing;
    }

    private static double toBase(double qty, String unit) {
        if (unit == null) return qty;
        Double f = UNIT_TO_BASE.get(unit.toLowerCase().trim());
        return f == null ? qty : qty * f;
    }
}