import os
import tensorflow as tf
import numpy as np

print("TensorFlow:", tf.__version__)

dataset_dir = r'C:\Users\Alumno\Desktop\inventario_tinyml_training\dataset'
export_dir = r'C:\Users\Alumno\Desktop\inventario_tinyml_training\modelos_exportados'
os.makedirs(export_dir, exist_ok=True)

img_height, img_width = 224, 224
batch_size = 32

# 1. Preparar Dataset
print("Cargando dataset...")
train_datagen = tf.keras.preprocessing.image.ImageDataGenerator(
    validation_split=0.2,
    rotation_range=15,
    zoom_range=0.15,
    brightness_range=[0.7, 1.3],
    horizontal_flip=True
)
val_datagen = tf.keras.preprocessing.image.ImageDataGenerator(validation_split=0.2)

train_generator = train_datagen.flow_from_directory(
    dataset_dir, target_size=(img_height, img_width),
    batch_size=batch_size, class_mode='categorical', subset='training'
)
val_generator = val_datagen.flow_from_directory(
    dataset_dir, target_size=(img_height, img_width),
    batch_size=batch_size, class_mode='categorical', subset='validation'
)

labels = dict((v, k) for k, v in train_generator.class_indices.items())
num_classes = len(labels)
print(f"Etiquetas: {labels}")

with open(os.path.join(export_dir, 'labels.txt'), 'w') as f:
    for i in range(num_classes):
        f.write(f"{labels[i]}\n")

# 2. Construir Modelo
print("Construyendo modelo...")
inputs = tf.keras.Input(shape=(img_height, img_width, 3), dtype=tf.uint8, name='image_input')
x = tf.keras.layers.Lambda(lambda t: (tf.cast(t, tf.float32) / 127.5) - 1.0)(inputs)

base_model = tf.keras.applications.MobileNetV2(
    input_shape=(img_height, img_width, 3),
    include_top=False, weights='imagenet'
)
base_model.trainable = False

x = base_model(x, training=False)
x = tf.keras.layers.GlobalAveragePooling2D()(x)
x = tf.keras.layers.Dropout(0.2)(x)
outputs = tf.keras.layers.Dense(num_classes, activation='softmax', name='prediction')(x)

model = tf.keras.Model(inputs, outputs)

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# 3. Entrenar
print("Iniciando entrenamiento...")
history = model.fit(
    train_generator,
    validation_data=val_generator,
    epochs=10,
    callbacks=[
        tf.keras.callbacks.EarlyStopping(patience=2, restore_best_weights=True),
        tf.keras.callbacks.ReduceLROnPlateau(patience=1, factor=0.5)
    ]
)

model.save(os.path.join(export_dir, 'modelo_base.h5'))
print("✅ Modelo entrenado y guardado.")

# 4. Cuantización INT8
print("Iniciando cuantización INT8...")
def representative_data_gen():
    for input_value, _ in train_generator:
        yield [input_value.astype(np.uint8)]
        break

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_data_gen
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.uint8
converter.inference_output_type = tf.uint8

tflite_model = converter.convert()

tflite_path = os.path.join(export_dir, 'modelo_int8.tflite')
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

size_mb = os.path.getsize(tflite_path) / (1024*1024)
print(f"✅ modelo_int8.tflite guardado — Tamaño: {size_mb:.2f} MB")
