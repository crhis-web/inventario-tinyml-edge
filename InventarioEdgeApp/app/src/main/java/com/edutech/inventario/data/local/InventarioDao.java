package com.edutech.inventario.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface InventarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventarioSession session);

    @Query("SELECT * FROM inventario_session ORDER BY timestamp DESC")
    LiveData<List<InventarioSession>> getAllSessions();

    @Query("SELECT clase_detectada as clase, COUNT(*) as cantidad FROM inventario_session GROUP BY clase_detectada")
    List<ResumenClase> obtenerResumenAgrupado();

    @Query("DELETE FROM inventario_session")
    void clearAll();
}
