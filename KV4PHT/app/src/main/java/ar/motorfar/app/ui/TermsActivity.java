/*
MotoRFAR — Pantalla de términos y condiciones
Basado en kv4p HT (https://kv4p.com) por Vance Vagell
Licencia: GNU GPL v3
*/

package ar.motorfar.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import ar.motorfar.app.R;
import ar.motorfar.app.data.AppDatabase;

import java.util.concurrent.Executors;

public class TermsActivity extends AppCompatActivity {

    // Clave en AppSetting para saber si el usuario ya aceptó los términos
    public static final String TERMS_ACCEPTED_KEY  = "terms_accepted";
    public static final String TERMS_ACCEPTED_VALUE = "v1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        CheckBox checkBox = findViewById(R.id.termsCheckBox);
        Button acceptButton = findViewById(R.id.termsAcceptButton);
        Button rejectButton = findViewById(R.id.termsRejectButton);

        // El botón Aceptar empieza deshabilitado hasta que tilden el checkbox
        acceptButton.setEnabled(false);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
            acceptButton.setEnabled(isChecked));

        acceptButton.setOnClickListener(v -> {
            // Guardar aceptación en la base de datos
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(this).saveAppSetting(
                    TERMS_ACCEPTED_KEY,
                    TERMS_ACCEPTED_VALUE
                );
                runOnUiThread(() -> {
                    // Ir a la pantalla principal
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            });
        });

        rejectButton.setOnClickListener(v -> {
            // No aceptó — cerrar la app
            finishAffinity();
        });
    }

    /**
     * Verifica si el usuario ya aceptó los términos.
     * Llamar desde MainActivity antes de mostrar la UI principal.
     */
    public static boolean wereTermsAccepted(AppDatabase db) {
        ar.motorfar.app.data.AppSetting setting =
            db.appSettingDao().getByName(TERMS_ACCEPTED_KEY);
        return setting != null &&
            TERMS_ACCEPTED_VALUE.equals(setting.value);
    }
}
