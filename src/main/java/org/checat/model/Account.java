package org.checat.model;

import org.checat.storage.StoredAccount;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {

    private final long id;
    private long money;
    private long hold;
    private Lock lock = new ReentrantLock();

    public Account(long id) {
        this.id = id;
        money = 0;
        hold = 0;
    }

    public Account(StoredAccount storedAccount) {
        id = storedAccount.getId();
        money = storedAccount.getMoney();
        hold = storedAccount.getHold();
    }

    public long getId() {
        return id;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getHold() {
        return hold;
    }

    public void setHold(long hold) {
        this.hold = hold;
    }
}
