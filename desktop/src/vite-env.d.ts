/// <reference types="vite/client" />

interface Window {
  emvDesktop?: {
    saveCsv(fileName: string, content: string): Promise<{ saved: boolean; filePath?: string }>;
  };
}
