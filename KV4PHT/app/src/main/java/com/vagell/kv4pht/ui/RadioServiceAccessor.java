package com.vagell.kv4pht.ui;

import com.vagell.kv4pht.aprs.parser.APRSPacket;
import com.vagell.kv4pht.aprs.parser.InformationField;
import com.vagell.kv4pht.data.AppDatabase;
import com.vagell.kv4pht.radio.RadioAudioService;

/**
 * Puente Kotlin ↔ Java para métodos generados por Lombok que kapt no expone al compilador Kotlin.
 * Centraliza el acceso a setCallbacks() y getAppDb() sin tocar las clases originales.
 */
public final class RadioServiceAccessor {

    private RadioServiceAccessor() {}

    public static void setCallbacks(
            RadioAudioService service,
            RadioAudioService.RadioAudioServiceCallbacks callbacks
    ) {
        service.setCallbacks(callbacks);
    }

    public static AppDatabase getAppDb(MainViewModel viewModel) {
        return viewModel.getAppDb();
    }

    public static InformationField getAprsPayload(APRSPacket packet) {
        return packet.getPayload();
    }

    public static String getAprsSourceCall(APRSPacket packet) {
        return packet.getSourceCall();
    }
}
