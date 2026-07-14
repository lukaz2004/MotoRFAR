/*
kv4p HT (see http://kv4p.com)
Copyright (C) 2024 Vance Vagell

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package ar.motorfar.app.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import androidx.lifecycle.MutableLiveData;
import ar.motorfar.app.data.APRSMessage;
import ar.motorfar.app.data.AppDatabase;
import ar.motorfar.app.data.ChannelMemory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainViewModel extends AndroidViewModel {
    // Database holding various user-defined app parameters
    @Getter
    @NonNull
    private final AppDatabase appDb;

    AtomicBoolean loaded = new AtomicBoolean(false);
    // LiveData reactiva de Room (InvalidationTracker) -- se re-emite sola en
    // cada insert/update/delete de channel_memories, no es un snapshot único
    // cacheado como antes (esa versión vieja se desincronizaba del canal real
    // si la tabla cambiaba después del primer load).
    private final LiveData<List<ChannelMemory>> channelMemories;
    // LiveData holding the list of APRSMessage objects
    private final MutableLiveData<List<APRSMessage>> aprsMessages = new MutableLiveData<>();

    private static final java.util.concurrent.ExecutorService databaseExecutor = Executors.newFixedThreadPool(2);

    public MainViewModel(@NotNull Application application) {
        super(application);
        appDb = AppDatabase.getInstance(application.getApplicationContext());
        channelMemories = appDb.channelMemoryDao().observeAll();
    }

    private void loadData() {
        // 2026-07-07: AppDatabase.getInstance() dispara el seed de canales en SU
        // PROPIO executor (fire-and-forget) -- llamarlo acá, sincrónico y ANTES
        // de leer, garantiza que la tabla ya esté poblada la primera vez que se
        // usa. Ya no hace falta ganarle una carrera a un postValue manual:
        // channelMemories es reactiva (ver constructor) y se re-emite sola si
        // el seed termina después del primer observe.
        AppDatabase.ensureArgentinaChannelsSeeded(getAppDb());
        aprsMessages.postValue(getAppDb().aprsMessageDao().getAll());
        loaded.set(true);
    }

    public void loadDataAsync(Runnable callback) {
        databaseExecutor.execute(() -> {
            loadData();
            callback.run();
        });
    }

    public LiveData<List<APRSMessage>> getAPRSMessages() {
        return aprsMessages;
    }

    public LiveData<List<ChannelMemory>> getChannelMemories() {
        return channelMemories;
    }

    public void highlightMemory(ChannelMemory memory) {
        List<ChannelMemory> memories = channelMemories.getValue();
        if (memories == null) { return; }
        for (ChannelMemory channelMemory : memories) {
            channelMemory.setHighlighted(false);
        }
        if (memory != null) {
            memory.setHighlighted(true);
        }
    }

    private void deleteMemory(ChannelMemory memory) {
        getAppDb().channelMemoryDao().delete(memory);
    }

    public void deleteMemoryAsync(ChannelMemory memory, Runnable callback) {
        databaseExecutor.execute(() -> {
            deleteMemory(memory);
            callback.run();
        });
    }

    public boolean isLoaded() {
        return loaded.get();
    }
}
