# Resmi OpenJDK tabanını kullanıyoruz
FROM eclipse-temurin:21-jdk-alpine

# Çalışma dizinini ayarlıyoruz
WORKDIR /app

# Proje dosyalarını kopyalıyoruz
COPY . /app

# Java kodlarını derliyoruz
RUN javac -encoding UTF-8 FilmBilgiSistemi.java FilmServer.java

# Render veya Railway'in vereceği PORT ortam değişkenini kullanıyoruz
# Varsayılan olarak 8080'de başlatılacak
CMD ["java", "-Dfile.encoding=UTF-8", "FilmServer"]
