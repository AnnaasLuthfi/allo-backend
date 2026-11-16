# Split Bill API

REST API sederhana menggunakan Spring Boot untuk mencatat pengeluaran bersama (split bill) dan menghitung penyelesaian hutang antar anggota kelompok.

## Fitur

- **Kelola Grup**: Buat grup dan daftar anggota.
- **Pencatatan Pengeluaran (Expense)**:
  - Pembagian rata (`EQUAL`) dengan penanganan pembulatan sen.
  - Pembagian nominal spesifik (`EXACT`).
  - Pembagian persentase (`PERCENTAGE`).
  - Kategori pengeluaran (`FOOD`, `TRANSPORTATION`, `ACCOMMODATION`, `ENTERTAINMENT`, `SHOPPING`, `OTHER`).
- **Pencatatan Pembayaran**: Catat pembayaran/transfer antar anggota untuk melunasi hutang.
- **Kalkulasi Settlement**: Menghitung net balance dan menyederhanakan transaksi pelunasan agar jumlah transfer seminimal mungkin.
- **Service Charge Dinamis**: Perhitungan persentase biaya layanan berdasarkan username GitHub (modulo 10 dari total nilai karakter).

## Cara Menjalankan

### Prasyarat
- Java 17+
- Maven (bisa gunakan wrapper `mvnw.cmd` / `./mvnw`)

### Jalankan Aplikasi
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Aplikasi aktif di `http://localhost:4110`.

H2 Web Console tersedia di `http://localhost:4110/h2-console`:
- **JDBC URL**: `jdbc:h2:file:./data/splitbill`
- **Username**: `sa`
- **Password**: *(kosong)*

### Jalankan dengan Docker
```bash
docker build -t split-bill-api .
docker run -p 4110:4110 split-bill-api
```

## Daftar Endpoint

| Method | Endpoint | Deskripsi |
|---|---|---|
| POST | `/api/groups` | Buat grup baru |
| GET | `/api/groups` | Ambil semua grup |
| GET | `/api/groups/{id}` | Ambil detail grup |
| POST | `/api/groups/{id}/expenses` | Tambah pengeluaran baru |
| GET | `/api/groups/{id}/expenses` | Ambil riwayat pengeluaran grup |
| POST | `/api/groups/{id}/payments` | Catat pembayaran langsung antar anggota |
| GET | `/api/groups/{id}/payments` | Ambil riwayat pembayaran grup |
| GET | `/api/groups/{id}/settlement` | Hitung settlement & ringkasan saldo |

## Contoh Request

### 1. Buat Grup
```bash
curl -X POST http://localhost:4110/api/groups \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Trip Bali",
    "participants": ["Alice", "Bob", "Charlie"]
  }'
```

### 2. Tambah Expense (Equal)
```bash
curl -X POST http://localhost:4110/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Villa",
    "amount": 300000.00,
    "paidBy": "Alice",
    "splitType": "EQUAL",
    "category": "ACCOMMODATION",
    "participants": ["Alice", "Bob", "Charlie"]
  }'
```

### 3. Tambah Expense (Exact)
```bash
curl -X POST http://localhost:4110/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Makan Malam",
    "amount": 60000.00,
    "paidBy": "Bob",
    "splitType": "EXACT",
    "category": "FOOD",
    "customShares": [
      { "participantName": "Bob", "amount": 25000.00 },
      { "participantName": "Charlie", "amount": 35000.00 }
    ]
  }'
```

### 4. Catat Pembayaran
```bash
curl -X POST http://localhost:4110/api/groups/1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "paidBy": "Charlie",
    "paidTo": "Alice",
    "amount": 50000.00,
    "notes": "Transfer via Allo Bank"
  }'
```

### 5. Cek Settlement
```bash
curl -X GET http://localhost:4110/api/groups/1/settlement
```

Untuk override username GitHub saat testing:
```bash
curl -X GET "http://localhost:4110/api/groups/1/settlement?githubUsername=allobankcandidate"
```

## Postman Collection
File collection tersedia di root repository: `Split_Bill_API.postman_collection.json`.

## Desain & Trade-off

**"What was the hardest design decision you made while building this, and what trade-off did you accept?"**

Bagian yang paling butuh pertimbangan adalah penyederhanaan transaksi settlement (meminimalkan jumlah transfer antar anggota). Menggunakan algoritma greedy berdasarkan net balance memungkinkan transaksi ditekan seminimal mungkin (maksimal N-1 transaksi). Trade-off-nya adalah seorang anggota bisa saja diminta membayar ke orang yang tidak langsung bertransaksi dengannya. Untuk menjaga transparansi, sistem tetap menyimpan catatan detail per expense dan history payment sehingga semua perhitungan tetap bisa dilacak asal-usulnya.
