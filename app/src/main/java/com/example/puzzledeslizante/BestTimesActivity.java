package com.example.puzzledeslizante;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BestTimesActivity extends AppCompatActivity {

    private ListView listViewBestTimes;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_best_times);

        listViewBestTimes = findViewById(R.id.listViewBestTimes);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Leer los mejores tiempos de Firestore
        leerMejoresTiemposFirestore();
    }

    private void leerMejoresTiemposFirestore() {
        db.collection("best_times")
                .orderBy("timestamp")
                .limit(10) // Limitar a los primeros 10 resultados
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> tiempos = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String username = document.getString("username");
                            String time = document.getString("time");
                            tiempos.add(username + " - " + time);
                        }
                        // Mostrar los tiempos en el ListView
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tiempos);
                        listViewBestTimes.setAdapter(adapter);
                    } else {
                        Log.w("Firestore", "Error al leer los documentos.", task.getException());
                        Toast.makeText(BestTimesActivity.this, "Error al obtener los tiempos.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
