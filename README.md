# 🎬 CineBase - Film Bilgi Sistemi

**CineBase**, OMDb API entegrasyonu ile gerçek zamanlı film verilerini çeken, hem güçlü bir **Java Konsol Uygulaması** hem de modern bir **Web Arayüzü (Premium UI)** barındıran kapsamlı bir sinema arşivi projesidir.

🌐 **Canlı Demo:** [https://cinebase-3snq.onrender.com](https://cinebase-3snq.onrender.com)  
*(Not: Ücretsiz sunucu kullanıldığı için ilk açılışta uyanması 40-50 saniye sürebilir, sonrasında oldukça hızlıdır.)*

---

## 🚀 Proje Özellikleri

### 💻 Web Arayüzü (Premium Dashboard)
- **Modern Tasarım:** Glassmorphism (buzlu cam) efektleri, karanlık mod (dark mode) ve akıcı animasyonlar.
- **Dinamik İstatistikler:** Kütüphanedeki toplam film sayısı, favori sayısı gibi istatistiklerin canlı takibi.
- **Gelişmiş Sıralama ve Filtreleme:** 
  - Yıla, süreye, IMDb puanına ve isme göre sıralama.
  - Sadece posteri olanları, sadece favorileri veya belirli on yılları (2010'lar vb.) filtreleme yeteneği.
- **Tam Kontrol:** Arayüz üzerinden favoriye ekleme, film detaylarını güncelleme ve silme (Veriler arka planda Java ile txt dosyalarına kaydedilir).

### ⌨️ Terminal (Konsol) Uygulaması
- **Dosya İşlemleri (File I/O):** Veriler `filmler.txt` ve `favoriler.txt` içinde dinamik olarak yönetilir.
- **Güvenli Giriş:** Programa yetkisiz erişimi engellemek için kullanıcı doğrulama sistemi.
- **Hata Loglama:** API sorunları veya dosya hataları otomatik olarak `log.txt` dosyasına raporlanır.
- **String Manipülasyonu:** Dış kütüphane kullanılmadan API'den gelen JSON formatı saf Java yetenekleriyle ayrıştırılır.

---

## 🛠️ Kullanılan Teknolojiler

- **Arka Uç (Backend):** Java, JDK yerleşik HTTP Sunucusu (`com.sun.net.httpserver`)
- **Ön Uç (Frontend):** HTML5, CSS3 (Glassmorphism), Vanilla JavaScript, Bootstrap Icons
- **Veri Sağlayıcı:** [OMDb REST API](http://www.omdbapi.com/)
- **Bulut & Dağıtım:** Docker, Render Platformu

---

## ⚙️ Kurulum ve Çalıştırma

Projeyi kendi bilgisayarınızda çalıştırmak için:

1. Depoyu klonlayın:
```bash
git clone https://github.com/Murat2402/FilmBilgiSistemi.git
```
2. Proje dizinine gidin ve Java dosyalarını derleyin:
```bash
javac -encoding UTF-8 FilmBilgiSistemi.java FilmServer.java
```
3. Sunucuyu başlatın:
```bash
java -Dfile.encoding=UTF-8 FilmServer
```
4. Tarayıcınızda [http://localhost:8080](http://localhost:8080) adresine gidin.

**Sistem Giriş Bilgileri:**
- **Kullanıcı Adı:** `murat`
- **Şifre:** `1903`

---
*Bu proje, Süleyman Demirel Üniversitesi "Programlama 1" dersi kapsamında Murat Altuntop tarafından geliştirilmiş ve sonrasında gelişmiş web arayüzü ile modernleştirilmiştir.*
