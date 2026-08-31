import { Fragment, useEffect, useLayoutEffect, useMemo, useState } from "react";
import {
  displayTagValue,
  parseInspectionOutput,
  type InspectionCommand,
  type InspectionViewModel
} from "./inspectionParser";

type BackendStatus = "checking" | "online" | "offline";
type InspectionStatus = "completed" | "no_reader" | "no_card" | "failed" | "busy";
type CardInterface = "contact" | "contactless";
type Theme = "light" | "dark";

type CardInspectionResponse = {
  status: InspectionStatus;
  output: string;
  durationMillis: number;
};

const statusLabels: Record<InspectionStatus, string> = {
  completed: "İnceleme tamamlandı",
  no_reader: "Uygun okuyucu bulunamadı",
  no_card: "Kart algılanmadı",
  failed: "İnceleme başarısız",
  busy: "Başka bir inceleme devam ediyor"
};

function commandLabel(command: InspectionCommand): string {
  if (command.name.startsWith("READ RECORD")) return "Kayıt okuma";
  if (command.name === "SELECT PSE") return "PSE seçimi";
  if (command.name === "SELECT PPSE") return "PPSE seçimi";
  if (command.name === "SELECT AID") return "Uygulama seçimi";
  if (command.name === "GET PROCESSING OPTIONS") return "İşlem seçenekleri";
  return command.name;
}

function Detail({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="detail-item">
      <span>{label}</span>
      <strong className={mono ? "mono-value" : ""} title={value}>{value}</strong>
    </div>
  );
}

function CommandDetails({ command }: { command: InspectionCommand }) {
  return (
    <div className="command-details">
      <div className="command-detail-header">
        <div>
          <p className="section-label">KOMUT DETAYI</p>
          <h3>{command.name}</h3>
        </div>
        <span className={`command-status ${command.successful ? "command-success" : "command-failed"}`}>
          {command.statusWord} · {command.successful ? "Başarılı" : command.status}
        </span>
      </div>

      <div className="command-facts">
        <Detail label="Gönderilen APDU" value={command.apdu} mono />
        <Detail label="İşlem süresi" value={command.duration} />
        <Detail label="Ayrıştırma" value={command.parseStatus} />
        <Detail label="TLV nesnesi" value={`${command.tags.length} adet`} />
      </div>

      {command.notes.length > 0 && (
        <div className="command-notes">
          {command.notes.map((note, index) => <span key={`${note}-${index}`}>{note}</span>)}
        </div>
      )}

      <div className="tlv-section-heading">
        <div><p className="section-label">AYRIŞTIRILMIŞ CEVAP</p><h3>TLV ağacı</h3></div>
        <span>{command.tags.length ? `${command.tags.length} düğüm` : "TLV verisi yok"}</span>
      </div>

      {command.tags.length ? (
        <div className="tlv-tree">
          {command.tags.map((tag) => (
            <div className={`tlv-node ${tag.depth === 0 ? "tlv-root" : ""}`} key={tag.id} style={{ marginLeft: `${tag.depth * 24}px` }}>
              <span className="tlv-connector" />
              <span className={`tlv-kind ${tag.type === "constructed" ? "tlv-constructed" : ""}`}>
                {tag.type === "constructed" ? "◇" : "•"}
              </span>
              <span className="tag-code">{tag.tag}</span>
              <div className="tlv-node-copy">
                <strong>{tag.name}</strong>
                <span>{tag.type === "constructed" ? "Yapılandırılmış" : "Basit"} · {tag.length} byte</span>
              </div>
              <code title={tag.sensitive ? "Hassas değer maskelendi" : tag.value}>{displayTagValue(tag)}</code>
              {tag.sensitive && <span className="sensitive-label">Hassas</span>}
            </div>
          ))}
        </div>
      ) : (
        <div className="no-tlv"><span>◇</span><p>Bu komut için ayrıştırılabilir TLV verisi bulunmuyor.</p></div>
      )}

      <details className="raw-exchange">
        <summary>Ham APDU alışverişini göster</summary>
        <div className="raw-exchange-grid">
          <div><span>Komut</span><code>{command.apdu}</code></div>
          <div><span>Ham cevap</span><code>{command.rawResponse}</code></div>
          {command.responseData !== "—" && <div><span>Cevap verisi</span><code>{command.responseData}</code></div>}
          <div><span>Durum baytları</span><code>SW1 {command.sw1} · SW2 {command.sw2} · {command.statusWord}</code></div>
        </div>
      </details>
    </div>
  );
}

function ResultsDashboard({ response, cardInterface }: { response: CardInspectionResponse; cardInterface: CardInterface }) {
  const data = useMemo(() => parseInspectionOutput(response.output), [response.output]);
  const [tagQuery, setTagQuery] = useState("");
  const [expandedCommand, setExpandedCommand] = useState<number | null>(null);
  const filteredTags = data.tags.filter((tag) => {
    const query = tagQuery.trim().toLocaleLowerCase("tr-TR");
    return !query || tag.tag.toLowerCase().includes(query) || tag.name.toLocaleLowerCase("tr-TR").includes(query);
  });
  const successfulCommands = data.commands.filter((command) => command.successful).length;
  const recordCount = data.commands.filter((command) => command.name.startsWith("READ RECORD")).length;
  const isContactless = cardInterface === "contactless";
  const workflow = [
    { label: "Okuyucu", detail: data.reader === "—" ? "Bekleniyor" : "Bağlandı", done: data.reader !== "—" },
    { label: isContactless ? "PPSE" : "PSE", detail: "Dizin seçimi", done: hasSuccessfulCommand(data, isContactless ? "SELECT PPSE" : "SELECT PSE") },
    { label: "Uygulama", detail: "AID seçimi", done: hasSuccessfulCommand(data, "SELECT AID") },
    { label: "GPO", detail: "AIP ve AFL", done: hasSuccessfulCommand(data, "GET PROCESSING OPTIONS") },
    { label: "Kayıtlar", detail: `${recordCount} kayıt`, done: recordCount > 0 }
  ];

  if (response.status !== "completed") {
    return (
      <section className="result-message-card">
        <div className={`result-icon result-icon-${response.status}`}>!</div>
        <div>
          <p className="section-label">İNCELEME DURUMU</p>
          <h2>{statusLabels[response.status]}</h2>
          <p>{friendlyFailureMessage(response.status, cardInterface)}</p>
        </div>
        <details className="developer-details compact-details">
          <summary>Teknik ayrıntıyı göster</summary>
          <pre>{response.output}</pre>
        </details>
      </section>
    );
  }

  return (
    <div className="results-dashboard">
      <section className="result-hero">
        <div className="result-hero-copy">
          <span className="success-mark">✓</span>
          <div>
            <p className="section-label">İNCELEME BAŞARILI</p>
            <h2>{isContactless ? "Temassız" : "Temaslı"} kart verileri hazır</h2>
            <p>{data.reader} üzerinden salt okunur EMV akışı tamamlandı.</p>
          </div>
        </div>
        <div className="metric-grid">
          <div className="metric"><strong>{response.durationMillis}</strong><span>milisaniye</span></div>
          <div className="metric"><strong>{successfulCommands}/{data.commands.length}</strong><span>başarılı komut</span></div>
          <div className="metric"><strong>{data.tags.length}</strong><span>EMV tag</span></div>
          <div className="metric"><strong>{data.applications.length}</strong><span>uygulama</span></div>
        </div>
      </section>

      <section className="panel workflow-panel">
        <div className="panel-heading">
          <div><p className="section-label">İŞLEM AKIŞI</p><h2>{isContactless ? "Temassız" : "Temaslı"} okuma adımları</h2></div>
          <span className="panel-note">Salt okunur</span>
        </div>
        <div className="workflow">
          {workflow.map((step, index) => (
            <div className={`workflow-step ${step.done ? "workflow-done" : ""}`} key={step.label}>
              <span className="workflow-index">{step.done ? "✓" : index + 1}</span>
              <div><strong>{step.label}</strong><small>{step.detail}</small></div>
            </div>
          ))}
        </div>
      </section>

      <section className="two-column-grid">
        <div className="panel">
          <div className="panel-heading"><div><p className="section-label">BAĞLANTI</p><h2>Okuyucu ve kart</h2></div></div>
          <div className="detail-grid">
            <Detail label="Okuyucu" value={data.reader} />
            <Detail label="Arayüz" value={data.interfaceName} />
            <Detail label="Protokol" value={data.protocol} />
            <Detail label="ATR" value={data.atr} mono />
          </div>
        </div>

        <div className="panel">
          <div className="panel-heading"><div><p className="section-label">ÖDEME UYGULAMASI</p><h2>Kart uygulaması</h2></div></div>
          {data.applications.length ? data.applications.map((application) => (
            <div className="application-card" key={application.aid}>
              <div className="scheme-logo">{application.scheme.slice(0, 1)}</div>
              <div className="application-main">
                <strong>{application.name !== "—" ? application.name : application.scheme}</strong>
                <span>{application.scheme}</span>
                <code>{application.aid}</code>
              </div>
              <div className="application-meta">
                <span>PDOL <code>{application.pdol}</code></span>
                <span>AIP <code>{application.aip}</code></span>
                <span>AFL <code>{application.afl}</code></span>
              </div>
            </div>
          )) : <p className="muted-copy">Seçilebilir ödeme uygulaması bulunamadı.</p>}
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div><p className="section-label">APDU İŞLEMLERİ</p><h2>Kartla yapılan iletişim</h2></div>
          <span className="panel-note">{data.commands.length} komut</span>
        </div>
        <div className="table-scroll">
          <table className="data-table command-table">
            <thead><tr><th>İşlem</th><th>APDU</th><th>Durum</th><th>Süre</th></tr></thead>
            <tbody>{data.commands.map((command, index) => (
              <Fragment key={`${command.name}-${index}`}>
              <tr
                className={`command-row ${expandedCommand === index ? "command-row-open" : ""}`}
                onClick={() => setExpandedCommand((current) => current === index ? null : index)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    setExpandedCommand((current) => current === index ? null : index);
                  }
                }}
                role="button"
                tabIndex={0}
              >
                <td>
                  <div className="command-name-cell">
                    <span className="expand-chevron">›</span>
                    <div><strong>{commandLabel(command)}</strong><small>{command.name}</small></div>
                  </div>
                </td>
                <td><code>{command.apdu}</code></td>
                <td><span className={`command-status ${command.successful ? "command-success" : "command-failed"}`}>
                  {command.statusWord} · {command.successful ? "Başarılı" : command.status}
                </span></td>
                <td>{command.duration}</td>
              </tr>
              {expandedCommand === index && (
                <tr className="command-detail-row"><td colSpan={4}><CommandDetails command={command} /></td></tr>
              )}
              </Fragment>
            ))}</tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading tag-heading">
          <div><p className="section-label">EMV VERİLERİ</p><h2>Tag sözlüğü</h2></div>
          <label className="search-box">
            <span>⌕</span>
            <input value={tagQuery} onChange={(event) => setTagQuery(event.target.value)} placeholder="Tag veya açıklama ara" />
          </label>
        </div>
        <div className="table-scroll tag-table-scroll">
          <table className="data-table tag-table">
            <thead><tr><th>Tag</th><th>Açıklama</th><th>Tür</th><th>Uzunluk</th><th>Değer</th></tr></thead>
            <tbody>{filteredTags.map((tag) => (
              <tr key={tag.id}>
                <td><span className="tag-code">{tag.tag}</span></td>
                <td>{tag.name}{tag.sensitive && <span className="sensitive-label">Hassas</span>}</td>
                <td>{tag.type === "constructed" ? "Yapılandırılmış" : "Basit"}</td>
                <td>{tag.length} byte</td>
                <td><code title={tag.sensitive ? "Hassas değer maskelendi" : tag.value}>{displayTagValue(tag)}</code></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        {!filteredTags.length && <p className="empty-filter">Aramayla eşleşen tag bulunamadı.</p>}
      </section>

      <details className="developer-details">
        <summary>Geliştirici ayrıntıları ve ham tanılama günlüğü</summary>
        <p>Bu bölüm APDU cevapları dahil teknik veriler içerebilir. Yalnızca test ve hata ayıklama amacıyla kullan.</p>
        <pre>{response.output}</pre>
      </details>
    </div>
  );
}

function hasSuccessfulCommand(data: InspectionViewModel, name: string): boolean {
  return data.commands.some((command) => command.name === name && command.successful);
}

function friendlyFailureMessage(status: InspectionStatus, cardInterface: CardInterface): string {
  const isContactless = cardInterface === "contactless";
  if (status === "no_reader") return `Bilgisayara bağlı uygun bir ${isContactless ? "temassız" : "temaslı"} PC/SC okuyucu bulunamadı.`;
  if (status === "no_card") return isContactless
    ? "30 saniye içinde temassız okuyucuda kart algılanmadı. Kartı okuyucunun üzerinde sabit tutup tekrar dene."
    : "30 saniye içinde temaslı yuvada kart algılanmadı. Kartı yerleştirip tekrar dene.";
  if (status === "busy") return "Devam eden kart incelemesi tamamlandıktan sonra yeniden deneyebilirsin.";
  return "Kartla iletişim kurulamadı. Okuyucu bağlantısını ve kartın yerleşimini kontrol et.";
}

function App() {
  const [theme, setTheme] = useState<Theme>(() => {
    const savedTheme = window.localStorage.getItem("emv-theme");
    return savedTheme === "dark" ? "dark" : "light";
  });
  const [backendStatus, setBackendStatus] = useState<BackendStatus>("checking");
  const [selectedInterface, setSelectedInterface] = useState<CardInterface | null>(null);
  const [inspection, setInspection] = useState<CardInspectionResponse | null>(null);
  const [scanError, setScanError] = useState<string | null>(null);
  const [isScanning, setIsScanning] = useState(false);

  async function checkBackend() {
    setBackendStatus("checking");
    try {
      const response = await fetch("/api/health");
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      setBackendStatus("online");
    } catch {
      setBackendStatus("offline");
    }
  }

  async function inspectCard() {
    if (!selectedInterface) return;
    setIsScanning(true);
    setInspection(null);
    setScanError(null);
    try {
      const response = await fetch(`/api/inspections/${selectedInterface}`, { method: "POST" });
      const result = (await response.json()) as CardInspectionResponse;
      if (!response.ok && result.status !== "busy") throw new Error(result.output || `HTTP ${response.status}`);
      setInspection(result);
    } catch (error) {
      setScanError(error instanceof Error ? error.message : "Kart incelemesi başlatılamadı.");
      await checkBackend();
    } finally {
      setIsScanning(false);
    }
  }

  useEffect(() => { void checkBackend(); }, []);
  useLayoutEffect(() => {
    document.documentElement.dataset.theme = theme;
    window.localStorage.setItem("emv-theme", theme);
  }, [theme]);

  const backendStatusText = {
    checking: "Backend kontrol ediliyor",
    online: "Sistem hazır",
    offline: "Backend bağlantısı yok"
  }[backendStatus];

  const interfaceCopy = selectedInterface === "contactless" ? {
    eyebrow: "TEMASSIZ KART ANALİZİ",
    title: "Kartı yaklaştır, EMV yapısını inceleyelim",
    description: "PPSE dizininden uygulamaları bulur, PDOL verisini hazırlar; GPO cevabındaki Tag 94/AFL aralıklarına göre kayıtları okur.",
    ready: "Temassız okuyucu hazır",
    waiting: "Kart bekleniyor…",
    instruction: "Kartı okuyucunun üzerinde sabit tut",
    idleInstruction: "Kartı yaklaştır ve taramayı başlat",
    progress: "Temassız kart okunuyor",
    button: "Temassız kartı incele"
  } : {
    eyebrow: "TEMASLI KART ANALİZİ",
    title: "Kartın EMV yapısını güvenle incele",
    description: "PSE dizininden başlayarak uygulama seçimi, GPO ve Tag 94/AFL kayıtlarını salt okunur komutlarla analiz et.",
    ready: "Temaslı okuyucu hazır",
    waiting: "Kart bekleniyor…",
    instruction: "Kartı yuvadan çıkarma",
    idleInstruction: "Kartı yerleştir ve taramayı başlat",
    progress: "Temaslı kart okunuyor",
    button: "Temaslı kartı incele"
  };

  function selectInterface(cardInterface: CardInterface) {
    setSelectedInterface(cardInterface);
    setInspection(null);
    setScanError(null);
  }

  function returnToMenu() {
    if (isScanning) return;
    setSelectedInterface(null);
    setInspection(null);
    setScanError(null);
  }

  return (
    <main className="app-shell">
      <header className="app-header">
        <div className="brand-block">
          <div className="brand-icon"><span /></div>
          <div><p className="eyebrow">EMV LABORATUVARI</p><h1>Card Inspector</h1></div>
        </div>
        <div className="header-actions">
          <div className="theme-switch" role="group" aria-label="Renk teması">
            <button className={theme === "light" ? "theme-active" : ""} onClick={() => setTheme("light")} aria-pressed={theme === "light"}>
              <span>☀</span> Light
            </button>
            <button className={theme === "dark" ? "theme-active" : ""} onClick={() => setTheme("dark")} aria-pressed={theme === "dark"}>
              <span>◐</span> Dark
            </button>
          </div>
          <div className={`status status-${backendStatus}`}><span className="status-dot" />{backendStatusText}</div>
        </div>
      </header>

      {!selectedInterface ? (
        <section className="interface-menu">
          <div className="menu-intro">
            <p className="section-label">HOŞ GELDİN</p>
            <h2>Merhaba, hangi kartı incelemek istersin?</h2>
            <p>Okuma türünü seçtiğinde sana uygun okuyucu ve EMV adımlarının bulunduğu inceleme ekranı açılacak.</p>
          </div>
          <div className="interface-options">
            <button className="interface-card interface-contact" onClick={() => selectInterface("contact")}>
              <span className="interface-icon contact-card-icon"><i /></span>
              <span className="interface-card-copy">
                <small>ÇİPLİ KART</small>
                <strong>Temaslı kart</strong>
                <span>Kartı okuyucu yuvasına takarak PSE, GPO ve AFL kayıtlarını incele.</span>
              </span>
              <span className="interface-flow">PSE → AID → GPO → RECORD</span>
              <span className="interface-arrow">→</span>
            </button>
            <button className="interface-card interface-contactless" onClick={() => selectInterface("contactless")}>
              <span className="interface-icon contactless-card-icon"><i /><i /><i /></span>
              <span className="interface-card-copy">
                <small>NFC / PICC</small>
                <strong>Temassız kart</strong>
                <span>Kartı yaklaştırarak PPSE, PDOL, GPO ve Tag 94 kayıtlarını incele.</span>
              </span>
              <span className="interface-flow">PPSE → AID → GPO → RECORD</span>
              <span className="interface-arrow">→</span>
            </button>
          </div>
          <div className="menu-assurance">
            <span>◇ Salt okunur APDU komutları</span>
            <span>◇ Hassas veri maskeleme</span>
            <span>◇ Ayrıntılı TLV ağacı</span>
          </div>
        </section>
      ) : (
        <>
          <button className="back-button" onClick={returnToMenu} disabled={isScanning}>← Kart türü seçimine dön</button>
          <section className={`scan-console scan-console-${selectedInterface}`}>
            <div className="scan-copy">
              <p className="section-label">{interfaceCopy.eyebrow}</p>
              <h2>{interfaceCopy.title}</h2>
              <p>{interfaceCopy.description}</p>
              <div className="scan-hints"><span>✓ Otomatik okuyucu seçimi</span><span>✓ Salt okunur APDU</span><span>✓ Hassas veri maskeleme</span></div>
            </div>
            <div className="scan-action">
              <div className={`reader-visual reader-${selectedInterface} ${isScanning ? "reader-active" : ""}`}>
                <div className="reader-slot" /><div className="reader-light" />
                {selectedInterface === "contactless" && <div className="nfc-waves"><i /><i /><i /></div>}
              </div>
              <div>
                <strong>{isScanning ? interfaceCopy.waiting : interfaceCopy.ready}</strong>
                <span>{isScanning ? interfaceCopy.instruction : interfaceCopy.idleInstruction}</span>
              </div>
              <button className="button button-primary" onClick={() => void inspectCard()} disabled={backendStatus !== "online" || isScanning}>
                {isScanning ? "İnceleniyor…" : interfaceCopy.button}
              </button>
              {backendStatus !== "online" && <button className="text-button" onClick={() => void checkBackend()}>Bağlantıyı yeniden kontrol et</button>}
            </div>
          </section>

          {isScanning && (
            <section className="scan-progress">
              <span className="progress-spinner" />
              <div><strong>{interfaceCopy.progress}</strong><p>Kart algılama ve EMV komutları tamamlanana kadar bekle.</p></div>
              <div className="progress-track"><span /></div>
            </section>
          )}

          {scanError && <section className="inline-error"><strong>İstek tamamlanamadı</strong><span>{scanError}</span></section>}
          {inspection ? <ResultsDashboard response={inspection} cardInterface={selectedInterface} /> : !isScanning && !scanError && (
            <section className="welcome-grid">
              <div className="welcome-card"><span>01</span><strong>{selectedInterface === "contactless" ? "Kartı yaklaştır" : "Kartı yerleştir"}</strong><p>{selectedInterface === "contactless" ? "Test kartını temassız okuyucunun üzerinde sabit tut." : "Test kartını temaslı okuyucunun yuvasına tam olarak yerleştir."}</p></div>
              <div className="welcome-card"><span>02</span><strong>İncelemeyi başlat</strong><p>Uygun okuyucu otomatik seçilir ve kart en fazla 30 saniye beklenir.</p></div>
              <div className="welcome-card"><span>03</span><strong>Sonuçları keşfet</strong><p>Uygulamaları, APDU adımlarını, AFL kayıtlarını ve açıklamalı EMV tag’lerini incele.</p></div>
            </section>
          )}
        </>
      )}
    </main>
  );
}

export default App;
