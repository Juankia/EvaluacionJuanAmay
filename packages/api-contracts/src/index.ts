// Types mirroring openapi/inquiries-v1.yaml, bff-v1.yaml and common-v1.yaml.
// Kept in sync by test/contract-sync.test.ts.

export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'IDEMPOTENCY_KEY_REQUIRED'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'RESOURCE_NOT_FOUND'
  | 'IDEMPOTENCY_CONFLICT'
  | 'EQUIVALENT_OPERATION_IN_PROGRESS'
  | 'ONBOARDING_CANNOT_CONTINUE_DIGITALLY'
  | 'CORE_UNAVAILABLE'
  | 'CAPACITY_EXCEEDED'
  | 'IDENTITY_PROVIDER_UNAVAILABLE'
  | 'AUDIT_UNAVAILABLE'
  | 'SERVICE_IN_QUARANTINE'
  | 'INTERNAL_ERROR';

export interface FieldViolation {
  field: string;
  reason: string;
}

export interface ErrorResponse {
  code: ErrorCode;
  messageKey: string;
  correlationId?: string | null;
  retryable: boolean;
  retryAfterSeconds?: number | null;
  details: FieldViolation[];
}

export type Freshness = 'FRESH' | 'CACHED' | 'DEGRADED';

export interface Money {
  value: string;
  currency: string;
}

export interface AccountSummary {
  accountId: string;
  maskedNumber: string;
  productType: 'SAVINGS' | 'CHECKING' | 'OTHER';
  status: 'ACTIVE' | 'BLOCKED' | 'OTHER';
  availableBalance: Money;
  ledgerBalance: Money;
}

export interface AccountSummaryResponse {
  items: AccountSummary[];
  asOf: string;
  freshness: Freshness;
}

export interface Movement {
  movementId: string;
  bookingDate: string;
  valueDate: string;
  amount: Money;
  direction: 'DEBIT' | 'CREDIT';
  description: string;
}

export interface MovementPageResponse {
  items: Movement[];
  nextCursor?: string | null;
  asOf: string;
  freshness: Freshness;
}

export interface SessionResponse {
  displayName: string;
  csrfToken: string;
  expiresAt: string;
}

export const ERROR_CODES: readonly ErrorCode[] = [
  'VALIDATION_ERROR',
  'IDEMPOTENCY_KEY_REQUIRED',
  'UNAUTHENTICATED',
  'FORBIDDEN',
  'RESOURCE_NOT_FOUND',
  'IDEMPOTENCY_CONFLICT',
  'EQUIVALENT_OPERATION_IN_PROGRESS',
  'ONBOARDING_CANNOT_CONTINUE_DIGITALLY',
  'CORE_UNAVAILABLE',
  'CAPACITY_EXCEEDED',
  'IDENTITY_PROVIDER_UNAVAILABLE',
  'AUDIT_UNAVAILABLE',
  'SERVICE_IN_QUARANTINE',
  'INTERNAL_ERROR',
];

/** Required keys per type, compared against the OpenAPI "required" lists by test. */
export const REQUIRED_KEYS = {
  ErrorResponse: ['code', 'messageKey', 'retryable', 'details'],
  AccountSummary: [
    'accountId',
    'maskedNumber',
    'productType',
    'status',
    'availableBalance',
    'ledgerBalance',
  ],
  AccountSummaryResponse: ['items', 'asOf', 'freshness'],
  Movement: ['movementId', 'bookingDate', 'valueDate', 'amount', 'direction', 'description'],
  MovementPageResponse: ['items', 'asOf', 'freshness'],
  SessionResponse: ['displayName', 'csrfToken', 'expiresAt'],
} as const satisfies Record<string, readonly string[]>;
