package org.checat.storage;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StoredTransaction {
    public long id;
    public long source;
    public long destination;
    public long amount;
    public State state;

    public static StoredTransaction cancelTransaction(StoredTransaction transaction) {
        return new StoredTransaction(transaction.getId(), transaction.getSource(), transaction.getDestination(), transaction.getAmount(), State.CANCELED);
    }

    public static StoredTransaction confirmTransaction(StoredTransaction transaction) {
        return new StoredTransaction(transaction.getId(), transaction.getSource(), transaction.getDestination(), transaction.getAmount(), State.CONFIRMED);
    }

    public static boolean isTransactionInitiated(StoredTransaction transaction) {
        return transaction.getState().equals(State.INITIATED);
    }

    public enum State {
        INITIATED,
        CONFIRMED,
        CANCELED,
    }

    public long getId() {
        return id;
    }

    public long getSource() {
        return source;
    }

    public long getDestination() {
        return destination;
    }

    public long getAmount() {
        return amount;
    }

    public State getState() {
        return state;
    }

    public StoredTransaction() {}

    public StoredTransaction(long id, long source, long destination, long amount, State state) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.state = state;
    }

}
