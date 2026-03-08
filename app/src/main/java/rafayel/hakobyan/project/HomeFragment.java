/*package rafayel.hakobyan.project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private static final String LOGMEAL_API_KEY = "your_logmeal_token_here";

    // ── History entry ──────────────────────────────────────────────────────
    static class FoodEntry {
        Uri    imageUri;
        String name;
        String emoji;
        String category;
        int    calories;
        float  protein;
        float  carbs;
        float  fats;
        String date;

        FoodEntry(Uri imageUri, FoodDatabase.FoodInfo info, String date) {
            this.imageUri = imageUri;
            this.name     = info.name;
            this.emoji    = info.emoji;
            this.category = info.category;
            this.calories = info.calories;
            this.protein  = info.protein;
            this.carbs    = info.carbs;
            this.fats     = info.fats;
            this.date     = date;
        }
    }

    // ── Views ──────────────────────────────────────────────────────────────
    private CardView       cardCamera;
    private ImageView      ivFoodPreview;
    private LinearLayout   layoutPlaceholder;
    private FrameLayout    layoutLoading;
    private ImageButton    btnRetake;
    private MaterialButton btnCamera, btnGallery;
    private CardView       cardResult;
    private TextView       tvFoodEmoji, tvFoodName;
    private TextView       tvCalories, tvProtein, tvCarbs, tvFats;
    private LinearLayout   layoutHistoryContainer;
    private LinearLayout   layoutEmptyHistory;
    private TextView       tvHistoryCount;

    // ── State ──────────────────────────────────────────────────────────────
    private Uri photoUri;
    private final List<FoodEntry> historyList = new ArrayList<>();

    // ── Launchers ──────────────────────────────────────────────────────────

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

    // ── Lifecycle ──────────────────────────────────────────────────────────

    public HomeFragment() {}

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
        refreshHistoryUI();
    }

    // ── Bind views ─────────────────────────────────────────────────────────

    private void bindViews(View root) {
        cardCamera             = root.findViewById(R.id.cardCamera);
        ivFoodPreview          = root.findViewById(R.id.ivFoodPreview);
        layoutPlaceholder      = root.findViewById(R.id.layoutPlaceholder);
        layoutLoading          = root.findViewById(R.id.layoutLoading);
        btnRetake              = root.findViewById(R.id.btnRetake);
        btnCamera              = root.findViewById(R.id.btnCamera);
        btnGallery             = root.findViewById(R.id.btnGallery);
        cardResult             = root.findViewById(R.id.cardResult);
        tvFoodEmoji            = root.findViewById(R.id.tvFoodEmoji);
        tvFoodName             = root.findViewById(R.id.tvFoodName);
        tvCalories             = root.findViewById(R.id.tvCalories);
        tvProtein              = root.findViewById(R.id.tvProtein);
        tvCarbs                = root.findViewById(R.id.tvCarbs);
        tvFats                 = root.findViewById(R.id.tvFats);
        layoutHistoryContainer = root.findViewById(R.id.layoutHistoryContainer);
        layoutEmptyHistory     = root.findViewById(R.id.layoutEmptyHistory);
        tvHistoryCount         = root.findViewById(R.id.tvHistoryCount);
    }

    // ── Listeners ──────────────────────────────────────────────────────────

    private void setupListeners() {
        cardCamera.setOnClickListener(v -> requestCameraAndOpen());
        btnCamera.setOnClickListener(v  -> requestCameraAndOpen());
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnRetake.setOnClickListener(v  -> resetScanUI());
    }

    // ── Camera ─────────────────────────────────────────────────────────────

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
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            File imageFile = File.createTempFile("FOOD_" + timestamp, ".jpg",
                    requireContext().getExternalFilesDir(null));
            photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not open camera.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Scan UI ────────────────────────────────────────────────────────────

    private void showPhotoPreview(Uri uri) {
        ivFoodPreview.setImageURI(uri);
        ivFoodPreview.setVisibility(View.VISIBLE);
        layoutPlaceholder.setVisibility(View.GONE);
        btnRetake.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void resetScanUI() {
        ivFoodPreview.setVisibility(View.GONE);
        layoutPlaceholder.setVisibility(View.VISIBLE);
        btnRetake.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);
        photoUri = null;
    }

    // ── Analyze food ───────────────────────────────────────────────────────

    private void analyzeFood(Uri imageUri) {
        setLoading(true);
        cardResult.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                // Read image bytes
                InputStream inputStream = requireContext()
                        .getContentResolver().openInputStream(imageUri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] bytes = buffer.toByteArray();
                inputStream.close();

                // Build multipart request for LogMeal
                okhttp3.MultipartBody requestBody = new okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("image", "food.jpg",
                                RequestBody.create(bytes, MediaType.parse("image/jpeg")))
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.logmeal.com/v2/image/recognition/complete")
                        .addHeader("Authorization", "Bearer " + LOGMEAL_API_KEY)
                        .post(requestBody)
                        .build();

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                // Show first 200 chars in log to debug
                android.util.Log.d("LOGMEAL", responseBody);

                JSONObject jsonResponse = new JSONObject(responseBody);

                // Try all possible field names
                JSONArray recognition = null;
                String topRawName = "unknown";

                if (jsonResponse.has("recognition_results")) {
                    recognition = jsonResponse.getJSONArray("recognition_results");
                } else if (jsonResponse.has("dishes")) {
                    recognition = jsonResponse.getJSONArray("dishes");
                } else if (jsonResponse.has("foodName")) {
                    recognition = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.put("name", jsonResponse.getString("foodName"));
                    recognition.put(obj);
                } else if (jsonResponse.has("segmentation_results")) {
                    JSONArray seg = jsonResponse.getJSONArray("segmentation_results");
                    recognition = new JSONArray();
                    for (int i = 0; i < seg.length(); i++) {
                        JSONObject s = seg.getJSONObject(i);
                        if (s.has("recognition_results")) {
                            JSONArray inner = s.getJSONArray("recognition_results");
                            for (int j = 0; j < inner.length(); j++) {
                                recognition.put(inner.getJSONObject(j));
                            }
                        }
                    }
                } else {
                    // Show raw response in toast to debug
                    String raw = jsonResponse.toString();
                    throw new Exception(raw.substring(0, Math.min(200, raw.length())));
                }

                FoodDatabase.FoodInfo matched = null;

                if (recognition != null) {
                    for (int i = 0; i < Math.min(5, recognition.length()); i++) {
                        JSONObject item = recognition.getJSONObject(i);
                        String rawName = "";

                        if (item.has("name")) {
                            rawName = item.getString("name");
                        } else if (item.has("foodName")) {
                            rawName = item.getString("foodName");
                        }

                        rawName = rawName.toLowerCase(Locale.getDefault())
                                .replace(" fruit", "")
                                .replace(" vegetable", "")
                                .replace(" food", "")
                                .replace("grilled ", "")
                                .replace("fried ", "")
                                .replace("fresh ", "")
                                .replace("baked ", "")
                                .replace("cooked ", "")
                                .replace("whole ", "")
                                .replace("sliced ", "")
                                .replace("raw ", "")
                                .trim();

                        if (i == 0) topRawName = rawName;

                        matched = FoodDatabase.find(rawName);
                        if (matched == null) {
                            matched = FoodDatabase.find(rawName.split(" ")[0]);
                        }
                        if (matched == null) {
                            String[] words = rawName.split(" ");
                            matched = FoodDatabase.find(words[words.length - 1]);
                        }
                        if (matched != null) break;
                    }
                }

                final FoodDatabase.FoodInfo finalInfo = matched != null
                        ? matched
                        : FoodDatabase.unknown(topRawName);
                final boolean notFound = matched == null;

                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    if (notFound) {
                        Toast.makeText(requireContext(),
                                "Food not in database: " + finalInfo.name,
                                Toast.LENGTH_SHORT).show();
                    }
                    saveAndDisplay(imageUri, finalInfo);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Recognition failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Save + display result ──────────────────────────────────────────────

    private void saveAndDisplay(Uri imageUri, FoodDatabase.FoodInfo info) {
        tvFoodEmoji.setText(info.emoji);
        tvFoodName.setText(info.name);
        tvCalories.setText(info.calories + " kcal");
        tvProtein.setText(info.protein + "g");
        tvCarbs.setText(info.carbs + "g");
        tvFats.setText(info.fats + "g");
        cardResult.setVisibility(View.VISIBLE);
        cardResult.startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in));

        String date = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(new Date());
        historyList.add(0, new FoodEntry(imageUri, info, date));
        refreshHistoryUI();
    }

    // ── History UI ─────────────────────────────────────────────────────────

    private void refreshHistoryUI() {
        layoutHistoryContainer.removeAllViews();

        if (historyList.isEmpty()) {
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            tvHistoryCount.setText("0 items");
            return;
        }

        layoutEmptyHistory.setVisibility(View.GONE);
        tvHistoryCount.setText(historyList.size() + (historyList.size() == 1 ? " item" : " items"));

        for (int i = 0; i < historyList.size(); i++) {
            layoutHistoryContainer.addView(buildHistoryCard(historyList.get(i), i));
        }
    }

    private View buildHistoryCard(FoodEntry entry, int index) {
        CardView card = new CardView(requireContext());
        CardView.LayoutParams cardParams = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dpToPx(10);
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(14));
        card.setCardElevation(dpToPx(3));

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        row.setBackgroundColor(0xFFFFFFFF);

        ImageView img = new ImageView(requireContext());
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(64));
        imgParams.rightMargin = dpToPx(12);
        img.setLayoutParams(imgParams);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageURI(entry.imageUri);
        img.setBackgroundColor(0xFFE8F5E9);

        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(requireContext());
        tvName.setText(entry.emoji + "  " + entry.name);
        tvName.setTextSize(15);
        tvName.setTextColor(0xFF1B4332);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(requireContext());
        tvDate.setText(entry.date + "  ·  " + entry.category);
        tvDate.setTextSize(11);
        tvDate.setTextColor(0xFF9CA3AF);

        TextView tvNutrition = new TextView(requireContext());
        tvNutrition.setText("🔥 " + entry.calories + " kcal  💪 " + entry.protein
                + "g  🌾 " + entry.carbs + "g  🫐 " + entry.fats + "g");
        tvNutrition.setTextSize(11);
        tvNutrition.setTextColor(0xFF6B7280);
        tvNutrition.setPadding(0, dpToPx(4), 0, 0);

        info.addView(tvName);
        info.addView(tvDate);
        info.addView(tvNutrition);

        TextView btnDelete = new TextView(requireContext());
        btnDelete.setText("🗑");
        btnDelete.setTextSize(20);
        btnDelete.setPadding(dpToPx(8), 0, 0, 0);

        row.addView(img);
        row.addView(info);
        row.addView(btnDelete);
        card.addView(row);

        card.setOnClickListener(v -> showDetailDialog(entry));

        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Remove entry")
                        .setMessage("Remove " + entry.name + " from your history?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            historyList.remove(index);
                            refreshHistoryUI();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        return card;
    }

    private void showDetailDialog(FoodEntry entry) {
        String message =
                entry.emoji + "  " + entry.name + "\n" +
                        "Category: " + entry.category + "\n\n" +
                        "📅 " + entry.date + "\n\n" +
                        "🔥 Calories:  " + entry.calories + " kcal\n" +
                        "💪 Protein:   " + entry.protein + "g\n" +
                        "🌾 Carbs:     " + entry.carbs + "g\n" +
                        "🫐 Fats:      " + entry.fats + "g";

        new AlertDialog.Builder(requireContext())
                .setTitle("Nutrition Info")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .setNeutralButton("🗑 Remove", (dialog, which) -> {
                    int idx = historyList.indexOf(entry);
                    if (idx != -1) {
                        historyList.remove(idx);
                        refreshHistoryUI();
                    }
                })
                .show();
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}

 */
package rafayel.hakobyan.project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rafayel.hakobyan.project.R;

public class HomeFragment extends Fragment {

    // ── History entry ──────────────────────────────────────────────────────
    static class FoodEntry {
        Uri    imageUri;
        String name;
        String emoji;
        String category;
        int    calories;
        float  protein;
        float  carbs;
        float  fats;
        String date;

        FoodEntry(Uri imageUri, FoodDatabase.FoodInfo info, String date) {
            this.imageUri = imageUri;
            this.name     = info.name;
            this.emoji    = info.emoji;
            this.category = info.category;
            this.calories = info.calories;
            this.protein  = info.protein;
            this.carbs    = info.carbs;
            this.fats     = info.fats;
            this.date     = date;
        }
    }

    // ── Views ──────────────────────────────────────────────────────────────
    private CardView       cardCamera;
    private ImageView      ivFoodPreview;
    private LinearLayout   layoutPlaceholder;
    private FrameLayout    layoutLoading;
    private ImageButton    btnRetake;
    private MaterialButton btnCamera, btnGallery;
    private CardView       cardResult;
    private TextView       tvFoodEmoji, tvFoodName;
    private TextView       tvCalories, tvProtein, tvCarbs, tvFats;
    private LinearLayout   layoutHistoryContainer;
    private LinearLayout   layoutEmptyHistory;
    private TextView       tvHistoryCount;

    // ── State ──────────────────────────────────────────────────────────────
    private Uri photoUri;
    private final List<FoodEntry> historyList = new ArrayList<>();

    // ── Launchers ──────────────────────────────────────────────────────────

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

    // ── Lifecycle ──────────────────────────────────────────────────────────

    public HomeFragment() {}

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
        refreshHistoryUI();
    }

    // ── Bind views ─────────────────────────────────────────────────────────

    private void bindViews(View root) {
        cardCamera             = root.findViewById(R.id.cardCamera);
        ivFoodPreview          = root.findViewById(R.id.ivFoodPreview);
        layoutPlaceholder      = root.findViewById(R.id.layoutPlaceholder);
        layoutLoading          = root.findViewById(R.id.layoutLoading);
        btnRetake              = root.findViewById(R.id.btnRetake);
        btnCamera              = root.findViewById(R.id.btnCamera);
        btnGallery             = root.findViewById(R.id.btnGallery);
        cardResult             = root.findViewById(R.id.cardResult);
        tvFoodEmoji            = root.findViewById(R.id.tvFoodEmoji);
        tvFoodName             = root.findViewById(R.id.tvFoodName);
        tvCalories             = root.findViewById(R.id.tvCalories);
        tvProtein              = root.findViewById(R.id.tvProtein);
        tvCarbs                = root.findViewById(R.id.tvCarbs);
        tvFats                 = root.findViewById(R.id.tvFats);
        layoutHistoryContainer = root.findViewById(R.id.layoutHistoryContainer);
        layoutEmptyHistory     = root.findViewById(R.id.layoutEmptyHistory);
        tvHistoryCount         = root.findViewById(R.id.tvHistoryCount);
    }

    // ── Listeners ──────────────────────────────────────────────────────────

    private void setupListeners() {
        cardCamera.setOnClickListener(v -> requestCameraAndOpen());
        btnCamera.setOnClickListener(v  -> requestCameraAndOpen());
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnRetake.setOnClickListener(v  -> resetScanUI());
    }

    // ── Camera ─────────────────────────────────────────────────────────────

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
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            File imageFile = File.createTempFile("FOOD_" + timestamp, ".jpg",
                    requireContext().getExternalFilesDir(null));
            photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not open camera.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Scan UI ────────────────────────────────────────────────────────────

    private void showPhotoPreview(Uri uri) {
        ivFoodPreview.setImageURI(uri);
        ivFoodPreview.setVisibility(View.VISIBLE);
        layoutPlaceholder.setVisibility(View.GONE);
        btnRetake.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void resetScanUI() {
        ivFoodPreview.setVisibility(View.GONE);
        layoutPlaceholder.setVisibility(View.VISIBLE);
        btnRetake.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);
        photoUri = null;
    }

    // ── Analyze food ───────────────────────────────────────────────────────

    /**
     * This method looks up the food name in FoodDatabase.
     *
     * HOW TO CONNECT A REAL API:
     * 1. Send photoUri to your food recognition API (Clarifai, LogMeal, etc.)
     * 2. Get back a food name string (e.g. "banana", "pizza")
     * 3. Pass that name to: FoodDatabase.FoodInfo info = FoodDatabase.find(foodName);
     * 4. If info == null: info = FoodDatabase.unknown(foodName);
     * 5. Call saveAndDisplay(imageUri, info);
     *
     * For now it simulates recognition with "banana" as a demo.
     * Replace the recognizedFoodName with your real API result.
     */
    private void analyzeFood(Uri imageUri) {
        setLoading(true);
        cardResult.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setLoading(false);

            // ── TODO: Replace this string with your real API food name result ──
            String recognizedFoodName = "banana";

            // Look up in local database
            FoodDatabase.FoodInfo info = FoodDatabase.find(recognizedFoodName);
            if (info == null) {
                info = FoodDatabase.unknown(recognizedFoodName);
                Toast.makeText(requireContext(),
                        "Food not found in database: " + recognizedFoodName,
                        Toast.LENGTH_SHORT).show();
            }

            saveAndDisplay(imageUri, info);
        }, 1500);
    }

    // ── Save + display result ──────────────────────────────────────────────

    private void saveAndDisplay(Uri imageUri, FoodDatabase.FoodInfo info) {
        // Show result card
        tvFoodEmoji.setText(info.emoji);
        tvFoodName.setText(info.name);
        tvCalories.setText(info.calories + " kcal");
        tvProtein.setText(info.protein + "g");
        tvCarbs.setText(info.carbs + "g");
        tvFats.setText(info.fats + "g");
        cardResult.setVisibility(View.VISIBLE);
        cardResult.startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in));

        // Save to history
        String date = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(new Date());
        historyList.add(0, new FoodEntry(imageUri, info, date));
        refreshHistoryUI();
    }

    // ── History UI ─────────────────────────────────────────────────────────

    private void refreshHistoryUI() {
        layoutHistoryContainer.removeAllViews();

        if (historyList.isEmpty()) {
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            tvHistoryCount.setText("0 items");
            return;
        }

        layoutEmptyHistory.setVisibility(View.GONE);
        tvHistoryCount.setText(historyList.size() + (historyList.size() == 1 ? " item" : " items"));

        for (int i = 0; i < historyList.size(); i++) {
            layoutHistoryContainer.addView(buildHistoryCard(historyList.get(i), i));
        }
    }

    private View buildHistoryCard(FoodEntry entry, int index) {
        CardView card = new CardView(requireContext());
        CardView.LayoutParams cardParams = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dpToPx(10);
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(14));
        card.setCardElevation(dpToPx(3));

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        row.setBackgroundColor(0xFFFFFFFF);

        // Thumbnail
        ImageView img = new ImageView(requireContext());
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(64));
        imgParams.rightMargin = dpToPx(12);
        img.setLayoutParams(imgParams);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageURI(entry.imageUri);
        img.setBackgroundColor(0xFFE8F5E9);

        // Info block
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(requireContext());
        tvName.setText(entry.emoji + "  " + entry.name);
        tvName.setTextSize(15);
        tvName.setTextColor(0xFF1B4332);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(requireContext());
        tvDate.setText(entry.date + "  ·  " + entry.category);
        tvDate.setTextSize(11);
        tvDate.setTextColor(0xFF9CA3AF);

        TextView tvNutrition = new TextView(requireContext());
        tvNutrition.setText("🔥 " + entry.calories + " kcal  💪 " + entry.protein
                + "g  🌾 " + entry.carbs + "g  🫐 " + entry.fats + "g");
        tvNutrition.setTextSize(11);
        tvNutrition.setTextColor(0xFF6B7280);
        tvNutrition.setPadding(0, dpToPx(4), 0, 0);

        info.addView(tvName);
        info.addView(tvDate);
        info.addView(tvNutrition);

        // Delete button
        TextView btnDelete = new TextView(requireContext());
        btnDelete.setText("🗑");
        btnDelete.setTextSize(20);
        btnDelete.setPadding(dpToPx(8), 0, 0, 0);

        row.addView(img);
        row.addView(info);
        row.addView(btnDelete);
        card.addView(row);

        // Tap card → detail dialog
        card.setOnClickListener(v -> showDetailDialog(entry));

        // Tap delete → confirm
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Remove photo")
                        .setMessage("Remove " + entry.name + " from your history?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            historyList.remove(index);
                            refreshHistoryUI();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        return card;
    }

    private void showDetailDialog(FoodEntry entry) {
        String message =
                entry.emoji + "  " + entry.name + "\n" +
                        "Category: " + entry.category + "\n\n" +
                        "📅 " + entry.date + "\n\n" +
                        "🔥 Calories:  " + entry.calories + " kcal\n" +
                        "💪 Protein:   " + entry.protein + "g\n" +
                        "🌾 Carbs:     " + entry.carbs + "g\n" +
                        "🫐 Fats:      " + entry.fats + "g";

        new AlertDialog.Builder(requireContext())
                .setTitle("Nutrition Info")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .setNeutralButton("🗑 Remove", (dialog, which) -> {
                    int idx = historyList.indexOf(entry);
                    if (idx != -1) {
                        historyList.remove(idx);
                        refreshHistoryUI();
                    }
                })
                .show();
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}