# Pantrify - Smart Pantry Management (FIFO & Expiry Notification)

Aplikasi mobile berbasis Android (Jetpack Compose) dengan arsitektur MVVM dan REST API untuk mengelola stok bahan makanan menggunakan metode **First In, First Out (FIFO)** guna mencegah *food waste*.

## Tech Stack
- **Frontend:** Kotlin, Jetpack Compose, Navigation Compose, Retrofit2, MVVM
- **Backend:** Node.js, Express.js, JWT Authentication
- **Database:** MySQL

## Skema Database (4 Entitas Utama)
1. `users` — Data pengguna, kredensial, dan role (`admin` / `user`)
2. `categories` — Master kategori bahan makanan
3. `products` — Master katalog produk
4. `inventory_batches` — Batch stok masuk, jumlah sisa, dan tanggal kedaluwarsa (Acuan FIFO)

## Log Progres Mingguan
- [x] **Minggu 1:** Inisialisasi repo `0053_Pantrify`, desain skema 4 tabel MySQL, kerangka backend Express.js, dan struktur MVVM Android.
- [ ] **Minggu 2:** Autentikasi Login & Register (JWT Access & Refresh Token).
- [ ] **Minggu 3:** Navigasi multi-halaman, Role-based UI, dan CRUDS Kategori.
- [ ] **Minggu 4:** CRUDS Master Produk & integrasi JSON Retrofit.
- [ ] **Minggu 5:** CRUDS Inventaris & implementasi algoritma FIFO otomatis.
- [ ] **Minggu 6:** Fitur Search dinamis & filter peringatan kedaluwarsa.
- [ ] **Minggu 7:** Fitur File Storage lokal (ekspor laporan) & validasi form.
- [ ] **Minggu 8:** Pengujian akhir, bug fixing, dan finalisasi dokumentasi.
