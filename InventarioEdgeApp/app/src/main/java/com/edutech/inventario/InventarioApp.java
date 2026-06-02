package com.edutech.inventario;

import android.app.Application;
import com.edutech.inventario.data.InventoryRepository;
import com.edutech.inventario.data.local.InventarioDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventarioApp extends Application {
    public ExecutorService databaseExecutor = Executors.newFixedThreadPool(2);
    public InventoryRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        InventarioDatabase database = InventarioDatabase.getDatabase(this);
        repository = new InventoryRepository(database.inventarioDao(), databaseExecutor);
    }
}
