package com.edutech.inventario.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inventario_session")
public class InventarioSession {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "clase_detectada")
    private String className;
    
    @ColumnInfo(name = "confianza")
    private float confidence;
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    public InventarioSession(String className, float confidence, long timestamp) {
        this.className = className;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
