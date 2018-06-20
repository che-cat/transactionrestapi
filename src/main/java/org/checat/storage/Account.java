package org.checat.storage;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@XmlRootElement
public class Account {
    public long id;
    public long money;
    public long hold;

    public Account() {}

    public Account(long id) {
        hold = 0;
        money = 0;
        this.id = id;
    }

    /**
     * Checks if account specified by id is internal or external.
     * Account with positive id is internal others are external.
     *
     * @param id long Id of account to check.
     * @return true if account is internal, false otherwise.
     */
    public static boolean isAccountInternal(long id) {
        return id > 0;
    }

    /**
     * Returns UnaryOperator that will put specified amount of money on hold.
     * @param amount long Amount of money to hold.
     * @return UnaryOperator that put specified amount of money on hold.
     */
    public static UnaryOperator<Account> holdMoneyUpdater(long amount) {
        return account -> new Account(
                account.getId(),
                account.getMoney(),
                account.getHold() + amount);
    }

    /**
     * Returns UnaryOperator that will add specified amount of money to account.
     * @param amount long Amount of money to add.
     * @return UnaryOperator that add specified amount of money to account.
     */
    public static UnaryOperator<Account> addMoneyUpdater(long amount) {
        return account -> new Account(
                account.getId(),
                account.getMoney() + amount,
                account.getHold());
    }

    /**
     * Returns UnaryOperator that transfers specified amount of money from account.
     * @param amount long Amount of money to transfer.
     * @return UnaryOperator that transfer specified amount of money from account.
     */
    public static UnaryOperator<Account> transferMoneyUpdater(long amount) {
        return account -> new Account(
                account.getId(),
                account.getMoney() - amount,
                account.getHold() - amount);
    }

    /**
     * Returns UnaryOperator that will release specified amount of money from being held.
     * @param amount long Amount of money to release.
     * @return UnaryOperator that release specified amount of money from being held.
     */
    public static UnaryOperator<Account> releaseMoneyUpdater(long amount) {
        return account -> new Account(
                account.getId(),
                account.getMoney(),
                account.getHold() - amount);
    }

    public static Predicate<Account> haveEnoughMoneyPredicate(long money) {
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

    public Account(long id, long money, long hold) {
        this.id = id;
        this.money = money;
        this.hold = hold;
    }
}
