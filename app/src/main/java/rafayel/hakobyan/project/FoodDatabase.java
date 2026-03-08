package rafayel.hakobyan.project;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FoodDatabase {

    public static class FoodInfo {
        public final String name;
        public final String emoji;
        public final String category;
        public final int    calories;
        public final float  protein;
        public final float  carbs;
        public final float  fats;

        public FoodInfo(String name, String emoji, String category,
                        int calories, float protein, float carbs, float fats) {
            this.name     = name;
            this.emoji    = emoji;
            this.category = category;
            this.calories = calories;
            this.protein  = protein;
            this.carbs    = carbs;
            this.fats     = fats;
        }
    }

    private static final Map<String, FoodInfo> DB = new HashMap<>();

    static {
        DB.put("apple",        new FoodInfo("Apple",        "🍎", "Fruit",      52,  0.3f, 13.8f,  0.2f));
        DB.put("banana",       new FoodInfo("Banana",       "🍌", "Fruit",      89,  1.1f, 22.8f,  0.3f));
        DB.put("orange",       new FoodInfo("Orange",       "🍊", "Fruit",      47,  0.9f, 11.8f,  0.1f));
        DB.put("grapes",       new FoodInfo("Grapes",       "🍇", "Fruit",      69,  0.7f, 18.1f,  0.2f));
        DB.put("strawberry",   new FoodInfo("Strawberry",   "🍓", "Fruit",      32,  0.7f,  7.7f,  0.3f));
        DB.put("watermelon",   new FoodInfo("Watermelon",   "🍉", "Fruit",      30,  0.6f,  7.6f,  0.2f));
        DB.put("pineapple",    new FoodInfo("Pineapple",    "🍍", "Fruit",      50,  0.5f, 13.1f,  0.1f));
        DB.put("mango",        new FoodInfo("Mango",        "🥭", "Fruit",      60,  0.8f, 15.0f,  0.4f));
        DB.put("peach",        new FoodInfo("Peach",        "🍑", "Fruit",      39,  0.9f,  9.5f,  0.3f));
        DB.put("pear",         new FoodInfo("Pear",         "🍐", "Fruit",      57,  0.4f, 15.2f,  0.1f));
        DB.put("cherry",       new FoodInfo("Cherry",       "🍒", "Fruit",      63,  1.1f, 16.0f,  0.2f));
        DB.put("kiwi",         new FoodInfo("Kiwi",         "🥝", "Fruit",      61,  1.1f, 14.7f,  0.5f));
        DB.put("avocado",      new FoodInfo("Avocado",      "🥑", "Fruit",     160,  2.0f,  8.5f, 14.7f));
        DB.put("blueberry",    new FoodInfo("Blueberry",    "🫐", "Fruit",      57,  0.7f, 14.5f,  0.3f));
        DB.put("lemon",        new FoodInfo("Lemon",        "🍋", "Fruit",      29,  1.1f,  9.3f,  0.3f));
        DB.put("coconut",      new FoodInfo("Coconut",      "🥥", "Fruit",     354,  3.3f, 15.2f, 33.5f));

        DB.put("broccoli",     new FoodInfo("Broccoli",     "🥦", "Vegetable",  34,  2.8f,  6.6f,  0.4f));
        DB.put("carrot",       new FoodInfo("Carrot",       "🥕", "Vegetable",  41,  0.9f,  9.6f,  0.2f));
        DB.put("tomato",       new FoodInfo("Tomato",       "🍅", "Vegetable",  18,  0.9f,  3.9f,  0.2f));
        DB.put("potato",       new FoodInfo("Potato",       "🥔", "Vegetable",  77,  2.0f, 17.5f,  0.1f));
        DB.put("onion",        new FoodInfo("Onion",        "🧅", "Vegetable",  40,  1.1f,  9.3f,  0.1f));
        DB.put("garlic",       new FoodInfo("Garlic",       "🧄", "Vegetable", 149,  6.4f, 33.1f,  0.5f));
        DB.put("cucumber",     new FoodInfo("Cucumber",     "🥒", "Vegetable",  15,  0.7f,  3.6f,  0.1f));
        DB.put("spinach",      new FoodInfo("Spinach",      "🥬", "Vegetable",  23,  2.9f,  3.6f,  0.4f));
        DB.put("corn",         new FoodInfo("Corn",         "🌽", "Vegetable",  86,  3.3f, 19.0f,  1.4f));
        DB.put("pepper",       new FoodInfo("Bell Pepper",  "🫑", "Vegetable",  31,  1.0f,  6.0f,  0.3f));
        DB.put("mushroom",     new FoodInfo("Mushroom",     "🍄", "Vegetable",  22,  3.1f,  3.3f,  0.3f));
        DB.put("eggplant",     new FoodInfo("Eggplant",     "🍆", "Vegetable",  25,  1.0f,  5.9f,  0.2f));
        DB.put("cabbage",      new FoodInfo("Cabbage",      "🥬", "Vegetable",  25,  1.3f,  5.8f,  0.1f));
        DB.put("sweet potato", new FoodInfo("Sweet Potato", "🍠", "Vegetable",  86,  1.6f, 20.1f,  0.1f));

        DB.put("chicken",        new FoodInfo("Chicken",        "🍗", "Meat",  239, 27.3f,  0.0f, 13.6f));
        DB.put("chicken breast", new FoodInfo("Chicken Breast", "🍗", "Meat",  165, 31.0f,  0.0f,  3.6f));
        DB.put("beef",           new FoodInfo("Beef",           "🥩", "Meat",  250, 26.1f,  0.0f, 15.4f));
        DB.put("salmon",         new FoodInfo("Salmon",         "🐟", "Fish",  208, 20.4f,  0.0f, 13.4f));
        DB.put("tuna",           new FoodInfo("Tuna",           "🐟", "Fish",  144, 23.3f,  0.0f,  4.9f));
        DB.put("shrimp",         new FoodInfo("Shrimp",         "🦐", "Fish",   99, 24.0f,  0.2f,  0.3f));
        DB.put("egg",            new FoodInfo("Egg",            "🥚", "Protein",155, 12.6f,  1.1f, 10.6f));
        DB.put("tofu",           new FoodInfo("Tofu",           "🫘", "Protein", 76,  8.1f,  1.9f,  4.8f));

        DB.put("milk",    new FoodInfo("Milk",    "🥛", "Dairy",  61,  3.2f,  4.8f,  3.3f));
        DB.put("cheese",  new FoodInfo("Cheese",  "🧀", "Dairy", 402, 25.0f,  1.3f, 33.1f));
        DB.put("yogurt",  new FoodInfo("Yogurt",  "🥛", "Dairy",  59,  3.5f,  5.0f,  3.3f));
        DB.put("butter",  new FoodInfo("Butter",  "🧈", "Dairy", 717,  0.9f,  0.1f, 81.1f));

        DB.put("bread", new FoodInfo("Bread", "🍞", "Grain", 265,  9.0f, 49.2f,  3.2f));
        DB.put("rice",  new FoodInfo("Rice",  "🍚", "Grain", 130,  2.7f, 28.2f,  0.3f));
        DB.put("pasta", new FoodInfo("Pasta", "🍝", "Grain", 131,  5.0f, 25.1f,  1.1f));
        DB.put("oats",  new FoodInfo("Oats",  "🌾", "Grain", 389, 16.9f, 66.3f,  6.9f));

        DB.put("almond",  new FoodInfo("Almond",  "🌰", "Nut", 579, 21.2f, 21.6f, 49.9f));
        DB.put("almonds", new FoodInfo("Almonds", "🌰", "Nut", 579, 21.2f, 21.6f, 49.9f));
        DB.put("peanut",  new FoodInfo("Peanut",  "🥜", "Nut", 567, 25.8f, 16.1f, 49.2f));
        DB.put("walnut",  new FoodInfo("Walnut",  "🌰", "Nut", 654, 15.2f, 13.7f, 65.2f));

        DB.put("pizza",        new FoodInfo("Pizza",        "🍕", "Fast Food", 266, 11.4f, 32.9f,  9.8f));
        DB.put("burger",       new FoodInfo("Burger",       "🍔", "Fast Food", 295, 17.0f, 24.0f, 14.0f));
        DB.put("french fries", new FoodInfo("French Fries", "🍟", "Fast Food", 312,  3.4f, 41.4f, 15.0f));
        DB.put("chocolate",    new FoodInfo("Chocolate",    "🍫", "Snack",     546,  4.9f, 59.4f, 31.3f));

        DB.put("orange juice", new FoodInfo("Orange Juice", "🍊", "Drink",  45, 0.7f, 10.4f, 0.2f));
        DB.put("coffee",       new FoodInfo("Coffee",       "☕", "Drink",   2, 0.3f,  0.0f, 0.0f));
        DB.put("green tea",    new FoodInfo("Green Tea",    "🍵", "Drink",   1, 0.2f,  0.0f, 0.0f));
    }


    /**
     * Find food by name. Returns null if not found.
     * Usage: FoodDatabase.find("apple")
     */
    public static FoodInfo find(String name) {
        if (name == null || name.isEmpty()) return null;
        return DB.get(name.toLowerCase(Locale.getDefault()).trim());
    }

    /**
     * Returns a placeholder entry when food is not in the database.
     * Usage: FoodDatabase.unknown("SomeFoodName")
     */
    public static FoodInfo unknown(String name) {
        String displayName = (name != null && !name.isEmpty()) ? name : "Unknown Food";
        return new FoodInfo(displayName, "🍽️", "Unknown", 0, 0f, 0f, 0f);
    }
}