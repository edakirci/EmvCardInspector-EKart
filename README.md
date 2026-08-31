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

## Masaüstü arayüzünü geliştirme

Java REST API'yi proje kökünde başlatın:

```powershell
.\mvnw.cmd spring-boot:run
```

İlk kurulumda masaüstü bağımlılıklarını yükleyin:

```powershell
cd desktop
npm install
```

React geliştirme sunucusunu ve Electron penceresini birlikte başlatın:

```powershell
npm run dev
```

React/TypeScript üretim derlemesini kontrol etmek için:

```powershell
npm run build
```

Başarılı bir kart incelemesinin ardından **CSV’ye Dönüştür** düğmesi; bağlantı,
ödeme uygulaması, APDU işlem özeti ve EMV tag verilerini önizlemeye hazırlar.
Hassas tag değerleri CSV içinde de maskeli tutulur. Önizleme penceresindeki
**İndir** düğmesiyle dosyanın kaydedileceği konum seçilir.

Yerel backend yalnızca `127.0.0.1:8080` adresinde dinler. Bağlantı kontrolü
`GET /api/health` endpoint'i üzerinden yapılır.

Masaüstü arayüzü önce temaslı veya temassız kart seçimi sunar. Temaslı inceleme
`POST /api/inspections/contact`, temassız inceleme ise
`POST /api/inspections/contactless` endpoint'iyle de başlatılabilir. Backend
uygun okuyucuyu otomatik seçer, kartı en fazla 30 saniye bekler ve salt okunur
PSE/PPSE, AID seçimi, GPO ve AFL kayıt okuma akışının teknik çıktısını JSON
cevabında döndürür. Aynı anda yalnızca bir kart incelemesi çalıştırılır.

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

Keşfedilen her ödeme uygulaması daha sonra kendi AID değeriyle seçilir. Başarılı
`SELECT AID` cevabı BER-TLV olarak ayrıştırılır; AID'nin RID bölümünden Visa,
Mastercard ve diğer desteklenen ödeme ağları belirlenir. Kartın verdiği `50`
Application Label, `9F12` Application Preferred Name ve `9F38` PDOL alanları
uygulama bazlı bir dal altında gösterilir.

Başarılı uygulama seçiminden sonra GET PROCESSING OPTIONS komutu gönderilir.
Temaslı akış mevcut boş PDOL verisini (`83 00`) kullanır. Temassız akışta kartın
`9F38` PDOL alanında istediği terminal verileri hazırlanıp `83` şablonuna
yerleştirilir. Format-1 (`80`) ve format-2 (`77`) cevaplarda `82` Application
Interchange Profile (AIP) ile `94` Application File Locator (AFL) alanları
doğrulanır ve TLV ağacıyla birlikte gösterilir.

AFL içindeki her dört byte'lık girdi SFI, ilk kayıt, son kayıt ve offline
authentication kayıt sayısı olarak ayrıştırılır. Her iki akışta her AFL girdisi
sırayla dolaşılır ve ilan edilen ilk-son kayıt aralığının tamamı ilgili SFI ile
okunur. Dördüncü byte kayıt atlama bilgisi değildir; yalnızca aralıktaki kaç
kaydın offline data authentication işlemine dahil olduğunu belirtir. Örneğin
`10 02 02 01`, SFI 2 / kayıt 2'nin okunacağını ve bu tek kaydın offline
authentication verisine dahil olduğunu ifade eder. Bu kayıt için
`00 B2 02 14 00`; SFI 3 / kayıt 1-2 için
`00 B2 01 1C 00` ve `00 B2 02 1C 00`; SFI 5 / kayıt 1-2 için
`00 B2 01 2C 00` ve `00 B2 02 2C 00` gönderilir. Başarılı kayıt cevapları
BER-TLV olarak ayrıştırılıp gösterilir.

Temaslı PSE cevabındaki `88` Short File Identifier alanı ayrıştırılır. Uygulama,
bu SFI üzerindeki yalnızca ilk kaydı `READ RECORD 1` ile okur. İlk kayıttaki
ödeme uygulamaları PPSE ile aynı özet biçiminde gösterilir.

EMV tag sözlüğü iki seviyelidir. Standart EMV tag'leri ortak sözlükte tutulur;
Visa (`A000000003`), Mastercard (`A000000004`), American Express
(`A000000025`), China UnionPay/CUP (`A000000333`) ve TROY (`A000000672`) için
uygulamaya özel tag alanları ayrı tutulur. SELECT AID sonrasında FCI, GPO ve
READ RECORD çıktıları seçilen AID bağlamıyla çözümlenir. Bir tag için en uzun
eşleşen AID öneki önceliklidir; uygulamaya özel tanım bulunamazsa ortak EMV
tanımına geri dönülür. Böylece gerektiğinde hem bir ödeme şemasının tamamına
hem de tek bir ürün AID'sine özel tag tanımı eklenebilir.

## Planlanan katmanlar

- `reader`: PC/SC okuyucu keşfi ve kart bağlantısı
- `apdu`: APDU komut/cevap modelleri ve durum kodları
- `tlv`: BER-TLV ayrıştırma; çok baytlı tag, definite length ve iç içe yapılar
- `emv`: EMV tag sözlüğü ve doğrulama
- `report`: Maskelenmiş JSON ve HTML raporları
