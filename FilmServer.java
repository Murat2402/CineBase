import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Film Bilgi Sistemi - Web arayüzü için sunucu.
 *
 * Terminal uygulamasıyla (FilmBilgiSistemi) AYNI veri dosyalarını kullanır:
 *   filmler.txt   ->  FilmAdı;Yıl;imdbRating;Tür;Süre;Yönetmen[;PosterURL]
 *   favoriler.txt ->  aynı format
 * (7. alan olan poster isteğe bağlıdır; terminal uygulaması onu görmezden gelir.)
 *
 * Çalıştırma:   javac -encoding UTF-8 *.java   sonra   java FilmServer   ->  http://localhost:8080
 */
public class FilmServer {

    static final Path FILMLER = Paths.get(FilmBilgiSistemi.DOSYA_ADI);
    static final Path FAVORILER = Paths.get(FilmBilgiSistemi.FAVORI_DOSYA);
    static final Path WEB = Paths.get("web").toAbsolutePath().normalize();

    static final String KULLANICI = "murat";
    static final String SIFRE = "1903";
    static final String API_KEY = "c62f3707";
    static final int ALAN_SAYISI = 7; // ad, yıl, imdb, tür, süre, yönetmen, poster

    static final String[] FILM_ADLARI = {
            "The Shawshank Redemption", "The Godfather", "The Dark Knight",
            "Pulp Fiction", "Schindler's List", "12 Angry Men",
            "Inception", "Fight Club", "Forrest Gump", "The Matrix",
            "The Lord of the Rings: The Return of the King", "Interstellar",
            "Gladiator", "The Green Mile", "The Intouchables",
            "Saving Private Ryan", "The Prestige", "Memento",
            "The Truman Show", "Whiplash", "The Departed",
            "Back to the Future", "The Silence of the Lambs", "Parasite", "Joker",
            "Toy Story", "Braveheart", "Coco", "The Social Network", "The Good, the Bad and the Ugly",
            "Se7en", "The Lion King", "The Avengers", "Avengers: Endgame", 
            "Avengers: Infinity War", "Spider-Man: No Way Home", "WALL-E", 
            "Up", "Django Unchained", "The Shining", "Alien", "Aliens", 
            "Jurassic Park", "Terminator 2: Judgment Day", "Die Hard", 
            "Indiana Jones and the Raiders of the Lost Ark", "Star Wars: Episode IV - A New Hope",
            "Star Wars: Episode V - The Empire Strikes Back", "The Pianist", 
            "The Wolf of Wall Street", "Catch Me If You Can", "Shutter Island", 
            "A Beautiful Mind", "Good Will Hunting", "Dead Poets Society", 
            "The Sixth Sense", "Mad Max: Fury Road", "Blade Runner 2049", 
            "Dune", "Avatar", "Titanic", "Oppenheimer", "Barbie", "Spider-Man: Into the Spider-Verse",
            "Your Name.", "Spirited Away", "Princess Mononoke", "Howl's Moving Castle",
            "The Batman", "Logan", "Deadpool", "Guardians of the Galaxy",
            "Black Panther", "Thor: Ragnarok", "Iron Man", "Captain America: The Winter Soldier",
            "John Wick", "Mission: Impossible - Fallout", "Top Gun: Maverick"
    };

    static final Object KILIT = new Object();                       // dosya erişimi
    static final Set<String> TOKENLAR = ConcurrentHashMap.newKeySet();
    static final AtomicInteger hataliGiris = new AtomicInteger(0);
    static volatile long kilitBitis = 0;

    static final AtomicBoolean cekiliyor = new AtomicBoolean(false);
    static final AtomicInteger cekilen = new AtomicInteger(0);
    static final AtomicInteger toplam = new AtomicInteger(0);
    static volatile String sonMesaj = "";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (System.getenv("PORT") != null) {
            port = Integer.parseInt(System.getenv("PORT"));
        } else if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        
        // InetSocketAddress(port) varsayılan olarak tüm arayüzleri (0.0.0.0) dinler. Bulut için bu gereklidir.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", FilmServer::isle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Film Bilgi Sistemi web arayüzü çalışıyor. Port: " + port);
    }

    // ------------------------------------------------------------------ yönlendirme

    static void isle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                api(ex, path, ex.getRequestMethod());
            } else {
                statik(ex, path, ex.getRequestMethod());
            }
        } catch (Exception e) {
            FilmBilgiSistemi.logHata("Sunucu hatası: " + e);
            hata(ex, 500, "Sunucu hatası oluştu.");
        } finally {
            ex.close();
        }
    }

    static void api(HttpExchange ex, String path, String metot) throws IOException {
        if (path.equals("/api/login") && metot.equals("POST")) {
            giris(ex);
            return;
        }
        String token = ex.getRequestHeaders().getFirst("X-Token");
        if (token == null || !TOKENLAR.contains(token)) {
            hata(ex, 401, "Giriş yapmanız gerekiyor.");
            return;
        }

        if (metot.equals("GET")) {
            switch (path) {
                case "/api/films": filmleriGonder(ex); return;
                case "/api/fetch/status": durumGonder(ex); return;
                default: hata(ex, 404, "Bulunamadı."); return;
            }
        }
        if (metot.equals("POST")) {
            Map<String, String> p = form(ex);
            switch (path) {
                case "/api/logout": TOKENLAR.remove(token); json(ex, 200, "{\"ok\":true}"); return;
                case "/api/fetch": veriCek(ex); return;
                case "/api/films/update": guncelle(ex, p); return;
                case "/api/films/delete": sil(ex, p); return;
                case "/api/favorites/add": favoriEkle(ex, p); return;
                case "/api/favorites/remove": favoriCikar(ex, p); return;
                default: hata(ex, 404, "Bulunamadı."); return;
            }
        }
        hata(ex, 405, "Desteklenmeyen istek.");
    }

    // ------------------------------------------------------------------ uç noktalar

    static void giris(HttpExchange ex) throws IOException {
        long kalan = kilitBitis - System.currentTimeMillis();
        if (kalan > 0) {
            hata(ex, 429, "Çok fazla hatalı giriş. " + (kalan / 1000 + 1) + " sn sonra tekrar deneyin.");
            return;
        }
        Map<String, String> p = form(ex);
        if (KULLANICI.equals(p.get("user")) && SIFRE.equals(p.get("pass"))) {
            hataliGiris.set(0);
            String token = UUID.randomUUID().toString();
            TOKENLAR.add(token);
            json(ex, 200, "{\"token\":" + q(token) + ",\"user\":" + q(KULLANICI) + "}");
        } else if (hataliGiris.incrementAndGet() >= 3) {
            hataliGiris.set(0);
            kilitBitis = System.currentTimeMillis() + 30_000;
            hata(ex, 429, "Çok fazla hatalı giriş. 30 sn sonra tekrar deneyin.");
        } else {
            hata(ex, 401, "Kullanıcı adı veya şifre hatalı. Kalan deneme hakkı: " + (3 - hataliGiris.get()));
        }
    }

    static void filmleriGonder(HttpExchange ex) throws IOException {
        List<String[]> filmler;
        Set<String> favoriler = new HashSet<>();
        synchronized (KILIT) {
            filmler = oku(FILMLER);
            for (String[] f : oku(FAVORILER)) favoriler.add(f[0].toLowerCase());
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < filmler.size(); i++) {
            String[] f = filmler.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"title\":").append(q(f[0]))
              .append(",\"year\":").append(q(f[1]))
              .append(",\"imdb\":").append(q(f[2]))
              .append(",\"genre\":").append(q(f[3]))
              .append(",\"runtime\":").append(q(f[4]))
              .append(",\"director\":").append(q(f[5]))
              .append(",\"poster\":").append(q(f[6].equals("N/A") ? "" : f[6]))
              .append(",\"favorite\":").append(favoriler.contains(f[0].toLowerCase()))
              .append('}');
        }
        json(ex, 200, sb.append(']').toString());
    }

    static void guncelle(HttpExchange ex, Map<String, String> p) throws IOException {
        String eski = temiz(p.get("title"));
        String yeniAd = temiz(p.get("newTitle"));
        String imdb = temiz(p.get("imdb"));
        if (eski.isEmpty() || yeniAd.isEmpty()) {
            hata(ex, 400, "Film adı boş olamaz.");
            return;
        }
        if (!imdb.isEmpty()) {
            try {
                double d = Double.parseDouble(imdb);
                if (d < 0 || d > 10) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                hata(ex, 400, "IMDb puanı 0 ile 10 arasında bir sayı olmalı.");
                return;
            }
        }
        synchronized (KILIT) {
            List<String[]> filmler = oku(FILMLER);
            String[] hedef = bul(filmler, eski);
            if (hedef == null) {
                hata(ex, 404, "Film bulunamadı.");
                return;
            }
            String[] cakisan = bul(filmler, yeniAd);
            if (cakisan != null && cakisan != hedef) {
                hata(ex, 409, "Bu isimde başka bir film zaten var.");
                return;
            }
            hedef[0] = yeniAd;
            hedef[1] = temiz(p.get("year"));
            hedef[2] = imdb;
            hedef[3] = temiz(p.get("genre"));
            hedef[4] = temiz(p.get("runtime"));
            hedef[5] = temiz(p.get("director"));
            yaz(FILMLER, filmler);

            List<String[]> favoriler = oku(FAVORILER);
            String[] fav = bul(favoriler, eski);
            if (fav != null) {
                System.arraycopy(hedef, 0, fav, 0, ALAN_SAYISI);
                yaz(FAVORILER, favoriler);
            }
        }
        json(ex, 200, "{\"ok\":true}");
    }

    static void sil(HttpExchange ex, Map<String, String> p) throws IOException {
        String ad = temiz(p.get("title"));
        synchronized (KILIT) {
            List<String[]> filmler = oku(FILMLER);
            String[] hedef = bul(filmler, ad);
            if (hedef == null) {
                hata(ex, 404, "Film bulunamadı.");
                return;
            }
            filmler.remove(hedef);
            yaz(FILMLER, filmler);

            List<String[]> favoriler = oku(FAVORILER);
            String[] fav = bul(favoriler, ad);
            if (fav != null) {
                favoriler.remove(fav);
                yaz(FAVORILER, favoriler);
            }
        }
        json(ex, 200, "{\"ok\":true}");
    }

    static void favoriEkle(HttpExchange ex, Map<String, String> p) throws IOException {
        String ad = temiz(p.get("title"));
        synchronized (KILIT) {
            String[] film = bul(oku(FILMLER), ad);
            if (film == null) {
                hata(ex, 404, "Film bulunamadı.");
                return;
            }
            List<String[]> favoriler = oku(FAVORILER);
            if (bul(favoriler, ad) == null) {
                favoriler.add(film);
                yaz(FAVORILER, favoriler);
            }
        }
        json(ex, 200, "{\"ok\":true}");
    }

    static void favoriCikar(HttpExchange ex, Map<String, String> p) throws IOException {
        String ad = temiz(p.get("title"));
        synchronized (KILIT) {
            List<String[]> favoriler = oku(FAVORILER);
            String[] fav = bul(favoriler, ad);
            if (fav != null) {
                favoriler.remove(fav);
                yaz(FAVORILER, favoriler);
            }
        }
        json(ex, 200, "{\"ok\":true}");
    }

    // ------------------------------------------------------------------ API'den veri çekme

    static void veriCek(HttpExchange ex) throws IOException {
        if (!cekiliyor.compareAndSet(false, true)) {
            json(ex, 202, "{\"started\":false}");
            return;
        }
        cekilen.set(0);
        toplam.set(FILM_ADLARI.length);
        sonMesaj = "";
        Thread t = new Thread(() -> {
            try {
                List<String[]> sonuc = new ArrayList<>();
                for (String ad : FILM_ADLARI) {
                    String[] f = omdbFilmGetir(ad);
                    if (f != null) sonuc.add(f);
                    cekilen.incrementAndGet();
                }
                if (sonuc.isEmpty()) {
                    sonMesaj = "Hiç film alınamadı. İnternet bağlantınızı kontrol edin; mevcut veriler korundu.";
                } else {
                    synchronized (KILIT) {
                        yaz(FILMLER, sonuc);
                    }
                    sonMesaj = sonuc.size() + " film " + FilmBilgiSistemi.DOSYA_ADI + " dosyasına kaydedildi.";
                }
            } catch (Exception e) {
                FilmBilgiSistemi.logHata("Veri çekme hatası: " + e);
                sonMesaj = "Veri çekilirken hata oluştu: " + e.getMessage();
            } finally {
                cekiliyor.set(false);
            }
        }, "veri-cek");
        t.setDaemon(true);
        t.start();
        json(ex, 202, "{\"started\":true}");
    }

    static void durumGonder(HttpExchange ex) throws IOException {
        json(ex, 200, "{\"running\":" + cekiliyor.get()
                + ",\"done\":" + cekilen.get()
                + ",\"total\":" + toplam.get()
                + ",\"message\":" + q(sonMesaj) + "}");
    }

    /** OMDb'den tek film çeker; başarısızsa null döner. */
    static String[] omdbFilmGetir(String filmAdi) {
        try {
            String sorgu = URLEncoder.encode(filmAdi, StandardCharsets.UTF_8);
            URI uri = URI.create("https://www.omdbapi.com/?t=" + sorgu + "&apikey=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String satir;
                while ((satir = br.readLine()) != null) sb.append(satir);
            }
            String veri = sb.toString();
            if (veri.contains("\"Response\":\"False\"")) {
                System.out.println(filmAdi + " filmi bulunamadı.");
                return null;
            }
            String[] f = new String[ALAN_SAYISI];
            f[0] = temiz(FilmBilgiSistemi.parcaGetir(veri, "\"Title\":\""));
            f[1] = temiz(FilmBilgiSistemi.parcaGetir(veri, "\"Year\":\""));
            f[2] = temiz(FilmBilgiSistemi.parcaGetir(veri, "\"imdbRating\":\""));
            f[3] = temiz(FilmBilgiSistemi.parcaGetir(veri, "\"Genre\":\""));
            f[4] = temiz(FilmBilgiSistemi.parcaGetir(veri, "\"Runtime\":\""));
            f[5] = temiz(FilmBilgiSistemi.parcaGetir(veri, "\"Director\":\""));
            f[6] = temiz(FilmBilgiSistemi.parcaGetir(veri, "\"Poster\":\""));
            return f;
        } catch (Exception e) {
            FilmBilgiSistemi.logHata("API çağrısında hata (" + filmAdi + "): " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ dosya işlemleri

    static List<String[]> oku(Path dosya) throws IOException {
        List<String[]> liste = new ArrayList<>();
        if (!Files.exists(dosya)) return liste;
        for (String satir : Files.readAllLines(dosya, StandardCharsets.UTF_8)) {
            if (satir.isBlank()) continue;
            String[] ham = satir.split(";", -1);
            String[] f = new String[ALAN_SAYISI];
            Arrays.fill(f, "");
            System.arraycopy(ham, 0, f, 0, Math.min(ham.length, ALAN_SAYISI));
            liste.add(f);
        }
        return liste;
    }

    static void yaz(Path dosya, List<String[]> liste) throws IOException {
        List<String> satirlar = new ArrayList<>();
        for (String[] f : liste) {
            int son = ALAN_SAYISI;
            while (son > 6 && f[son - 1].isEmpty()) son--; // poster yoksa terminal formatı (6 alan) korunur
            satirlar.add(String.join(";", Arrays.copyOf(f, son)));
        }
        Path gecici = Paths.get(dosya.toString() + ".tmp");
        Files.write(gecici, satirlar, StandardCharsets.UTF_8);
        Files.move(gecici, dosya, StandardCopyOption.REPLACE_EXISTING);
    }

    static String[] bul(List<String[]> liste, String ad) {
        for (String[] f : liste) if (f[0].equalsIgnoreCase(ad)) return f;
        return null;
    }

    /** Alan ayırıcı (;) ve satır sonlarını temizler, böylece dosya formatı bozulmaz. */
    static String temiz(String s) {
        if (s == null) return "";
        return s.replace(';', ',').replace('\r', ' ').replace('\n', ' ').trim();
    }

    // ------------------------------------------------------------------ HTTP yardımcıları

    static Map<String, String> form(HttpExchange ex) throws IOException {
        String govde = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String cift : govde.split("&")) {
            if (cift.isEmpty()) continue;
            int i = cift.indexOf('=');
            String k = i < 0 ? cift : cift.substring(0, i);
            String v = i < 0 ? "" : cift.substring(i + 1);
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }

    static void json(HttpExchange ex, int kod, String icerik) throws IOException {
        gonder(ex, kod, "application/json; charset=utf-8", icerik.getBytes(StandardCharsets.UTF_8));
    }

    static void hata(HttpExchange ex, int kod, String mesaj) throws IOException {
        json(ex, kod, "{\"error\":" + q(mesaj) + "}");
    }

    static void gonder(HttpExchange ex, int kod, String tur, byte[] veri) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tur);
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(kod, veri.length == 0 ? -1 : veri.length);
        if (veri.length > 0) ex.getResponseBody().write(veri);
    }

    static void statik(HttpExchange ex, String path, String metot) throws IOException {
        if (!metot.equals("GET") && !metot.equals("HEAD")) {
            hata(ex, 405, "Desteklenmeyen istek.");
            return;
        }
        if (path.equals("/")) path = "/index.html";
        Path dosya = WEB.resolve(path.substring(1)).normalize();
        if (!dosya.startsWith(WEB) || !Files.isRegularFile(dosya)) {
            gonder(ex, 404, "text/plain; charset=utf-8", "404 - Sayfa bulunamadı".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String ad = dosya.getFileName().toString().toLowerCase();
        String tur = ad.endsWith(".html") ? "text/html; charset=utf-8"
                : ad.endsWith(".css") ? "text/css; charset=utf-8"
                : ad.endsWith(".js") ? "text/javascript; charset=utf-8"
                : ad.endsWith(".svg") ? "image/svg+xml"
                : ad.endsWith(".png") ? "image/png"
                : ad.endsWith(".ico") ? "image/x-icon"
                : "application/octet-stream";
        gonder(ex, 200, tur, metot.equals("HEAD") ? new byte[0] : Files.readAllBytes(dosya));
    }

    /** JSON string literal üretir. */
    static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
