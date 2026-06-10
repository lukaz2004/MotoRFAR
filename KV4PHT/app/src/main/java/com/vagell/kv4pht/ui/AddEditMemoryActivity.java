/*
kv4p HT (see http://kv4p.com)
Copyright (C) 2024 Vance Vagell

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
*/

package com.vagell.kv4pht.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.vagell.kv4pht.R;
import com.vagell.kv4pht.data.ChannelMemory;
import com.vagell.kv4pht.radio.RadioAudioService;
import com.vagell.kv4pht.radio.RadioServiceConnector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AddEditMemoryActivity extends AppCompatActivity {

    // -----------------------------------------------------------------------
    // Frecuencias habilitadas en Argentina — ENACOM (uso libre sin licencia)
    // -----------------------------------------------------------------------
    // VHF: Resolución 5/2015 Secretaría de Comunicaciones
    private static final List<String> ALLOWED_VHF_FREQUENCIES_AR = Arrays.asList(
        "138.5100",   // Canal secundario
        "139.9700",   // Canal prioritario / normal
        "140.9700"    // Solo emergencias (mantener despejado)
    );

    // UHF: sin frecuencias preconfiguradas — MotoRFAR opera solo en VHF (Res. 5/2015)
    // La lista vacía evita que aparezcan opciones UHF en el selector de canales.
    // Eliminación completa del panel UHF pendiente para Sprint 2 (refactor de UI).
    private static final List<String> ALLOWED_UHF_FREQUENCIES_AR = Arrays.asList();
    // -----------------------------------------------------------------------

    // IDs de los botones VHF
    private static final int[] VHF_BUTTON_IDS = {
        R.id.freqVhf1, R.id.freqVhf2, R.id.freqVhf3
    };

    // IDs de los botones UHF
    private static final int[] UHF_BUTTON_IDS = {
        R.id.freqUhf1,  R.id.freqUhf2,  R.id.freqUhf3,  R.id.freqUhf4,
        R.id.freqUhf5,  R.id.freqUhf6,  R.id.freqUhf7,  R.id.freqUhf8,
        R.id.freqUhf9,  R.id.freqUhf10, R.id.freqUhf11, R.id.freqUhf12,
        R.id.freqUhf13, R.id.freqUhf14, R.id.freqUhf15, R.id.freqUhf16
    };

    private boolean isAdd = true;
    private boolean isVhfRadio = true;
    private String selectedFrequency = "";
    private Button lastSelectedButton = null;

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
        2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private ChannelMemory mMemory;
    private MainViewModel viewModel;
    private RadioServiceConnector serviceConnector;
    private RadioAudioService radioAudioService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        super.onCreate(savedInstanceState);
        serviceConnector = new RadioServiceConnector(this);
        setContentView(R.layout.activity_add_edit_memory);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isAdd = (extras.getInt("requestCode") == MainActivityLegacy.REQUEST_ADD_MEMORY);
            isVhfRadio = extras.getBoolean("isVhfRadio");

            if (!isAdd) {
                int mMemoryId = extras.getInt("memoryId");
                threadPoolExecutor.execute(() -> {
                    mMemory = viewModel.getAppDb().channelMemoryDao().getById(mMemoryId);
                    populateOriginalValues();
                });
            } else {
                populateDefaults();

                // Pre-seleccionar frecuencia activa si está permitida
                String activeFrequencyStr = extras.getString("activeFrequencyStr");
                if (activeFrequencyStr != null && isAllowedFrequency(activeFrequencyStr)) {
                    selectedFrequency = activeFrequencyStr;
                }

                String selectedMemoryGroup = extras.getString("selectedMemoryGroup");
                if (selectedMemoryGroup != null) {
                    AutoCompleteTextView g = findViewById(R.id.editMemoryGroupTextInputEditText);
                    g.setText(selectedMemoryGroup, false);
                }
                String offset = extras.getString("offset");
                if (offset != null) {
                    AutoCompleteTextView o = findViewById(R.id.editOffsetTextView);
                    o.setText(offset, false);
                }
                String tone = extras.getString("tone");
                if (tone != null) {
                    AutoCompleteTextView t = findViewById(R.id.editToneTxTextView);
                    t.setText(tone, false);
                }
                String name = extras.getString("name");
                if (name != null) {
                    TextInputEditText n = findViewById(R.id.editNameTextInputEditText);
                    n.setText(name);
                }
            }
        }

        TextView titleTextView = findViewById(R.id.addEditToolbarTitle);
        titleTextView.setText(isAdd ? getString(R.string.add_memory_display) : getString(R.string.edit_memory));

        setAdvancedOptionsVisible(false);
        populateMemoryGroups();
        populateOffsets();
        populateTones();
        setupFrequencyButtons();
    }

    // -----------------------------------------------------------------------
    // Botones de frecuencia
    // -----------------------------------------------------------------------

    private void setupFrequencyButtons() {
        // Mostrar solo el panel correcto según el tipo de radio
        findViewById(R.id.vhfFrequencyButtons).setVisibility(isVhfRadio ? View.VISIBLE : View.GONE);
        findViewById(R.id.uhfFrequencyButtons).setVisibility(isVhfRadio ? View.GONE : View.VISIBLE);

        int[] buttonIds = isVhfRadio ? VHF_BUTTON_IDS : UHF_BUTTON_IDS;

        for (int id : buttonIds) {
            Button btn = findViewById(id);
            if (btn == null) continue;
            btn.setOnClickListener(v -> {
                String freq = (String) v.getTag();
                selectFrequency(freq, (Button) v);
            });
            styleButtonUnselected(btn);
        }

        // Si ya hay una frecuencia pre-seleccionada, resaltarla
        if (!selectedFrequency.isEmpty()) {
            for (int id : buttonIds) {
                Button btn = findViewById(id);
                if (btn != null && selectedFrequency.equals(btn.getTag())) {
                    selectFrequency(selectedFrequency, btn);
                    break;
                }
            }
        }
    }

    private void selectFrequency(String freq, Button btn) {
        selectedFrequency = freq;

        // Actualizar campo oculto
        TextInputEditText hiddenFreq = findViewById(R.id.editFrequencyTextInputEditText);
        hiddenFreq.setText(freq);

        // Actualizar display
        TextView display = findViewById(R.id.selectedFrequencyDisplay);
        display.setText(freq + " MHz");

        // Quitar resaltado del botón anterior
        if (lastSelectedButton != null) {
            styleButtonUnselected(lastSelectedButton);
        }
        // Resaltar el nuevo
        styleButtonSelected(btn);
        lastSelectedButton = btn;
    }

    private void styleButtonSelected(Button btn) {
        btn.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        btn.setTextColor(ContextCompat.getColor(this, R.color.black));
    }

    private void styleButtonUnselected(Button btn) {
        btn.setBackgroundColor(0x22FFFFFF); // blanco semitransparente
        btn.setTextColor(ContextCompat.getColor(this, R.color.primary));
    }

    // -----------------------------------------------------------------------
    // Validación
    // -----------------------------------------------------------------------

    private List<String> getAllowedFrequencies() {
        return isVhfRadio ? ALLOWED_VHF_FREQUENCIES_AR : ALLOWED_UHF_FREQUENCIES_AR;
    }

    private boolean isAllowedFrequency(String freqStr) {
        try {
            float input = Float.parseFloat(freqStr.trim());
            for (String allowed : getAllowedFrequencies()) {
                if (Math.abs(input - Float.parseFloat(allowed)) < 0.002f) return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Ciclo de vida
    // -----------------------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        serviceConnector.bind(rs -> this.radioAudioService = rs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EditText nameEditText = findViewById(R.id.editNameTextInputEditText);
        nameEditText.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPoolExecutor.shutdownNow();
        serviceConnector.unbind();
    }

    // -----------------------------------------------------------------------
    // Populate helpers
    // -----------------------------------------------------------------------

    private void populateMemoryGroups() {
        final Activity activity = this;
        threadPoolExecutor.execute(() -> {
            List<String> memoryGroups = viewModel.getAppDb().channelMemoryDao().getGroups();
            for (int i = 0; i < memoryGroups.size(); i++) {
                String n = memoryGroups.get(i);
                if (n == null || n.trim().length() == 0) { memoryGroups.remove(i); i--; }
            }
            activity.runOnUiThread(() -> {
                AutoCompleteTextView v = findViewById(R.id.editMemoryGroupTextInputEditText);
                v.setAdapter(new ArrayAdapter<>(activity, R.layout.dropdown_item, memoryGroups));
            });
        });
    }

    private void populateOffsets() {
        AutoCompleteTextView v = findViewById(R.id.editOffsetTextView);
        v.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item,
            Arrays.asList("None", "Down", "Up")));
    }

    private void populateTones() {
        AutoCompleteTextView tx = findViewById(R.id.editToneTxTextView);
        tx.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, ToneHelper.VALID_TONE_STRINGS));
        AutoCompleteTextView rx = findViewById(R.id.editToneRxTextView);
        rx.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, ToneHelper.VALID_TONE_STRINGS));
    }

    private void populateDefaults() {
        TextInputEditText offset = findViewById(R.id.customOffsetTextInputEditText);
        offset.setText(isVhfRadio ? "600" : "5000");
    }

    private void populateOriginalValues() {
        if (isAdd) return;
        runOnUiThread(() -> {
            ((TextInputEditText) findViewById(R.id.editNameTextInputEditText)).setText(mMemory.name);
            ((AutoCompleteTextView) findViewById(R.id.editMemoryGroupTextInputEditText)).setText(mMemory.group, false);

            // Restaurar frecuencia guardada y resaltar su botón
            selectedFrequency = mMemory.frequency;
            TextView display = findViewById(R.id.selectedFrequencyDisplay);
            display.setText(mMemory.frequency + " MHz");
            int[] buttonIds = isVhfRadio ? VHF_BUTTON_IDS : UHF_BUTTON_IDS;
            for (int id : buttonIds) {
                Button btn = findViewById(id);
                if (btn != null && isAllowedFrequency((String) btn.getTag())
                        && Math.abs(Float.parseFloat((String) btn.getTag()) - Float.parseFloat(mMemory.frequency)) < 0.002f) {
                    styleButtonSelected(btn);
                    lastSelectedButton = btn;
                } else if (btn != null) {
                    styleButtonUnselected(btn);
                }
            }

            AutoCompleteTextView offsetView = findViewById(R.id.editOffsetTextView);
            if (mMemory.offset == ChannelMemory.OFFSET_NONE) offsetView.setText("None", false);
            else if (mMemory.offset == ChannelMemory.OFFSET_DOWN) offsetView.setText("Down", false);
            else if (mMemory.offset == ChannelMemory.OFFSET_UP) offsetView.setText("Up", false);

            ((AutoCompleteTextView) findViewById(R.id.editToneTxTextView)).setText(mMemory.txTone, false);
            ((AutoCompleteTextView) findViewById(R.id.editToneRxTextView)).setText(mMemory.rxTone, false);
            ((TextInputEditText) findViewById(R.id.customOffsetTextInputEditText)).setText("" + mMemory.offsetKhz);
            ((Switch) findViewById(R.id.skipDuringScanSwitch)).setChecked(mMemory.skipDuringScan);
        });
    }

    // -----------------------------------------------------------------------
    // Acciones de botones
    // -----------------------------------------------------------------------

    public void cancelButtonClicked(View view) {
        setResult(Activity.RESULT_CANCELED, getIntent());
        finish();
    }

    public void saveButtonClicked(View view) {
        TextInputEditText editNameView = findViewById(R.id.editNameTextInputEditText);
        String name = editNameView.getText().toString().trim();

        AutoCompleteTextView groupView = findViewById(R.id.editMemoryGroupTextInputEditText);
        String group = groupView.getText().toString().trim();

        AutoCompleteTextView offsetView = findViewById(R.id.editOffsetTextView);
        String offset = offsetView.getText().toString().trim();

        AutoCompleteTextView toneTxView = findViewById(R.id.editToneTxTextView);
        String txTone = toneTxView.getText().toString().trim();

        AutoCompleteTextView toneRxView = findViewById(R.id.editToneRxTextView);
        String rxTone = toneRxView.getText().toString().trim();

        TextInputEditText customOffsetView = findViewById(R.id.customOffsetTextInputEditText);
        String offsetKhz = customOffsetView.getText().toString().trim();

        Switch skipSwitch = findViewById(R.id.skipDuringScanSwitch);
        boolean skipDuringScan = skipSwitch.isChecked();

        // Validar nombre
        if (name.length() == 0) {
            editNameView.setError("Poné un nombre al canal");
            editNameView.requestFocus();
            return;
        }

        // Validar que se haya elegido una frecuencia
        if (selectedFrequency.isEmpty()) {
            TextView display = findViewById(R.id.selectedFrequencyDisplay);
            display.setText("⚠ Seleccioná una frecuencia");
            display.setTextColor(0xFFFF5555);
            return;
        }

        // Validar que la frecuencia esté en la lista permitida (doble chequeo)
        if (!isAllowedFrequency(selectedFrequency)) {
            TextView display = findViewById(R.id.selectedFrequencyDisplay);
            display.setText("⚠ Frecuencia no habilitada por ENACOM");
            display.setTextColor(0xFFFF5555);
            return;
        }

        // Formatear frecuencia
        if (radioAudioService == null) {
            TextView display = findViewById(R.id.selectedFrequencyDisplay);
            display.setText("⚠ Servicio no disponible, intentá de nuevo");
            display.setTextColor(0xFFFF5555);
            return;
        }
        String frequency = radioAudioService.makeSafeHamFreq(selectedFrequency);
        if (frequency == null) {
            TextView display = findViewById(R.id.selectedFrequencyDisplay);
            display.setText("⚠ Frecuencia inválida");
            display.setTextColor(0xFFFF5555);
            return;
        }

        // Validar offset
        int offsetKhzInt;
        if (offsetKhz.length() == 0) {
            customOffsetView.setError("Ingresá un offset");
            return;
        }
        try {
            offsetKhzInt = Integer.parseInt(offsetKhz);
            if (offsetKhzInt > 30000 || offsetKhzInt < 0) {
                customOffsetView.setError("Offset inválido");
                return;
            }
        } catch (NumberFormatException e) {
            customOffsetView.setError("Offset inválido");
            return;
        }

        // Guardar
        ChannelMemory memory = isAdd ? new ChannelMemory() : mMemory;
        memory.name = name;
        memory.group = group;
        memory.frequency = frequency;
        memory.offset = offset.equals("Down") ? ChannelMemory.OFFSET_DOWN
                      : offset.equals("Up")   ? ChannelMemory.OFFSET_UP
                                              : ChannelMemory.OFFSET_NONE;
        memory.txTone = txTone;
        memory.rxTone = rxTone;
        memory.offsetKhz = offsetKhzInt;
        memory.skipDuringScan = skipDuringScan;

        final ChannelMemory finalMemory = memory;
        threadPoolExecutor.execute(() -> {
            if (isAdd) viewModel.getAppDb().channelMemoryDao().insertAll(finalMemory);
            else       viewModel.getAppDb().channelMemoryDao().update(finalMemory);
            setResult(Activity.RESULT_OK, getIntent());
            finish();
        });
    }

    public void advancedMemoryOptionsButtonClicked(View view) {
        setAdvancedOptionsVisible(true);
    }

    private void setAdvancedOptionsVisible(boolean visible) {
        findViewById(R.id.advancedMemoryOptionsButton).setVisibility(visible ? View.GONE : View.VISIBLE);
        findViewById(R.id.skipDuringScanSwitch).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.customOffsetTextInputLayout).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.editToneRxTextInputLayout).setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
