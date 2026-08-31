import { displayTagValue, type InspectionViewModel } from "./inspectionParser";

export type CsvRow = {
  category: string;
  item: string;
  field: string;
  value: string;
};

export type CsvDocument = {
  fileName: string;
  content: string;
  rows: CsvRow[];
};

function escapeCsv(value: string): string {
  return `"${value.replaceAll('"', '""')}"`;
}

function fileTimestamp(date: Date): string {
  return date.toISOString().replace(/[:.]/g, "-");
}

export function createInspectionCsv(
  data: InspectionViewModel,
  cardInterface: "contact" | "contactless",
  durationMillis: number,
  generatedAt = new Date()
): CsvDocument {
  const rows: CsvRow[] = [
    { category: "İnceleme", item: "Genel", field: "Oluşturulma zamanı", value: generatedAt.toISOString() },
    { category: "İnceleme", item: "Genel", field: "Kart arayüzü", value: cardInterface === "contactless" ? "Temassız" : "Temaslı" },
    { category: "İnceleme", item: "Genel", field: "Toplam süre (ms)", value: String(durationMillis) },
    { category: "Bağlantı", item: "Okuyucu", field: "Okuyucu adı", value: data.reader },
    { category: "Bağlantı", item: "Kart", field: "Arayüz", value: data.interfaceName },
    { category: "Bağlantı", item: "Kart", field: "Protokol", value: data.protocol },
    { category: "Bağlantı", item: "Kart", field: "ATR", value: data.atr }
  ];

  data.applications.forEach((application, index) => {
    const item = `Uygulama ${index + 1}`;
    rows.push(
      { category: "Ödeme uygulaması", item, field: "Şema", value: application.scheme },
      { category: "Ödeme uygulaması", item, field: "AID", value: application.aid },
      { category: "Ödeme uygulaması", item, field: "Dizin etiketi", value: application.directoryLabel },
      { category: "Ödeme uygulaması", item, field: "Uygulama", value: application.name },
      { category: "Ödeme uygulaması", item, field: "Tercih edilen ad", value: application.preferredName },
      { category: "Ödeme uygulaması", item, field: "PDOL", value: application.pdol },
      { category: "Ödeme uygulaması", item, field: "AIP", value: application.aip },
      { category: "Ödeme uygulaması", item, field: "AFL", value: application.afl }
    );
  });

  data.commands.forEach((command, index) => {
    const item = `${index + 1}. ${command.name}`;
    rows.push(
      { category: "APDU işlemi", item, field: "Komut", value: command.apdu },
      { category: "APDU işlemi", item, field: "Durum kelimesi", value: command.statusWord },
      { category: "APDU işlemi", item, field: "Durum", value: command.successful ? "Başarılı" : command.status },
      { category: "APDU işlemi", item, field: "Süre", value: command.duration },
      { category: "APDU işlemi", item, field: "Ayrıştırma", value: command.parseStatus }
    );
    command.notes.forEach((note, noteIndex) => rows.push({
      category: "APDU işlemi",
      item,
      field: `Not ${noteIndex + 1}`,
      value: note
    }));
  });

  data.tags.forEach((tag, index) => {
    const item = `${index + 1}. Tag ${tag.tag}`;
    rows.push(
      { category: "EMV tag", item, field: "Açıklama", value: tag.name },
      { category: "EMV tag", item, field: "Tür", value: tag.type === "constructed" ? "Yapılandırılmış" : "Basit" },
      { category: "EMV tag", item, field: "Uzunluk (byte)", value: String(tag.length) },
      { category: "EMV tag", item, field: "Değer", value: displayTagValue(tag) },
      { category: "EMV tag", item, field: "Hassas", value: tag.sensitive ? "Evet (maskeli)" : "Hayır" }
    );
  });

  const header = ["Kategori", "Kayıt", "Alan", "Değer"];
  const content = "\uFEFF" + [header, ...rows.map((row) => [row.category, row.item, row.field, row.value])]
    .map((columns) => columns.map(escapeCsv).join(";"))
    .join("\r\n");

  return {
    fileName: `emv-inceleme-${fileTimestamp(generatedAt)}.csv`,
    content,
    rows
  };
}
