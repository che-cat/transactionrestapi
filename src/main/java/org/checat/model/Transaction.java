package org.checat.model;

import org.checat.storage.StoredTransaction.State;

public class Transaction {
    private final long id;
    private final long source;
    private final long destination;
    private final long amount;
    private State state;


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

    public Transaction(long id, long source, long destination, long amount) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
    }

    public void setState(State state) {
        this.state = state;
    }
}
