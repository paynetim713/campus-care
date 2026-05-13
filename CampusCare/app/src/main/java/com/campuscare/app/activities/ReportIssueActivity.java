package com.campuscare.app.activities;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.campuscare.app.R;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.UploadResponse;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportIssueActivity extends AppCompatActivity {

    private static final String[] BUILDINGS = {
            "Block A","Block B","Block C","Block D",
            "Block E","Block F","Block G","Block H"
    };
    private static final Map<String, String[]> BUILDING_FLOORS  = new LinkedHashMap<>();
    private static final Map<String, Map<String, String[]>> FLOOR_ROOMS = new LinkedHashMap<>();

    static {
        BUILDING_FLOORS.put("Block A", new String[]{"Lower Level (Aras Bawah)","Upper Level (Aras Atas)"});
        BUILDING_FLOORS.put("Block B", new String[]{"Ground Floor (Aras G)","Level 1 (Aras 1)","Level 2 (Aras 2)"});
        BUILDING_FLOORS.put("Block C", new String[]{"Ground Floor (Aras G)"});
        BUILDING_FLOORS.put("Block D", new String[]{"Level 1 (Aras 1)","Level 2 (Aras 2)","Level 3 (Aras 3)"});
        BUILDING_FLOORS.put("Block E", new String[]{"Level 1 (Aras 1)","Level 2 (Aras 2)","Level 3 (Aras 3)","Level 4 (Aras 4)"});
        BUILDING_FLOORS.put("Block F", new String[]{"Ground Floor"});
        BUILDING_FLOORS.put("Block G", new String[]{"Ground Floor (Aras G)","Level 1 (Aras 1)","Level 2 (Aras 2)","Level 3 (Aras 3)"});
        BUILDING_FLOORS.put("Block H", new String[]{"Level 1 (Aras 1)","Level 2 (Aras 2)","Level 3 (Aras 3)","Level 4 (Aras 4)"});

        Map<String,String[]> a = new LinkedHashMap<>();
        a.put("Lower Level (Aras Bawah)", new String[]{"Pejabat Unit Akademik","Bilik Timbalan Dekan (Akademik)","Bilik Penolong Dekan (Hal-Ehwal Pelajar)","Bilik Ketua Program Sarjana","Bilik Ketua Program Kedoktoran","Bilik Penolong Pendaftar Kanan","Pusat Pelajar"});
        a.put("Upper Level (Aras Atas)",  new String[]{"Bilik Penolong Dekan (Keusahawanan & Kreativiti)","Bilik Ketua Program Kecerdasan Buatan","Bilik Ketua Program Teknologi Maklumat","Bilik Ketua Program Kejuruteraan Perisian","Bilik Ketua Program Teknologi Perisian & Rangkaian","Bilik Penyelaras Program Sarjana Khas","Bilik Perbincangan Kaca"});
        FLOOR_ROOMS.put("Block A", a);
        Map<String,String[]> b = new LinkedHashMap<>();
        b.put("Ground Floor (Aras G)", new String[]{"Bilik Mesyuarat 1","Bilik Mesyuarat 2","Bilik Viva"});
        b.put("Level 1 (Aras 1)",      new String[]{"Bilik Kuliah 1","Bilik Kuliah 2","Bilik Kuliah 3"});
        b.put("Level 2 (Aras 2)",      new String[]{"Bilik Kuliah 4","Bilik Kuliah 5","Bilik Kuliah 6"});
        FLOOR_ROOMS.put("Block B", b);
        Map<String,String[]> c = new LinkedHashMap<>();
        c.put("Ground Floor (Aras G)", new String[]{"Makmal ARVIS","Makmal MyXLab (Mixed Reality & Pervasive Lab)","Makmal Teknologi Pembuatan (AI Lab)"});
        FLOOR_ROOMS.put("Block C", c);
        Map<String,String[]> d = new LinkedHashMap<>();
        d.put("Level 1 (Aras 1)", new String[]{"Ruang Inovasi","Bilik Perbincangan Pelajar 1","Bilik Perbincangan Pelajar 2","Bilik Perbincangan Pelajar 3","Bilik PERTAMA"});
        d.put("Level 2 (Aras 2)", new String[]{"Studio Inovasi","Bilik Audio","Makmal Teknologi Platform & Teragih"});
        d.put("Level 3 (Aras 3)", new String[]{"Taman Inovasi (INNOFARM)"});
        FLOOR_ROOMS.put("Block D", d);
        Map<String,String[]> e = new LinkedHashMap<>();
        e.put("Level 1 (Aras 1)", new String[]{"Bilik Pensyarah E1-01","Bilik Pensyarah E1-02","Bilik Pensyarah E1-03"});
        e.put("Level 2 (Aras 2)", new String[]{"Bilik Pensyarah E2-01","Bilik Pensyarah E2-02","Surau Lelaki"});
        e.put("Level 3 (Aras 3)", new String[]{"Bilik Pensyarah E3-01","Bilik Pensyarah E3-02","Surau Wanita"});
        e.put("Level 4 (Aras 4)", new String[]{"Bilik Pensyarah E4-01","Bilik Pensyarah E4-02","Bilik Tutorial 5"});
        FLOOR_ROOMS.put("Block E", e);
        Map<String,String[]> f = new LinkedHashMap<>();
        f.put("Ground Floor", new String[]{"Institut Informatik Visual"});
        FLOOR_ROOMS.put("Block F", f);
        Map<String,String[]> g = new LinkedHashMap<>();
        g.put("Ground Floor (Aras G)", new String[]{"Dewan Persembahan Multimedia","Makmal Industri","Makmal Keselamatan Siber","Makmal Sains Data","Bilik Seminar Eksekutif Siber","Bilik Perbincangan Siswazah","Bilik Sokongan ICT","Kafeteria / Kedai Fotostat"});
        g.put("Level 1 (Aras 1)",      new String[]{"Pejabat Dekan","Bilik Timbalan Dekan (Penyelidikan & Inovasi)","Bilik Timbalan Dekan (Jaringan Industri & Alumni)","Bilik Penolong Dekan (Kualiti & Strategi)","Bilik Penolong Dekan (Pengajaran & CITRA)","Bilik Ketua HEJIM","Bilik Ketua Unit ICT","Bilik Pegawai Teknologi Maklumat","Pejabat Bendahari Zon Kewangan 2","Bilik Kuliah 7","Bilik Kuliah 8","Bilik Kuliah 9"});
        g.put("Level 2 (Aras 2)",      new String[]{"Bilik Perbincangan Eksekutif","Bilik Pensyarah G2-01","Bilik Pensyarah G2-02","Bilik Tutorial 1","Bilik Tutorial 2"});
        g.put("Level 3 (Aras 3)",      new String[]{"Bilik Pensyarah G3-01","Bilik Pensyarah G3-02","Bilik Tutorial 3","Bilik Tutorial 4"});
        FLOOR_ROOMS.put("Block G", g);
        Map<String,String[]> h = new LinkedHashMap<>();
        h.put("Level 1 (Aras 1)", new String[]{"Makmal Penyelidikan CYBER H1-01","Makmal Penyelidikan CYBER H1-02","Bilik Mesyuarat H1"});
        h.put("Level 2 (Aras 2)", new String[]{"Pejabat Pusat Penyelidikan","Bilik Pengerusi CAIT","Bilik Pengerusi SOFTAM","Bilik Pengerusi CYBER","Makmal Penyelidikan CAIT H2","Bilik Mesyuarat H2","Bilik Sumber","Surau H2"});
        h.put("Level 3 (Aras 3)", new String[]{"Makmal Penyelidikan SOFTAM H3-01","Makmal Penyelidikan SOFTAM H3-02","Bilik Mesyuarat H3","Surau H3"});
        h.put("Level 4 (Aras 4)", new String[]{"Makmal Penyelidikan CAIT H4-01","Makmal Penyelidikan CAIT H4-02","Bilik Mesyuarat H4"});
        FLOOR_ROOMS.put("Block H", h);
    }

    private static final int MAX_PHOTOS   = 3;

    private static final int MAX_KB       = 800;

    private String selectedCategory = "ELECTRIC";
    private TextView[] chips;

    private final List<Uri> selectedPhotos = new ArrayList<>();
    private LinearLayout llPhotoStrip;
    private HorizontalScrollView hsvPhotos;
    private LinearLayout llUpload;
    private TextView tvPhotoCount;
    private Button btnSubmit;
    private Spinner spBuilding, spFloor, spRoom;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || selectedPhotos.size() >= MAX_PHOTOS) return;

                ioExecutor.execute(() -> {
                    Uri persistent = copyToInternalStorage(uri, selectedPhotos.size());
                    Uri toAdd = (persistent != null) ? persistent : uri;
                    runOnUiThread(() -> {
                        selectedPhotos.add(toAdd);
                        rebuildPhotoStrip();
                    });
                });
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        btnSubmit    = findViewById(R.id.btn_submit);
        llUpload     = findViewById(R.id.ll_upload);
        hsvPhotos    = findViewById(R.id.hsv_photos);
        llPhotoStrip = findViewById(R.id.ll_photo_strip);
        tvPhotoCount = findViewById(R.id.tv_photo_count);

        chips = new TextView[]{
                findViewById(R.id.chip_electric), findViewById(R.id.chip_plumbing),
                findViewById(R.id.chip_furniture), findViewById(R.id.chip_network),
                findViewById(R.id.chip_other)
        };
        String[] cats = {"ELECTRIC","PLUMBING","FURNITURE","NETWORK","OTHER"};
        for (int i = 0; i < chips.length; i++) {
            final String cat = cats[i];
            chips[i].setOnClickListener(v -> selectCategory(cat));
        }
        selectCategory("ELECTRIC");

        spBuilding = findViewById(R.id.sp_building);
        spFloor    = findViewById(R.id.sp_floor);
        spRoom     = findViewById(R.id.sp_room);

        ArrayAdapter<String> bldAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, BUILDINGS);
        bldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBuilding.setAdapter(bldAdapter);

        spBuilding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                updateFloorSpinner(BUILDINGS[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        spFloor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String building = (String) spBuilding.getSelectedItem();
                String floor    = (String) spFloor.getSelectedItem();
                if (building != null && floor != null) updateRoomSpinner(building, floor);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        updateFloorSpinner(BUILDINGS[0]);

        llUpload.setOnClickListener(v -> requestPickPhoto());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        EditText etDetails = findViewById(R.id.et_details);
        btnSubmit.setOnClickListener(v -> {
            String details = etDetails.getText().toString().trim();
            if (details.isEmpty()) {
                Toast.makeText(this, "Please describe the problem", Toast.LENGTH_SHORT).show();
                return;
            }
            String building = (String) spBuilding.getSelectedItem();
            String floor    = (String) spFloor.getSelectedItem();
            String room     = (String) spRoom.getSelectedItem();
            if (building == null || floor == null || room == null) {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting...");

            if (!selectedPhotos.isEmpty() && ApiClient.getAuthToken() != null) {
                uploadAllPhotosThenSubmit(building, floor, room, details);
            } else {
                submitRepair(building, floor, room, details, null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    private void updateFloorSpinner(String building) {
        String[] floors = BUILDING_FLOORS.getOrDefault(building, new String[]{"Ground Floor"});
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, floors);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFloor.setAdapter(a);
    }

    private void updateRoomSpinner(String building, String floor) {
        Map<String,String[]> fm = FLOOR_ROOMS.get(building);
        String[] rooms = (fm != null) ? fm.get(floor) : null;
        if (rooms == null || rooms.length == 0) rooms = new String[]{"General Area"};
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rooms);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRoom.setAdapter(a);
    }

    private void requestPickPhoto() {
        if (selectedPhotos.size() >= MAX_PHOTOS) {
            Toast.makeText(this, "Maximum " + MAX_PHOTOS + " photos allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        } else {
            pickImageLauncher.launch("image/*");
        }
    }

    private void rebuildPhotoStrip() {
        llPhotoStrip.removeAllViews();
        int thumbSize = (int)(getResources().getDisplayMetrics().density * 90);
        int margin    = (int)(getResources().getDisplayMetrics().density * 8);

        for (int i = 0; i < selectedPhotos.size(); i++) {
            final int index = i;
            final Uri uri   = selectedPhotos.get(i);

            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(thumbSize, thumbSize);
            cp.setMargins(0, 0, margin, 0);
            container.setLayoutParams(cp);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageURI(uri);
            iv.setBackground(getDrawable(R.drawable.bg_input));
            container.addView(iv);

            TextView btnDel = new TextView(this);
            android.widget.FrameLayout.LayoutParams xp = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
            xp.gravity = Gravity.TOP | Gravity.END;
            btnDel.setLayoutParams(xp);
            btnDel.setText("✕");
            btnDel.setTextColor(0xFFFFFFFF);
            btnDel.setTextSize(12f);
            btnDel.setPadding(6, 2, 6, 2);
            btnDel.setBackground(new android.graphics.drawable.ColorDrawable(0xCC000000));
            btnDel.setOnClickListener(v -> {

                deleteInternalFile(selectedPhotos.get(index));
                selectedPhotos.remove(index);
                rebuildPhotoStrip();
            });
            container.addView(btnDel);
            llPhotoStrip.addView(container);
        }

        boolean hasPhotos = !selectedPhotos.isEmpty();
        hsvPhotos.setVisibility(hasPhotos ? View.VISIBLE : View.GONE);
        tvPhotoCount.setText(selectedPhotos.size() + "/" + MAX_PHOTOS);
        llUpload.setVisibility(selectedPhotos.size() >= MAX_PHOTOS ? View.GONE : View.VISIBLE);
    }

    private void uploadAllPhotosThenSubmit(String building, String floor,
                                           String room, String details) {
        List<String> uploadedUrls = new ArrayList<>();
        uploadNext(0, uploadedUrls, building, floor, room, details);
    }

    private void uploadNext(int idx, List<String> urls,
                            String building, String floor,
                            String room, String details) {
        if (idx >= selectedPhotos.size()) {
            String photoUrl = urls.isEmpty() ? null : String.join(",", urls);
            submitRepair(building, floor, room, details, photoUrl);
            return;
        }

        Uri uri = selectedPhotos.get(idx);

        ioExecutor.execute(() -> {
            try {
                byte[] bytes = compressImage(uri, MAX_KB);
                String mime  = "image/jpeg";

                RequestBody reqFile = RequestBody.create(MediaType.parse(mime), bytes);
                MultipartBody.Part part = MultipartBody.Part.createFormData(
                        "file", "photo_" + idx + ".jpg", reqFile);

                ApiClient.getService()
                        .uploadPhoto(ApiClient.getAuthToken(), part)
                        .enqueue(new Callback<UploadResponse>() {
                            @Override public void onResponse(Call<UploadResponse> call,
                                                             Response<UploadResponse> r) {
                                if (r.isSuccessful() && r.body() != null
                                        && r.body().url != null)
                                    urls.add(r.body().url);
                                uploadNext(idx + 1, urls, building, floor, room, details);
                            }
                            @Override public void onFailure(Call<UploadResponse> call,
                                                            Throwable t) {

                                uploadNext(idx + 1, urls, building, floor, room, details);
                            }
                        });
            } catch (IOException e) {

                uploadNext(idx + 1, urls, building, floor, room, details);
            }
        });
    }

    private void submitRepair(String building, String floor, String room,
                              String details, String photoUrl) {
        String uid = DataManager.getInstance().getCurrentUser().id;

        StringBuilder paths = new StringBuilder();
        for (Uri u : selectedPhotos) {
            if (paths.length() > 0) paths.append(",");
            paths.append(u.toString());
        }

        Ticket t = new Ticket(null, selectedCategory + " Issue",
                building, floor, room, selectedCategory, details, "NEW", uid);
        t.photoUrl  = photoUrl;
        t.photoPath = paths.toString();
        DataManager.getInstance().addTicket(t);
        final String localId = t.id;

        String token = ApiClient.getAuthToken();
        if (token == null) {
            Toast.makeText(this, "Submitted offline. Will sync when connected.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("building",  building);
        request.put("floor",     floor);
        request.put("room",      room);
        request.put("category",  selectedCategory);
        request.put("details",   details);
        request.put("photoUrl",  photoUrl);

        ApiClient.getService().createRepair(token, request)
                .enqueue(new Callback<Ticket>() {
                    @Override public void onResponse(Call<Ticket> call, Response<Ticket> r) {
                        runOnUiThread(() -> {
                            if (r.isSuccessful() && r.body() != null) {
                                Ticket created = r.body();

                                if (created.photoPath == null)
                                    created.photoPath = t.photoPath;
                                DataManager.getInstance().removeTicket(localId);
                                DataManager.getInstance().addTicket(created);

                                if (photoUrl == null && !selectedPhotos.isEmpty()) {

                                    Toast.makeText(ReportIssueActivity.this,
                                            "Request submitted! ⚠ Photos couldn't be uploaded – " +
                                                    "open the request later to retry.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(ReportIssueActivity.this,
                                            "Request submitted! Ticket #" + created.getShortId(),
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(ReportIssueActivity.this,
                                        "Saved locally – server error. Will retry.",
                                        Toast.LENGTH_LONG).show();
                            }
                            finish();
                        });
                    }
                    @Override public void onFailure(Call<Ticket> call, Throwable th) {
                        runOnUiThread(() -> {
                            Toast.makeText(ReportIssueActivity.this,
                                    "Submitted offline. Will sync when connected.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                });
    }

    private byte[] compressImage(Uri uri, int maxKB) throws IOException {
        ContentResolver cr = getContentResolver();

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) throw new IOException("cannot open " + uri);
            BitmapFactory.decodeStream(is, null, opts);
        }

        int sampleSize = 1;
        long pixels = (long) opts.outWidth * opts.outHeight;
        while (pixels / (sampleSize * sampleSize) > 2_000_000L) sampleSize *= 2;

        opts = new BitmapFactory.Options();
        opts.inSampleSize   = sampleSize;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap;
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) throw new IOException("cannot open " + uri);
            bitmap = BitmapFactory.decodeStream(is, null, opts);
        }
        if (bitmap == null) throw new IOException("failed to decode bitmap from " + uri);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 85;
        do {
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            quality -= 10;
        } while (baos.size() > maxKB * 1024 && quality > 20);

        bitmap.recycle();
        return baos.toByteArray();
    }

    private Uri copyToInternalStorage(Uri sourceUri, int index) {
        try {
            File dir = new File(getFilesDir(), "evidence");
            if (!dir.exists()) dir.mkdirs();

            File dest = new File(dir, "photo_" + System.currentTimeMillis() + "_" + index + ".jpg");

            byte[] compressed = compressImage(sourceUri, MAX_KB);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(compressed);
            }
            return Uri.fromFile(dest);
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteInternalFile(Uri uri) {
        if (uri != null && "file".equals(uri.getScheme())) {
            try { new File(uri.getPath()).delete(); } catch (Exception ignored) {}
        }
    }

    private void selectCategory(String cat) {
        selectedCategory = cat;
        String[] cats = {"ELECTRIC","PLUMBING","FURNITURE","NETWORK","OTHER"};
        for (int i = 0; i < chips.length; i++) {
            if (cats[i].equals(cat)) {
                chips[i].setBackgroundResource(R.drawable.bg_chip_selected);
                chips[i].setTextColor(getColor(R.color.white));
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_chip_unselected);
                chips[i].setTextColor(getColor(R.color.text_secondary));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            pickImageLauncher.launch("image/*");
    }
}