import json

nb_path = r"C:\Users\Alumno\Desktop\inventario_tinyml_training\notebooks\entrenamiento_mobilenet.ipynb"

with open(nb_path, "r", encoding="utf-8") as f:
    nb = json.load(f)

for cell in nb.get("cells", []):
    if cell.get("cell_type") == "code":
        src = "".join(cell.get("source", []))
        if "ImageDataGenerator" in src and "validation_split=0.2" in src:
            new_source = [
                "dataset_dir = '../dataset'\n",
                "batch_size = 32\n",
                "img_height = 224\n",
                "img_width = 224\n",
                "\n",
                "# Generador para entrenamiento CON aumentación\n",
                "train_datagen = tf.keras.preprocessing.image.ImageDataGenerator(\n",
                "    validation_split=0.2,\n",
                "    rotation_range=15,\n",
                "    zoom_range=0.15,\n",
                "    brightness_range=[0.8, 1.2],\n",
                "    horizontal_flip=True, # Permitido\n",
                "    vertical_flip=False   # PROHIBIDO explícitamente\n",
                ")\n",
                "\n",
                "# Generador para validación SIN aumentación (Muro de Contención)\n",
                "val_datagen = tf.keras.preprocessing.image.ImageDataGenerator(\n",
                "    validation_split=0.2\n",
                ")\n",
                "\n",
                "train_generator = train_datagen.flow_from_directory(\n",
                "    dataset_dir,\n",
                "    target_size=(img_height, img_width),\n",
                "    batch_size=batch_size,\n",
                "    class_mode='categorical',\n",
                "    subset='training'\n",
                ")\n",
                "\n",
                "val_generator = val_datagen.flow_from_directory(\n",
                "    dataset_dir,\n",
                "    target_size=(img_height, img_width),\n",
                "    batch_size=batch_size,\n",
                "    class_mode='categorical',\n",
                "    subset='validation'\n",
                ")\n",
                "\n",
                "labels = (train_generator.class_indices)\n",
                "labels = dict((v,k) for k,v in labels.items())\n",
                "print(\"Etiquetas encontradas:\", labels)\n",
                "\n",
                "with open('../modelos_exportados/labels.txt', 'w') as f:\n",
                "    for i in range(len(labels)):\n",
                "        f.write(f\"{labels[i]}\\n\")\n"
            ]
            cell["source"] = new_source
            break

with open(nb_path, "w", encoding="utf-8") as f:
    json.dump(nb, f, indent=1)

print("Notebook actualizado con éxito.")
