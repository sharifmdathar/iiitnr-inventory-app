import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { getTableConfig } from 'drizzle-orm/pg-core';
import {
  auditLog,
  component,
  notification,
  request,
  requestEmail,
  requestItem,
  user,
} from '../src/drizzle/schema.js';

const TABLE_NAME = Symbol.for('drizzle:Name');

const EXCLUDED_TABLES = new Set(['_prisma_migrations']);

const RELATION_LABELS: Record<string, string> = {
  'Request.userId': 'submits',
  'Request.targetFacultyId': 'supervises',
  'RequestItem.requestId': 'contains',
  'RequestItem.componentId': 'included_in',
  'AuditLog.userId': 'triggers',
  'Notification.recipientId': 'notified',
  'Notification.requestId': 'notifies about',
  'RequestEmail.requestId': 'emailed for',
  'RequestEmail.recipientId': 'emailed',
};

const TABLES = [user, request, requestItem, component, auditLog, notification, requestEmail];

type Column = ReturnType<typeof getTableConfig>['columns'][number];

function mermaidType(column: Column): string {
  if (column.enumValues?.length) {
    try {
      return column.getSQLType();
    } catch {
      return 'text';
    }
  }

  switch (column.columnType) {
    case 'PgText':
      return 'text';
    case 'PgVarchar':
      return 'varchar';
    case 'PgInteger':
    case 'PgSmallInt':
    case 'PgBigInt':
      return 'integer';
    case 'PgNumeric':
    case 'PgReal':
    case 'PgDoublePrecision':
      return 'numeric';
    case 'PgBoolean':
      return 'boolean';
    case 'PgTimestamp':
      return 'timestamp';
    case 'PgDate':
      return 'date';
    case 'PgUUID':
      return 'uuid';
    case 'PgJson':
      return 'json';
    default:
      return 'text';
  }
}

interface Relation {
  from: string;
  to: string;
  label: string;
  localColumn: string;
}

function buildRelations(): Relation[] {
  const relations: Relation[] = [];

  for (const table of TABLES) {
    const config = getTableConfig(table);
    const tableName = config.name;

    for (const fk of config.foreignKeys) {
      const ref = fk.reference();
      const foreignTable = ref.foreignTable as unknown as Record<symbol, string>;
      const target = foreignTable?.[TABLE_NAME] ?? config.name;
      if (EXCLUDED_TABLES.has(target)) continue;

      const localColumn = ref.columns[0]?.name ?? 'unknown';
      const key = `${tableName}.${localColumn}`;

      relations.push({
        from: tableName,
        to: target,
        label: RELATION_LABELS[key] ?? localColumn,
        localColumn,
      });
    }
  }

  return relations;
}

function isUnique(config: ReturnType<typeof getTableConfig>, column: Column): boolean {
  if (column.isUnique) return true;
  return config.indexes.some(
    (index) =>
      index.config.unique &&
      // Only a single-column unique index makes the column itself unique.
      index.config.columns.length === 1 &&
      index.config.columns.some((c) => 'name' in c && c.name === column.name),
  );
}

function buildDiagram(): string {
  const lines: string[] = ['```mermaid', 'erDiagram'];

  for (const table of TABLES) {
    const config = getTableConfig(table);
    if (EXCLUDED_TABLES.has(config.name)) continue;

    lines.push(`    ${config.name} {`);

    for (const column of config.columns) {
      const keys: string[] = [];
      if (column.primary) keys.push('PK');
      if (isUnique(config, column)) keys.push('UK');
      if (
        config.foreignKeys.some((fk) => fk.reference().columns.some((c) => c.name === column.name))
      )
        keys.push('FK');

      const comment = column.enumValues?.length ? ` "${column.enumValues.join('|')}"` : '';

      lines.push(
        `        ${mermaidType(column)} ${column.name}${keys.length ? ` ${keys.join(', ')}` : ''}${comment}`,
      );
    }

    lines.push('    }');
  }

  for (const relation of buildRelations()) {
    lines.push(`    ${relation.to} ||--o{ ${relation.from} : "${relation.label}"`);
  }

  lines.push('```');
  return lines.join('\n');
}

function buildDocument(): string {
  return `# Database Schema & Entities

The PostgreSQL schema is powered by [Drizzle ORM](https://orm.drizzle.team/). The
diagram below is **generated from \`backend/src/drizzle/schema.ts\`** — regenerate it
with:

\`\`\`bash
just gen-schema
\`\`\`

> [!WARNING]
> This file is generated. Do not edit it by hand — change the schema in
> \`backend/src/drizzle/schema.ts\` and run the generator again. CI fails if this
> page is out of date.

${buildDiagram()}

## Notes

- The legacy \`_prisma_migrations\` table is excluded: it is a leftover from the
  Prisma era and is not part of the application domain.
- \`writtenOffQuantity\` on \`RequestItem\` tracks stock that was issued but never
  returned before a request expired. It is subtracted from \`Component.totalQuantity\`
  but not from \`availableQuantity\` (the stock was already deducted at issue time).
- \`renewalReminderSentAt\` on \`Request\` guarantees the 7-day renewal reminder
  email is sent exactly once per request.
- \`Notification\` rows are the in-app notification feed; \`read\` drives the unread
  badge in the client.
`;
}

function buildRelationsDiagram(): string {
  const lines: string[] = ['```mermaid', 'erDiagram'];
  for (const relation of buildRelations()) {
    lines.push(`    ${relation.to} ||--o{ ${relation.from} : "${relation.label}"`);
  }
  lines.push('```');
  return lines.join('\n');
}

const README_BEGIN = '<!-- BEGIN GENERATED: schema-relations -->';
const README_END = '<!-- END GENERATED: schema-relations -->';

/** Keeps the relationship snippet in the README in sync with the schema. */
function updateReadmeSnippet(content: string, snippet: string): string {
  const begin = content.indexOf(README_BEGIN);
  const end = content.indexOf(README_END);
  if (begin === -1 || end === -1 || end < begin) {
    console.warn(`⚠️  Could not find the generated snippet markers in README.md`);
    return content;
  }
  return content.slice(0, begin + README_BEGIN.length) + '\n' + snippet + '\n' + content.slice(end);
}

function main() {
  const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
  const outPath = join(repoRoot, 'docs', 'schema.md');
  const readmePath = join(repoRoot, 'README.md');
  const content = buildDocument();
  const relations = buildRelationsDiagram();
  const readme = readFileSync(readmePath, 'utf8');
  const updatedReadme = updateReadmeSnippet(readme, relations);

  if (process.argv.includes('--check')) {
    let current = '';
    try {
      current = readFileSync(outPath, 'utf8');
    } catch {
      console.error(`❌ ${outPath} is missing. Run: bun run scripts/generate-schema-docs.ts`);
      process.exitCode = 1;
      return;
    }
    if (current !== content) {
      console.error('❌ docs/schema.md is out of date. Run: just gen-schema');
      process.exitCode = 1;
      return;
    }
    if (updatedReadme !== readme) {
      console.error('❌ The generated snippet in README.md is out of date. Run: just gen-schema');
      process.exitCode = 1;
      return;
    }
    console.log('✅ docs/schema.md and the README snippet are up to date');
    return;
  }

  mkdirSync(dirname(outPath), { recursive: true });
  writeFileSync(outPath, content);
  writeFileSync(readmePath, updatedReadme);
  console.log(`✅ Wrote ${outPath}`);
  console.log(`✅ Updated the generated snippet in ${readmePath}`);
}

main();
