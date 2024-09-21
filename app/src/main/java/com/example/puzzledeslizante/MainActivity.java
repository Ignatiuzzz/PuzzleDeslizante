package com.example.puzzledeslizante;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    TextView[][] tiles = new TextView[3][3];
    TextView emptyTile;
    Chronometer chronometer;
    Button btnMezclar;
    Button btnComenzar;
    Button btnParar;
    Button btnSolve;
    Button btnSelectImage;
    List<Long> mejoresTiempos = new ArrayList<>();
    boolean isPuzzleMixed = false;
    AStarSolver solver = new AStarSolver();
    private Handler handler = new Handler();
    private Runnable solutionRunnable;
    private boolean solvingInProgress = false;
    private Image puzzleImage;

    // Firebase Firestore
    private FirestoreDatabase firestoreDatabase; // Instancia de FirestoreDatabase
    private SharedPreferences preferences;
    FirebaseFirestore db;

    // DrawerLayout para el menú deslizable
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private Button btnLogout;
    private boolean isDarkMode = false; // Variable para el modo oscuro
    private Button btnToggleTheme;

    private View.OnClickListener switchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView clickedTile = (TextView) v;
            if (esVecino(clickedTile, emptyTile)) {
                intercambiarVacio(clickedTile);
                if (estaResulto()) {
                    chronometer.stop();
                    guardarTiempoFirestore();  // Ahora usando FirestoreDatabase
                    mostrarVictoria();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar FirestoreDatabase y SharedPreferences
        firestoreDatabase = new FirestoreDatabase();
        preferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Inicializar el botón de Logout en el NavigationView
        btnLogout = findViewById(R.id.btnLogout);

        // Lógica para el botón Logout
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

                // Aquí puedes añadir la lógica para cerrar sesión
                SharedPreferences preferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();  // Limpiar las preferencias guardadas (si usas autenticación persistente)
                editor.apply();

                // Redirigir a la pantalla de SignInActivity
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(intent);
                finish(); // Finaliza la actividad actual para que no pueda volver atrás
            }
        });

        // Inicializar el botón de cambio de tema
        btnToggleTheme = findViewById(R.id.btnToggleTheme);

        // Verificar el estado actual del modo de la interfaz
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        isDarkMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        // Configurar el evento OnClickListener para cambiar el tema
        btnToggleTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDarkMode = !isDarkMode;
                if (isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Cambiar a modo oscuro
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Cambiar a modo claro
                }
            }
        });

        Button btnRemoveImage = findViewById(R.id.btnRemoveImage);
        btnRemoveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPuzzleToInitialState();
            }
        });

        // Inicializar el botón de Mejores Tiempos
        Button btnBestTimes = findViewById(R.id.btnBestTimes);

        // Abrir la actividad de Mejores Tiempos al hacer clic
        btnBestTimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BestTimesActivity.class);
                startActivity(intent);
            }
        });

        // Initialize the tiles with corresponding TextViews
        tiles[0][0] = findViewById(R.id.tv1);
        tiles[0][1] = findViewById(R.id.tv2);
        tiles[0][2] = findViewById(R.id.tv3);
        tiles[1][0] = findViewById(R.id.tv4);
        tiles[1][1] = findViewById(R.id.tv5);
        tiles[1][2] = findViewById(R.id.tv6);
        tiles[2][0] = findViewById(R.id.tv7);
        tiles[2][1] = findViewById(R.id.tv8);
        tiles[2][2] = findViewById(R.id.tvEmpty);
        emptyTile = tiles[2][2];
        chronometer = findViewById(R.id.cronometro);
        btnMezclar = findViewById(R.id.btnMezclar);
        btnComenzar = findViewById(R.id.btnComenzar);
        btnParar = findViewById(R.id.btnParar);
        btnSolve = findViewById(R.id.btnSolve);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tiles[i][j].setOnClickListener(switchListener);
            }
        }

        btnMezclar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mezclarPuzzle();
            }
        });

        btnComenzar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPuzzleMixed) {
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    enablePuzzleInteraction();
                } else {
                    Toast.makeText(MainActivity.this, "Primero mezcla el puzzle.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnParar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chronometer.stop();
                disablePuzzleInteraction();
            }
        });

        btnSolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solvePuzzle();
            }
        });

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        // Shuffle puzzle at the start of the app
        mezclarPuzzle();
        disablePuzzleInteraction();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                puzzleImage = new Image(bitmap);
                updateTilesWithImage(puzzleImage.getPuzzlePieces());
                isPuzzleMixed = true; // Set to true since the image is now loaded
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void guardarTiempoFirestore() {
        long chronometerBase = chronometer.getBase();
        firestoreDatabase.guardarTiempoFirestore(chronometerBase, preferences);  // Usar FirestoreDatabase
    }

    private void resetPuzzleToInitialState() {
        // Restablecer el puzzle con números del 1 al 8
        List<String> tileTexts = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            tileTexts.add(String.valueOf(i));
        }
        tileTexts.add(""); // Añadir el espacio vacío

        // Aplicar al tablero
        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // Restablecer texto
                tiles[i][j].setText(tileTexts.get(index));

                // Restaurar el fondo predeterminado para cada pieza
                int tileId = tiles[i][j].getId();
                if (tileId == R.id.tv1) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor1);
                } else if (tileId == R.id.tv2) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor2);
                } else if (tileId == R.id.tv3) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor3);
                } else if (tileId == R.id.tv4) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor4);
                } else if (tileId == R.id.tv5) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor5);
                } else if (tileId == R.id.tv6) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor6);
                } else if (tileId == R.id.tv7) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor7);
                } else if (tileId == R.id.tv8) {
                    tiles[i][j].setBackgroundResource(R.color.tileColor8);
                } else if (tileId == R.id.tvEmpty) {
                    tiles[i][j].setBackgroundResource(R.color.tileEmptyColor);
                }

                if (tileTexts.get(index).isEmpty()) {
                    emptyTile = tiles[i][j];  // Establecer la casilla vacía
                }
                index++;
            }
        }

        // Detener el cronómetro y deshabilitar interacción del puzzle
        chronometer.stop();
        disablePuzzleInteraction();
        isPuzzleMixed = false;
    }

    private void updateTilesWithImage(List<Bitmap> pieces) {
        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (index < pieces.size()) {
                    tiles[i][j].setBackground(new BitmapDrawable(getResources(), pieces.get(index)));
                } else {
                    tiles[i][j].setBackgroundColor(getResources().getColor(android.R.color.transparent)); // Empty tile
                }
                index++;
            }
        }
    }

    private void mezclarPuzzle() {
        List<String> tileTexts = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            tileTexts.add(String.valueOf(i));
        }
        tileTexts.add(""); // Espacio vacío

        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tiles[i][j].setText(tileTexts.get(index));
                if (tileTexts.get(index).isEmpty()) {
                    emptyTile = tiles[i][j];
                }
                index++;
            }
        }

        realizarMovimientosAleatorios(100);
        isPuzzleMixed = true;
        chronometer.setBase(SystemClock.elapsedRealtime());
    }

    private void realizarMovimientosAleatorios(int numMovimientos) {
        for (int i = 0; i < numMovimientos; i++) {
            List<TextView> vecinos = obtenerVecinos(emptyTile);
            if (!vecinos.isEmpty()) {
                TextView vecinoSeleccionado = vecinos.get((int) (Math.random() * vecinos.size()));
                intercambiarVacio(vecinoSeleccionado);
            }
        }
    }

    private List<TextView> obtenerVecinos(TextView tileVacio) {
        int[] posVacio = obtPosicion(tileVacio);
        List<TextView> vecinos = new ArrayList<>();

        if (posVacio[0] > 0) vecinos.add(tiles[posVacio[0] - 1][posVacio[1]]);
        if (posVacio[0] < 2) vecinos.add(tiles[posVacio[0] + 1][posVacio[1]]);
        if (posVacio[1] > 0) vecinos.add(tiles[posVacio[0]][posVacio[1] - 1]);
        if (posVacio[1] < 2) vecinos.add(tiles[posVacio[0]][posVacio[1] + 1]);

        return vecinos;
    }

    private void disablePuzzleInteraction() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tiles[i][j].setOnClickListener(null);
            }
        }
    }

    private void enablePuzzleInteraction() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tiles[i][j].setOnClickListener(switchListener);
            }
        }
    }

    private boolean esVecino(TextView tile1, TextView tile2) {
        int[] pos1 = obtPosicion(tile1);
        int[] pos2 = obtPosicion(tile2);

        return (Math.abs(pos1[0] - pos2[0]) == 1 && pos1[1] == pos2[1])
                || (Math.abs(pos1[1] - pos2[1]) == 1 && pos1[0] == pos2[0]);
    }

    private int[] obtPosicion(TextView tile) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tiles[i][j] == tile) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    private void intercambiarVacio(TextView clickedTile) {
        CharSequence temp = clickedTile.getText();
        clickedTile.setText(emptyTile.getText());
        emptyTile.setText(temp);
        emptyTile = clickedTile;
    }

    private boolean estaResulto() {
        int count = 1;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tiles[i][j].getText().toString().isEmpty()) {
                    if (i == 2 && j == 2) {
                        return true;
                    }
                } else {
                    if (!tiles[i][j].getText().toString().equals(String.valueOf(count))) {
                        return false;
                    }
                    count++;
                }
            }
        }
        return false;
    }

    private void mostrarVictoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("¡Felicidades!")
                .setMessage("¡Has resuelto el puzzle!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void solvePuzzle() {
        int[][] initialBoard = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String text = tiles[i][j].getText().toString();
                initialBoard[i][j] = text.isEmpty() ? 0 : Integer.parseInt(text);
            }
        }

        List<int[][]> solutionSteps = solver.solve(initialBoard);
        if (!solutionSteps.isEmpty()) {
            showSolutionSteps(solutionSteps);
        } else {
            Toast.makeText(MainActivity.this, "No se puede resolver el puzzle.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSolutionSteps(List<int[][]> steps) {
        if (solvingInProgress) {
            handler.removeCallbacks(solutionRunnable);
        }

        final int[] stepIndex = {0};

        solutionRunnable = new Runnable() {
            @Override
            public void run() {
                if (stepIndex[0] < steps.size()) {
                    int[][] step = steps.get(stepIndex[0]);
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            if (step[i][j] == 0) {
                                tiles[i][j].setText("");
                                emptyTile = tiles[i][j];
                            } else {
                                tiles[i][j].setText(String.valueOf(step[i][j]));
                            }
                        }
                    }
                    stepIndex[0]++;
                    handler.postDelayed(this, 500);
                } else {
                    solvingInProgress = false;
                }
            }
        };

        solvingInProgress = true;
        handler.post(solutionRunnable);
    }
}
