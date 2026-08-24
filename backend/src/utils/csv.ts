/**
 * Minimal RFC 4180 compliant CSV parser.
 *
 * Supports quoted fields, escaped double quotes (""), commas and newlines
 * inside quotes, CRLF/LF line endings, and trailing newlines.
 */
export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = '';
  let inQuotes = false;
  let i = 0;

  const endField = () => {
    row.push(field);
    field = '';
  };

  const endRow = () => {
    endField();
    rows.push(row);
    row = [];
  };

  while (i < text.length) {
    const ch = text[i];
    const next = text[i + 1];

    if (inQuotes) {
      if (ch === '"') {
        if (next === '"') {
          field += '"';
          i += 2;
          continue;
        }        inQuotes = false;
        i += 1;
        continue;
      }
      field += ch;
      i += 1;
      continue;
    }

    if (ch === '"' && field === '') {
      inQuotes = true;
      i += 1;
      continue;
    }

    if (ch === ',') {
      endField();
      i += 1;
      continue;
    }

    if (ch === '\r' || ch === '\n') {
      if (ch === '\r' && next === '\n') {
        i += 1;
      }
      endRow();
      i += 1;
      continue;
    }

    field += ch;
    i += 1;
  }

  // Flush the final field/row when input does not end with a newline.
  if (field !== '' || row.length > 0) {
    endRow();
  }

  return rows.filter((r) => !(r.length === 1 && (r[0] ?? '').trim() === ''));
}
