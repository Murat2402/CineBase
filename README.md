# 🎬 Film Bilgi Sistemi (Java)

Bu proje, sinemaseverlerin film bilgilerine kolayca ulaşabilmesini ve bu bilgileri yönetebilmesini sağlamak amacıyla Java ile geliştirilmiş konsol tabanlı bir uygulamadır. OMDb API entegrasyonu kullanılarak gerçek zamanlı film verileri çekilmektedir.

## 🚀 Proje Özellikleri

*   **🌐 OMDb API Entegrasyonu:** Kullanıcının girdiği film adı parametre alınarak JSON formatında (Ad, Yıl, IMDb, Tür, Süre, Yönetmen) veriler çekilir ve `.txt` formatında saklanır.
*   **🔒 Kullanıcı Doğrulama Sistemi:** Programa yetkisiz erişimi engellemek için sabit bir kullanıcı adı ve şifre kontrol mekanizması mevcuttur.
*   **📂 Gelişmiş Dosya İşlemleri (File I/O):** Filmleri listeleme, güncelleme ve silme işlemleri doğrudan metin belgeleri üzerinden dinamik olarak yapılır.
*   **🔍 Detaylı Filtreleme:** Kaydedilen filmler; IMDb puanı (8.0 ve üzeri), Film Türü veya Yönetmen adına göre filtrelenebilir.
*   **⭐ Favori Sistemi:** Kullanıcılar istedikleri filmleri özel bir `favoriler.txt` dosyasına ekleyebilir ve ayrı olarak görüntüleyebilir.
*   **📝 Hata Loglama (Logging):** API bağlantı sorunları veya dosya okuma/yazma sırasında oluşan hatalar otomatik olarak `log.txt` dosyasına kaydedilerek hata yönetimi sağlanır.

## 🛠️ Kullanılan Teknolojiler
*   **Dil:** Java (Standart Kütüphaneler: `java.io`, `java.net`, `java.util.Scanner`)
*   **API:** [OMDb REST API](http://www.omdbapi.com/)
*   **Veri Formatı:** JSON Ayrıştırma & String Manipülasyonu (`indexOf`, `substring`, `split`)

## 💻 Kurulum ve Çalıştırma

1. Projeyi bilgisayarınıza indirin veya klonlayın.
2. `FilmBilgiSistemi.java` dosyasını kullandığınız IDE (Eclipse, IntelliJ, VS Code) üzerinden açın.
3. Projeyi derleyip çalıştırın.
4. **Giriş Bilgileri:**
   * **Kullanıcı Adı:** `murat`
   * **Şifre:** `1903`

---
*Bu proje, Süleyman Demirel Üniversitesi "Programlama 1" dersi kapsamında Murat Altuntop tarafından geliştirilmiştir.*# FilmBilgiSistemi
Programlama 1 Proje
