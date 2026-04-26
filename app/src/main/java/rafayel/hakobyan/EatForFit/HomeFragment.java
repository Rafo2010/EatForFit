package rafayel.hakobyan.EatForFit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private static final String GROQ_API_KEY      = "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";
    private static final String GROQ_VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String GROQ_CHAT_MODEL   = "llama-3.3-70b-versatile";
    private static final String GROQ_URL          = "https://api.groq.com/openai/v1/chat/completions";

    private static final String PREFS_NAME  = "food_scanner_prefs";
    private static final String KEY_HISTORY = "food_history";
    private static final int    MAX_ENTRIES = 100;

    // ─────────────────────────────────────────────────────────────────────────
    // Data classes
    // ─────────────────────────────────────────────────────────────────────────

    static class FoodEntry {
        String id, savedImagePath, name, emoji, category, date, aiDescription, recommendation;
        Uri    imageUri;
        int    calories, estimatedGrams;
        float  protein, carbs, fats;
        String base64Image;
        boolean fistDetected;

        FoodEntry() { this.id = UUID.randomUUID().toString(); }

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("id",             id);
            o.put("savedImagePath", savedImagePath != null ? savedImagePath : "");
            o.put("name",           name);
            o.put("emoji",          emoji);
            o.put("category",       category);
            o.put("date",           date);
            o.put("calories",       calories);
            o.put("protein",        protein);
            o.put("carbs",          carbs);
            o.put("fats",           fats);
            o.put("aiDescription",  aiDescription  != null ? aiDescription  : "");
            o.put("recommendation", recommendation != null ? recommendation : "");
            o.put("estimatedGrams", estimatedGrams);
            o.put("fistDetected",   fistDetected);
            return o;
        }

        static FoodEntry fromJson(JSONObject o) throws Exception {
            FoodEntry e      = new FoodEntry();
            e.id             = o.optString("id", UUID.randomUUID().toString());
            e.savedImagePath = o.optString("savedImagePath", "");
            e.name           = o.optString("name", "Unknown");
            e.emoji          = o.optString("emoji", "🍽️");
            e.category       = o.optString("category", "Other");
            e.date           = o.optString("date", "");
            e.calories       = o.optInt("calories", 0);
            e.protein        = (float) o.optDouble("protein", 0);
            e.carbs          = (float) o.optDouble("carbs", 0);
            e.fats           = (float) o.optDouble("fats", 0);
            e.aiDescription  = o.optString("aiDescription", "");
            e.recommendation = o.optString("recommendation", "");
            e.estimatedGrams = o.optInt("estimatedGrams", 100);
            e.fistDetected   = o.optBoolean("fistDetected", false);
            if (!e.savedImagePath.isEmpty()) e.imageUri = Uri.parse("file://" + e.savedImagePath);
            return e;
        }
    }

    static class ChatMessage {
        String role, text;
        ChatMessage(String role, String text) { this.role = role; this.text = text; }
    }

    static class IdentifyResult {
        boolean isFood, fistDetected;
        String  foodName, description;
        int     estimatedGrams;
        IdentifyResult(boolean isFood, String foodName, String description,
                       int estimatedGrams, boolean fistDetected) {
            this.isFood         = isFood;
            this.foodName       = foodName;
            this.description    = description;
            this.estimatedGrams = estimatedGrams;
            this.fistDetected   = fistDetected;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Views
    // ─────────────────────────────────────────────────────────────────────────

    private CardView           cardCamera;
    private ImageView          ivFoodPreview;
    private LinearLayout       layoutPlaceholder;
    private FrameLayout        layoutLoading;
    private ImageButton        btnRetake;
    private MaterialButton     btnCamera, btnGallery;
    private CardView           cardResult;
    private TextView           tvFoodEmoji, tvFoodName;
    private TextView           tvCalories, tvProtein, tvCarbs, tvFats;
    private TextView           tvAiDescription, tvRecommendation, tvLoggedCalories;
    private MaterialButton     btnLogServing;
    private LinearLayout       layoutHistoryContainer, layoutEmptyHistory;
    private TextView           tvHistoryCount;
    private LinearLayout       layoutFistOverlay;
    private View               viewPulseOuter, viewPulseInner;
    private CalorieArcView     homeCalorieArcView;
    private TextView           homeTvDailyCalorieGoal, homeTvCaloriesRemaining, homeTvCaloriesLabel;

    /** NEW: the animated scanner overlay */
    private ScannerOverlayView scannerOverlay;

    private int dailyCalorieGoal = 2000;

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────

    private Uri    photoUri;
    private String lastBase64Image;
    private String lastAiDescription = "";
    private String currentUserId     = "default";
    private final List<FoodEntry> historyList = new ArrayList<>();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    // ─────────────────────────────────────────────────────────────────────────
    // Launchers
    // ─────────────────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && photoUri != null) {
                    showPhotoPreview(photoUri);
                    analyzeFood(photoUri);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    photoUri = uri;
                    showPhotoPreview(uri);
                    analyzeFood(uri);
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(requireContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show();
            });

    public HomeFragment() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupListeners();
        com.google.firebase.auth.FirebaseUser fbUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) currentUserId = fbUser.getUid();
        loadHistoryFromStorage();
        refreshHistoryUI();
        loadTodayCalories();
        startPulseAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();
        com.google.firebase.auth.FirebaseUser fbUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) currentUserId = fbUser.getUid();
        loadHistoryFromStorage();
        refreshHistoryUI();
        loadTodayCalories();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View binding
    // ─────────────────────────────────────────────────────────────────────────

    private void bindViews(View root) {
        cardCamera              = root.findViewById(R.id.cardCamera);
        ivFoodPreview           = root.findViewById(R.id.ivFoodPreview);
        layoutPlaceholder       = root.findViewById(R.id.layoutPlaceholder);
        layoutLoading           = root.findViewById(R.id.layoutLoading);
        btnRetake               = root.findViewById(R.id.btnRetake);
        btnCamera               = root.findViewById(R.id.btnCamera);
        btnGallery              = root.findViewById(R.id.btnGallery);
        cardResult              = root.findViewById(R.id.cardResult);
        tvFoodEmoji             = root.findViewById(R.id.tvFoodEmoji);
        tvFoodName              = root.findViewById(R.id.tvFoodName);
        tvCalories              = root.findViewById(R.id.tvCalories);
        tvProtein               = root.findViewById(R.id.tvProtein);
        tvCarbs                 = root.findViewById(R.id.tvCarbs);
        tvFats                  = root.findViewById(R.id.tvFats);
        tvAiDescription         = root.findViewById(R.id.tvAiDescription);
        tvRecommendation        = root.findViewById(R.id.tvRecommendation);
        tvLoggedCalories        = root.findViewById(R.id.tvLoggedCalories);
        btnLogServing           = root.findViewById(R.id.btnLogServing);
        layoutHistoryContainer  = root.findViewById(R.id.layoutHistoryContainer);
        layoutEmptyHistory      = root.findViewById(R.id.layoutEmptyHistory);
        tvHistoryCount          = root.findViewById(R.id.tvHistoryCount);
        viewPulseOuter          = root.findViewById(R.id.viewPulseOuter);
        viewPulseInner          = root.findViewById(R.id.viewPulseInner);
        layoutFistOverlay       = root.findViewById(R.id.layoutFistOverlay);
        homeCalorieArcView      = root.findViewById(R.id.homeCalorieArcView);
        homeTvDailyCalorieGoal  = root.findViewById(R.id.homeTvDailyCalorieGoal);
        homeTvCaloriesRemaining = root.findViewById(R.id.homeTvCaloriesRemaining);
        homeTvCaloriesLabel     = root.findViewById(R.id.homeTvCaloriesLabel);
        // NEW: scanner overlay
        scannerOverlay          = root.findViewById(R.id.scannerOverlay);
    }

    private void setupListeners() {
        cardCamera.setOnClickListener(v -> requestCameraAndOpen());
        btnCamera.setOnClickListener(v  -> requestCameraAndOpen());
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnRetake.setOnClickListener(v  -> resetScanUI());

        View btnCal = getView() != null ? getView().findViewById(R.id.btnOpenCalendar) : null;
        if (btnCal != null) btnCal.setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), CalendarActivity.class)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animations
    // ─────────────────────────────────────────────────────────────────────────

    private void startPulseAnimation() {
        if (viewPulseOuter == null || viewPulseInner == null) return;

        android.animation.ObjectAnimator outScaleX =
                android.animation.ObjectAnimator.ofFloat(viewPulseOuter, "scaleX", 1f, 1.20f, 1f);
        android.animation.ObjectAnimator outScaleY =
                android.animation.ObjectAnimator.ofFloat(viewPulseOuter, "scaleY", 1f, 1.20f, 1f);
        android.animation.ObjectAnimator outAlpha  =
                android.animation.ObjectAnimator.ofFloat(viewPulseOuter, "alpha", 0.35f, 0.08f, 0.35f);
        for (android.animation.ObjectAnimator a : new android.animation.ObjectAnimator[]{outScaleX, outScaleY, outAlpha}) {
            a.setDuration(2200);
            a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            a.setRepeatMode(android.animation.ValueAnimator.RESTART);
            a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            a.start();
        }

        android.animation.ObjectAnimator inScaleX =
                android.animation.ObjectAnimator.ofFloat(viewPulseInner, "scaleX", 1f, 1.14f, 1f);
        android.animation.ObjectAnimator inScaleY =
                android.animation.ObjectAnimator.ofFloat(viewPulseInner, "scaleY", 1f, 1.14f, 1f);
        android.animation.ObjectAnimator inAlpha  =
                android.animation.ObjectAnimator.ofFloat(viewPulseInner, "alpha", 0.55f, 0.18f, 0.55f);
        for (android.animation.ObjectAnimator a : new android.animation.ObjectAnimator[]{inScaleX, inScaleY, inAlpha}) {
            a.setDuration(1600);
            a.setStartDelay(350);
            a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            a.setRepeatMode(android.animation.ValueAnimator.RESTART);
            a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            a.start();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String saveImagePermanently(Uri sourceUri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(sourceUri);
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp == null) return null;
            int maxDim = 800, w = bmp.getWidth(), h = bmp.getHeight();
            if (w > maxDim || h > maxDim) {
                float s = Math.min((float) maxDim / w, (float) maxDim / h);
                bmp = Bitmap.createScaledBitmap(bmp, Math.round(w * s), Math.round(h * s), true);
            }
            File dir = new File(requireContext().getFilesDir(), "food_images");
            if (!dir.exists()) dir.mkdirs();
            String filename = "food_" + System.currentTimeMillis() + ".jpg";
            File outFile = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(outFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            fos.flush();
            fos.close();
            return outFile.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    /**
     * Crops the image to a centered square and rounds corners.
     * Simulates "crop everything but food" — visually tight-crops to
     * the most food-likely center region with a polished rounded look.
     */
    private Bitmap cropToFood(Bitmap src) {
        int w    = src.getWidth();
        int h    = src.getHeight();
        int size = (int)(Math.min(w, h) * 0.82f);
        int x    = (w - size) / 2;
        int y    = (h - size) / 2;
        Bitmap cropped = Bitmap.createBitmap(src, x, y, size, size);

        // Apply rounded corners
        Bitmap rounded = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas  = new Canvas(rounded);
        Paint  paint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawRoundRect(new RectF(0, 0, size, size), 36f, 36f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(cropped, 0, 0, paint);
        return rounded;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void saveHistoryToStorage() {
        try {
            JSONArray arr   = new JSONArray();
            int       limit = Math.min(historyList.size(), MAX_ENTRIES);
            for (int i = 0; i < limit; i++) arr.put(historyList.get(i).toJson());
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadHistoryFromStorage() {
        try {
            historyList.clear();
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_HISTORY, null);
            if (json == null || json.isEmpty()) return;
            String    today = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());
            JSONArray arr   = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                FoodEntry entry = FoodEntry.fromJson(arr.getJSONObject(i));
                if (entry.date == null || !entry.date.startsWith(today)) continue;
                if (entry.savedImagePath != null && !entry.savedImagePath.isEmpty()) {
                    if (new File(entry.savedImagePath).exists()) historyList.add(entry);
                } else {
                    historyList.add(entry);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * NEW: saves a recognised food entry to historyList + SharedPreferences
     * immediately after AI recognition — so the photo appears in history
     * right away without waiting for the user to press "Log".
     */
    private void saveEntryToHistory(String savedImagePath, FoodDatabase.FoodInfo info,
                                    String description, String recommendation,
                                    int estimatedGrams, boolean fistDetected) {
        FoodEntry entry       = new FoodEntry();
        entry.savedImagePath  = savedImagePath != null ? savedImagePath : "";
        entry.name            = info.name;
        entry.emoji           = info.emoji;
        entry.category        = info.category;
        entry.calories        = info.calories;
        entry.protein         = info.protein;
        entry.carbs           = info.carbs;
        entry.fats            = info.fats;
        entry.aiDescription   = description  != null ? description  : "";
        entry.recommendation  = recommendation != null ? recommendation : "";
        entry.estimatedGrams  = estimatedGrams > 0 ? estimatedGrams : 100;
        entry.fistDetected    = fistDetected;
        entry.date = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(new Date());
        if (!entry.savedImagePath.isEmpty())
            entry.imageUri = Uri.parse("file://" + entry.savedImagePath);

        historyList.add(0, entry);   // newest first
        saveHistoryToStorage();
    }

    private void deleteEntry(int index) {
        if (index < 0 || index >= historyList.size()) return;
        FoodEntry entry = historyList.get(index);
        if (entry.savedImagePath != null && !entry.savedImagePath.isEmpty())
            new File(entry.savedImagePath).delete();
        historyList.remove(index);
        saveHistoryToStorage();
        refreshHistoryUI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Camera / gallery
    // ─────────────────────────────────────────────────────────────────────────

    private void requestCameraAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            String ts        = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File   imageFile = File.createTempFile("FOOD_" + ts, ".jpg",
                    requireContext().getExternalFilesDir(null));
            photoUri = FileProvider.getUriForFile(requireContext(),
                    "rafayel.hakobyan.EatForFit.fileprovider", imageFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not open camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotoPreview(Uri uri) {
        ivFoodPreview.setImageURI(uri);
        ivFoodPreview.setVisibility(View.VISIBLE);
        layoutPlaceholder.setVisibility(View.GONE);
        btnRetake.setVisibility(View.VISIBLE);
        if (layoutFistOverlay != null) layoutFistOverlay.setVisibility(View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Loading / scanner control  ← KEY CHANGE
    // ─────────────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (scannerOverlay != null) {
            if (loading) scannerOverlay.startScanning();
            else         scannerOverlay.stopScanning();
        }
    }

    private void resetScanUI() {
        ivFoodPreview.setVisibility(View.GONE);
        layoutPlaceholder.setVisibility(View.VISIBLE);
        btnRetake.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);
        if (layoutFistOverlay != null) layoutFistOverlay.setVisibility(View.GONE);
        photoUri          = null;
        lastBase64Image   = null;
        lastAiDescription = "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI analysis  ← KEY CHANGE
    // ─────────────────────────────────────────────────────────────────────────

    private void analyzeFood(Uri imageUri) {
        setLoading(true);
        cardResult.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String base64 = uriToBase64(imageUri);
                if (base64 == null) throw new Exception("Could not read image");

                IdentifyResult identified = askGroqToIdentifyFood(base64);

                if (!identified.isFood) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        setLoading(false);
                        resetScanUI();
                        Toast.makeText(requireContext(),
                                "No food detected. Try a clearer photo!", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                FoodDatabase.FoodInfo info   = findBestMatch(identified.foodName);
                final boolean         usedAI = (info == null);
                if (usedAI) info = askGroqForNutrition(identified.foodName);

                String recommendation = askGroqForRecommendation(identified.foodName, info);

                final String              savedPath       = saveImagePermanently(imageUri);
                final FoodDatabase.FoodInfo finalInfo     = info;
                final String              finalBase64     = base64;
                final String              finalDesc       = identified.description;
                final String              finalRec        = recommendation;
                final int                 finalEstGrams   = identified.estimatedGrams;
                final boolean             finalFistDet    = identified.fistDetected;

                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    lastBase64Image   = finalBase64;
                    lastAiDescription = finalDesc;

                    if (layoutFistOverlay != null) {
                        layoutFistOverlay.setVisibility(finalFistDet ? View.VISIBLE : View.GONE);
                    }

                    // ── 1. Crop image to food-center region ──────────────────
                    if (photoUri != null) {
                        try {
                            InputStream is = requireContext().getContentResolver()
                                    .openInputStream(photoUri);
                            if (is != null) {
                                Bitmap original = BitmapFactory.decodeStream(is);
                                is.close();
                                if (original != null) {
                                    Bitmap cropped = cropToFood(original);
                                    ivFoodPreview.setImageBitmap(cropped);
                                    // Animate the food reveal
                                    Animation anim = AnimationUtils.loadAnimation(
                                            requireContext(), R.anim.scanner_success);
                                    ivFoodPreview.startAnimation(anim);
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    if (usedAI) Toast.makeText(requireContext(),
                            "AI estimated nutrition for: " + finalInfo.name,
                            Toast.LENGTH_SHORT).show();

                    saveAndDisplay(savedPath, finalInfo, finalBase64, finalDesc, finalRec,
                            finalEstGrams, finalFistDet);
                });

            } catch (Exception e) {
                final String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Analysis Failed")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .setNeutralButton("Copy", (d, w) -> {
                                android.content.ClipboardManager cm =
                                        (android.content.ClipboardManager)
                                                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                if (cm != null) cm.setPrimaryClip(
                                        android.content.ClipData.newPlainText("error", msg));
                                Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show();
                            })
                            .show();
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Groq API helpers  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private IdentifyResult askGroqToIdentifyFood(String base64Image) throws Exception {
        String prompt =
                "You are an expert food recognition AI. Analyze this photo carefully.\n\n" +
                        "1. Is there food in this image? If not, reply with exactly: NOT_FOOD\n\n" +
                        "2. If there IS food, look for a human fist in the photo. A closed adult fist " +
                        "is approximately 240ml volume and represents roughly 1 cup or about 150–200g " +
                        "of most solid foods. Use the fist as a size reference to calibrate your " +
                        "portion estimate. If no fist is visible, estimate based on the food's " +
                        "appearance alone.\n\n" +
                        "Reply ONLY with this JSON (no markdown, no extra text):\n" +
                        "{\n" +
                        "  \"name\": \"food name in lowercase\",\n" +
                        "  \"description\": \"one sentence about this food\",\n" +
                        "  \"estimated_grams\": 150,\n" +
                        "  \"fist_detected\": true\n" +
                        "}\n\n" +
                        "For estimated_grams: use the fist size as your ruler when visible. " +
                        "Examples without fist — 1 apple: ~180g, 1 slice pizza: ~120g, " +
                        "1 burger: ~200g, bowl of rice: ~250g. " +
                        "Set fist_detected to true only if a hand/fist is clearly visible.\n" +
                        "Rules:\n" +
                        "- Identify ANY food from ANY cuisine worldwide\n" +
                        "- Be specific: prefer 'chicken shawarma' over 'sandwich'\n" +
                        "- Reply ONLY with NOT_FOOD or the JSON.";

        String raw = callGroqVision(prompt, base64Image, 280).trim();

        if (raw.toUpperCase(Locale.getDefault()).contains("NOT_FOOD"))
            return new IdentifyResult(false, null, null, 0, false);

        try {
            JSONObject obj       = new JSONObject(extractJson(raw));
            String     foodName  = obj.optString("name", raw).toLowerCase(Locale.getDefault()).trim();
            String     desc      = obj.optString("description", "");
            int        estGrams  = obj.optInt("estimated_grams", 100);
            boolean    fistDet   = obj.optBoolean("fist_detected", false);
            if (estGrams <= 0) estGrams = 100;
            return new IdentifyResult(true, foodName, desc, estGrams, fistDet);
        } catch (Exception e) {
            String cleaned = raw.toLowerCase(Locale.getDefault())
                    .replaceAll("[^a-z0-9 ]", "").trim();
            return new IdentifyResult(true,
                    cleaned.isEmpty() ? "unknown food" : cleaned, "", 100, false);
        }
    }

    private FoodDatabase.FoodInfo askGroqForNutrition(String foodName) throws Exception {
        String prompt =
                "You are a professional nutritionist. Give accurate nutrition per 100g for: " + foodName + "\n\n" +
                        "Reply ONLY with this exact JSON (no markdown, no extra text):\n" +
                        "{\"calories\":0,\"protein\":0.0,\"carbs\":0.0,\"fats\":0.0," +
                        "\"category\":\"Fruit|Vegetable|Meat|Fish|Grain|Dairy|Snack|Fast Food|Drink|Dessert|Other\"}";

        String raw  = callGroqText(prompt, 200).trim();
        String json = extractJson(raw);

        try {
            JSONObject n = new JSONObject(json);
            return new FoodDatabase.FoodInfo(
                    toTitleCase(foodName),
                    guessEmoji(foodName),
                    n.optString("category", "Other"),
                    n.optInt("calories", 150),
                    (float) n.optDouble("protein", 5.0),
                    (float) n.optDouble("carbs", 15.0),
                    (float) n.optDouble("fats", 5.0));
        } catch (Exception e) {
            return FoodDatabase.unknown(toTitleCase(foodName));
        }
    }

    private String askGroqForRecommendation(String foodName, FoodDatabase.FoodInfo info) {
        try {
            SharedPreferences prefs         = requireContext()
                    .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            String            goalWeightStr = prefs.getString("goal_weight", "");
            String            curWeightStr  = prefs.getString("current_weight", "");

            if (goalWeightStr.isEmpty() && curWeightStr.isEmpty())
                return getDefaultRecommendation(foodName, info);

            StringBuilder prompt = new StringBuilder();
            prompt.append("You are a nutrition expert. A user scanned: ").append(foodName).append(".\n");
            prompt.append("Nutrition per 100g: ").append(info.calories).append(" kcal, ");
            prompt.append(info.protein).append("g protein, ").append(info.carbs).append("g carbs, ").append(info.fats).append("g fats.\n");
            if (!curWeightStr.isEmpty())  prompt.append("Current weight: ").append(curWeightStr).append(" kg.\n");
            if (!goalWeightStr.isEmpty()) prompt.append("Goal weight: ").append(goalWeightStr).append(" kg.\n");
            prompt.append("How many servings or portions of ").append(foodName).append(" can this person eat per day to support their goal?\n");
            prompt.append("Reply in ONE short sentence only, like: You can eat 2 apples per day to support your goal.\n");
            prompt.append("Be specific with quantity and unit (pieces, grams, cups). No extra text.");

            return callGroqText(prompt.toString(), 100).trim();
        } catch (Exception e) {
            return getDefaultRecommendation(foodName, info);
        }
    }

    private String getDefaultRecommendation(String foodName, FoodDatabase.FoodInfo info) {
        int maxServings = info.calories > 0 ? Math.max(1, 400 / info.calories) : 2;
        return "You can eat up to " + maxServings + " servings (100g each) of "
                + foodName + " per day as part of a balanced diet.";
    }

    private String askGroqChat(List<ChatMessage> history, FoodEntry entry) throws Exception {
        JSONArray  messages = new JSONArray();
        JSONObject sys      = new JSONObject();
        sys.put("role", "system");
        sys.put("content",
                "You are a friendly nutrition expert and chef AI. " +
                        "The user scanned: " + entry.name + " " + entry.emoji + ". " +
                        "Nutrition per 100g: " + entry.calories + " kcal, " +
                        entry.protein + "g protein, " + entry.carbs + "g carbs, " +
                        entry.fats + "g fats. Category: " + entry.category + ". " +
                        (entry.fistDetected ? "Portion was estimated using a fist reference (~150-200g). " : "") +
                        "Answer about recipes, health, alternatives, portions, dietary compatibility. " +
                        "Be concise, warm and practical.");
        messages.put(sys);
        for (ChatMessage msg : history) {
            JSONObject m = new JSONObject();
            m.put("role",    msg.role);
            m.put("content", msg.text);
            messages.put(m);
        }
        JSONObject body = new JSONObject();
        body.put("model",       GROQ_CHAT_MODEL);
        body.put("messages",    messages);
        body.put("max_tokens",  1024);
        body.put("temperature", 0.7);
        return parseGroqText(callGroqRaw(body.toString())).trim();
    }

    private String callGroqVision(String prompt, String base64Image, int maxTokens) throws Exception {
        JSONObject imageUrlObj  = new JSONObject();
        imageUrlObj.put("url", "data:image/jpeg;base64," + base64Image);
        JSONObject imagePart    = new JSONObject();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlObj);
        JSONObject textPart     = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        JSONArray  contentArray = new JSONArray();
        contentArray.put(imagePart);
        contentArray.put(textPart);
        JSONObject userMessage  = new JSONObject();
        userMessage.put("role",    "user");
        userMessage.put("content", contentArray);
        JSONArray  messages     = new JSONArray();
        messages.put(userMessage);
        JSONObject body = new JSONObject();
        body.put("model",       GROQ_VISION_MODEL);
        body.put("messages",    messages);
        body.put("max_tokens",  maxTokens);
        body.put("temperature", 0.2);
        return parseGroqText(callGroqRaw(body.toString()));
    }

    private String callGroqText(String prompt, int maxTokens) throws Exception {
        JSONObject userMessage = new JSONObject();
        userMessage.put("role",    "user");
        userMessage.put("content", prompt);
        JSONArray  messages    = new JSONArray();
        messages.put(userMessage);
        JSONObject body        = new JSONObject();
        body.put("model",       GROQ_CHAT_MODEL);
        body.put("messages",    messages);
        body.put("max_tokens",  maxTokens);
        body.put("temperature", 0.2);
        return parseGroqText(callGroqRaw(body.toString()));
    }

    private String callGroqRaw(String jsonBody) throws Exception {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        if (ni == null || !ni.isConnected()) throw new Exception("No internet connection.");

        RequestBody body    = RequestBody.create(jsonBody,
                MediaType.parse("application/json; charset=utf-8"));
        Request     request = new Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) throw new Exception("Empty response from Groq");
            String rb = response.body().string();
            if (!response.isSuccessful()) {
                try {
                    JSONObject err  = new JSONObject(rb);
                    JSONObject errO = err.optJSONObject("error");
                    String     msg  = errO != null ? errO.optString("message", rb) : rb;
                    String     type = errO != null ? errO.optString("type", "")   : "";
                    throw new Exception("Groq " + response.code() + " [" + type + "]\n\n" + msg);
                } catch (org.json.JSONException je) {
                    throw new Exception("Groq " + response.code() + ":\n" + rb);
                }
            }
            return rb;
        } catch (java.net.UnknownHostException e) {
            throw new Exception("Cannot reach Groq. Check internet connection.");
        } catch (java.net.SocketTimeoutException e) {
            throw new Exception("Request timed out. Try again.");
        }
    }

    private String parseGroqText(String body) throws Exception {
        JSONObject json    = new JSONObject(body);
        JSONArray  choices = json.optJSONArray("choices");
        if (choices == null || choices.length() == 0)
            throw new Exception("No response from Groq:\n" + body);
        JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
        if (msg == null) throw new Exception("No message in Groq response");
        return msg.optString("content", "");
    }

    private String extractJson(String raw) {
        String s = raw.replaceAll("```json", "").replaceAll("```", "").trim();
        int start = s.indexOf('{'), end = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Display / navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void saveAndDisplay(String savedImagePath, FoodDatabase.FoodInfo info,
                                String base64, String description, String recommendation,
                                int estimatedGrams, boolean fistDetected) {
        android.content.Intent intent = new android.content.Intent(requireContext(), FoodResultActivity.class);
        intent.putExtra(FoodResultActivity.EXTRA_IMAGE_PATH,     savedImagePath != null ? savedImagePath : "");
        intent.putExtra(FoodResultActivity.EXTRA_NAME,           info.name);
        intent.putExtra(FoodResultActivity.EXTRA_EMOJI,          info.emoji);
        intent.putExtra(FoodResultActivity.EXTRA_CATEGORY,       info.category);
        intent.putExtra(FoodResultActivity.EXTRA_CALORIES,       info.calories);
        intent.putExtra(FoodResultActivity.EXTRA_PROTEIN,        info.protein);
        intent.putExtra(FoodResultActivity.EXTRA_CARBS,          info.carbs);
        intent.putExtra(FoodResultActivity.EXTRA_FATS,           info.fats);
        intent.putExtra(FoodResultActivity.EXTRA_DESCRIPTION,    description   != null ? description   : "");
        intent.putExtra(FoodResultActivity.EXTRA_RECOMMENDATION, recommendation != null ? recommendation : "");
        intent.putExtra(FoodResultActivity.EXTRA_EST_GRAMS,      estimatedGrams > 0 ? estimatedGrams : 100);
        intent.putExtra(FoodResultActivity.EXTRA_FIST_DETECTED,  fistDetected);
        startActivity(intent);
        if (getActivity() != null) getActivity().overridePendingTransition(R.anim.slide_up, 0);
        refreshHistoryUI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Log-serving dialog  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private void showLogServingDialog(FoodEntry entry) {
        if (getContext() == null) return;

        int estGrams    = entry.estimatedGrams > 0 ? entry.estimatedGrams : 100;
        int estCalories = Math.round((entry.calories * estGrams) / 100f);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(8));

        TextView tvInfo = new TextView(requireContext());
        tvInfo.setText(entry.emoji + "  " + entry.name);
        tvInfo.setTextSize(16); tvInfo.setTextColor(0xFFCC5803);
        tvInfo.setTypeface(null, Typeface.BOLD);
        tvInfo.setPadding(0, 0, 0, dpToPx(4));
        layout.addView(tvInfo);

        TextView tvPer100 = new TextView(requireContext());
        tvPer100.setText(entry.calories + " kcal per 100g  ·  " + entry.protein + "g protein  ·  " + entry.carbs + "g carbs  ·  " + entry.fats + "g fats");
        tvPer100.setTextSize(11); tvPer100.setTextColor(0xFF888888);
        tvPer100.setPadding(0, 0, 0, dpToPx(14));
        layout.addView(tvPer100);

        TextView tvAiEstimate = new TextView(requireContext());
        String fistNote = entry.fistDetected ? " (fist reference used ✊)" : "";
        tvAiEstimate.setText("📸 AI estimated: ~" + estGrams + "g in your photo (~" + estCalories + " kcal)" + fistNote);
        tvAiEstimate.setTextSize(12); tvAiEstimate.setTextColor(0xFF4CAF50);
        tvAiEstimate.setTypeface(null, Typeface.BOLD);
        tvAiEstimate.setBackgroundColor(0xFFE8F5E9);
        tvAiEstimate.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        layout.addView(tvAiEstimate);

        TextView tvDivider = new TextView(requireContext());
        tvDivider.setText("— or enter manually —");
        tvDivider.setTextSize(11); tvDivider.setTextColor(0xFFBBBBBB);
        tvDivider.setGravity(Gravity.CENTER);
        tvDivider.setPadding(0, dpToPx(14), 0, dpToPx(6));
        layout.addView(tvDivider);

        TextView tvPortionLabel = new TextView(requireContext());
        tvPortionLabel.setText("Number of portions:");
        tvPortionLabel.setTextSize(13); tvPortionLabel.setTextColor(0xFF333333);
        tvPortionLabel.setPadding(0, 0, 0, dpToPx(6));
        layout.addView(tvPortionLabel);

        LinearLayout portionRow = new LinearLayout(requireContext());
        portionRow.setOrientation(LinearLayout.HORIZONTAL);
        portionRow.setGravity(Gravity.CENTER_VERTICAL);
        portionRow.setPadding(0, 0, 0, dpToPx(8));

        MaterialButton btnMinus = new MaterialButton(requireContext());
        btnMinus.setText("−"); btnMinus.setTextSize(20); btnMinus.setTextColor(0xFFFFFFFF);
        btnMinus.setPadding(0,0,0,0); btnMinus.setInsetTop(0); btnMinus.setInsetBottom(0);
        LinearLayout.LayoutParams minusP = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        btnMinus.setLayoutParams(minusP);
        btnMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFCC5803));
        btnMinus.setCornerRadius(dpToPx(24));

        final int[]   portions    = {1};
        final float   portionGrams= estGrams;

        TextView tvPortionCount = new TextView(requireContext());
        tvPortionCount.setText("1"); tvPortionCount.setTextSize(22);
        tvPortionCount.setTextColor(0xFFCC5803); tvPortionCount.setTypeface(null, Typeface.BOLD);
        tvPortionCount.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvPortionCount.setLayoutParams(countP);

        TextView tvPortionCalc = new TextView(requireContext());
        tvPortionCalc.setTextSize(11); tvPortionCalc.setTextColor(0xFF888888);
        tvPortionCalc.setGravity(Gravity.CENTER);
        int initCal = Math.round((entry.calories * portionGrams) / 100f);
        tvPortionCalc.setText("= " + (int) portionGrams + "g · " + initCal + " kcal");

        MaterialButton btnPlus = new MaterialButton(requireContext());
        btnPlus.setText("+"); btnPlus.setTextSize(20); btnPlus.setTextColor(0xFFFFFFFF);
        btnPlus.setPadding(0,0,0,0); btnPlus.setInsetTop(0); btnPlus.setInsetBottom(0);
        LinearLayout.LayoutParams plusP = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        btnPlus.setLayoutParams(plusP);
        btnPlus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFCC5803));
        btnPlus.setCornerRadius(dpToPx(24));

        btnMinus.setOnClickListener(v -> {
            if (portions[0] > 1) {
                portions[0]--;
                tvPortionCount.setText(String.valueOf(portions[0]));
                float totalG = portionGrams * portions[0];
                int   cal    = Math.round((entry.calories * totalG) / 100f);
                tvPortionCalc.setText("= " + (int) totalG + "g · " + cal + " kcal");
            }
        });
        btnPlus.setOnClickListener(v -> {
            portions[0]++;
            tvPortionCount.setText(String.valueOf(portions[0]));
            float totalG = portionGrams * portions[0];
            int   cal    = Math.round((entry.calories * totalG) / 100f);
            tvPortionCalc.setText("= " + (int) totalG + "g · " + cal + " kcal");
        });

        portionRow.addView(btnMinus);
        portionRow.addView(tvPortionCount);
        portionRow.addView(btnPlus);
        layout.addView(portionRow);
        layout.addView(tvPortionCalc);

        TextView tvCustomLabel = new TextView(requireContext());
        tvCustomLabel.setText("Or enter exact grams:");
        tvCustomLabel.setTextSize(13); tvCustomLabel.setTextColor(0xFF333333);
        tvCustomLabel.setPadding(0, dpToPx(14), 0, dpToPx(6));
        layout.addView(tvCustomLabel);

        com.google.android.material.textfield.TextInputLayout til =
                new com.google.android.material.textfield.TextInputLayout(requireContext());
        til.setHint("Custom grams (optional)");
        til.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        com.google.android.material.textfield.TextInputEditText etGrams =
                new com.google.android.material.textfield.TextInputEditText(requireContext());
        etGrams.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        etGrams.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etGrams.setTextColor(0xFF000000);
        til.addView(etGrams);
        layout.addView(til);

        new AlertDialog.Builder(requireContext())
                .setTitle("🍽️ Log What You Ate")
                .setView(layout)
                .setPositiveButton("Log", (dialog, which) -> {
                    float finalGrams;
                    String customInput = etGrams.getText() != null ? etGrams.getText().toString().trim() : "";
                    if (!customInput.isEmpty()) {
                        try { finalGrams = Float.parseFloat(customInput); }
                        catch (NumberFormatException ex) { finalGrams = portionGrams * portions[0]; }
                    } else { finalGrams = portionGrams * portions[0]; }
                    int loggedCalories = Math.round((entry.calories * finalGrams) / 100f);
                    int loggedProtein  = Math.round((entry.protein  * finalGrams) / 100f);
                    int loggedCarbs    = Math.round((entry.carbs    * finalGrams) / 100f);
                    int loggedFats     = Math.round((entry.fats     * finalGrams) / 100f);
                    logCaloriesToday(loggedCalories, entry.name);
                    if (tvLoggedCalories != null) {
                        tvLoggedCalories.setText("✅ Logged " + (int) finalGrams + "g — " +
                                loggedCalories + " kcal  💪 " + loggedProtein + "g  🌾 " + loggedCarbs + "g  🫙 " + loggedFats + "g");
                        tvLoggedCalories.setVisibility(View.VISIBLE);
                    }
                    loadTodayCalories();
                    Toast.makeText(requireContext(), "✅ " + loggedCalories + " kcal added to today!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Use AI estimate", (dialog, which) -> {
                    int loggedCalories = Math.round((entry.calories * estGrams) / 100f);
                    int loggedProtein  = Math.round((entry.protein  * estGrams) / 100f);
                    int loggedCarbs    = Math.round((entry.carbs    * estGrams) / 100f);
                    int loggedFats     = Math.round((entry.fats     * estGrams) / 100f);
                    logCaloriesToday(loggedCalories, entry.name);
                    if (tvLoggedCalories != null) {
                        tvLoggedCalories.setText("✅ AI logged " + estGrams + "g — " +
                                loggedCalories + " kcal  💪 " + loggedProtein + "g  🌾 " + loggedCarbs + "g  🫙 " + loggedFats + "g");
                        tvLoggedCalories.setVisibility(View.VISIBLE);
                    }
                    loadTodayCalories();
                    Toast.makeText(requireContext(), "✅ " + loggedCalories + " kcal added to today!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logCaloriesToday(int calories, String foodName) {
        try {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            String    json     = prefs.getString(KEY_HISTORY, "[]");
            JSONArray arr      = new JSONArray(json);
            JSONObject logEntry = new JSONObject();
            logEntry.put("id",             UUID.randomUUID().toString());
            logEntry.put("name",           foodName);
            logEntry.put("emoji",          "🍽️");
            logEntry.put("category",       "Logged");
            logEntry.put("date",           new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(new Date()));
            logEntry.put("calories",       calories);
            logEntry.put("protein",        0);
            logEntry.put("carbs",          0);
            logEntry.put("fats",           0);
            logEntry.put("savedImagePath", "");
            logEntry.put("aiDescription",  "");
            logEntry.put("recommendation", "");
            JSONArray newArr = new JSONArray();
            newArr.put(logEntry);
            for (int i = 0; i < arr.length() && i < MAX_ENTRIES - 1; i++) newArr.put(arr.getJSONObject(i));
            prefs.edit().putString(KEY_HISTORY, newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History UI  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshHistoryUI() {
        layoutHistoryContainer.removeAllViews();
        if (historyList.isEmpty()) {
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            tvHistoryCount.setText("0 items");
            return;
        }
        layoutEmptyHistory.setVisibility(View.GONE);
        tvHistoryCount.setText("Today · " + historyList.size() + (historyList.size() == 1 ? " item" : " items"));
        for (int i = 0; i < historyList.size(); i++)
            layoutHistoryContainer.addView(buildHistoryCard(historyList.get(i), i));
    }

    private View buildHistoryCard(FoodEntry entry, int index) {
        CardView card = new CardView(requireContext());
        CardView.LayoutParams cp = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dpToPx(10);
        card.setLayoutParams(cp);
        card.setRadius(dpToPx(14));
        card.setCardElevation(dpToPx(3));

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        row.setBackgroundColor(0xFFFFFFFF);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView img = new ImageView(requireContext());
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dpToPx(68), dpToPx(68));
        ip.rightMargin = dpToPx(12);
        img.setLayoutParams(ip);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(0xFFFFF0D6);

        if (entry.savedImagePath != null && !entry.savedImagePath.isEmpty()) {
            File f = new File(entry.savedImagePath);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(entry.savedImagePath);
                if (bmp != null) img.setImageBitmap(bmp);
                else             img.setImageURI(entry.imageUri);
            }
        } else if (entry.imageUri != null) {
            img.setImageURI(entry.imageUri);
        }

        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(requireContext());
        tvName.setText(entry.emoji + "  " + entry.name + (entry.fistDetected ? "  ✊" : ""));
        tvName.setTextSize(15); tvName.setTextColor(0xFFCC5803);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvDate = new TextView(requireContext());
        tvDate.setText(entry.date + "  ·  " + entry.category);
        tvDate.setTextSize(11); tvDate.setTextColor(0xFF9CA3AF);

        TextView tvNutrition = new TextView(requireContext());
        tvNutrition.setText("🔥 " + entry.calories + " kcal   protein " + entry.protein
                + "g   carbs " + entry.carbs + "g   fat " + entry.fats + "g");
        tvNutrition.setTextSize(11); tvNutrition.setTextColor(0xFF6B7280);
        tvNutrition.setPadding(0, dpToPx(4), 0, 0);

        info.addView(tvName);
        info.addView(tvDate);
        info.addView(tvNutrition);

        if (entry.aiDescription != null && !entry.aiDescription.isEmpty()) {
            TextView tvDesc = new TextView(requireContext());
            tvDesc.setText("ℹ️ " + entry.aiDescription);
            tvDesc.setTextSize(10); tvDesc.setTextColor(0xFFE2711D);
            tvDesc.setPadding(0, dpToPx(3), 0, 0);
            tvDesc.setMaxLines(2);
            tvDesc.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(tvDesc);
        }

        if (entry.recommendation != null && !entry.recommendation.isEmpty()) {
            TextView tvRec = new TextView(requireContext());
            tvRec.setText("🎯 " + entry.recommendation);
            tvRec.setTextSize(10); tvRec.setTextColor(0xFFCC5803);
            tvRec.setTypeface(null, Typeface.BOLD);
            tvRec.setPadding(0, dpToPx(3), 0, 0);
            tvRec.setMaxLines(2);
            tvRec.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(tvRec);
        }

        TextView btnAi = new TextView(requireContext());
        btnAi.setText("🤖"); btnAi.setTextSize(20);
        btnAi.setPadding(dpToPx(6), 0, dpToPx(6), 0);
        btnAi.setOnClickListener(v -> openAiChat(entry));

        TextView btnDel = new TextView(requireContext());
        btnDel.setText("🗑"); btnDel.setTextSize(20);
        btnDel.setPadding(dpToPx(4), 0, 0, 0);
        btnDel.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Remove entry")
                        .setMessage("Remove " + entry.name + " from history?")
                        .setPositiveButton("Remove", (d, w) -> deleteEntry(index))
                        .setNegativeButton("Cancel", null).show());

        row.addView(img);
        row.addView(info);
        row.addView(btnAi);
        row.addView(btnDel);
        card.addView(row);
        card.setOnClickListener(v -> openAiChat(entry));
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI Chat dialog  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private void openAiChat(FoodEntry entry) {
        if (getContext() == null) return;

        if (entry.base64Image == null && entry.savedImagePath != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(entry.savedImagePath);
                if (bmp != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                    entry.base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                }
            } catch (Exception ignored) {}
        }

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0xFFFFF5E6);

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(0xFFCC5803);
        header.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("🤖 Ask AI about " + entry.emoji + " " + entry.name);
        tvTitle.setTextColor(0xFFFFFFFF); tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvSub = new TextView(requireContext());
        tvSub.setText("Powered by Groq · Free & Fast ⚡");
        tvSub.setTextColor(0xAAFFFFFF); tvSub.setTextSize(11);
        tvSub.setPadding(0, dpToPx(2), 0, 0);

        header.addView(tvTitle);
        header.addView(tvSub);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(360)));
        scrollView.setBackgroundColor(0xFFFFF5E6);

        LinearLayout chatContainer = new LinearLayout(requireContext());
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        scrollView.addView(chatContainer);

        LinearLayout inputRow = new LinearLayout(requireContext());
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(10));
        inputRow.setBackgroundColor(0xFFFFFFFF);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        EditText etInput = new EditText(requireContext());
        etInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        etInput.setHint("Ask about recipes, nutrition, health...");
        etInput.setHintTextColor(0xFF9CA3AF);
        etInput.setTextColor(0xFFCC5803);
        etInput.setTextSize(14);
        etInput.setBackgroundColor(0xFFFFF0D6);
        etInput.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        etInput.setMaxLines(3);

        MaterialButton btnSend = new MaterialButton(requireContext());
        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(dpToPx(52), dpToPx(44));
        btnP.leftMargin = dpToPx(8);
        btnSend.setLayoutParams(btnP);
        btnSend.setText("➤"); btnSend.setTextSize(16);
        btnSend.setPadding(0, 0, 0, 0);
        btnSend.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFCC5803));
        btnSend.setCornerRadius(dpToPx(10));

        inputRow.addView(etInput);
        inputRow.addView(btnSend);

        root.addView(header);
        root.addView(scrollView);
        root.addView(inputRow);

        List<ChatMessage> chatHistory = new ArrayList<>();

        String greeting = entry.emoji + " " + entry.name + "\n" +
                "🔥 " + entry.calories + " kcal  💪 " + entry.protein + "g  " +
                "🌾 " + entry.carbs + "g  🫙 " + entry.fats + "g\n\n";
        if (entry.fistDetected)
            greeting += "✊ Portion estimated with fist reference (~" + entry.estimatedGrams + "g)\n\n";
        if (entry.aiDescription != null && !entry.aiDescription.isEmpty())
            greeting += "ℹ️ " + entry.aiDescription + "\n\n";
        if (entry.recommendation != null && !entry.recommendation.isEmpty())
            greeting += "🎯 " + entry.recommendation + "\n\n";
        greeting += "Ask me anything! 🌿";
        addChatBubble(chatContainer, scrollView, greeting, false);
        addSuggestionChips(chatContainer, scrollView, etInput, btnSend, entry);

        btnSend.setOnClickListener(v -> {
            String userText = etInput.getText().toString().trim();
            if (userText.isEmpty()) return;
            etInput.setText("");
            hideSoftKeyboard(etInput);
            addChatBubble(chatContainer, scrollView, userText, true);
            TextView loadingBubble = addChatBubble(chatContainer, scrollView, "⏳ Thinking...", false);
            btnSend.setEnabled(false);
            chatHistory.add(new ChatMessage("user", userText));

            new Thread(() -> {
                try {
                    String reply = askGroqChat(chatHistory, entry);
                    chatHistory.add(new ChatMessage("assistant", reply));
                    new Handler(Looper.getMainLooper()).post(() -> {
                        chatContainer.removeView(loadingBubble);
                        addChatBubble(chatContainer, scrollView, reply, false);
                        btnSend.setEnabled(true);
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        chatContainer.removeView(loadingBubble);
                        addChatBubble(chatContainer, scrollView, "Sorry, error: " + e.getMessage(), false);
                        btnSend.setEnabled(true);
                    });
                }
            }).start();
        });

        new AlertDialog.Builder(requireContext())
                .setView(root)
                .setNegativeButton("Close", null)
                .create().show();
    }

    private void addSuggestionChips(LinearLayout chatContainer, ScrollView scrollView,
                                    EditText etInput, MaterialButton btnSend, FoodEntry entry) {
        String[] suggestions = {
                "Give me a recipe with " + entry.name,
                "Is " + entry.name + " healthy?",
                "Healthier alternatives?",
                "Best time to eat this?"
        };
        android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(requireContext());
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dpToPx(6); hp.bottomMargin = dpToPx(4);
        hScroll.setLayoutParams(hp);
        hScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout chipContainer = new LinearLayout(requireContext());
        chipContainer.setOrientation(LinearLayout.HORIZONTAL);
        for (String s : suggestions) {
            TextView chip = new TextView(requireContext());
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.rightMargin = dpToPx(8);
            chip.setLayoutParams(cp);
            chip.setText(s.length() > 28 ? s.substring(0, 26) + "…" : s);
            chip.setTextSize(11); chip.setTextColor(0xFFCC5803);
            chip.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xFFFFF0D6); bg.setCornerRadius(dpToPx(20)); bg.setStroke(1, 0xFFCC5803);
            chip.setBackground(bg);
            chip.setOnClickListener(v -> { etInput.setText(s); btnSend.performClick(); });
            chipContainer.addView(chip);
        }
        hScroll.addView(chipContainer);
        chatContainer.addView(hScroll);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private TextView addChatBubble(LinearLayout container, ScrollView scroll,
                                   String text, boolean isUser) {
        LinearLayout wrapper = new LinearLayout(requireContext());
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wp.topMargin = dpToPx(6);
        wrapper.setLayoutParams(wp);
        wrapper.setGravity(isUser ? Gravity.END : Gravity.START);

        TextView bubble = new TextView(requireContext());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.leftMargin  = isUser ? dpToPx(48) : 0;
        bp.rightMargin = isUser ? 0 : dpToPx(48);
        bubble.setLayoutParams(bp);
        bubble.setText(text); bubble.setTextSize(13);
        bubble.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        bubble.setLineSpacing(dpToPx(2), 1f);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(isUser ? 0xFFCC5803 : 0xFFFFFFFF);
        bg.setCornerRadii(isUser
                ? new float[]{dpToPx(16), dpToPx(16), 0, 0, dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16)}
                : new float[]{0, 0, dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16)});
        if (!isUser) bg.setStroke(1, 0xFFFFB627);
        bubble.setBackground(bg);
        bubble.setTextColor(isUser ? 0xFFFFFFFF : 0xFFCC5803);

        wrapper.addView(bubble);
        container.addView(wrapper);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
        return bubble;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Food matching helpers  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private static final String[] ALL_KEYS = {
            "apple","banana","orange","grapes","strawberry","watermelon","pineapple",
            "mango","peach","pear","cherry","kiwi","avocado","blueberry","lemon","coconut",
            "broccoli","carrot","tomato","potato","onion","garlic","cucumber","spinach",
            "corn","pepper","mushroom","eggplant","cabbage","sweet potato",
            "chicken","chicken breast","beef","salmon","tuna","shrimp","egg","tofu",
            "milk","cheese","yogurt","butter","bread","rice","pasta","oats",
            "almond","almonds","peanut","walnut",
            "pizza","burger","french fries","chocolate",
            "orange juice","coffee","green tea"
    };

    private FoodDatabase.FoodInfo findBestMatch(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String name = raw.toLowerCase(Locale.getDefault())
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\b(grilled|fried|fresh|baked|cooked|whole|sliced|raw|ripe|"
                        + "a |an |the |some |piece of |bowl of |plate of )\\b", "")
                .replaceAll("\\s+", " ").trim();
        FoodDatabase.FoodInfo r = FoodDatabase.find(name);
        if (r != null) return r;
        r = FoodDatabase.find(raw.toLowerCase(Locale.getDefault()).trim());
        if (r != null) return r;
        for (String word : name.split(" ")) {
            if (word.length() < 3) continue;
            r = FoodDatabase.find(word);
            if (r != null) return r;
        }
        for (String key : ALL_KEYS) {
            if (name.contains(key) || key.contains(name)) {
                r = FoodDatabase.find(key);
                if (r != null) return r;
            }
        }
        return null;
    }

    private String uriToBase64(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) return null;
            int maxDim = 512, w = bmp.getWidth(), h = bmp.getHeight();
            if (w > maxDim || h > maxDim) {
                float s = Math.min((float) maxDim / w, (float) maxDim / h);
                bmp = Bitmap.createScaledBitmap(bmp, Math.round(w * s), Math.round(h * s), true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

    private String toTitleCase(String s) {
        if (s == null || s.isEmpty()) return "Unknown Food";
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" "))
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private String guessEmoji(String name) {
        if (name == null) return "🍽️";
        name = name.toLowerCase(Locale.getDefault());
        if (name.contains("pizza"))                               return "🍕";
        if (name.contains("burger"))                              return "🍔";
        if (name.contains("chicken"))                             return "🍗";
        if (name.contains("salad"))                               return "🥗";
        if (name.contains("rice"))                                return "🍚";
        if (name.contains("pasta") || name.contains("spaghetti")) return "🍝";
        if (name.contains("apple"))                               return "🍎";
        if (name.contains("banana"))                              return "🍌";
        if (name.contains("orange"))                              return "🍊";
        if (name.contains("steak") || name.contains("beef"))      return "🥩";
        if (name.contains("fish") || name.contains("salmon"))     return "🐟";
        if (name.contains("egg"))                                  return "🥚";
        if (name.contains("bread"))                                return "🍞";
        if (name.contains("soup"))                                 return "🍜";
        if (name.contains("cake"))                                 return "🎂";
        if (name.contains("cookie"))                               return "🍪";
        if (name.contains("ice cream"))                            return "🍦";
        if (name.contains("coffee"))                               return "☕";
        if (name.contains("juice"))                                return "🧃";
        if (name.contains("sandwich"))                             return "🥪";
        if (name.contains("taco"))                                 return "🌮";
        if (name.contains("sushi"))                                return "🍱";
        if (name.contains("kebab") || name.contains("shawarma"))   return "🥙";
        if (name.contains("dumpling") || name.contains("khinkali"))return "🥟";
        if (name.contains("curry"))                                return "🍛";
        if (name.contains("waffle"))                               return "🧇";
        if (name.contains("pancake"))                              return "🥞";
        if (name.contains("donut"))                                return "🍩";
        if (name.contains("tea"))                                   return "🍵";
        if (name.contains("cheese"))                               return "🧀";
        if (name.contains("shrimp"))                               return "🦐";
        if (name.contains("hot dog"))                              return "🌭";
        if (name.contains("fries"))                                return "🍟";
        return "🍽️";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Calorie arc  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadTodayCalories() {
        if (!isAdded()) return;
        try {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            dailyCalorieGoal = prefs.getInt("daily_calorie_goal", 2000);
            String json      = prefs.getString(KEY_HISTORY, null);
            int    totalConsumed = 0;
            if (json != null && !json.isEmpty()) {
                String    today = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());
                JSONArray arr   = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.getJSONObject(i);
                    if (e.optString("date", "").startsWith(today))
                        totalConsumed += e.optInt("calories", 0);
                }
            }
            updateCalorieArc(totalConsumed);
        } catch (Exception e) { updateCalorieArc(0); }
    }

    private void updateCalorieArc(int consumed) {
        int remaining = Math.max(0, dailyCalorieGoal - consumed);
        if (homeCalorieArcView != null) homeCalorieArcView.setValues(consumed, dailyCalorieGoal);
        if (homeTvDailyCalorieGoal != null)
            homeTvDailyCalorieGoal.setText("Daily goal: " + dailyCalorieGoal + " kcal");
        if (homeTvCaloriesRemaining != null) {
            if (consumed >= dailyCalorieGoal) {
                homeTvCaloriesRemaining.setText("🎉 Goal reached! +" + (consumed - dailyCalorieGoal) + " kcal over");
                homeTvCaloriesRemaining.setTextColor(0xFF4CAF50);
            } else {
                homeTvCaloriesRemaining.setText(remaining + " kcal remaining today");
                homeTvCaloriesRemaining.setTextColor(0xFFCC5803);
            }
        }
        if (homeTvCaloriesLabel != null) {
            float  pct   = dailyCalorieGoal > 0 ? (float) consumed / dailyCalorieGoal : 0f;
            String label; int color;
            if (consumed == 0)      { label = "No food logged today";              color = 0xFF2196F3; }
            else if (pct < 0.60f)   { label = "Keep eating! Under 60% of your goal"; color = 0xFFE53935; }
            else if (pct < 1.0f)    { label = "Great progress! Almost at your goal"; color = 0xFFFFC107; }
            else                    { label = "🎉 Daily calorie goal reached!";      color = 0xFF4CAF50; }
            homeTvCaloriesLabel.setText(label);
            homeTvCaloriesLabel.setTextColor(color);
        }
    }

    private void hideSoftKeyboard(EditText et) {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}