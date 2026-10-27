import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { parse } from 'yaml';
import { ERROR_CODES, REQUIRED_KEYS } from '../src/index';

type Spec = { components: { schemas: Record<string, { required?: string[]; properties?: Record<string, { enum?: string[] }> }> } };

function load(name: string): Spec {
  const path = fileURLToPath(new URL(`../openapi/${name}`, import.meta.url));
  return parse(readFileSync(path, 'utf8')) as Spec;
}

describe('TypeScript types stay in sync with OpenAPI', () => {
  const common = load('common-v1.yaml');
  const inquiries = load('inquiries-v1.yaml');
  const bff = load('bff-v1.yaml');

  it('error codes match', () => {
    expect([...ERROR_CODES].sort()).toEqual(
      [...(common.components.schemas.ErrorResponse.properties!.code.enum ?? [])].sort(),
    );
  });

  it.each(Object.entries(REQUIRED_KEYS))('%s required keys match', (name, keys) => {
    const schema =
      common.components.schemas[name] ?? inquiries.components.schemas[name] ?? bff.components.schemas[name];
    expect(schema, name).toBeDefined();
    expect([...keys].sort()).toEqual([...(schema.required ?? [])].sort());
  });

  it('the transfer boundary contract stays marked as blocked', () => {
    const raw = readFileSync(
      fileURLToPath(new URL('../openapi/transfers-boundary-v1.yaml', import.meta.url)),
      'utf8',
    );
    const spec = parse(raw) as Record<string, unknown>;
    expect(spec['x-status']).toBe('BLOCKED');
  });
});
