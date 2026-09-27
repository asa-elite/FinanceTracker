export type BankName = "CHASE" | "SOFI" | "NAVY_FED" | "FIDELITY";
export type AccountType = "CHECKING" | "SAVING" | "CREDIT_CARD";

export type AuthResponse = {
  message: string;
  token?: string;
  tokenType?: string;
  email?: string;
};

export type BankResponse = {
  id: number;
  name: BankName | string;
};

export type BankDeletePreviewResponse = {
  bankId: number;
  bankName: string;
  accountsDeleted: number;
  transactionsDeleted: number;
  transactionSplitsDeleted: number;
  sharedTransactionsAffected: number;
};

export type BankAccountResponse = {
  account_name: string;
  account_type: AccountType | string;
  balance: number | string;
  bank_account_id: number;
};

export type AllBankAccountResponse = {
  bankId: number;
  bankName: BankName | string;
  accounts: BankAccountResponse[];
};

export type TransactionResponse = {
  transaction_id: number | null;
  transaction_date: string;
  total_amount: number | string;
  num_of_splits: number;
  merchant?: string | null;
  description?: string | null;
  categories_set?: string[] | null;
  bank_account_response?: BankAccountResponse | null;
};

export type StatementPreviewTransaction = {
  totalAmount: number | string;
  description?: string | null;
  merchant?: string | null;
  transactionDate: string;
  splits?: Array<{
    amount: number | string;
    category?: {
      name?: string | null;
      parent?: unknown;
    } | null;
  }> | null;
  bankId?: number | null;
};

export type StatementImportPreviewResponse = {
  bankAccountId: number;
  statementPeriod?: {
    startDate?: string;
    endDate?: string;
  };
  transactions: StatementPreviewTransaction[];
};

export type ConfirmStatementImportPayload = {
  bankAccountId: number;
  statementStartDate: string | null;
  statementEndDate: string | null;
  transactions: TransactionResponse[];
};

export type CreateTransactionPayload = {
  totalAmount: number;
  description: string;
  merchant: string;
  transactionDate: string;
  bankId: number;
  splits: Array<{
    amount: number;
    category: {
      name: string;
      parent: null;
    };
  }>;
};
