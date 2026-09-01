import { displayTagValue, type InspectionViewModel } from "./inspectionParser";

export type CsvField = {
  name: string;
  value: string;
};

export type CsvRow = {
  category: string;
  item: string;
  fields: CsvField[];
};

export type CsvDocument = {
  fileName: string;
  content: string;
  rows: CsvRow[];
  fieldPairCount: number;
};

function escapeCsv(value: string): string {
  return `"${value.replaceAll('"', '""')}"`;
}

function fileTimestamp(date: Date): string {
  return date.toISOString().replace(/[:.]/g, "-");
}

function field(name: string, value: string): CsvField {
  return { name, value };
}

function flattenRow(row: CsvRow, fieldPairCount: number): string[] {
  const columns = [row.category, row.item];
  for (let index = 0; index < fieldPairCount; index += 1) {
    const currentField = row.fields[index];
    columns.push(currentField?.name ?? "", currentField?.value ?? "");
  }
  return columns;
}

export function createInspectionCsv(
  data: InspectionViewModel,
  cardInterface: "contact" | "contactless",
  durationMillis: number,
  generatedAt = new Date()
): CsvDocument {
  const rows: CsvRow[] = [
    {
      category: "İnceleme",
      item: "Genel",
      fields: [
        field("Oluşturulma zamanı", generatedAt.toISOString()),
        field("Kart arayüzü", cardInterface === "contactless" ? "Temassız" : "Temaslı"),
        field("Toplam süre (ms)", String(durationMillis))
      ]
    },
    { category: "Bağlantı", item: "Okuyucu", fields: [field("Okuyucu adı", data.reader)] },
    {
      category: "Bağlantı",
      item: "Kart",
      fields: [field("Arayüz", data.interfaceName), field("Protokol", data.protocol), field("ATR", data.atr)]
    }
  ];

  data.applications.forEach((application, index) => {
    rows.push({
      category: "Ödeme uygulaması",
      item: `Uygulama ${index + 1}`,
      fields: [
        field("Şema", application.scheme),
        field("AID", application.aid),
        field("Dizin etiketi", application.directoryLabel),
        field("Uygulama", application.name),
        field("Tercih edilen ad", application.preferredName),
        field("PDOL", application.pdol),
        field("AIP", application.aip),
        field("AFL", application.afl)
      ]
    });
  });

  data.commands.forEach((command, index) => {
    rows.push({
      category: "APDU işlemi",
      item: `${index + 1}. ${command.name}`,
      fields: [
        field("Komut", command.apdu),
        field("Durum kelimesi", command.statusWord),
        field("Durum", command.successful ? "Başarılı" : command.status),
        field("Süre", command.duration),
        field("Ayrıştırma", command.parseStatus),
        ...command.notes.map((note, noteIndex) => field(`Not ${noteIndex + 1}`, note))
      ]
    });
  });

  data.tags.forEach((tag, index) => {
    rows.push({
      category: "EMV tag",
      item: `${index + 1}. Tag ${tag.tag}`,
      fields: [
        field("Açıklama", tag.name),
        field("Tür", tag.type === "constructed" ? "Yapılandırılmış" : "Basit"),
        field("Uzunluk (byte)", String(tag.length)),
        field("Değer", displayTagValue(tag)),
        field("Hassas", tag.sensitive ? "Evet (maskeli)" : "Hayır")
      ]
    });
  });

  const fieldPairCount = Math.max(0, ...rows.map((row) => row.fields.length));
  const header = ["Kategori", "Kayıt"];
  for (let index = 1; index <= fieldPairCount; index += 1) {
    header.push(`Alan ${index}`, `Değer ${index}`);
  }

  const content = "\uFEFF" + [header, ...rows.map((row) => flattenRow(row, fieldPairCount))]
    .map((columns) => columns.map(escapeCsv).join(";"))
    .join("\r\n");

  return {
    fileName: `emv-inceleme-${fileTimestamp(generatedAt)}.csv`,
    content,
    rows,
    fieldPairCount
  };
}
