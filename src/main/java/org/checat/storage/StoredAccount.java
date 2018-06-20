package org.checat.storage;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;


public class StoredAccount {
    private final long id;
    private final long money;
    private final long hold;

    public StoredAccount(long id) {
        hold = 0;
        money = 0;
        this.id = id;
    }

    public static UnaryOperator<StoredAccount> holdMoneyUpdater(long amount) {
        return account -> new StoredAccount(account.getId(), account.getMoney(), account.getHold() + amount);
    }

    public static UnaryOperator<StoredAccount> addMoneyUpdater(long amount) {
        return account -> new StoredAccount(account.getId(), account.getMoney() + amount, account.getHold());
    }

    public static UnaryOperator<StoredAccount> transferMoneyUpdater(long amount) {
        return account -> new StoredAccount(account.getId(), account.getMoney() - amount, account.getHold() - amount);
    }

    public static UnaryOperator<StoredAccount> releaseMoneyUpdater(long amount) {
        return account -> new StoredAccount(account.getId(), account.getMoney(), account.getHold() - amount);
    }

    public static Predicate<StoredAccount> haveEnoughMoneyPredicate(long money) {
        return account -> account.getMoney() >= account.getHold() + money;
    }

    public long getId() {
        return id;
    }

    public long getMoney() {
        return money;
    }

    public long getHold() {
        return hold;
    }

    public StoredAccount(long id, long money, long hold) {
        this.id = id;
        this.money = money;
        this.hold = hold;
    }
}
