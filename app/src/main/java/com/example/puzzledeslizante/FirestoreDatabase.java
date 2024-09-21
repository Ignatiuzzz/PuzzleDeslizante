package com.example.puzzledeslizante;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.os.SystemClock;
import android.util.Log;
import android.content.SharedPreferences;

public class FirestoreDatabase {

    // Firebase Firestore
    private FirebaseFirestore db;

    public FirestoreDatabase() {
        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();
    }

    // Método para guardar el tiempo en Firestore
    public void guardarTiempoFirestore(long chronometerBase, SharedPreferences preferences) {
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometerBase;

        // Obtener el nombre de usuario desde SharedPreferences
        String username = preferences.getString("username", "");  // Devuelve una cadena vacía si no hay usuario

        // Convertir el tiempo en milisegundos a minutos y segundos
        int minutes = (int) (elapsedMillis / 1000) / 60;
        int seconds = (int) (elapsedMillis / 1000) % 60;

        // Formatear el tiempo como "MM:SS"
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // Crear un documento con el tiempo y el nombre de usuario
        Map<String, Object> timeRecord = new HashMap<>();
        timeRecord.put("time", formattedTime);
        timeRecord.put("username", username);  // Registrar el nombre de usuario con el tiempo
        timeRecord.put("timestamp", FieldValue.serverTimestamp());

        db.collection("best_times")
                .add(timeRecord)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Documento guardado con ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error al guardar el documento", e);
                });
    }
}
