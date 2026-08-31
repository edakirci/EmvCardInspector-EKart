const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("emvDesktop", {
  saveCsv: (fileName, content) => ipcRenderer.invoke("csv:save", { fileName, content })
});
