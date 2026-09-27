import type { FormEvent, ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import {
  ArrowDownUp,
  BadgeDollarSign,
  Banknote,
  Check,
  CreditCard,
  FileText,
  Landmark,
  LayoutDashboard,
  Loader2,
  LockKeyhole,
  LogOut,
  Plus,
  RefreshCcw,
  Send,
  ShieldCheck,
  Split,
  Trash2,
  Upload,
  UserCircle,
  WalletCards
} from "lucide-react";
import { api } from "./api";
import type {
  AccountType,
  AllBankAccountResponse,
  BankDeletePreviewResponse,
  BankName,
  BankResponse,
  CreateTransactionPayload,
  StatementPreviewTransaction,
  StatementImportPreviewResponse,
  TransactionResponse
} from "./types";

type View = "summary" | "bank" | "transaction" | "split" | "account";

const BANKS: BankName[] = ["CHASE", "SOFI", "NAVY_FED", "FIDELITY"];
const ACCOUNT_TYPES: AccountType[] = ["CHECKING", "SAVING", "CREDIT_CARD"];

const navItems: Array<{ id: View; label: string; icon: ReactNode }> = [
  { id: "summary", label: "Summary", icon: <LayoutDashboard size={18} /> },
  { id: "bank", label: "Bank", icon: <Landmark size={18} /> },
  { id: "transaction", label: "Transaction", icon: <BadgeDollarSign size={18} /> },
  { id: "split", label: "Split", icon: <Split size={18} /> },
  { id: "account", label: "Account", icon: <UserCircle size={18} /> }
];

function money(value: number | string) {
  const numeric = Number(value);
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD"
  }).format(Number.isFinite(numeric) ? numeric : 0);
}

function bankLabel(value: string) {
  return value.replaceAll("_", " ");
}

function localDateTimeValue() {
  const date = new Date();
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}

function dateOnly(value?: string) {
  if (!value) {
    return null;
  }

  return value.slice(0, 10);
}

function previewToTransactionResponse(
  transaction: StatementPreviewTransaction,
  bankAccountId: number
): TransactionResponse {
  const categories = transaction.splits
    ?.map((split) => split.category?.name)
    .filter((name): name is string => Boolean(name)) ?? [];

  return {
    transaction_id: null,
    transaction_date: transaction.transactionDate,
    total_amount: transaction.totalAmount,
    num_of_splits: transaction.splits?.length ?? 0,
    merchant: transaction.merchant ?? null,
    description: transaction.description ?? null,
    categories_set: categories,
    bank_account_response: {
      account_name: "Statement import",
      account_type: "CHECKING",
      balance: 0,
      bank_account_id: bankAccountId
    }
  };
}

function transactionTitle(transaction: TransactionResponse | StatementPreviewTransaction) {
  if ("totalAmount" in transaction) {
    const categories =
      transaction.splits
        ?.map((split) => split.category?.name)
        .filter((name): name is string => Boolean(name)) ?? [];
    return transaction.merchant || categories.join(", ") || "Parsed transaction";
  }

  return transaction.merchant || transaction.categories_set?.join(", ") || "Transaction";
}

function transactionAmount(transaction: TransactionResponse | StatementPreviewTransaction) {
  return "totalAmount" in transaction ? transaction.totalAmount : transaction.total_amount;
}

function transactionDateValue(transaction: TransactionResponse | StatementPreviewTransaction) {
  return "transactionDate" in transaction ? transaction.transactionDate : transaction.transaction_date;
}

export function App() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(() => localStorage.getItem("financeTrackerToken") ?? "");
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [activeView, setActiveView] = useState<View>("summary");
  const [status, setStatus] = useState("Ready.");
  const [apiStatus, setApiStatus] = useState<"checking" | "online" | "offline">("checking");
  const [isBusy, setIsBusy] = useState(false);
  const [banks, setBanks] = useState<BankResponse[]>([]);
  const [bankAccounts, setBankAccounts] = useState<AllBankAccountResponse[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [selectedBank, setSelectedBank] = useState<BankName>("CHASE");
  const [accountName, setAccountName] = useState("");
  const [accountType, setAccountType] = useState<AccountType>("CHECKING");
  const [accountBankId, setAccountBankId] = useState("");
  const [rangeSeconds, setRangeSeconds] = useState(2592000);
  const [merchant, setMerchant] = useState("");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState("");
  const [transactionDate, setTransactionDate] = useState(localDateTimeValue());
  const [transactionAccountId, setTransactionAccountId] = useState("");
  const [shareTransactionId, setShareTransactionId] = useState("");
  const [shareEmail, setShareEmail] = useState("");
  const [deletePreview, setDeletePreview] = useState<BankDeletePreviewResponse | null>(null);
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [transactionDeleteTarget, setTransactionDeleteTarget] = useState<TransactionResponse | null>(null);
  const [bulkDeleteTargets, setBulkDeleteTargets] = useState<TransactionResponse[] | null>(null);
  const [selectedTransactionIds, setSelectedTransactionIds] = useState<number[]>([]);
  const [statementAccountId, setStatementAccountId] = useState("");
  const [statementFile, setStatementFile] = useState<File | null>(null);
  const [statementPreview, setStatementPreview] = useState<StatementImportPreviewResponse | null>(null);

  const isAuthed = Boolean(token);

  const flatAccounts = useMemo(
    () =>
      bankAccounts.flatMap((bank) =>
        bank.accounts.map((account) => ({
          ...account,
          bankId: bank.bankId,
          bankName: bank.bankName
        }))
      ),
    [bankAccounts]
  );

  const totalBalance = flatAccounts.reduce((sum, account) => sum + Number(account.balance), 0);
  const visibleMovement = transactions.reduce((sum, txn) => sum + Math.abs(Number(txn.total_amount)), 0);

  useEffect(() => {
    api
      .test()
      .then((result) => {
        setApiStatus("online");
        setStatus(result.message);
      })
      .catch(() => {
        setApiStatus("offline");
        setStatus("Backend is offline.");
      });
  }, []);

  useEffect(() => {
    if (!isAuthed) {
      return;
    }

    void refreshAll("Dashboard synced.");
  }, [isAuthed, rangeSeconds]);

  async function run(action: () => Promise<void>, success: string) {
    setIsBusy(true);
    try {
      await action();
      setStatus(success);
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Something went wrong.");
    } finally {
      setIsBusy(false);
    }
  }

  async function refreshAll(success = "Fresh data loaded.") {
    await run(async () => {
      const [bankList, accountList, txnList] = await Promise.all([
        api.listBanks(token),
        api.listBanksWithAccounts(token),
        api.listTransactions(token, rangeSeconds)
      ]);

      setBanks(bankList);
      setBankAccounts(accountList);
      setTransactions(txnList);
      setSelectedTransactionIds((ids) => ids.filter((id) => txnList.some((txn) => txn.transaction_id === id)));

      if (!accountBankId && bankList[0]) {
        setAccountBankId(String(bankList[0].id));
      }

      const firstAccount = accountList.flatMap((bank) => bank.accounts)[0];
      if (!transactionAccountId && firstAccount) {
        setTransactionAccountId(String(firstAccount.bank_account_id));
      }
      if (!statementAccountId && firstAccount) {
        setStatementAccountId(String(firstAccount.bank_account_id));
      }
    }, success);
  }

  function handleAuth(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      const result = authMode === "login" ? await api.login(email, password) : await api.register(email, password);

      if (result.token) {
        localStorage.setItem("financeTrackerToken", result.token);
        setToken(result.token);
        setActiveView("summary");
      }

      if (authMode === "register") {
        setAuthMode("login");
      }
    }, authMode === "login" ? "Signed in." : "Account created. Sign in to continue.");
  }

  function handleLogout() {
    localStorage.removeItem("financeTrackerToken");
    setToken("");
    setBanks([]);
    setBankAccounts([]);
    setTransactions([]);
    setPassword("");
    setActiveView("summary");
    setStatus("Signed out.");
  }

  function handleCreateBank(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      await api.createBank(token, selectedBank);
      await refreshAll(`${bankLabel(selectedBank)} connected.`);
    }, `${bankLabel(selectedBank)} connected.`);
  }

  function handleCreateAccount(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      await api.createAccount(token, accountName, accountType, Number(accountBankId));
      await refreshAll("Account created.");
      setAccountName("");
    }, "Account created.");
  }

  function handleCreateTransaction(event: FormEvent) {
    event.preventDefault();
    const payload: CreateTransactionPayload = {
      totalAmount: Number(amount),
      merchant,
      description,
      transactionDate: transactionDate.length === 16 ? `${transactionDate}:00` : transactionDate,
      bankId: Number(transactionAccountId),
      splits: [
        {
          amount: Number(amount),
          category: {
            name: category,
            parent: null
          }
        }
      ]
    };

    void run(async () => {
      await api.createTransaction(token, payload);
      await refreshAll("Transaction posted.");
      setMerchant("");
      setDescription("");
      setAmount("");
      setCategory("");
    }, "Transaction posted.");
  }

  function handleShare(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      await api.shareTransaction(token, Number(shareTransactionId), shareEmail);
      setShareTransactionId("");
      setShareEmail("");
    }, "Share request created.");
  }

  function handleStatementPreview(event: FormEvent) {
    event.preventDefault();

    if (!statementFile) {
      setStatus("Choose a PDF statement before previewing import.");
      return;
    }

    void run(async () => {
      const preview = await api.previewStatementImport(token, Number(statementAccountId), statementFile);
      setStatementPreview(preview);
    }, "Statement preview generated.");
  }

  function handleConfirmStatementImport() {
    if (!statementPreview) {
      setStatus("Preview a statement before confirming import.");
      return;
    }

    void run(async () => {
      await api.confirmStatementImport(token, {
        bankAccountId: statementPreview.bankAccountId,
        statementStartDate: dateOnly(statementPreview.statementPeriod?.startDate),
        statementEndDate: dateOnly(statementPreview.statementPeriod?.endDate),
        transactions: statementPreview.transactions.map((transaction) =>
          previewToTransactionResponse(transaction, statementPreview.bankAccountId)
        )
      });
      setStatementPreview(null);
      setStatementFile(null);
      await refreshAll("Statement transactions imported.");
    }, "Statement transactions imported.");
  }

  function showUnavailableDelete(kind: string) {
    setStatus(`Delete ${kind} needs a backend DELETE endpoint before the frontend can wire it safely.`);
  }

  function requestTransactionDelete(transaction: TransactionResponse) {
    if (!transaction.transaction_id) {
      setStatus("This transaction has no persisted id yet.");
      return;
    }

    setTransactionDeleteTarget(transaction);
  }

  function cancelTransactionDelete() {
    setTransactionDeleteTarget(null);
    setStatus("Delete cancelled.");
  }

  function confirmTransactionDelete() {
    if (!transactionDeleteTarget?.transaction_id) {
      return;
    }

    const target = transactionDeleteTarget;
    void run(async () => {
      await api.deleteTransaction(token, target.transaction_id as number);
      setTransactionDeleteTarget(null);
      await refreshAll("Transaction deleted.");
    }, "Transaction deleted.");
  }

  function toggleTransactionSelection(transactionId: number, selected: boolean) {
    setSelectedTransactionIds((ids) => {
      if (selected) {
        return ids.includes(transactionId) ? ids : [...ids, transactionId];
      }

      return ids.filter((id) => id !== transactionId);
    });
  }

  function selectAllVisibleTransactions() {
    const visibleIds = transactions
      .map((transaction) => transaction.transaction_id)
      .filter((id): id is number => typeof id === "number");
    setSelectedTransactionIds(visibleIds);
  }

  function clearTransactionSelection() {
    setSelectedTransactionIds([]);
  }

  function requestBulkTransactionDelete() {
    const targets = transactions.filter(
      (transaction) =>
        typeof transaction.transaction_id === "number" && selectedTransactionIds.includes(transaction.transaction_id)
    );

    if (!targets.length) {
      setStatus("Select at least one persisted transaction first.");
      return;
    }

    setBulkDeleteTargets(targets);
  }

  function cancelBulkTransactionDelete() {
    setBulkDeleteTargets(null);
    setStatus("Bulk delete cancelled.");
  }

  function confirmBulkTransactionDelete() {
    if (!bulkDeleteTargets?.length) {
      return;
    }

    const ids = bulkDeleteTargets
      .map((transaction) => transaction.transaction_id)
      .filter((id): id is number => typeof id === "number");

    void run(async () => {
      await api.deleteTransactionsBulk(token, ids);
      setBulkDeleteTargets(null);
      setSelectedTransactionIds([]);
      await refreshAll(`${ids.length} transactions deleted.`);
    }, `${ids.length} transactions deleted.`);
  }

  function requestBankDelete(bankId: number) {
    void run(async () => {
      const preview = await api.previewDeleteBank(token, bankId);
      setDeletePreview(preview);
      setDeleteConfirmation("");
    }, "Review the delete impact before confirming.");
  }

  function cancelBankDelete() {
    setDeletePreview(null);
    setDeleteConfirmation("");
    setStatus("Delete cancelled.");
  }

  function confirmBankDelete() {
    if (!deletePreview) {
      return;
    }

    const preview = deletePreview;
    void run(async () => {
      await api.deleteBank(token, preview.bankId);
      setDeletePreview(null);
      setDeleteConfirmation("");
      await refreshAll(`${bankLabel(preview.bankName)} deleted.`);
    }, `${bankLabel(preview.bankName)} deleted.`);
  }

  if (!isAuthed) {
    return (
      <main className="login-shell">
        <section className="login-card">
          <div className="brand-lockup">
            <div className="brand-mark">
              <WalletCards size={28} />
            </div>
            <div>
              <span>Finance Tracker</span>
              <h1>Sign in</h1>
            </div>
          </div>

          <form onSubmit={handleAuth} className="stack">
            <div className="segmented">
              <button type="button" className={authMode === "login" ? "active" : ""} onClick={() => setAuthMode("login")}>
                Login
              </button>
              <button
                type="button"
                className={authMode === "register" ? "active" : ""}
                onClick={() => setAuthMode("register")}
              >
                Register
              </button>
            </div>
            <label>
              Email
              <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
            </label>
            <label>
              Password
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
            </label>
            <button className="primary-action full" type="submit" disabled={isBusy}>
              {isBusy ? <Loader2 className="spin" size={18} /> : <LockKeyhole size={18} />}
              {authMode === "login" ? "Enter dashboard" : "Create account"}
            </button>
          </form>

          <div className="login-status">
            <span className={`health ${apiStatus}`}>
              <span />
              {apiStatus === "checking" ? "Checking API" : apiStatus === "online" ? "API online" : "API offline"}
            </span>
            <strong>{status}</strong>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand-line">
          <div className="brand-mark small">
            <WalletCards size={20} />
          </div>
          <div>
            <span>Finance Tracker</span>
            <strong>Financial cockpit</strong>
          </div>
        </div>

        <nav className="nav-tabs" aria-label="Primary navigation">
          {navItems.map((item) => (
            <button
              key={item.id}
              type="button"
              className={activeView === item.id ? "active" : ""}
              onClick={() => setActiveView(item.id)}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </nav>

        <div className="topbar-actions">
          <span className={`health ${apiStatus}`}>
            <span />
            {apiStatus === "online" ? "API online" : "API offline"}
          </span>
          <button className="icon-action" type="button" onClick={() => void refreshAll()} disabled={isBusy} title="Sync API">
            {isBusy ? <Loader2 className="spin" size={18} /> : <RefreshCcw size={18} />}
          </button>
          <button className="icon-action" type="button" onClick={handleLogout} title="Sign out">
            <LogOut size={18} />
          </button>
        </div>
      </header>

      <section className="status-strip">
        <span>Status</span>
        <strong>{status}</strong>
      </section>

      {activeView === "summary" && (
        <section className="dashboard">
          <MetricGrid
            totalBalance={totalBalance}
            visibleMovement={visibleMovement}
            bankCount={banks.length}
            transactionCount={transactions.length}
          />
          <AccountPortfolio bankAccounts={bankAccounts} onDeleteAccount={showUnavailableDelete} />
          <Ledger
            transactions={transactions}
            rangeSeconds={rangeSeconds}
            onRangeChange={setRangeSeconds}
            onDeleteTransaction={requestTransactionDelete}
            selectedTransactionIds={selectedTransactionIds}
            onToggleTransaction={toggleTransactionSelection}
            onSelectAllVisible={selectAllVisibleTransactions}
            onClearSelection={clearTransactionSelection}
            onBulkDelete={requestBulkTransactionDelete}
          />
        </section>
      )}

      {activeView === "bank" && (
        <section className="management-grid">
          <section className="board">
            <SectionTitle eyebrow="Bank" title="Connected banks" />
            <div className="bank-list">
              {banks.length ? (
                banks.map((bank) => (
                  <article className="simple-row" key={bank.id}>
                    <Landmark size={20} />
                    <div>
                      <strong>{bankLabel(bank.name)}</strong>
                      <span>Bank ID #{bank.id}</span>
                    </div>
                    <button className="row-action danger" type="button" onClick={() => requestBankDelete(bank.id)}>
                      <Trash2 size={16} />
                      Delete
                    </button>
                  </article>
                ))
              ) : (
                <EmptyState text="No banks returned by the API yet." />
              )}
            </div>
          </section>

          <section className="board">
            <SectionTitle eyebrow="Create" title="Add a bank" />
            <form onSubmit={handleCreateBank} className="stack">
              <label>
                Bank
                <select value={selectedBank} onChange={(event) => setSelectedBank(event.target.value as BankName)}>
                  {BANKS.map((bank) => (
                    <option key={bank} value={bank}>
                      {bankLabel(bank)}
                    </option>
                  ))}
                </select>
              </label>
              <button className="primary-action full" disabled={isBusy}>
                <Plus size={18} />
                Add bank
              </button>
            </form>
          </section>

          <section className="board wide-board">
            <SectionTitle eyebrow="Accounts" title="All bank accounts" />
            <AccountTable accounts={flatAccounts} onDeleteAccount={showUnavailableDelete} />
          </section>

          <section className="board wide-board">
            <SectionTitle eyebrow="Create" title="New account" />
            <AccountForm
              accountName={accountName}
              accountType={accountType}
              accountBankId={accountBankId}
              banks={banks}
              isBusy={isBusy}
              onSubmit={handleCreateAccount}
              onNameChange={setAccountName}
              onTypeChange={setAccountType}
              onBankChange={setAccountBankId}
            />
          </section>
        </section>
      )}

      {activeView === "transaction" && (
        <section className="management-grid">
          <section className="board wide-board">
            <SectionTitle eyebrow="Create" title="New transaction" />
            <TransactionForm
              merchant={merchant}
              description={description}
              amount={amount}
              category={category}
              transactionDate={transactionDate}
              transactionAccountId={transactionAccountId}
              accounts={flatAccounts}
              isBusy={isBusy}
              onSubmit={handleCreateTransaction}
              onMerchantChange={setMerchant}
              onDescriptionChange={setDescription}
              onAmountChange={setAmount}
              onCategoryChange={setCategory}
              onDateChange={setTransactionDate}
              onAccountChange={setTransactionAccountId}
            />
          </section>

          <section className="board wide-board">
            <SectionTitle eyebrow="Import" title="Preview statement PDF" />
            <StatementPreviewPanel
              accounts={flatAccounts}
              accountId={statementAccountId}
              selectedFile={statementFile}
              preview={statementPreview}
              isBusy={isBusy}
              onAccountChange={setStatementAccountId}
              onFileChange={setStatementFile}
              onSubmit={handleStatementPreview}
              onConfirm={handleConfirmStatementImport}
            />
          </section>

          <section className="board wide-board">
            <Ledger
              transactions={transactions}
              rangeSeconds={rangeSeconds}
              onRangeChange={setRangeSeconds}
              onDeleteTransaction={requestTransactionDelete}
              selectedTransactionIds={selectedTransactionIds}
              onToggleTransaction={toggleTransactionSelection}
              onSelectAllVisible={selectAllVisibleTransactions}
              onClearSelection={clearTransactionSelection}
              onBulkDelete={requestBulkTransactionDelete}
            />
          </section>
        </section>
      )}

      {activeView === "split" && (
        <section className="management-grid single">
          <section className="board">
            <SectionTitle eyebrow="Split" title="Create share request" />
            <form onSubmit={handleShare} className="stack">
              <label>
                Transaction split ID
                <input
                  inputMode="numeric"
                  value={shareTransactionId}
                  onChange={(event) => setShareTransactionId(event.target.value)}
                  required
                />
              </label>
              <label>
                Recipient email
                <input type="email" value={shareEmail} onChange={(event) => setShareEmail(event.target.value)} required />
              </label>
              <button className="primary-action full" disabled={isBusy}>
                <Send size={18} />
                Send share
              </button>
            </form>
          </section>
        </section>
      )}

      {activeView === "account" && (
        <section className="management-grid single">
          <section className="board">
            <SectionTitle eyebrow="Session" title="Account access" />
            <div className="session-box">
              <div className="token-lock">
                <Check size={18} />
                Bearer token stored locally
              </div>
              <button className="ghost-action full" type="button" onClick={handleLogout}>
                <LogOut size={18} />
                Sign out
              </button>
            </div>
          </section>

          <section className="board">
            <SectionTitle eyebrow="Profile" title="Signed-in email" />
            <div className="simple-row">
              <ShieldCheck size={20} />
              <div>
                <strong>{email || "Authenticated user"}</strong>
                <span>Identity comes from your JWT-backed Spring Security session.</span>
              </div>
            </div>
          </section>
        </section>
      )}

      {deletePreview && (
        <BankDeleteDialog
          preview={deletePreview}
          confirmation={deleteConfirmation}
          isBusy={isBusy}
          onConfirmationChange={setDeleteConfirmation}
          onCancel={cancelBankDelete}
          onConfirm={confirmBankDelete}
        />
      )}

      {transactionDeleteTarget && (
        <TransactionDeleteDialog
          transaction={transactionDeleteTarget}
          isBusy={isBusy}
          onCancel={cancelTransactionDelete}
          onConfirm={confirmTransactionDelete}
        />
      )}

      {bulkDeleteTargets && (
        <BulkTransactionDeleteDialog
          transactions={bulkDeleteTargets}
          isBusy={isBusy}
          onCancel={cancelBulkTransactionDelete}
          onConfirm={confirmBulkTransactionDelete}
        />
      )}
    </main>
  );
}

function BankDeleteDialog({
  preview,
  confirmation,
  isBusy,
  onConfirmationChange,
  onCancel,
  onConfirm
}: {
  preview: BankDeletePreviewResponse;
  confirmation: string;
  isBusy: boolean;
  onConfirmationChange: (value: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const requiredConfirmation = bankLabel(preview.bankName);
  const canConfirm = confirmation.trim().toUpperCase() === requiredConfirmation.toUpperCase();
  const impactItems = [
    { label: "Bank accounts", value: preview.accountsDeleted },
    { label: "Transactions", value: preview.transactionsDeleted },
    { label: "Transaction splits", value: preview.transactionSplitsDeleted },
    { label: "Shared transactions affected", value: preview.sharedTransactionsAffected }
  ];

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="delete-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-bank-title">
        <div className="delete-dialog-header">
          <div className="danger-mark">
            <Trash2 size={22} />
          </div>
          <div>
            <span>Confirm delete</span>
            <h2 id="delete-bank-title">Delete {requiredConfirmation}?</h2>
          </div>
        </div>

        <p className="delete-warning">
          This will remove the bank from your account and delete the related data listed below.
        </p>

        <div className="impact-grid">
          {impactItems.map((item) => (
            <article key={item.label}>
              <strong>{item.value}</strong>
              <span>{item.label}</span>
            </article>
          ))}
        </div>

        <label>
          Type {requiredConfirmation} to confirm
          <input
            value={confirmation}
            onChange={(event) => onConfirmationChange(event.target.value)}
            autoFocus
          />
        </label>

        <div className="dialog-actions">
          <button className="ghost-action" type="button" onClick={onCancel}>
            Cancel
          </button>
          <button className="primary-action destructive" type="button" onClick={onConfirm} disabled={!canConfirm || isBusy}>
            {isBusy ? <Loader2 className="spin" size={18} /> : <Trash2 size={18} />}
            Delete bank
          </button>
        </div>
      </section>
    </div>
  );
}

function TransactionDeleteDialog({
  transaction,
  isBusy,
  onCancel,
  onConfirm
}: {
  transaction: TransactionResponse;
  isBusy: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const title = transactionTitle(transaction);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="delete-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-transaction-title">
        <div className="delete-dialog-header">
          <div className="danger-mark">
            <Trash2 size={22} />
          </div>
          <div>
            <span>Confirm delete</span>
            <h2 id="delete-transaction-title">Delete transaction?</h2>
          </div>
        </div>

        <p className="delete-warning">
          This will remove the selected transaction from your ledger.
        </p>

        <div className="delete-detail-list">
          <article>
            <span>Transaction</span>
            <strong>{title}</strong>
          </article>
          <article>
            <span>Amount</span>
            <strong>{money(transaction.total_amount)}</strong>
          </article>
          <article>
            <span>Date</span>
            <strong>{new Date(transaction.transaction_date).toLocaleString()}</strong>
          </article>
          <article>
            <span>ID</span>
            <strong>#{transaction.transaction_id}</strong>
          </article>
        </div>

        <div className="dialog-actions">
          <button className="ghost-action" type="button" onClick={onCancel}>
            Cancel
          </button>
          <button className="primary-action destructive" type="button" onClick={onConfirm} disabled={isBusy}>
            {isBusy ? <Loader2 className="spin" size={18} /> : <Trash2 size={18} />}
            Delete transaction
          </button>
        </div>
      </section>
    </div>
  );
}

function BulkTransactionDeleteDialog({
  transactions,
  isBusy,
  onCancel,
  onConfirm
}: {
  transactions: TransactionResponse[];
  isBusy: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const total = transactions.reduce((sum, transaction) => sum + Math.abs(Number(transaction.total_amount)), 0);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="delete-dialog" role="dialog" aria-modal="true" aria-labelledby="bulk-delete-title">
        <div className="delete-dialog-header">
          <div className="danger-mark">
            <Trash2 size={22} />
          </div>
          <div>
            <span>Confirm bulk delete</span>
            <h2 id="bulk-delete-title">Delete {transactions.length} transactions?</h2>
          </div>
        </div>

        <p className="delete-warning">
          This will remove every selected transaction from your ledger.
        </p>

        <div className="impact-grid">
          <article>
            <strong>{transactions.length}</strong>
            <span>Transactions</span>
          </article>
          <article>
            <strong>{money(total)}</strong>
            <span>Total movement</span>
          </article>
        </div>

        <div className="bulk-delete-list">
          {transactions.map((transaction) => (
            <article key={transaction.transaction_id}>
              <div>
                <strong>{transactionTitle(transaction)}</strong>
                <span>
                  #{transaction.transaction_id} on {new Date(transaction.transaction_date).toLocaleString()}
                </span>
              </div>
              <b>{money(transaction.total_amount)}</b>
            </article>
          ))}
        </div>

        <div className="dialog-actions">
          <button className="ghost-action" type="button" onClick={onCancel}>
            Cancel
          </button>
          <button className="primary-action destructive" type="button" onClick={onConfirm} disabled={isBusy}>
            {isBusy ? <Loader2 className="spin" size={18} /> : <Trash2 size={18} />}
            Delete selected
          </button>
        </div>
      </section>
    </div>
  );
}

function MetricGrid({
  totalBalance,
  visibleMovement,
  bankCount,
  transactionCount
}: {
  totalBalance: number;
  visibleMovement: number;
  bankCount: number;
  transactionCount: number;
}) {
  return (
    <div className="metric-grid">
      <Metric icon={<WalletCards size={20} />} label="Net account value" value={money(totalBalance)} />
      <Metric icon={<ArrowDownUp size={20} />} label="Visible movement" value={money(visibleMovement)} />
      <Metric icon={<Banknote size={20} />} label="Banks" value={String(bankCount)} />
      <Metric icon={<CreditCard size={20} />} label="Transactions" value={String(transactionCount)} />
    </div>
  );
}

function Metric({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <article className="metric-card">
      <div>{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function SectionTitle({ eyebrow, title }: { eyebrow: string; title: string }) {
  return (
    <div className="board-header">
      <div>
        <span>{eyebrow}</span>
        <h2>{title}</h2>
      </div>
    </div>
  );
}

function EmptyState({ text }: { text: string }) {
  return <div className="empty-state">{text}</div>;
}

function AccountPortfolio({
  bankAccounts,
  onDeleteAccount
}: {
  bankAccounts: AllBankAccountResponse[];
  onDeleteAccount: (kind: string) => void;
}) {
  return (
    <section className="board">
      <SectionTitle eyebrow="Accounts" title="Bank portfolio" />
      {bankAccounts.length ? (
        <div className="account-grid">
          {bankAccounts.map((bank) => (
            <article className="bank-card" key={bank.bankId}>
              <div className="bank-card-top">
                <Landmark size={20} />
                <strong>{bankLabel(bank.bankName)}</strong>
                <small>#{bank.bankId}</small>
              </div>
              {bank.accounts.map((account) => (
                <div className="account-row" key={account.bank_account_id}>
                  <div>
                    <strong>{account.account_name}</strong>
                    <span>
                      {bankLabel(account.account_type)} #{account.bank_account_id}
                    </span>
                  </div>
                  <b>{money(account.balance)}</b>
                  <button className="row-action danger" type="button" onClick={() => onDeleteAccount("account")}>
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
            </article>
          ))}
        </div>
      ) : (
        <EmptyState text="No bank accounts returned by the API yet." />
      )}
    </section>
  );
}

function AccountTable({
  accounts,
  onDeleteAccount
}: {
  accounts: Array<{
    account_name: string;
    account_type: string;
    balance: number | string;
    bank_account_id: number;
    bankName: string;
  }>;
  onDeleteAccount: (kind: string) => void;
}) {
  if (!accounts.length) {
    return <EmptyState text="Create a bank account to populate this table." />;
  }

  return (
    <div className="data-table">
      {accounts.map((account) => (
        <article className="table-row" key={account.bank_account_id}>
          <div>
            <strong>{account.account_name}</strong>
            <span>{bankLabel(account.bankName)}</span>
          </div>
          <span>{bankLabel(account.account_type)}</span>
          <b>{money(account.balance)}</b>
          <button className="row-action danger" type="button" onClick={() => onDeleteAccount("account")}>
            <Trash2 size={16} />
            Delete
          </button>
        </article>
      ))}
    </div>
  );
}

function AccountForm({
  accountName,
  accountType,
  accountBankId,
  banks,
  isBusy,
  onSubmit,
  onNameChange,
  onTypeChange,
  onBankChange
}: {
  accountName: string;
  accountType: AccountType;
  accountBankId: string;
  banks: BankResponse[];
  isBusy: boolean;
  onSubmit: (event: FormEvent) => void;
  onNameChange: (value: string) => void;
  onTypeChange: (value: AccountType) => void;
  onBankChange: (value: string) => void;
}) {
  return (
    <form onSubmit={onSubmit} className="form-grid">
      <label>
        Account name
        <input value={accountName} onChange={(event) => onNameChange(event.target.value)} required />
      </label>
      <label>
        Type
        <select value={accountType} onChange={(event) => onTypeChange(event.target.value as AccountType)}>
          {ACCOUNT_TYPES.map((type) => (
            <option key={type} value={type}>
              {bankLabel(type)}
            </option>
          ))}
        </select>
      </label>
      <label className="wide">
        Bank
        <select value={accountBankId} onChange={(event) => onBankChange(event.target.value)} required>
          <option value="">Choose bank</option>
          {banks.map((bank) => (
            <option key={bank.id} value={bank.id}>
              {bankLabel(bank.name)} #{bank.id}
            </option>
          ))}
        </select>
      </label>
      <button className="primary-action" disabled={isBusy || !banks.length}>
        <Plus size={18} />
        Add account
      </button>
    </form>
  );
}

function TransactionForm({
  merchant,
  description,
  amount,
  category,
  transactionDate,
  transactionAccountId,
  accounts,
  isBusy,
  onSubmit,
  onMerchantChange,
  onDescriptionChange,
  onAmountChange,
  onCategoryChange,
  onDateChange,
  onAccountChange
}: {
  merchant: string;
  description: string;
  amount: string;
  category: string;
  transactionDate: string;
  transactionAccountId: string;
  accounts: Array<{ bank_account_id: number; account_name: string; bankName: string }>;
  isBusy: boolean;
  onSubmit: (event: FormEvent) => void;
  onMerchantChange: (value: string) => void;
  onDescriptionChange: (value: string) => void;
  onAmountChange: (value: string) => void;
  onCategoryChange: (value: string) => void;
  onDateChange: (value: string) => void;
  onAccountChange: (value: string) => void;
}) {
  return (
    <form onSubmit={onSubmit} className="form-grid">
      <label>
        Merchant
        <input value={merchant} onChange={(event) => onMerchantChange(event.target.value)} required />
      </label>
      <label>
        Amount
        <input value={amount} onChange={(event) => onAmountChange(event.target.value)} inputMode="decimal" required />
      </label>
      <label>
        Category
        <input value={category} onChange={(event) => onCategoryChange(event.target.value)} required />
      </label>
      <label>
        Date
        <input type="datetime-local" value={transactionDate} onChange={(event) => onDateChange(event.target.value)} required />
      </label>
      <label className="wide">
        Account
        <select value={transactionAccountId} onChange={(event) => onAccountChange(event.target.value)} required>
          <option value="">Choose account</option>
          {accounts.map((account) => (
            <option key={account.bank_account_id} value={account.bank_account_id}>
              {account.account_name} at {bankLabel(account.bankName)}
            </option>
          ))}
        </select>
      </label>
      <label className="wide">
        Description
        <textarea value={description} onChange={(event) => onDescriptionChange(event.target.value)} rows={3} />
      </label>
      <button className="primary-action" disabled={isBusy || !accounts.length}>
        <BadgeDollarSign size={18} />
        Post transaction
      </button>
    </form>
  );
}

function StatementPreviewPanel({
  accounts,
  accountId,
  selectedFile,
  preview,
  isBusy,
  onAccountChange,
  onFileChange,
  onSubmit,
  onConfirm
}: {
  accounts: Array<{ bank_account_id: number; account_name: string; bankName: string }>;
  accountId: string;
  selectedFile: File | null;
  preview: StatementImportPreviewResponse | null;
  isBusy: boolean;
  onAccountChange: (value: string) => void;
  onFileChange: (value: File | null) => void;
  onSubmit: (event: FormEvent) => void;
  onConfirm: () => void;
}) {
  return (
    <div className="statement-import">
      <form onSubmit={onSubmit} className="form-grid">
        <label>
          Account
          <select value={accountId} onChange={(event) => onAccountChange(event.target.value)} required>
            <option value="">Choose account</option>
            {accounts.map((account) => (
              <option key={account.bank_account_id} value={account.bank_account_id}>
                {account.account_name} at {bankLabel(account.bankName)}
              </option>
            ))}
          </select>
        </label>

        <label>
          PDF statement
          <input
            type="file"
            accept="application/pdf,.pdf"
            onChange={(event) => onFileChange(event.target.files?.[0] ?? null)}
            required
          />
        </label>

        <div className="file-chip wide">
          <FileText size={18} />
          <span>{selectedFile ? selectedFile.name : "No file selected"}</span>
        </div>

        <button className="primary-action" disabled={isBusy || !accounts.length || !selectedFile}>
          {isBusy ? <Loader2 className="spin" size={18} /> : <Upload size={18} />}
          Preview import
        </button>
      </form>

      {preview && (
        <div className="statement-preview">
          <div className="preview-summary">
            <article>
              <span>Account ID</span>
              <strong>#{preview.bankAccountId}</strong>
            </article>
            <article>
              <span>Start date</span>
              <strong>{preview.statementPeriod?.startDate ? new Date(preview.statementPeriod.startDate).toLocaleDateString() : "Unknown"}</strong>
            </article>
            <article>
              <span>End date</span>
              <strong>{preview.statementPeriod?.endDate ? new Date(preview.statementPeriod.endDate).toLocaleDateString() : "Unknown"}</strong>
            </article>
            <article>
              <span>Transactions</span>
              <strong>{preview.transactions.length}</strong>
            </article>
          </div>

          <div className="confirm-import-bar">
            <div>
              <span>Ready to import</span>
              <strong>{preview.transactions.length} preview transaction(s)</strong>
            </div>
            <button
              className="primary-action"
              type="button"
              onClick={onConfirm}
              disabled={isBusy || !preview.transactions.length}
            >
              {isBusy ? <Loader2 className="spin" size={18} /> : <Check size={18} />}
              Confirm import
            </button>
          </div>

          {preview.transactions.length ? (
            <div className="transaction-list compact">
              {preview.transactions.map((txn, index) => (
                <article className="transaction-row" key={`${transactionDateValue(txn)}-${index}`}>
                  <div className="transaction-mark">{transactionTitle(txn).slice(0, 1) || "#"}</div>
                  <div>
                    <strong>{transactionTitle(txn)}</strong>
                    <span>
                      {txn.description ? `${txn.description} on ` : ""}
                      {new Date(transactionDateValue(txn)).toLocaleString()}
                    </span>
                  </div>
                  <div className="transaction-meta">
                    <b>{money(transactionAmount(txn))}</b>
                    <span>{txn.splits?.length ?? 0} split(s)</span>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <EmptyState text="The parser did not return any preview transactions." />
          )}
        </div>
      )}
    </div>
  );
}

function Ledger({
  transactions,
  rangeSeconds,
  onRangeChange,
  onDeleteTransaction,
  selectedTransactionIds,
  onToggleTransaction,
  onSelectAllVisible,
  onClearSelection,
  onBulkDelete
}: {
  transactions: TransactionResponse[];
  rangeSeconds: number;
  onRangeChange: (value: number) => void;
  onDeleteTransaction: (transaction: TransactionResponse) => void;
  selectedTransactionIds: number[];
  onToggleTransaction: (transactionId: number, selected: boolean) => void;
  onSelectAllVisible: () => void;
  onClearSelection: () => void;
  onBulkDelete: () => void;
}) {
  const selectableCount = transactions.filter((transaction) => typeof transaction.transaction_id === "number").length;

  return (
    <section className="ledger-section">
      <div className="board-header transaction-header">
        <div>
          <span>Ledger</span>
          <h2>Recent transactions</h2>
        </div>
        <div className="ledger-actions">
          <select value={rangeSeconds} onChange={(event) => onRangeChange(Number(event.target.value))}>
            <option value={604800}>7 days</option>
            <option value={2592000}>30 days</option>
            <option value={7776000}>90 days</option>
          </select>
        </div>
      </div>
      {transactions.length ? (
        <>
          <div className="bulk-toolbar">
            <div>
              <strong>{selectedTransactionIds.length}</strong>
              <span>selected</span>
            </div>
            <button className="ghost-action" type="button" onClick={onSelectAllVisible} disabled={!selectableCount}>
              Select visible
            </button>
            <button className="ghost-action" type="button" onClick={onClearSelection} disabled={!selectedTransactionIds.length}>
              Clear
            </button>
            <button className="primary-action destructive" type="button" onClick={onBulkDelete} disabled={!selectedTransactionIds.length}>
              <Trash2 size={18} />
              Delete selected
            </button>
          </div>

          <div className="transaction-list">
            {transactions.map((txn, index) => {
              const transactionId = txn.transaction_id;
              const isSelectable = typeof transactionId === "number";
              const isSelected = isSelectable && selectedTransactionIds.includes(transactionId);

              return (
                <article className="transaction-row" key={txn.transaction_id ?? `${txn.transaction_date}-${index}`}>
                  <label className="row-check" aria-label="Select transaction">
                    <input
                      type="checkbox"
                      checked={isSelected}
                      disabled={!isSelectable}
                      onChange={(event) => {
                        if (isSelectable) {
                          onToggleTransaction(transactionId, event.target.checked);
                        }
                      }}
                    />
                  </label>
                  <div className="transaction-mark">{transactionTitle(txn).slice(0, 1) || "#"}</div>
                  <div>
                    <strong>{transactionTitle(txn)}</strong>
                    <span>
                      #{txn.transaction_id} on {new Date(txn.transaction_date).toLocaleString()}
                    </span>
                  </div>
                  <div className="transaction-meta">
                    <b>{money(txn.total_amount)}</b>
                    <span>{txn.num_of_splits} split(s)</span>
                  </div>
                  <button
                    className="row-action danger"
                    type="button"
                    onClick={() => onDeleteTransaction(txn)}
                    disabled={!txn.transaction_id}
                    title={txn.transaction_id ? "Delete transaction" : "Preview transactions cannot be deleted"}
                  >
                    <Trash2 size={16} />
                  </button>
                </article>
              );
            })}
          </div>
        </>
      ) : (
        <EmptyState text="No transactions returned for this interval." />
      )}
    </section>
  );
}
