import os
import shutil
from pathlib import Path

base_dir = Path(r"C:\Users\Alumno\Desktop\inventario_tinyml_training")
src_projects = {
    "Proyecto_Clase1_Tecnologia": "clase1_tecnologia",
    "Proyecto_Clase2_Utiles": "clase2_utiles",
    "Proyecto_Clase3_MesaVacia": "clase3_vacio"
}

dataset_dir = base_dir / "dataset"

print("Inicializando reestructuracion del dataset...")

# Limpiar directorio de dataset si existe
if dataset_dir.exists():
    shutil.rmtree(dataset_dir)
dataset_dir.mkdir(exist_ok=True)

for src_name, target_name in src_projects.items():
    target_path = dataset_dir / target_name
    target_path.mkdir(exist_ok=True)
    
    src_path = base_dir / "proyectos_separados" / src_name
    
    # YOLO format images are in train/images and valid/images
    img_dirs = [src_path / "train" / "images", src_path / "valid" / "images"]
    
    count = 0
    for d in img_dirs:
        if d.exists():
            for ext in ('*.jpg', '*.jpeg', '*.png'):
                for img_file in d.glob(ext):
                    dest_file = target_path / f"{target_name}_{count}{img_file.suffix}"
                    shutil.copy2(img_file, dest_file)
                    count += 1
                    
    print(f"Copiadas {count} imagenes para {target_name}")

print("¡Estructura lista para Keras ImageDataGenerator!")
