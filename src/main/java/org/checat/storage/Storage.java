package org.checat.storage;

import com.sun.istack.internal.Nullable;
import org.checat.model.Account;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;


public class Storage {

    private final Map<Long, StoredAccount> accounts = new HashMap<>();
    private final Map<Long, Lock> accountLocks = new HashMap<>();
    private final Map<Long, StoredTransaction> transactions = new HashMap<>();

    private long max_transaction_id = 0;

    private final Lock lock = new ReentrantLock();

    public DataTransaction startDataTransaction() {
        return new DataTransaction(this);
    }

    public Lock getAccountLock(long id) {
        lock.lock();
        try {
            return accountLocks.computeIfAbsent(id, unused_key -> new ReentrantLock());
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public StoredTransaction getTransaction(long id) {
        lock.lock();
        try {
            return transactions.get(id);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    StoredAccount getAccount(long id) {
        lock.lock();
        try {
            return accounts.computeIfAbsent(id, StoredAccount::new);
        } finally {
            lock.unlock();
        }

    }

    void updateAccount(StoredAccount account) {
        lock.lock();
        try {
            accounts.put(account.getId(), account);
        } finally {
            lock.unlock();
        }
    }

    public void updateTransaction(StoredTransaction transaction) {
        lock.lock();
        try {
            transactions.put(transaction.getId(), transaction);
        } finally {
            lock.unlock();
        }
    }

    public Long insertTransaction(long source_id, long destination_id, long amount) {
        lock.lock();
        try {
            StoredTransaction tr = new StoredTransaction(max_transaction_id + 1,
                    source_id,
                    destination_id,
                    amount, StoredTransaction.State.INITIATED);
            transactions.put(tr.getId(), tr);
            max_transaction_id++;
            return tr.getId();
        } finally {
            lock.unlock();
        }
    }

    public Collection<StoredTransaction> getTransactions() {
        return transactions.values();
    }
}
