package org.checat.storage;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
public class Transaction {
    public long id;
    public long source;
    public long destination;
    public long amount;
    public State state;

    /**
     * Makes a copy of transaction with state changed to State.CANCELED.
     * @param transaction transaction to transform
     * @return new transformed transaction
     */
    public static Transaction cancelTransaction(Transaction transaction) {
        return new Transaction(
                transaction.getId(),
                transaction.getSource(),
                transaction.getDestination(),
                transaction.getAmount(),
                State.CANCELED);
    }

    /**
     * Makes a copy of transaction with state changed to State.CONFIRMED.
     * @param transaction transaction to transform
     * @return new transformed transaction
     */
    public static Transaction confirmTransaction(Transaction transaction) {
        return new Transaction(
                transaction.getId(),
                transaction.getSource(),
                transaction.getDestination(),
                transaction.getAmount(),
                State.CONFIRMED);
    }

    /**
     * Checks that transaction is in State.INITIATED state.
     * @param transaction Transaction object to check.
     * @return true if transaction is in State.INITIATED state, false otherwise.
     */
    public static boolean isTransactionInitiated(Transaction transaction) {
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

    public Transaction() {}

    public Transaction(long id, long source, long destination, long amount, State state) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return getId() == that.getId() &&
                getSource() == that.getSource() &&
                getDestination() == that.getDestination() &&
                getAmount() == that.getAmount() &&
                getState() == that.getState();
    }

    @Override
    public int hashCode() {

        return Objects.hash(getId(), getSource(), getDestination(), getAmount(), getState());
    }
}
