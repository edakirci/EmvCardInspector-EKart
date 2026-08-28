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

Uygulama başlangıç menüsünde temaslı veya temassız kart akışını seçtirir.
PC/SC okuyucularını algılar, seçilen arayüze uygun okuyucuyu otomatik belirler,
bu okuyucuda kartı 15 saniye bekler ve
bağlantı başarılıysa ATR ile iletişim protokolünü gösterir. İşlem tamamlandıktan
sonra bağlantıyı kapatıp ana menüye döner.

## Salt okunur uygulama keşfi

Ana menüde aşağıdaki işlemler sunulur:

- `1`: Temaslı kart için `SELECT PSE` komutunu gönderir.
- `2`: Temassız kart için `SELECT PPSE` komutunu gönderir.
- `0`: Uygulamadan çıkar.

SELECT sonucunda komut, tam ham cevap, cevap verisi, SW1, SW2, durum açıklaması
ve işlem süresi gösterilir. Başarılı cevap verisi daha sonra BER-TLV olarak
ayrıştırılır.

Kart yeni takıldığında ilk `SELECT PSE` veya `SELECT PPSE` boş veriyle `6D00`
dönerse uygulama kartın hazırlanması için 250 ms bekler ve aynı komutu yalnızca
bir kez yeniden gönderir.

Başarılı ve veri içeren PPSE cevapları komut satırında BER-TLV ağacı olarak
gösterilir. `61` Application Template nesnelerinden `4F` AID, isteğe bağlı `50`
Application Label ve `87` Application Priority Indicator alanları doğrulanarak
ayrı bir ödeme uygulamaları özetine çıkarılır. Başarısız durum kodlarında veya
boş cevap verisinde ayrıştırma yapılmaz ve atlanma nedeni gösterilir.

Temaslı PSE cevabındaki `88` Short File Identifier alanı ayrıştırılır. Uygulama,
bu SFI üzerindeki yalnızca ilk kaydı `READ RECORD 1` ile okur. İlk kayıttaki
ödeme uygulamaları PPSE ile aynı özet biçiminde gösterilir.

## Planlanan katmanlar

- `reader`: PC/SC okuyucu keşfi ve kart bağlantısı
- `apdu`: APDU komut/cevap modelleri ve durum kodları
- `tlv`: BER-TLV ayrıştırma; çok baytlı tag, definite length ve iç içe yapılar
- `emv`: EMV tag sözlüğü ve doğrulama
- `report`: Maskelenmiş JSON ve HTML raporları
