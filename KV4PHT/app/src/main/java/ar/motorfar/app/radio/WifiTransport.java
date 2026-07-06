package ar.motorfar.app.radio;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WiFi UDP transport for the ESP32 SoftAP (Contrato A, F0=WiFi).
 *
 * <p>Protocol: one KISS frame = one UDP datagram.
 * AP IP: 192.168.4.1 (fixed, softAPConfig). Port: 4210 (ESP32 listens there).
 * The ESP32 learns the Android client's port from the first inbound datagram.
 *
 * <p>KEY: {@code network.bindSocket()} forces the DatagramSocket onto the WiFi
 * interface so Android doesn't route through the mobile-data interface (which has
 * internet and is preferred by default).
 *
 * <p>Flow control: disabled — UDP has no backpressure. The FW-3a deadman (400 ms)
 * handles stuck-TX; windowing will be removed in FW-3b per Contrato A.
 */
public class WifiTransport {
    private static final String TAG = "WifiTransport";

    public static final String AP_SSID  = "MotoRFAR-HT";
    public static final String AP_IP    = "192.168.4.1";
    public static final int    UDP_PORT = 4210;
    private static final int   RECV_BUFFER_SIZE = 4096;

    // ── Listener ────────────────────────────────────────────────────────────

    public interface Listener {
        /** Called on receive thread with a fresh copy of the incoming bytes. */
        void onData(byte[] data);
        /** Called on receive or callback thread when the connection breaks. */
        void onError(Exception e);
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    private final Context context;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private volatile DatagramSocket socket;
    private volatile InetAddress    espAddress;
    private Thread receiveThread;
    private volatile boolean running = false;

    // Sends run here so callers (which may be on the main thread, e.g. an
    // emergency alert or beacon tick) never trip NetworkOnMainThreadException.
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    // ── FrameWriter (injected into Protocol.Sender) ─────────────────────────

    /**
     * Sends one fully-encoded KISS frame as a single UDP datagram.
     * Safe to call from any thread; drops frame silently if socket is closed.
     */
    public final Protocol.FrameWriter frameWriter = (frame, frameSize) -> {
        DatagramSocket s   = socket;
        InetAddress    addr = espAddress;
        if (s == null || addr == null || s.isClosed()) return;
        // Caller (Protocol.Sender.writeEncodedFrame) already hands us a fresh
        // copy per call, so it's safe to send it from this background thread.
        sendExecutor.execute(() -> {
            try {
                s.send(new DatagramPacket(frame, frameSize, addr, UDP_PORT));
            } catch (IOException e) {
                Log.w(TAG, "UDP send error", e);
            }
        });
    };

    // ── Constructor ──────────────────────────────────────────────────────────

    public WifiTransport(Context context) {
        this.context = context;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Requests the WiFi network from Android and, once available, opens the UDP
     * socket and starts receiving. May call {@code listener.onError()} from the
     * network callback thread or from the receive thread.
     *
     * <p>Requires {@code CHANGE_NETWORK_STATE} permission in the manifest.
     */
    public void connect(Listener listener) {
        connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "WiFi network available — binding UDP socket");
                try {
                    InetAddress addr    = InetAddress.getByName(AP_IP);
                    DatagramSocket sock = new DatagramSocket();   // any ephemeral port
                    network.bindSocket(sock);                     // ← forces WiFi interface
                    espAddress = addr;
                    socket     = sock;
                    startReceiving(listener);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to bind UDP socket to WiFi network", e);
                    listener.onError(e);
                }
            }

            @Override
            public void onLost(Network network) {
                Log.w(TAG, "WiFi network lost");
                close();
                listener.onError(new IOException("WiFi network lost"));
            }
        };

        connectivityManager.requestNetwork(request, networkCallback);
    }

    /** @return true if the socket is open and the ESP32 address is known. */
    public boolean isConnected() {
        DatagramSocket s = socket;
        return s != null && !s.isClosed() && espAddress != null;
    }

    /** Closes the socket and unregisters the network callback. */
    public void close() {
        running = false;
        espAddress = null;
        DatagramSocket s = socket;
        socket = null;
        sendExecutor.shutdownNow();
        if (s != null) s.close();   // unblocks socket.receive() in the receive thread

        ConnectivityManager.NetworkCallback cb = networkCallback;
        networkCallback = null;
        if (cb != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(cb);
            } catch (Exception ignored) {}
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void startReceiving(Listener listener) {
        running = true;
        receiveThread = new Thread(() -> {
            byte[] buf = new byte[RECV_BUFFER_SIZE];
            while (running) {
                DatagramSocket s = socket;
                if (s == null || s.isClosed()) break;
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    s.receive(pkt);
                    // 2026-07-06 (auditoria de seguridad, MEDIO-3): el ESP32 tiene IP fija
                    // por diseño del SoftAP (192.168.4.1). Descartar paquetes de cualquier
                    // otro origen cierra el vector de spoofing UDP desde otro dispositivo
                    // asociado al mismo WiFi (defensa en profundidad, barata y sin downside).
                    InetAddress trustedAddr = espAddress;
                    if (trustedAddr != null && !trustedAddr.equals(pkt.getAddress())) {
                        continue;
                    }
                    int len = pkt.getLength();
                    if (len > 0) {
                        byte[] data = new byte[len];
                        System.arraycopy(buf, 0, data, 0, len);
                        listener.onData(data);
                    }
                } catch (SocketException e) {
                    if (running) {
                        Log.w(TAG, "Socket closed while receiving", e);
                        listener.onError(e);
                    }
                    break;
                } catch (IOException e) {
                    if (running) {
                        Log.w(TAG, "UDP receive error", e);
                        listener.onError(e);
                    }
                    break;
                }
            }
            Log.d(TAG, "Receive thread exiting");
        }, "WifiTransport-recv");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
}
