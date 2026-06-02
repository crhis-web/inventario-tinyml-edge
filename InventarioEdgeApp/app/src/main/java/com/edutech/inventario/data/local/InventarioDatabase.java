package com.edutech.inventario.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {InventarioSession.class}, version = 1, exportSchema = false)
public abstract class InventarioDatabase extends RoomDatabase {
    public abstract InventarioDao inventarioDao();

    private static volatile InventarioDatabase INSTANCE;

    public static InventarioDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (InventarioDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            InventarioDatabase.class, "inventario_db")
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
