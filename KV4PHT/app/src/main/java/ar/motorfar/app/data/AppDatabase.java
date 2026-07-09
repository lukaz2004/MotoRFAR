/*
kv4p HT (see http://kv4p.com)
Copyright (C) 2024 Vance Vagell

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
*/

package ar.motorfar.app.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;

import ar.motorfar.app.data.migrations.*;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Singleton Room database para kv4p HT.
 * Al primer inicio carga automáticamente los canales habilitados en Argentina (ENACOM).
 */
@Database(
    version = 8,
    entities = {AppSetting.class, ChannelMemory.class, APRSMessage.class, RoutePoint.class}
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";

    public abstract AppSettingDao appSettingDao();
    public abstract ChannelMemoryDao channelMemoryDao();
    public abstract APRSMessageDao aprsMessageDao();
    public abstract RoutePointDao routePointDao();

    // Migraciones
    public static final Migration MIGRATION_1_2 = new MigrationFrom1To2();
    public static final Migration MIGRATION_2_3 = new MigrationFrom2To3();
    public static final Migration MIGRATION_3_4 = new MigrationFrom3To4();
    public static final Migration MIGRATION_4_5 = new MigrationFrom4To5();
    public static final Migration MIGRATION_5_6 = new MigrationFrom5To6();
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `route_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `alias` TEXT NOT NULL)");
        }
    };
    public static final Migration MIGRATION_7_8 = new MigrationFrom7To8();

    @SuppressWarnings({"java:S3077", "java:S3008"})
    private static volatile AppDatabase INSTANCE;

    /**
     * Devuelve la instancia singleton de la base de datos.
     * Al primer acceso precarga los canales argentinos si no están cargados.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context);
                    preloadArgentinaChannelsIfNeeded(INSTANCE);
                }
            }
        }
        return INSTANCE;
    }

    private static AppDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "kv4pht-db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8
            )
            // Sin fallbackToDestructiveMigration: si falta una migración, la app
            // lanzará IllegalStateException en lugar de borrar datos silenciosamente.
            .build();
    }

    /**
     * Precarga los canales habilitados en Argentina al primer inicio.
     * Usa un setting como flag para no volver a insertarlos en reinicios posteriores.
     */
    private static void preloadArgentinaChannelsIfNeeded(AppDatabase db) {
        Executors.newSingleThreadExecutor().execute(() -> ensureArgentinaChannelsSeeded(db));
    }

    /**
     * 2026-07-07: antes esto vivía solo dentro del executor de fire-and-forget
     * de getInstance(), y MainViewModel.loadData() leía channelMemoryDao().getAll()
     * en OTRO executor sin ninguna sincronización entre ambos -- una race real:
     * si loadData() ganaba la carrera, leía la tabla todavía vacía y esa lista
     * vacía quedaba cacheada en el MutableLiveData para toda la sesión (nunca
     * se vuelve a leer), y el tono/canal activo desaparecían para siempre.
     * Ahora es un método sincrónico invocable desde loadData() antes de leer,
     * así se garantiza el orden sin importar qué tan rápido corra cada thread.
     */
    public static void ensureArgentinaChannelsSeeded(AppDatabase db) {
        try {
            AppSetting setting = db.appSettingDao()
                .getByName(ArgentinaChannels.PRELOADED_KEY);

            if (setting != null &&
                ArgentinaChannels.PRELOADED_VALUE.equals(setting.value)) {
                Log.d(TAG, "Canales Argentina ya precargados, saltando.");
                return;
            }

            // 2026-07-06: delete+insert en una sola transacción -- si no, cada
            // delete() y el insertAll() disparan la LiveData de getAll() por
            // separado, y cualquier observador (pantalla principal) puede
            // agarrar la tabla momentáneamente vacía a mitad del reseed
            // (bug real: nombre de canal caía a "SIMPLEX" y el tono
            // desaparecía justo después de un rename como GRUPO->PRINCIPAL).
            List<ChannelMemory> channels = ArgentinaChannels.getAll();
            db.runInTransaction(() -> {
                List<ChannelMemory> existing = db.channelMemoryDao().getAll();
                for (ChannelMemory ch : existing) {
                    db.channelMemoryDao().delete(ch);
                }
                db.channelMemoryDao().insertAll(
                    channels.toArray(new ChannelMemory[0])
                );
            });

            // Marcar como precargado
            db.saveAppSetting(
                ArgentinaChannels.PRELOADED_KEY,
                ArgentinaChannels.PRELOADED_VALUE
            );

            Log.i(TAG, "Canales Argentina precargados: " + channels.size() + " canales.");

        } catch (Exception e) {
            Log.e(TAG, "Error al precargar canales Argentina.", e);
        }
    }

    public void saveAppSetting(String key, String value) {
        AppSettingDao dao = appSettingDao();
        AppSetting setting = dao.getByName(key);
        if (setting == null) {
            setting = new AppSetting(key, value);
            dao.insertAll(setting);
        } else {
            setting.value = value;
            dao.update(setting);
        }
    }
}
