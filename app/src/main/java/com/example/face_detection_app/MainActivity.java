package com.example.face_detection_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int TAKE_PHOTO_REQUEST = 101;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 102;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 103;

    private ImageView imageView;
    private TextView resultText;
    private Button detectButton, saveButton, cameraButton, galleryButton;
    private Bitmap selectedImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        detectButton = findViewById(R.id.detectButton);
        saveButton = findViewById(R.id.saveButton);
        cameraButton = findViewById(R.id.cameraButton);
        galleryButton = findViewById(R.id.galleryButton);

        // Button click listeners
        cameraButton.setOnClickListener(v -> openCamera());
        galleryButton.setOnClickListener(v -> pickImageFromGallery());
        detectButton.setOnClickListener(v -> {
            if (selectedImage == null) {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            } else {
                detectFaces(selectedImage);
            }
        });

        saveButton.setOnClickListener(v -> saveFaces());
    }

    private void openCamera() {
        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Proceed with opening the camera
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, TAKE_PHOTO_REQUEST);
            } else {
                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pickImageFromGallery() {
        // Check for storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            // Open the gallery
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                try {
                    Uri imageUri = data.getData();
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == TAKE_PHOTO_REQUEST) {
                Bundle extras = data.getExtras();
                selectedImage = (Bitmap) extras.get("data");
                imageView.setImageBitmap(selectedImage);
            }
        }
    }

    private void detectFaces(Bitmap image) {
        // Create an InputImage from the Bitmap
        InputImage inputImage = InputImage.fromBitmap(image, 0);

        // Configure the face detector with options
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        // Create an instance of the FaceDetector
        FaceDetector detector = FaceDetection.getClient(options);

        // Process the image
        detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        resultText.setText("No faces detected.");
                    } else {
                        StringBuilder resultBuilder = new StringBuilder();
                        Bitmap mutableImage = image.copy(Bitmap.Config.ARGB_8888, true); // Make the image mutable

                        Canvas canvas = new Canvas(mutableImage);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED); // Set the color for the face boxes
                        paint.setStyle(Paint.Style.STROKE); // Set the style to draw only the border
                        paint.setStrokeWidth(5); // Set the stroke width for the border

                        for (int i = 0; i < faces.size(); i++) {
                            Face face = faces.get(i);

                            // Get the bounding box for each face
                            Rect bounds = face.getBoundingBox();
                            canvas.drawRect(bounds, paint); // Draw the rectangle on the canvas

                            // Optionally, add more information like smile probability to the result
                            float smileProbability = face.getSmilingProbability();
                            if (smileProbability > 0) {
                                resultBuilder.append("Face ").append(i + 1).append(" Smile: ")
                                        .append(smileProbability * 100).append("%\n");
                            }
                        }

                        // Set the updated image with bounding boxes
                        imageView.setImageBitmap(mutableImage);

                        // Display the results
                        resultText.setText(resultBuilder.toString());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Face detection failed.", Toast.LENGTH_SHORT).show());
    }

    private void saveFaces() {
        if (selectedImage == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Image = convertBitmapToBase64(selectedImage);
        // Add logic to save the image or process it further.
        Toast.makeText(this, "Face saved!", Toast.LENGTH_SHORT).show();
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(this, "Storage permission is required to select an image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
