# EMV Card Inspector

Java 21 ile geliştirilen, test kartlarına standart ve salt okunur APDU
komutları gönderen; cevapları BER-TLV olarak ayrıştıran, EMV verilerini
doğrulayan ve hassas alanları maskeleyerek raporlayan bir inceleme aracıdır.

## Gereksinimler

- JDK 21
- Maven 3.9 veya Maven Wrapper

## Derleme ve test

Windows:

```powershell
.\mvnw.cmd clean test
```

Sistem Maven kurulumu ile:

```powershell
mvn clean test
```

## Okuyucu teşhisini çalıştırma

Önce projeyi derleyin:

```powershell
.\mvnw.cmd compile
```

Ardından komut satırı uygulamasını başlatın:

```powershell
java -cp target\classes com.emvcardinspector.app.Main
```

Uygulama PC/SC okuyucularını listeler, seçilen okuyucuda kartı 15 saniye
bekler ve bağlantı başarılıysa ATR ile iletişim protokolünü gösterir. Bu
teşhis adımı karta APDU göndermez.

## Salt okunur SELECT PPSE işlemi

Bağlantı kurulduktan sonra uygulama iki işlem sunar:

- `0`: Yalnızca bağlantı teşhisi; karta APDU gönderilmez.
- `1`: Önceden tanımlanmış, salt okunur `SELECT PPSE` komutunu gönderir.

`SELECT PPSE` sonucunda komut, tam ham cevap, cevap verisi, SW1, SW2, durum
açıklaması ve işlem süresi gösterilir. Bu aşamada cevap verisi henüz BER-TLV
olarak ayrıştırılmaz.

## Planlanan katmanlar

- `reader`: PC/SC okuyucu keşfi ve kart bağlantısı
- `apdu`: APDU komut/cevap modelleri ve durum kodları
- `tlv`: BER-TLV ayrıştırma
- `emv`: EMV tag sözlüğü ve doğrulama
- `report`: Maskelenmiş JSON ve HTML raporları
