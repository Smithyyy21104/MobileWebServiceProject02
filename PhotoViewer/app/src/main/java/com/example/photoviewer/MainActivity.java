
package com.example.photoviewer;






import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


import android.graphics.Matrix;
import android.media.ExifInterface;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private Uri selectedImageUri;
    private ImageView uploadPreview;
    private TextView selectedImageLabel;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null
                                && uploadPreview != null
                                && selectedImageLabel != null) {

                            selectedImageUri = uri;

                            uploadPreview.setImageURI(uri);
                            uploadPreview.setVisibility(View.VISIBLE);

                            selectedImageLabel.setText(
                                    "Photograph selected successfully"
                            );
                        }
                    }
            );




    private static final String API_URL =
            "https://samz21.pythonanywhere.com/api_root/Post/";


    private Button btnSync;
    private Button btnUpload;
    private TextView tvStatus;
    private LinearLayout galleryContainer;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static class Photo {
        String title;
        String text;
        String date;
        Bitmap bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return insets;
                }
        );

        btnSync = findViewById(R.id.btnSync);
        btnUpload = findViewById(R.id.btnUpload);
        tvStatus = findViewById(R.id.tvStatus);
        galleryContainer = findViewById(R.id.galleryContainer);

        btnSync.setEnabled(true);
        btnUpload.setEnabled(true);

        btnSync.setOnClickListener(v -> syncPhotos());

        btnUpload.setOnClickListener(v -> openUploadDialog());
    }



    private void openUploadDialog() {
        selectedImageUri = null;

        View uploadForm = getLayoutInflater().inflate(
                R.layout.dialog_upload,
                null
        );

        Button btnChooseImage =
                uploadForm.findViewById(R.id.btnChooseImage);

        uploadPreview =
                uploadForm.findViewById(R.id.ivUploadPreview);

        selectedImageLabel =
                uploadForm.findViewById(R.id.tvSelectedImage);

        android.widget.EditText etTitle =
                uploadForm.findViewById(R.id.etUploadTitle);

        android.widget.EditText etText =
                uploadForm.findViewById(R.id.etUploadText);

        android.widget.EditText etUsername =
                uploadForm.findViewById(R.id.etUploadUsername);

        android.widget.EditText etPassword =
                uploadForm.findViewById(R.id.etUploadPassword);

        btnChooseImage.setOnClickListener(v -> {
            imagePicker.launch("image/*");
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(uploadForm)
                .setPositiveButton("Upload", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button uploadButton =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            Button cancelButton =
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            uploadButton.setTextColor(Color.parseColor("#D7B58D"));
            cancelButton.setTextColor(Color.parseColor("#D7B58D"));

            uploadButton.setAllCaps(false);
            cancelButton.setAllCaps(false);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new ColorDrawable(
                                Color.parseColor("#101115")
                        )
                );
            }

            uploadButton.setOnClickListener(v -> {
                Uri imageUri = selectedImageUri;

                String title = etTitle.getText().toString().trim();
                String description = etText.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString();

                if (imageUri == null) {
                    etTitle.clearFocus();
                    android.widget.Toast.makeText(
                            this,
                            "Please choose a photograph.",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                if (title.isEmpty()) {
                    etTitle.setError("Please enter a title.");
                    etTitle.requestFocus();
                    return;
                }

                if (description.isEmpty()) {
                    etText.setError("Please enter a description.");
                    etText.requestFocus();
                    return;
                }

                if (username.isEmpty()) {
                    etUsername.setError("Please enter your username.");
                    etUsername.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    etPassword.setError("Please enter your password.");
                    etPassword.requestFocus();
                    return;
                }

                uploadButton.setEnabled(false);
                cancelButton.setEnabled(false);
                uploadButton.setText("Uploading...");

                executor.execute(() -> {
                    try {
                        PhotoUploadClient.uploadPhoto(
                                MainActivity.this,
                                imageUri,
                                title,
                                description,
                                username,
                                password
                        );

                        runOnUiThread(() -> {
                            dialog.dismiss();

                            android.widget.Toast.makeText(
                                    this,
                                    "Photograph uploaded successfully!",
                                    android.widget.Toast.LENGTH_LONG
                            ).show();

                            syncPhotos();
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            uploadButton.setEnabled(true);
                            cancelButton.setEnabled(true);
                            uploadButton.setText("Upload");

                            android.widget.Toast.makeText(
                                    this,
                                    "Upload failed: " + e.getMessage(),
                                    android.widget.Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                });
            });
        });

        dialog.setOnDismissListener(d -> {
            uploadPreview = null;
            selectedImageLabel = null;
            selectedImageUri = null;
        });

        dialog.show();
    }



    private void syncPhotos() {
        btnSync.setEnabled(false);
        tvStatus.setText("Synchronizing photographs...");

        executor.execute(() -> {
            try {
                String json = downloadText(API_URL);
                JSONArray array = new JSONArray(json);

                ArrayList<Photo> photos = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);

                    Photo photo = new Photo();

                    photo.title = object.optString("title", "");
                    photo.text = object.optString("text", "");

                    String date = object.optString(
                            "published_date", ""
                    );

                    photo.date = date.length() >= 10
                            ? date.substring(0, 10)
                            : "";

                    String imageUrl = object.optString(
                            "image", ""
                    );

                    if (imageUrl.startsWith(
                            "http://127.0.0.1:8000/")) {
                        imageUrl = imageUrl.replace(
                                "http://127.0.0.1:8000/",
                                "http://10.0.2.2:8000/"
                        );
                    }

                    if (!imageUrl.isEmpty()
                            && !imageUrl.equals("null")) {
                        photo.bitmap = downloadImage(imageUrl);
                    }

                    photos.add(photo);
                }

                runOnUiThread(() -> showPhotos(photos));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvStatus.setText(
                            "Sync failed: " + e.getMessage()
                    );

                    btnSync.setEnabled(true);
                });
            }
        });
    }

    private String downloadText(String address)
            throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(address)
                        .openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty(
                "Accept", "application/json"
        );

        try {
            if (connection.getResponseCode() != 200) {
                throw new Exception(
                        "HTTP " + connection.getResponseCode()
                );
            }

            StringBuilder result = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                            StandardCharsets.UTF_8
                    ))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            return result.toString();

        } finally {
            connection.disconnect();
        }
    }


    private Bitmap downloadImage(String address) {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    new URL(address).openConnection();

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            if (connection.getResponseCode() != 200) {
                return null;
            }

            byte[] imageBytes;

            try (InputStream stream = connection.getInputStream();
                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int count;

                while ((count = stream.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }

                imageBytes = output.toByteArray();
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length
            );

            if (bitmap == null) {
                return null;
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            try {
                ExifInterface exif = new ExifInterface(
                        new ByteArrayInputStream(imageBytes)
                );

                orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                );
            } catch (Exception ignored) {
                return bitmap;
            }

            Matrix matrix = new Matrix();

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;

                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1);
                    break;

                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1);
                    break;

                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.postRotate(90);
                    matrix.postScale(-1, 1);
                    break;

                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.postRotate(270);
                    matrix.postScale(-1, 1);
                    break;

                default:
                    return bitmap;
            }

            Bitmap corrected = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

            if (corrected != bitmap) {
                bitmap.recycle();
            }

            return corrected;

        } catch (Exception e) {
            return null;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    private void showPhotos(ArrayList<Photo> photos) {
        galleryContainer.removeAllViews();

        if (photos.isEmpty()) {
            TextView empty = new TextView(this);

            empty.setText("No photographs found");
            empty.setTextColor(Color.WHITE);
            empty.setTextSize(16);

            galleryContainer.addView(empty);
        }

        for (Photo photo : photos) {
            addPhotoCard(photo);
        }

        tvStatus.setText(
                photos.size() + " photographs synchronized"
        );

        btnSync.setEnabled(true);
    }

    private void addPhotoCard(Photo photo) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.parseColor("#25272E")
        );

        background.setCornerRadius(dp(12));

        card.setBackground(background);
        card.setClipToOutline(true);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(20);

        if (photo.bitmap != null) {
            ImageView image = new ImageView(this);

            image.setImageBitmap(photo.bitmap);

            image.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            card.addView(
                    image,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(220)
                    )
            );
        }

        TextView title = new TextView(this);

        title.setText(photo.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);

        title.setPadding(
                dp(16), dp(16), dp(16), dp(5)
        );

        card.addView(title);

        TextView description = new TextView(this);

        description.setText(photo.text);

        description.setTextColor(
                Color.parseColor("#A5A5AD")
        );

        description.setTextSize(14);

        description.setPadding(
                dp(16), dp(4), dp(16), dp(8)
        );

        card.addView(description);

        TextView dateView = new TextView(this);

        dateView.setText(photo.date);

        dateView.setTextColor(
                Color.parseColor("#D7B58D")
        );

        dateView.setTextSize(12);

        dateView.setPadding(
                dp(16), dp(4), dp(16), dp(16)
        );

        card.addView(dateView);

        galleryContainer.addView(card, cardParams);
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
