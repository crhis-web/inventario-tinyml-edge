package com.edutech.inventario.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

public class TFLiteAnalyzer implements ImageAnalysis.Analyzer {

    public interface AnalyzerListener {
        void onResult(String className, float confidence, long inferenceTime);
    }

    private Interpreter interpreter;
    private List<String> labels = Collections.emptyList();
    private final AnalyzerListener listener;

    public TFLiteAnalyzer(Context context, AnalyzerListener listener) {
        this.listener = listener;
        try {
            ByteBuffer model = FileUtil.loadMappedFile(context, "modelo_int8.tflite");
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            interpreter = new Interpreter(model, options);
            labels = FileUtil.loadLabels(context, "labels.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runWarmUp() {
        if (interpreter == null) return;
        ByteBuffer emptyBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3);
        emptyBuffer.order(ByteOrder.nativeOrder());
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, labels.size()}, DataType.UINT8);
        for (int i = 0; i < 10; i++) {
            emptyBuffer.rewind();
            interpreter.run(emptyBuffer, outputBuffer.getBuffer().rewind());
        }
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (interpreter == null) {
            image.close();
            return;
        }

        long startTime = SystemClock.elapsedRealtime();
        int rotationDegrees = image.getImageInfo().getRotationDegrees();
        Bitmap bitmap = image.toBitmap();

        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(new org.tensorflow.lite.support.image.ops.Rot90Op(-rotationDegrees / 90))
                .build();

        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        try {
            TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, labels.size()}, DataType.UINT8);
            interpreter.run(tensorImage.getBuffer(), outputBuffer.getBuffer().rewind());

            int[] probabilities = outputBuffer.getIntArray();
            int maxIdx = -1;
            int maxProb = -1;

            for (int i = 0; i < probabilities.length; i++) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i];
                    maxIdx = i;
                }
            }

            float confidence = maxProb / 255.0f;
            String className = (maxIdx != -1 && maxIdx < labels.size()) ? labels.get(maxIdx) : "Desconocido";
            long inferenceTime = SystemClock.elapsedRealtime() - startTime;

            listener.onResult(className, confidence, inferenceTime);
        } finally {
            image.close();
        }
    }
}
