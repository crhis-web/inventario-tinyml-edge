# 🧠 TinyML Edge AI - Inventario de Laboratorio Educativo

[![TensorFlow](https://img.shields.io/badge/TensorFlow-2.20+-FF6F00?logo=tensorflow)](https://www.tensorflow.org/)
[![TinyML](https://img.shields.io/badge/Edge%20Computing-TinyML-blue)](https://www.tinyml.org/)
[![Python](https://img.shields.io/badge/Python-3.12-3776AB?logo=python)](https://www.python.org/)

Este repositorio contiene la arquitectura de entrenamiento y exportación de un modelo de **Inteligencia Artificial para Edge Computing (TinyML)** diseñado para realizar inventarios automáticos en laboratorios de cómputo sin necesidad de conexión a internet.

## 📌 Contexto del Proyecto
El laboratorio de cómputo sufre pérdidas de periféricos (mouse, calculadoras, etc.) al finalizar las clases. El personal de inventario necesita una herramienta rápida para identificar qué objetos faltan. Debido a la falta de cobertura Wi-Fi en algunas zonas del laboratorio, **se descartó el uso de APIs en la nube**, optando por procesar la visión artificial 100% en el smartphone del encargado (Edge AI).

## 🚀 Arquitectura Técnica

1. **Modelo Base:** MobileNetV2 pre-entrenado en ImageNet.
2. **Transfer Learning:** Extracción de características acopladas a una nueva capa densa de 3 clases (`Tecnología`, `Útiles`, `Mesa Vacía`).
3. **Optimización:** Cuantización Entera Completa (Full Integer Quantization) a 8-bits (`INT8`) para compatibilidad extrema con dispositivos móviles antiguos.
4. **App Android (Java Nativo):** Despliegue mediante `.tflite` en una aplicación Android con arquitectura robusta.
   - **Motor SQLite Integrado (Room):** Agrupación reactiva en tiempo real y protección contra colapsos de RAM.
   - **UI/UX Premium:** Interfaz oscura (Glassmorphism), animaciones y sistema de Bottom Sheet para planes de acción de inteligencia artificial.
   - **Feedback Hardware:** Interfaz protegida por algoritmo de debouncing y **captura manual** con aviso sonoro (`ToneGenerator`) para otorgar control total al usuario.

## 📊 Métricas de Compresión y Rendimiento

La técnica de cuantización logró comprimir la red neuronal reduciendo su tamaño en más de un **80%**, permitiendo que corra a altas tasas de fotogramas por segundo (FPS) en un dispositivo móvil con recursos limitados, sin agotar la batería ni recalentar el CPU.

* Tamaño Original del Modelo: `~14 MB` (Float32)
* Tamaño Cuantizado a Edge: **`2.59 MB` (INT8)**
* Reducción Total: **-81.5%**

## 📂 Estructura del Repositorio

* `entrenar_local.py` - Pipeline completo de carga de datos, aumento de datos, entrenamiento (Transfer Learning) y cuantización TFLite.
* `notebooks/entrenamiento_mobilenet.ipynb` - Entorno interactivo de experimentación.
* `modelos_exportados/` - Contiene el archivo `.tflite` final y sus etiquetas (`labels.txt`).
* `INFORME_TECNICO.md` - Informe completo del proyecto (ver para detalles de la implementación de Android).

## 💡 Cómo entrenar localmente

Si se desea reentrenar el modelo con nuevas imágenes del laboratorio:

1. Coloca las imágenes en `dataset/` organizadas por carpetas (ej. `dataset/clase1_tecnologia`).
2. Ejecuta el pipeline:
   ```bash
   python entrenar_local.py
   ```
3. El nuevo modelo se guardará como `modelo_int8.tflite`.

---

## 👥 Colaboradores

* **CRHISTOPHER JENKO OSCCO AROTINCO**
* **RODRIGO ANDERSON AEDO TUTAYA**

🔗 **Repositorio Oficial:** [inventario-tinyml-edge](https://github.com/crhis-web/inventario-tinyml-edge.git)
