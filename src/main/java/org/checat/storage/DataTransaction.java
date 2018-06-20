package org.checat.storage;

import org.glassfish.jersey.internal.util.Producer;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;


public class DataTransaction {
    private final List<Producer<Optional<Long>>> operations = new LinkedList<>();
    private final Storage storage;
    // Should be ordered to avoid deadlocks.
    private final Set<Long> lockIds = new TreeSet<>();
    private final List<Producer<Boolean>> conditions = new LinkedList<>();

    DataTransaction(Storage storage) {
        this.storage = storage;
    }

    /**
     * Adds lock corresponding to account with specified id to execution of transaction.
     * The method does nothing If lockId isn't internal.
     * @param lockId id of lock to hold.
     */
    private void addLock(Long lockId) {
        if (Account.isAccountInternal(lockId)) {
            this.lockIds.add(lockId);
        }
    }

    /**
     * Add condition that transaction with particular id exists and matches predicate.
     * @param id long Id of transaction to test. It should already exist at time of call to this method.
     * @param condition Predicate that tests transaction.
     */
    public void addTransactionCondition(long id, Predicate<Transaction> condition) {
        Transaction transaction = storage.getTransaction(id);
        if (transaction != null) {
            addLock(transaction.getSource());
            addLock(transaction.getDestination());
            conditions.add(() -> condition.test(storage.getTransaction(id)));
        } else {
            conditions.add(() -> false);
        }
    }

    /**
     * Add condition that account with particular id matches predicate.
     * @param id long Id of account to test.
     * @param condition Predicate that test account.
     */
    public void addAccountCondition(long id, Predicate<Account> condition) {
        addLock(id);
        conditions.add(() -> condition.test(storage.getAccount(id)));
    }

    /**
     * Add updater, that will construct new Account object instead of one with specified id.
     * @param id long Id of account to update.
     * @param updater UnaryOperator that updates produces new Account object.
     */
    public void updateAccout(long id, UnaryOperator<Account> updater) {
        addLock(id);
        operations.add(() -> {
            storage.updateAccount(updater.apply(storage.getAccount(id)));
            return Optional.empty();
        });
    }

    /**
     * Add updater, that will construct new Account object instead of one with specified id.
     * @param id long Id of account to update.
     * @param updater UnaryOperator that updates produces new Account object.
     */
    public void updateTransaction(long id, UnaryOperator<Transaction> updater) {
        addTransactionCondition(id, Objects::nonNull);
        operations.add(() -> {
            storage.updateTransaction(updater.apply(storage.getTransaction(id)));
            return Optional.empty();
        });
    }

    /**
     * Add operation that will insert new Transaction into storage.
     * @param source_id long Id of payer account.
     * @param destination_id long Id of payee account.
     * @param amount long money to pay.
     */
    public void insertTransaction(long source_id,
                                  long destination_id,
                                  long amount) {
        addLock(source_id);
        addLock(destination_id);
        operations.add(() -> Optional.of(storage.insertTransaction(source_id, destination_id, amount)));
    }

    /**
     * Executes all stored operations.
     * @return List of ids of all inserted transactions.
     */
    public List<Long> commit() {
        Stack<Lock> lockedLocks = new Stack<>();
        try {
            for (Long lockId : lockIds) {
                Lock lock = storage.getAccountLock(lockId);
                lockedLocks.push(lock);
                lock.lock();
            }
            boolean condition_result = conditions.stream()
                    .map(Producer::call)
                    .reduce(true, (l, r) -> l && r);
            if (condition_result) {
                List<Long> result = new ArrayList<>();
                operations.stream()
                        .forEachOrdered(op -> op.call().ifPresent(result::add));
                return result;
            } else {
                return Collections.emptyList();
            }
        } finally {
            while (!lockedLocks.empty()) {
                Lock lock = lockedLocks.pop();
                lock.unlock();
            }
        }
    }
}
