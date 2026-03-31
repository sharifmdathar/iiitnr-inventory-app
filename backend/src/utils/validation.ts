import type { FastifyRequest } from 'fastify';
import { filterXSS } from 'xss';

export function isValidHttpUrl(urlString: string): boolean {
  try {
    const url = new URL(urlString);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export function sanitizeString(input: string | null | undefined): string | undefined {
  if (input == null) return undefined;
  return filterXSS(input.trim().slice(0, 2000));
}

export function getClientInfo(request: FastifyRequest): { ip: string; userAgent?: string } {
  return {
    ip: request.ip,
    userAgent: request.headers['user-agent'],
  };
}
