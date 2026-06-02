package com.edutech.inventario.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.edutech.inventario.InventarioApp;
import com.edutech.inventario.databinding.ActivityMainBinding;
import com.edutech.inventario.ml.TFLiteAnalyzer;
import com.edutech.inventario.data.local.ResumenClase;
import com.edutech.inventario.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ExecutorService cameraExecutor;
    private TFLiteAnalyzer analyzer;
    private ScanViewModel viewModel;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cameraExecutor = Executors.newSingleThreadExecutor();

        InventarioApp app = (InventarioApp) getApplication();
        ScanViewModel.Factory factory = new ScanViewModel.Factory(app.repository);
        viewModel = new ViewModelProvider(this, factory).get(ScanViewModel.class);

        // Observar los campos de la UI
        viewModel.getUiClase().observe(this, clase ->
                binding.tvClaseDetectada.setText(clase));

        viewModel.getUiMessage().observe(this, msg ->
                binding.tvMensaje.setText(msg));

        viewModel.getInferenceLatency().observe(this, latency ->
                binding.tvLatencia.setText(latency));

        viewModel.getInferenceConfidence().observe(this, conf ->
                binding.tvConfianza.setText(conf));

        // Observador del inventario
        viewModel.getAllSessions().observe(this, sessions -> {
            binding.tvInventarioActual.setText(sessions.size() + " objetos");
        });

        // Observador de progreso
        viewModel.getDetectionProgress().observe(this, progress -> {
            binding.progressDeteccion.setProgress(progress);
        });

        binding.fabFinalizar.setOnClickListener(v -> {
            viewModel.getResumenAgrupado(resumen -> {
                runOnUiThread(() -> mostrarBottomSheetResumen(resumen));
            });
        });

        binding.fabCapturar.setOnClickListener(v -> {
            viewModel.registrarCapturaManual();
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            analyzer = new TFLiteAnalyzer(getApplicationContext(), (className, confidence, time) ->
                    viewModel.processDetection(className, confidence, time));
            analyzer.runWarmUp();

            runOnUiThread(() -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });
        });
    }

    private void mostrarBottomSheetResumen(List<ResumenClase> resumen) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_plan, null);
        bottomSheetDialog.setContentView(sheetView);
        
        android.widget.LinearLayout llResumenItems = sheetView.findViewById(R.id.llResumenItems);
        
        if (resumen.isEmpty()) {
            android.widget.TextView tvEmpty = new android.widget.TextView(this);
            tvEmpty.setText("No se registraron objetos en esta sesión.");
            tvEmpty.setTextColor(android.graphics.Color.WHITE);
            llResumenItems.addView(tvEmpty);
        } else {
            for (ResumenClase r : resumen) {
                // Crear fila dinamica
                android.widget.LinearLayout row = new android.widget.LinearLayout(this);
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setPadding(0, 16, 0, 16);
                
                android.widget.TextView tvName = new android.widget.TextView(this);
                tvName.setText(viewModel.getReadableName(r.clase));
                tvName.setTextColor(android.graphics.Color.WHITE);
                tvName.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                
                android.widget.TextView tvCount = new android.widget.TextView(this);
                tvCount.setText(String.valueOf(r.cantidad));
                tvCount.setTextColor(android.graphics.Color.CYAN);
                tvCount.setTypeface(null, android.graphics.Typeface.BOLD);
                
                row.addView(tvName);
                row.addView(tvCount);
                llResumenItems.addView(row);
            }
        }

        sheetView.findViewById(R.id.btnNew).setOnClickListener(v -> {
            viewModel.clearInventory();
            Toast.makeText(this, "Inventario finalizado y borrado", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });
        
        sheetView.findViewById(R.id.btnExport).setOnClickListener(v -> {
            Toast.makeText(this, "Guardado (Demo)", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                if (analyzer != null) {
                    imageAnalyzer.setAnalyzer(cameraExecutor, analyzer);
                }

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);

            } catch (Exception exc) {
                Log.e("MainActivity", "Error al inicializar cámara", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
