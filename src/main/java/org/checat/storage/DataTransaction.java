package org.checat.storage;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.checat.model.Transaction;
import org.glassfish.jersey.internal.util.Producer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DataTransaction {
    private final List<Producer<Optional<Long>>> operations = new LinkedList<>();
    private final Storage storage;
    private final Set<Long> lockIds = new TreeSet<>();
    private final List<Producer<Boolean>> conditions = new LinkedList<>();

    DataTransaction(Storage storage) {
        this.storage = storage;
    }

    private void addLock(Long lockId) {
        this.lockIds.add(lockId);
    }

    public void addTransactionCondition(long id, Predicate<StoredTransaction> condition) {
        StoredTransaction transaction = storage.getTransaction(id);
        if (transaction != null) {
            addLock(transaction.getSource());
            addLock(transaction.getDestination());
            conditions.add(() -> condition.test(storage.getTransaction(id)));
        } else {
            conditions.add(() -> false);
        }
    }

    public void addAccountCondition(long id, Predicate<StoredAccount> condition) {
        addLock(id);
        conditions.add(() -> condition.test(storage.getAccount(id)));
    }

    public void updateAccout(long id, UnaryOperator<StoredAccount> updater) {
        addLock(id);
        operations.add(() -> {
            storage.updateAccount(updater.apply(storage.getAccount(id)));
            return Optional.empty();
        });
    }

    public void updateTransaction(long id, UnaryOperator<StoredTransaction> updater) {
        addTransactionCondition(id, Objects::nonNull);
        operations.add(() -> {
            storage.updateTransaction(updater.apply(storage.getTransaction(id)));
            return Optional.empty();
        });
    }

    public void insertTransaction(long source_id,
                                  long destination_id,
                                  long amount) {
        addLock(source_id);
        addLock(destination_id);
        operations.add(() -> Optional.of(storage.insertTransaction(source_id, destination_id, amount)));
    }

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
                return operations.stream()
                        .map(Producer::call)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
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
