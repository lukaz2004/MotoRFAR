#!/usr/bin/env python3
"""
MotoRFAR HT - FW-3a smoke test (transporte kv4p sobre WiFi/UDP).

Que valida:
  1) Unis la PC a la WiFi del ESP32 (SoftAP 'MotoRFAR-HT')  -> MANUAL, antes de correr.
  2) Este script manda un datagrama de disparo a 192.168.4.1:4210.
  3) El firmware, al ver un cliente nuevo, responde con Hello (KISS vendor frame).
  4) Decodifica los datagramas entrantes (Hello / DeviceState / debug / RX audio).

Modos:
  (sin args)   Test de TRANSPORTE puro. NO necesita el radio SA818 conectado.
  --open-rx    Manda HostDesiredState (RADIO_CONFIG_VALID | RX_AUDIO_OPEN) para que
               el firmware configure el radio y mande RX audio. Requiere SA818.

Solo stdlib. Si no tenes otro Python:
  & 'C:\\Users\\lukaz\\pio-venv\\Scripts\\python.exe' fw3a_smoke.py
"""
import socket
import struct
import sys
import time

AP_IP    = "192.168.4.1"
UDP_PORT = 4210

FEND, FESC, TFEND, TFESC = 0xC0, 0xDB, 0xDC, 0xDD
KISS_DATA, KISS_SETHARDWARE = 0x00, 0x06
KV4P_PREFIX = b"KV4P"
KV4P_VER = 0x01

CMD_HOST_DESIRED_STATE = 0x0D
SND = {
    0x01: "DEBUG_INFO", 0x02: "DEBUG_ERROR", 0x03: "DEBUG_WARN",
    0x04: "DEBUG_DEBUG", 0x05: "DEBUG_TRACE", 0x06: "HELLO",
    0x07: "RX_AUDIO", 0x09: "WINDOW_UPDATE", 0x0B: "DEVICE_STATE",
}

RADIO_CONFIG_VALID = 1 << 0
RX_AUDIO_OPEN      = 1 << 2


def kiss_escape(data: bytes) -> bytes:
    out = bytearray()
    for b in data:
        if b == FEND:
            out += bytes([FESC, TFEND])
        elif b == FESC:
            out += bytes([FESC, TFESC])
        else:
            out.append(b)
    return bytes(out)


def kiss_unescape(data: bytes) -> bytes:
    out = bytearray()
    esc = False
    for b in data:
        if esc:
            out.append(FEND if b == TFEND else FESC if b == TFESC else b)
            esc = False
        elif b == FESC:
            esc = True
        else:
            out.append(b)
    return bytes(out)


def build_vendor_frame(kv4p_cmd: int, payload: bytes) -> bytes:
    body = KV4P_PREFIX + bytes([KV4P_VER, kv4p_cmd]) + payload
    return bytes([FEND, KISS_SETHARDWARE]) + kiss_escape(body) + bytes([FEND])


def parse_frames(buf: bytes):
    frames = []
    for p in buf.split(bytes([FEND])):
        if not p:
            continue
        raw = kiss_unescape(p)
        if len(raw) < 1:
            continue
        kiss_cmd = raw[0] & 0x0F
        payload = raw[1:]
        if kiss_cmd == KISS_SETHARDWARE and payload[:4] == KV4P_PREFIX and len(payload) >= 6:
            frames.append((kiss_cmd, payload[5], payload[6:]))
        else:
            frames.append((kiss_cmd, None, payload))
    return frames


def decode_hello(p: bytes) -> str:
    if len(p) < 17:
        return f"(Hello corto, {len(p)} bytes)"
    ver, rms, win, rf, fmin, fmax, feat = struct.unpack_from("<HBIBffB", p, 0)
    radio = "OK" if chr(rms) == "f" else "NO"
    band = "UHF" if rf else "VHF"
    return (f"HELLO ver={ver} radio={radio} win={win} rf={band} "
            f"{fmin:.1f}-{fmax:.1f}MHz feat=0x{feat:02x}")


def main():
    open_rx = "--open-rx" in sys.argv
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.settimeout(2.0)
    s.bind(("0.0.0.0", 0))

    if open_rx:
        hds = struct.pack("<IiHBfffBBB", 1, -1,
                          RADIO_CONFIG_VALID | RX_AUDIO_OPEN, 0,
                          139.970, 145.000, 0, 0, 0)
        s.sendto(build_vendor_frame(CMD_HOST_DESIRED_STATE, hds), (AP_IP, UDP_PORT))
        print("[>] HostDesiredState enviado (RX audio abierto, freq_rx=145.000, requiere SA818)")
    else:
        s.sendto(b"\x00", (AP_IP, UDP_PORT))
        print("[>] datagrama de disparo enviado (test de transporte, sin radio)")

    print(f"[i] escuchando {AP_IP}:{UDP_PORT} ... (Ctrl-C para salir)")
    audio_count = 0
    got_hello = False
    t0 = time.time()
    try:
        while True:
            try:
                data, _ = s.recvfrom(4096)
            except socket.timeout:
                if not got_hello and audio_count == 0 and time.time() - t0 > 6:
                    print("[!] sin respuesta. Unido a la WiFi MotoRFAR-HT? Flasheaste FW-3a? IP 192.168.4.1?")
                    t0 = time.time()
                continue
            for kiss_cmd, kv4p_cmd, payload in parse_frames(data):
                if kv4p_cmd is None:
                    print(f"[<] KISS DATA (AX.25) {len(payload)} bytes")
                elif kv4p_cmd == 0x07:
                    audio_count += 1
                    if audio_count % 25 == 1:
                        print(f"[<] RX_AUDIO x{audio_count} (ultimo {len(payload)} bytes)")
                elif kv4p_cmd == 0x06:
                    got_hello = True
                    print(f"[<] {decode_hello(payload)}")
                    print("    >>> TRANSPORTE WiFi OK: el firmware emite frames kv4p por UDP.")
                else:
                    name = SND.get(kv4p_cmd, f"0x{kv4p_cmd:02x}")
                    extra = ""
                    if kv4p_cmd in (0x01, 0x02, 0x03, 0x04, 0x05):
                        extra = " :: " + payload.decode(errors="replace").strip()
                    print(f"[<] {name}{extra}")
    except KeyboardInterrupt:
        print(f"\n[i] fin. Hello={'si' if got_hello else 'NO'} | RX_AUDIO total={audio_count}")


if __name__ == "__main__":
    main()
