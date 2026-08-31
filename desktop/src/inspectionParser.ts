export type InspectionCommand = {
  name: string;
  apdu: string;
  rawResponse: string;
  responseData: string;
  sw1: string;
  sw2: string;
  statusWord: string;
  status: string;
  duration: string;
  successful: boolean;
  parseStatus: string;
  notes: string[];
  tags: InspectionTag[];
};

export type InspectionApplication = {
  scheme: string;
  aid: string;
  directoryLabel: string;
  name: string;
  preferredName: string;
  pdol: string;
  aip: string;
  afl: string;
};

export type InspectionTag = {
  id: string;
  tag: string;
  name: string;
  type: string;
  length: number;
  value: string;
  sensitive: boolean;
  depth: number;
};

export type InspectionViewModel = {
  reader: string;
  interfaceName: string;
  atr: string;
  protocol: string;
  commands: InspectionCommand[];
  applications: InspectionApplication[];
  tags: InspectionTag[];
};

const sensitiveTags = new Set([
  "57", "5A", "5F20", "9F0B", "9F1F", "9F24", "9F25", "9F5E", "9F60", "9F61"
]);

function fieldValue(line: string, field: string): string | null {
  const match = line.trim().match(new RegExp(`^${field}\\s*:\\s*(.*)$`, "i"));
  return match?.[1]?.trim() ?? null;
}

function valueAfterLabel(lines: string[], label: string): string {
  for (const line of lines) {
    const value = fieldValue(line, label);
    if (value !== null) return value;
  }
  return "—";
}

function emptyApplication(): InspectionApplication {
  return {
    scheme: "Bilinmeyen ödeme ağı",
    aid: "—",
    directoryLabel: "—",
    name: "—",
    preferredName: "—",
    pdol: "—",
    aip: "—",
    afl: "—"
  };
}

export function parseInspectionOutput(output: string): InspectionViewModel {
  const lines = output.replace(/\r/g, "").split("\n");
  const commands: InspectionCommand[] = [];
  const applications: InspectionApplication[] = [];
  const tags: InspectionTag[] = [];
  let currentCommand: InspectionCommand | null = null;
  let currentApplication: InspectionApplication | null = null;

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    const trimmed = line.trim();

    if (/^Application Branch \[\d+\]$/.test(trimmed)) {
      currentApplication = emptyApplication();
      applications.push(currentApplication);
      continue;
    }

    const commandName = fieldValue(line, "Command");
    if (commandName) {
      currentCommand = {
        name: commandName,
        apdu: "—",
        rawResponse: "—",
        responseData: "—",
        sw1: "—",
        sw2: "—",
        statusWord: "—",
        status: "—",
        duration: "—",
        successful: false,
        parseStatus: "—",
        notes: [],
        tags: []
      };
      commands.push(currentCommand);
      continue;
    }

    if (currentCommand) {
      const apdu = fieldValue(line, "APDU");
      const rawResponse = fieldValue(line, "Raw Response");
      const responseData = fieldValue(line, "Response Data");
      const sw1 = fieldValue(line, "SW1");
      const sw2 = fieldValue(line, "SW2");
      const statusWord = fieldValue(line, "Status Word");
      const status = fieldValue(line, "Status");
      const duration = fieldValue(line, "Duration");
      if (apdu !== null) currentCommand.apdu = apdu;
      if (rawResponse !== null) currentCommand.rawResponse = rawResponse;
      if (responseData !== null) currentCommand.responseData = responseData;
      if (sw1 !== null) currentCommand.sw1 = sw1;
      if (sw2 !== null) currentCommand.sw2 = sw2;
      if (statusWord !== null) {
        currentCommand.statusWord = statusWord;
        currentCommand.successful = statusWord === "9000";
      }
      if (status !== null) currentCommand.status = status;
      if (duration !== null) currentCommand.duration = duration;

      const parseStatus = trimmed.match(/^(?:TLV|Record|FCI|GPO) Parsing\s*:\s*(.*)$/i)?.[1];
      if (parseStatus) currentCommand.parseStatus = parseStatus;
      const note = trimmed.match(/^(?:AFL Entry|Reason|Parse Error|PSE Directory SFI)\s*:\s*(.*)$/i)?.[0];
      if (note) currentCommand.notes.push(note);
    }

    if (currentApplication) {
      const scheme = fieldValue(line, "Scheme");
      const aid = fieldValue(line, "AID");
      const directoryLabel = fieldValue(line, "Directory Label");
      const applicationName = fieldValue(line, "Application");
      const preferredName = fieldValue(line, "Preferred Name");
      const pdol = fieldValue(line, "PDOL");
      const aip = fieldValue(line, "AIP \\(82\\)");
      const afl = fieldValue(line, "AFL \\(94\\)");
      if (scheme !== null) currentApplication.scheme = scheme;
      if (aid !== null) currentApplication.aid = aid;
      if (directoryLabel !== null) currentApplication.directoryLabel = directoryLabel;
      if (applicationName !== null) currentApplication.name = applicationName;
      if (preferredName !== null) currentApplication.preferredName = preferredName;
      if (pdol !== null) currentApplication.pdol = pdol;
      if (aip !== null) currentApplication.aip = aip;
      if (afl !== null) currentApplication.afl = afl;
    }

    const tagMatch = trimmed.match(
      /^-\s+([0-9A-F]+)\s+\|\s+(.+?)\s+\|\s+(constructed|primitive)\s+\|\s+length=(\d+)/
    );
    if (tagMatch) {
      const tag = tagMatch[1];
      const nextValue = lines[index + 1]?.trim().match(/^Value:\s*(.*)$/)?.[1] ?? "—";
      const parsedTag: InspectionTag = {
        id: `${index}-${tag}`,
        tag,
        name: tagMatch[2],
        type: tagMatch[3],
        length: Number(tagMatch[4]),
        value: nextValue,
        sensitive: sensitiveTags.has(tag),
        depth: Math.floor((line.match(/^(\s*)-/)?.[1].length ?? 0) / 2)
      };
      tags.push(parsedTag);
      currentCommand?.tags.push(parsedTag);
    }
  }

  return {
    reader: valueAfterLabel(lines, "Reader"),
    interfaceName: valueAfterLabel(lines, "Interface"),
    atr: valueAfterLabel(lines, "ATR"),
    protocol: valueAfterLabel(lines, "Protocol"),
    commands,
    applications,
    tags
  };
}

export function displayTagValue(tag: InspectionTag): string {
  if (!tag.sensitive || tag.value === "—") return tag.value;
  const suffix = tag.value.slice(-4);
  return `••••••••${suffix ? ` ${suffix}` : ""}`;
}
