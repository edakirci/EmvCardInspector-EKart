const { app, BrowserWindow, dialog, ipcMain } = require("electron");
const path = require("node:path");
const fs = require("node:fs/promises");

ipcMain.handle("csv:save", async (_event, { fileName, content }) => {
  if (typeof fileName !== "string" || typeof content !== "string") {
    throw new TypeError("Invalid CSV save request");
  }
  const safeFileName = path.basename(fileName).toLowerCase().endsWith(".csv")
    ? path.basename(fileName)
    : `${path.basename(fileName)}.csv`;
  const result = await dialog.showSaveDialog({
    title: "CSV raporunu kaydet",
    defaultPath: safeFileName,
    filters: [{ name: "CSV dosyası", extensions: ["csv"] }]
  });
  if (result.canceled || !result.filePath) return { saved: false };
  await fs.writeFile(result.filePath, content, "utf8");
  return { saved: true, filePath: result.filePath };
});

function createWindow() {
  const window = new BrowserWindow({
    width: 1180,
    height: 760,
    minWidth: 900,
    minHeight: 620,
    title: "EMV Card Inspector",
    backgroundColor: "#0b1220",
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  if (app.isPackaged) {
    window.loadFile(path.join(__dirname, "..", "dist", "index.html"));
  } else {
    window.loadURL("http://127.0.0.1:5173");
  }
}

app.whenReady().then(() => {
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
