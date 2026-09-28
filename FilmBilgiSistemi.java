import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class FilmBilgiSistemi {
    static final String DOSYA_ADI = "filmler.txt";
    static final String FAVORI_DOSYA = "favoriler.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String dogruKullanici = "murat";
        String dogruSifre = "1903";

        int denemeHakki = 3;
        boolean girisBasarili = false;

        while (denemeHakki > 0) {
            System.out.print("Kullanıcı Adı: ");
            String girilenKullanici = scanner.nextLine();

            System.out.print("Şifre: ");
            String girilenSifre = scanner.nextLine();

            if (girilenKullanici.equals(dogruKullanici) && girilenSifre.equals(dogruSifre)) {
                System.out.println("Giriş başarılı! Hoş geldiniz, " + dogruKullanici + ".");
                girisBasarili = true;
                break;
            } else {
                denemeHakki--;
                System.out.println("Hatalı giriş. Kalan deneme hakkınız: " + denemeHakki);
            }
        }

        if (!girisBasarili) {
            System.out.println("Çok fazla hatalı giriş yapıldı. Program sonlandırılıyor.");
            System.exit(0);
        }
        while (true) {
            System.out.println("\n--- Film Bilgi Sistemi ---");
            System.out.println("1 - Veri Çek (API'den 30 film bilgisi çek ve kaydet)");
            System.out.println("2 - Listele");
            System.out.println("3 - Güncelle");
            System.out.println("4 - Sil");
            System.out.println("5 - Favoriye Ekle");
            System.out.println("6 - Favorileri Görüntüle");
            System.out.println("7 - Çıkış");
            System.out.print("Seçiminiz: ");

            String secim = scanner.nextLine();

            switch (secim) {
                case "1":
                    veriCekVeKaydet();
                    break;
                case "2":
                    listeleMenu(scanner);
                    break;
                case "3":
                    guncelle(scanner);
                    break;
                case "4":
                    sil(scanner);
                    break;
                case "5":
                    favoriyeEkle(scanner);
                    break;
                case "6":
                    favorileriListele();
                    break;
                case "7":
                    System.out.println("Programdan çıkılıyor...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Geçersiz seçim.");
            }
        }
    }
    // 1. Veri çekme fonksiyonu
    public static void veriCekVeKaydet() {
        String[] filmAdlari = {
                "The Shawshank Redemption", "The Godfather", "The Dark Knight",
                "Pulp Fiction", "Schindler's List", "12 Angry Men",
                "Inception", "Fight Club", "Forrest Gump", "The Matrix",
                "The Lord of the Rings: The Return of the King", "Interstellar",
                "Gladiator", "The Green Mile", "The Intouchables",
                "Saving Private Ryan", "The Prestige", "Memento",
                "The Truman Show", "Whiplash", "The Departed",
                "Back to the Future", "The Silence of the Lambs", "Parasite", "Joker",
                "Toy Story", "Braveheart", "Coco", "The Social Network", "The Good, the Bad and the Ugly"
        };

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DOSYA_ADI, false))) {
            for (String filmAdi : filmAdlari) {
                String veri = apiDenFilmBilgisiGetir(filmAdi);
                if (veri != null && !veri.isEmpty()) {
                    writer.write(veri);
                    writer.newLine();
                }
            }
            System.out.println("Veriler başarılı şekilde " + DOSYA_ADI + " dosyasına kaydedildi.");
        } catch (Exception e) {
            System.out.println("Hata: " + e.getMessage());
            logHata("Hata: " + e.getMessage());
    }
    }

    // API çağrısı ve düzenleme 
    public static String apiDenFilmBilgisiGetir(String filmAdi) {
        try {
            String apiKey = "c62f3707"; // Kendi API key'in
            String query = URLEncoder.encode(filmAdi, "UTF-8");
            String apiURL = "http://www.omdbapi.com/?t=" + query + "&apikey=" + apiKey;

            URI uri = URI.create(apiURL);
            URL url = uri.toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String satir;
            while ((satir = br.readLine()) != null) {
                sb.append(satir);
            }
            br.close();

            String veri = sb.toString();
            if (veri.contains("\"Response\":\"False\"")) {
                System.out.println(filmAdi + " filmi bulunamadı.");
                return null;
            }

            // Parça Çekme
            String baslik = parcaGetir(veri, "\"Title\":\"");
            String yil = parcaGetir(veri, "\"Year\":\"");
            String imdbPuani = parcaGetir(veri, "\"imdbRating\":\"");
            String tur = parcaGetir(veri, "\"Genre\":\"");
            String sure = parcaGetir(veri, "\"Runtime\":\"");
            String yonetmen = parcaGetir(veri, "\"Director\":\"");

           
            //FilmAdı;Yıl;imdbRating;Tür;Süre;Yönetmen
            return baslik + ";" + yil + ";" + imdbPuani + ";" + tur + ";" + sure + ";" + yonetmen;

        } catch (Exception e) {
            System.out.println("API çağrısında hata: " + e.getMessage());
            logHata("API çağrısında hata: " + e.getMessage());
            return null;
        }
    }

    // String methodları ile JSON veri alma
    public static String parcaGetir(String veri, String key) {
        int index = veri.indexOf(key);
        if (index == -1) return "";
        int basla = index + key.length();
        int bitis = veri.indexOf("\"", basla);
        if (bitis == -1) return "";
        return veri.substring(basla, bitis);
    }

    public static void listeleMenu(Scanner scanner) {
    System.out.println("Listeleme seçenekleri:");
    System.out.println("a) 8 IMDb ve üzeri filmleri listele");
    System.out.println("b) Türüne göre listele (örn:Drama,Comedy,Western,War,Action,Sci-Fi,Thriller...)");
    System.out.println("c) Yönetmene göre listele");
    System.out.println("d) Hepsini listele");
    System.out.print("Seçiminiz: ");
    String secim = scanner.nextLine();

    try {
        switch (secim) {
            case "a":
                try (BufferedReader reader = new BufferedReader(new FileReader(DOSYA_ADI))) {
                    System.out.println("IMDb puanı 8.0 ve üzeri filmler:");
                    String satir;
                    while ((satir = reader.readLine()) != null) {
                        String[] parcalar = satir.split(";");
                        if (parcalar.length >= 3) {
                            try {
                                double imdb = Double.parseDouble(parcalar[2]);
                                if (imdb >= 8.0) {
                                    System.out.println(düzenle(parcalar));
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
                break;
                
            case "b":
                System.out.print("Tür giriniz: ");
                String tur = scanner.nextLine().toLowerCase();
                try (BufferedReader reader = new BufferedReader(new FileReader(DOSYA_ADI))) {
                    System.out.println(tur + " türündeki filmler:");
                    String satir;
                    while ((satir = reader.readLine()) != null) {
                        String[] parcalar = satir.split(";");
                        if (parcalar.length >= 4) {
                            if (parcalar[3].toLowerCase().contains(tur)) {
                                System.out.println(düzenle(parcalar));
                            }
                        }
                    }
                }
                break;

            case "c":
                System.out.print("Yönetmen adı giriniz: ");
                String yonetmen = scanner.nextLine().toLowerCase().trim();
                try (BufferedReader reader = new BufferedReader(new FileReader(DOSYA_ADI))) {
                    System.out.println(yonetmen + " yönetmenliğindeki filmler:");
                    String satir;
                    while ((satir = reader.readLine()) != null) {
                        String[] parcalar = satir.split(";");
                        if (parcalar.length >= 6) {
                            String dosyadakiYonetmen = parcalar[5].toLowerCase().trim();
                            if (dosyadakiYonetmen.contains(yonetmen)) {
                                System.out.println(düzenle(parcalar));
                            }
                        }
                    }
                }
                break;

            case "d":
                try (BufferedReader reader = new BufferedReader(new FileReader(DOSYA_ADI))) {
                    System.out.println("Tüm filmler:");
                    String satir;
                    while ((satir = reader.readLine()) != null) {
                        String[] parcalar = satir.split(";");
                        System.out.println(düzenle(parcalar));
                    }
                }
                break;
            default:
                System.out.println("Geçersiz seçim.");
        }
    } catch (IOException e) {
        System.out.println("Dosya okunurken hata: " + e.getMessage());
        logHata("Dosya okunurken hata: " + e.getMessage());

    }
}

    // Film bilgilerini istenilen düzene getirme fonksiyonu
    public static String düzenle(String[] parcalar) {
        // FilmAdı;Yıl;imdbRating;Tür;Süre;Yönetmen
        return String.format("Film: %s | Yıl: %s | IMDb: %s | Tür: %s | Süre: %s | Yönetmen: %s",
                parcalar.length > 0 ? parcalar[0] : "",
                parcalar.length > 1 ? parcalar[1] : "",
                parcalar.length > 2 ? parcalar[2] : "",
                parcalar.length > 3 ? parcalar[3] : "",
                parcalar.length > 4 ? parcalar[4] : "",
                parcalar.length > 5 ? parcalar[5] : "");
    }

    // 3. Güncelleme fonksiyonu
    public static void guncelle(Scanner scanner) {
    System.out.print("Güncellemek istediğiniz film adını tam yazınız: ");
    String arananFilm = scanner.nextLine().toLowerCase();

    try {
        File tempFile = new File("temp_" + DOSYA_ADI);
        File originalFile = new File(DOSYA_ADI);
        BufferedReader reader = new BufferedReader(new FileReader(originalFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String satir;
        boolean bulundu = false;

        while ((satir = reader.readLine()) != null) {
            String[] parcalar = satir.split(";");
            if (parcalar.length > 0 && parcalar[0].toLowerCase().equals(arananFilm)) {
                System.out.println("Bulunan film: " + düzenle(parcalar));
                System.out.print("Bu filmi güncellemek istiyor musunuz? (e/h): ");
                String cevap = scanner.nextLine().toLowerCase();
                if (cevap.equals("e")) {
                
                    System.out.print("Yeni film adı (boş bırakılırsa değişmez): ");
                    String yeniFilmAdi = scanner.nextLine();
                    if (!yeniFilmAdi.isBlank()) parcalar[0] = yeniFilmAdi;

                    System.out.print("Yeni yıl (boş bırakılırsa değişmez): ");
                    String yeniYil = scanner.nextLine();
                    if (!yeniYil.isBlank()) parcalar[1] = yeniYil;

                    System.out.print("Yeni IMDb puanı (0-10, boş bırakılırsa değişmez): ");
                    String yeniImdb = scanner.nextLine();
                    if (!yeniImdb.isBlank()) {
                        try {
                            double imdbDeger = Double.parseDouble(yeniImdb);
                            if (imdbDeger >= 0 && imdbDeger <= 10) {
                                parcalar[2] = yeniImdb;
                            } else {
                                System.out.println("IMDb puanı 0 ile 10 arasında olmalı, güncellenmedi.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Geçersiz IMDb puanı, güncellenmedi.");
                            logHata("Geçersiz IMDb puanı, güncellenmedi.");
                        }
                    }

                    System.out.print("Yeni tür (boş bırakılırsa değişmez): ");
                    String yeniTur = scanner.nextLine();
                    if (!yeniTur.isBlank()) parcalar[3] = yeniTur;

                    System.out.print("Yeni süre (boş bırakılırsa değişmez): ");
                    String yeniSure = scanner.nextLine();
                    if (!yeniSure.isBlank()) parcalar[4] = yeniSure;

                    System.out.print("Yeni yönetmen (boş bırakılırsa değişmez): ");
                    String yeniYonetmen = scanner.nextLine();
                    if (!yeniYonetmen.isBlank()) parcalar[5] = yeniYonetmen;

                    bulundu = true;
                    System.out.println("Film bilgileri güncellendi: " + düzenle(parcalar));
                }
                writer.write(String.join(";", parcalar));
                writer.newLine();
            } else {
                writer.write(satir);
                writer.newLine();
            }
        }
        reader.close();
        writer.close();

        if (bulundu) {
            if (originalFile.delete()) {
                tempFile.renameTo(originalFile);
            } else {
                System.out.println("Eski dosya silinemedi, güncelleme tamamlanamadı.");
            }
        } else {
            tempFile.delete();
            System.out.println("Aranan film bulunamadı.");
        }
    } catch (IOException e) {
        System.out.println("Güncelleme sırasında hata: " + e.getMessage());
        logHata("Güncelleme sırasında hata: " + e.getMessage());
        
    }
}
    // 4. Silme fonksiyonu
    public static void sil(Scanner scanner) {
        System.out.print("Silmek istediğiniz film adını tam yazınız: ");
        String arananFilm = scanner.nextLine().toLowerCase();

        try {
            File tempFile = new File("temp_" + DOSYA_ADI);
            File originalFile = new File(DOSYA_ADI);
            BufferedReader reader = new BufferedReader(new FileReader(originalFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String satir;
            boolean bulundu = false;

            while ((satir = reader.readLine()) != null) {
                String[] parcalar = satir.split(";");
                if (parcalar.length > 0 && parcalar[0].toLowerCase().equals(arananFilm)) {
                    System.out.println("Silinen film: " + düzenle(parcalar));
                    bulundu = true;
                    continue;
                }
                writer.write(satir);
                writer.newLine();
            }
            reader.close();
            writer.close();

            if (bulundu) {
                if (!originalFile.delete() || !tempFile.renameTo(originalFile)) {
                    System.out.println("Dosya güncellenemedi.");
                } else {
                    System.out.println("Film başarıyla silindi.");
                }
            } else {
                System.out.println("Film bulunamadı.");
                tempFile.delete(); // geçici dosyayı sil
            }

        } catch (IOException e) {
            System.out.println("Hata: " + e.getMessage());
            logHata("Hata: " + e.getMessage());
        }
    }
    //5.Favoriye Ekleme Fonksiyonu
    public static void favoriyeEkle(Scanner scanner) {
        System.out.print("Favorilere eklemek istediğiniz film adını tam yazınız: ");
        String arananFilm = scanner.nextLine().toLowerCase();

        try (BufferedReader reader = new BufferedReader(new FileReader(DOSYA_ADI));
             BufferedWriter writer = new BufferedWriter(new FileWriter(FAVORI_DOSYA, true))) { // append modda aç

            String satir;
            boolean bulundu = false;
            while ((satir = reader.readLine()) != null) {
                String[] parcalar = satir.split(";");
                if (parcalar.length > 0 && parcalar[0].toLowerCase().equals(arananFilm)) {
                    writer.write(satir);
                    writer.newLine();
                    bulundu = true;
                    System.out.println("Film favorilere eklendi: " + düzenle(parcalar));
                    break;
                }
            }

            if (!bulundu) {
                System.out.println("Film bulunamadı.");
            }

        } catch (IOException e) {
            System.out.println("Hata: " + e.getMessage());
            logHata("Hata: " + e.getMessage());
        }
    }

    //6.Favorileri Görüntüleme Fonksiyonu
    public static void favorileriListele() {
    System.out.println("--- Favori Filmler ---");
    try (BufferedReader reader = new BufferedReader(new FileReader(FAVORI_DOSYA))) {
        String satir;
        boolean bosMu = true;
        while ((satir = reader.readLine()) != null) {
            bosMu = false;
            String[] parcalar = satir.split(";");
            System.out.println(düzenle(parcalar));
        }
        if (bosMu) {
            System.out.println("Favori listesi boş.");
        }
    } catch (IOException e) {
        System.out.println("Favori dosyası okunurken hata oluştu: " + e.getMessage());
        logHata("Favori dosyası oluşurken hata oluştu: " + e.getMessage());
        }
    }

    //Hata Loglama Fonksiyonu
    public static void logHata(String mesaj) {
    System.out.println(mesaj);
    try (FileWriter log = new FileWriter("log.txt", true)) {
        log.write(mesaj + "\n");
    } catch (IOException e) {
        System.out.println("Log dosyasına yazılamadı: " + e.getMessage());
    }
}
}