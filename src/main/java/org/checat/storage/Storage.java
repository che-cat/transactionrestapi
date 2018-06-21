package org.checat.storage;


import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Storage {

    public static final Storage STORAGE = new Storage();

    private final Map<Long, Account> accounts = new HashMap<>();
    private final Map<Long, Lock> accountLocks = new HashMap<>();
    private final Map<Long, Transaction> transactions = new HashMap<>();

    private long max_transaction_id = 0;

    private final Lock lock = new ReentrantLock();

    private Storage() {}

    /**
     * Creates DataTransaction object to manipulate data in storage.
     * @return new DataTransaction object.
     */
    public DataTransaction startDataTransaction() {
        return new DataTransaction(this);
    }

    /**
     * Requests transaction with specific id.
     * @param id long Id of requested transaction.
     * @return Transaction object or null if not present.
     */
    public Transaction getTransaction(long id) {
        lock.lock();
        try {
            return transactions.get(id);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Request all transactions.
     * @return copy of container containing all transactions.
     */
    public Collection<Transaction> getTransactions() {
        lock.lock();
        try {
            return new ArrayList<>(transactions.values());
        } finally {
            lock.unlock();
        }
    }


    Lock getAccountLock(long id) {
        lock.lock();
        try {
            return accountLocks.computeIfAbsent(id, unused_key -> new ReentrantLock());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return an Account object for given id. Creates new account if nothing found.
     * @param id long Id of account to search.
     * @return Account object.
     */
    @NotNull
    public Account getAccount(long id) {
        lock.lock();
        try {
            return accounts.computeIfAbsent(id, Account::new);
        } finally {
            lock.unlock();
        }

    }

    void updateAccount(Account account) {
        lock.lock();
        try {
            accounts.put(account.getId(), account);
        } finally {
            lock.unlock();
        }
    }

    void updateTransaction(Transaction transaction) {
        lock.lock();
        try {
            transactions.put(transaction.getId(), transaction);
        } finally {
            lock.unlock();
        }
    }

    Long insertTransaction(long source_id, long destination_id, long amount) {
        lock.lock();
        try {
            Transaction tr = new Transaction(max_transaction_id + 1,
                    source_id,
                    destination_id,
                    amount, Transaction.State.INITIATED);
            transactions.put(tr.getId(), tr);
            max_transaction_id++;
            return tr.getId();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Used to reset storage to start state in tests.
     */
    public void reset() {
        accounts.clear();
        accountLocks.clear();
        transactions.clear();
        max_transaction_id = 0;
    }

}
