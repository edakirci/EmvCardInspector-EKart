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

## Planlanan katmanlar

- `reader`: PC/SC okuyucu keşfi ve kart bağlantısı
- `apdu`: APDU komut/cevap modelleri ve durum kodları
- `tlv`: BER-TLV ayrıştırma
- `emv`: EMV tag sözlüğü ve doğrulama
- `report`: Maskelenmiş JSON ve HTML raporları
