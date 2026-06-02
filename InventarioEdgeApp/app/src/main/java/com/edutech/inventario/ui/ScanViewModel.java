package com.edutech.inventario.ui;

import android.media.AudioManager;
import android.media.ToneGenerator;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.edutech.inventario.data.InventoryRepository;
import com.edutech.inventario.data.local.InventarioSession;
import com.edutech.inventario.data.local.ResumenClase;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScanViewModel extends ViewModel {
    private final InventoryRepository repository;

    private final MutableLiveData<String> uiClase = new MutableLiveData<>("Sin detección");
    private final MutableLiveData<String> uiMessage = new MutableLiveData<>("Apunte la cámara al escritorio");
    private final MutableLiveData<String> inferenceLatency = new MutableLiveData<>("-- ms");
    private final MutableLiveData<String> inferenceConfidence = new MutableLiveData<>("--%");
    private final MutableLiveData<Integer> detectionProgress = new MutableLiveData<>(0);

    public LiveData<String> getUiClase()         { return uiClase; }
    public LiveData<String> getUiMessage()        { return uiMessage; }
    public LiveData<String> getInferenceLatency() { return inferenceLatency; }
    public LiveData<String> getInferenceConfidence() { return inferenceConfidence; }
    public LiveData<Integer> getDetectionProgress() { return detectionProgress; }

    public LiveData<List<InventarioSession>> getAllSessions() {
        return repository.getAllSessions();
    }

    private enum Estado { BUSCANDO, ENFRIAMIENTO }
    private Estado currentState = Estado.BUSCANDO;

    private String currentCandidate = null;
    private int candidateCount = 0;
    private int emptyCount = 0;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Object lock = new Object();

    public ScanViewModel(InventoryRepository repository) {
        this.repository = repository;
    }

    public void clearInventory() {
        repository.clearAll();
    }

    public void getResumenAgrupado(InventoryRepository.OnResumenReadyListener listener) {
        repository.getResumenAgrupado(listener);
    }

    public String getReadableName(String className) {
        if ("clase1_tecnologia".equals(className)) return "Herramienta Tecnológica";
        if ("clase2_utiles".equals(className))    return "Útil de Escritorio";
        if ("clase3_vacio".equals(className))     return "Entorno Vacío";
        return className;
    }

    private String getClassIcon(String className) {
        if ("clase1_tecnologia".equals(className)) return "💻";
        if ("clase2_utiles".equals(className))    return "✏️";
        if ("clase3_vacio".equals(className))     return "🗂️";
        return "❓";
    }

    public void processDetection(String className, float confidence, long inferenceTime) {
        inferenceLatency.postValue(String.format(Locale.US, "%d ms", inferenceTime));
        inferenceConfidence.postValue(String.format(Locale.US, "%.0f%%", confidence * 100));

        synchronized (lock) {
            if (currentState == Estado.ENFRIAMIENTO) {
                if ("clase3_vacio".equals(className)) {
                    emptyCount++;
                    if (emptyCount >= 15) {
                        currentState = Estado.BUSCANDO;
                        uiClase.postValue("Entorno Vacío");
                        uiMessage.postValue("Mesa libre. Listo para siguiente objeto.");
                    }
                } else {
                    emptyCount = 0;
                }
                return;
            }

            if (confidence < 0.60f) {
                uiClase.postValue("Analizando...");
                uiMessage.postValue("Confianza insuficiente, ajuste el ángulo");
                return;
            }

            if (!"clase3_vacio".equals(className)) {
                if (className.equals(currentCandidate)) {
                    candidateCount++;
                    detectionProgress.postValue(candidateCount * 10);
                    uiClase.postValue(getClassIcon(className) + " " + getReadableName(className));
                    uiMessage.postValue("Verificando: " + candidateCount + " de 10 muestras confirmadas");
                    if (candidateCount >= 10) {
                        registrarDeteccion(className, confidence);
                    }
                } else {
                    currentCandidate = className;
                    candidateCount = 1;
                    detectionProgress.postValue(10);
                    uiClase.postValue(getClassIcon(className) + " " + getReadableName(className));
                    uiMessage.postValue("Objeto detectado. Mantenga la cámara fija...");
                }
            } else {
                currentCandidate = null;
                candidateCount = 0;
                detectionProgress.postValue(0);
                uiClase.postValue("Entorno Vacío 🗂️");
                uiMessage.postValue("Apunte a un objeto del inventario");
            }
        }
    }

    private void registrarDeteccion(String className, float confidence) {
        currentState = Estado.ENFRIAMIENTO;
        currentCandidate = null;
        candidateCount = 0;
        emptyCount = 0;
        detectionProgress.postValue(100);

        uiClase.postValue("✅ " + getReadableName(className));
        uiMessage.postValue("¡REGISTRADO EN INVENTARIO!");
        repository.addDetection(className, confidence);

        // Retroalimentación auditiva
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (Exception e) {
            // Silently ignore if ToneGenerator fails
        }

        scheduler.schedule(() -> {
            synchronized (lock) {
                if (currentState == Estado.ENFRIAMIENTO) {
                    currentState = Estado.BUSCANDO;
                    uiMessage.postValue("Apunte al siguiente objeto o escritorio.");
                }
            }
        }, 4, TimeUnit.SECONDS);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        scheduler.shutdown();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final InventoryRepository repository;

        public Factory(InventoryRepository repository) {
            this.repository = repository;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new ScanViewModel(repository);
        }
    }
}
