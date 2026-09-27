import type {
  AllBankAccountResponse,
  AuthResponse,
  BankDeletePreviewResponse,
  BankName,
  BankResponse,
  ConfirmStatementImportPayload,
  CreateTransactionPayload,
  StatementImportPreviewResponse,
  TransactionResponse
} from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

type ApiOptions = {
  method?: "GET" | "POST" | "DELETE";
  token?: string | null;
  body?: unknown;
};

async function request<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {})
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    const message = typeof payload.message === "string" ? payload.message : "Request failed";
    throw new Error(message);
  }

  return payload as T;
}

async function uploadRequest<T>(path: string, token: string, formData: FormData): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: formData
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    const message = typeof payload.message === "string" ? payload.message : "Upload request failed";
    throw new Error(message);
  }

  return payload as T;
}

export const api = {
  test: () => request<{ message: string }>("/api/auth/test"),
  register: (email: string, password: string) =>
    request<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: { email, password }
    }),
  login: (email: string, password: string) =>
    request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password }
    }),
  listBanks: (token: string) => request<BankResponse[]>("/api/bank/list/banks", { token }),
  listBanksWithAccounts: (token: string) =>
    request<AllBankAccountResponse[]>("/api/bank/banks/accounts", { token }),
  createBank: (token: string, name: BankName) =>
    request<BankResponse>("/api/bank/create/bank", {
      method: "POST",
      token,
      body: { name }
    }),
  previewDeleteBank: (token: string, bankId: number) =>
    request<BankDeletePreviewResponse>(`/api/bank/${bankId}/delete-preview`, { token }),
  deleteBank: (token: string, bankId: number) =>
    request<void>(`/api/bank/${bankId}?confirmed=true`, {
      method: "DELETE",
      token
    }),
  createAccount: (token: string, name: string, type: string, bankId: number) =>
    request("/api/bank/create/account", {
      method: "POST",
      token,
      body: { name, type, bankId }
    }),
  listTransactions: (token: string, seconds: number) =>
    request<TransactionResponse[]>(`/api/txn/list?seconds=${seconds}`, { token }),
  createTransaction: (token: string, payload: CreateTransactionPayload) =>
    request<TransactionResponse>("/api/txn/create", {
      method: "POST",
      token,
      body: payload
    }),
  deleteTransaction: (token: string, transactionId: number) =>
    request<void>("/api/txn/delete", {
      method: "DELETE",
      token,
      body: { transactionId }
    }),
  deleteTransactionsBulk: (token: string, transactionIdList: number[]) =>
    request<void>("/api/txn/delete/bulk", {
      method: "DELETE",
      token,
      body: { transactionIdList }
    }),
  previewStatementImport: (token: string, bankAccountId: number, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return uploadRequest<StatementImportPreviewResponse>(
      `/api/txn/statement-imports/preview/bank-accounts/${bankAccountId}`,
      token,
      formData
    );
  },
  confirmStatementImport: (token: string, payload: ConfirmStatementImportPayload) =>
    request<void>("/api/txn/statement-import/confirm", {
      method: "POST",
      token,
      body: payload
    }),
  shareTransaction: (token: string, transactionId: number, email: string) =>
    request("/api/txn/create/share", {
      method: "POST",
      token,
      body: { transaction_id: transactionId, user_name: email }
    })
};
