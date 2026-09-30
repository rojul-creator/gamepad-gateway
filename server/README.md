# AetherPad PC Companion Receiver

Panduan instalasi dan menjalankan daemon receiver PC untuk menerima input kontrol nirkabel dari Android.

## 1. Persyaratan Sistem
- Windows 10/11 atau Linux (Ubuntu 22.04+)
- Python 3.8 - 3.12
- Driver **ViGEmBus** (Virtual Gamepad Emulation Bus) untuk Windows.

## 2. Instalasi Driver ViGEmBus (Khusus Windows)
1. Unduh installer ViGEmBus dari GitHub resmi:
   https://github.com/nefarius/ViGEmBus/releases
2. Jalankan `ViGEmBusSetup_x64.msi` dan ikuti panduan instalasi sampai selesai.
3. Restart PC jika diminta.

## 3. Instalasi Dependencies Python
Buka terminal/PowerShell di folder `server/` dan jalankan:
```bash
pip install -r requirements.txt
```

## 4. Menjalankan Receiver Daemon
```bash
python pc_receiver.py
```
Output sukses akan menampilkan:
```
[✓] Virtual Xbox 360 controller initialized successfully.
==================================================
  AETHERPAD HUD // PC CONTROLLER RECEIVER DAEMON
  Listening on UDP 0.0.0.0:47800
==================================================
```

## 5. Menghubungkan Gamepad Android
1. Pastikan PC dan HP Android terhubung ke jaringan Wi-Fi yang sama (disarankan 5 GHz untuk latency < 3ms).
2. Cari tahu IP lokal PC Anda (`ipconfig` di CMD Windows -> cari IPv4, misal: `192.168.1.3`).
3. Buka aplikasi **AetherPad HUD** di Android, masukkan IP tersebut dan port `47800`.
4. Tekan tombol **START RELAY**.
5. Buka game favorit Anda (Steam, Forza Horizon, Genshin Impact, RPCS3, Yuzu, dsb). Game akan langsung mendeteksi "Xbox 360 Controller"!

## 6. Troubleshooting
- **Tidak ada paket masuk**: Pastikan Windows Defender Firewall mengizinkan port UDP 47800:
  `netsh advfirewall firewall add rule name="AetherPad UDP" dir=in action=allow protocol=UDP localport=47800`
- **Wi-Fi Lag**: Gunakan pita Wi-Fi 5 GHz atau Wi-Fi Direct. Jangan gunakan mode hemat daya di router.
