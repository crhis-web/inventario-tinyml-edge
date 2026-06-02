package com.edutech.inventario.data;

import com.edutech.inventario.data.local.InventarioDao;
import com.edutech.inventario.data.local.InventarioSession;
import java.util.concurrent.ExecutorService;

public class    InventoryRepository {
    private final InventarioDao dao;
    private final ExecutorService executorService;

    public InventoryRepository(InventarioDao dao, ExecutorService executorService) {
        this.dao = dao;
        this.executorService = executorService;
    }

    public void addDetection(String className, float confidence) {
        executorService.execute(() -> {
            InventarioSession session = new InventarioSession(className, confidence, System.currentTimeMillis());
            dao.insert(session);
        });
    }

    public androidx.lifecycle.LiveData<java.util.List<InventarioSession>> getAllSessions() {
        return dao.getAllSessions();
    }

    public void clearAll() {
        executorService.execute(dao::clearAll);
    }

    public interface OnResumenReadyListener {
        void onReady(java.util.List<com.edutech.inventario.data.local.ResumenClase> resumen);
    }

    public void getResumenAgrupado(OnResumenReadyListener listener) {
        executorService.execute(() -> {
            java.util.List<com.edutech.inventario.data.local.ResumenClase> resumen = dao.obtenerResumenAgrupado();
            listener.onReady(resumen);
        });
    }
}
