package org.checat;

import org.checat.storage.Account;
import org.checat.storage.DataTransaction;
import org.checat.storage.Storage;
import org.checat.storage.Transaction;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Root resource (exposed at "transaction" path)
 */
@Path("transaction")
public class Transactions
{
    private static final Logger LOGGER = Logger.getLogger( Transactions.class.getName() );
    private static final Handler HANDLER = new ConsoleHandler();
    static {
        LOGGER.addHandler(HANDLER);
    }
    private final Storage storage = Storage.STORAGE;

    /**
     * Method handling HTTP GET requests to /{id} subpath.
     * Returns Transaction object with requested id. Null if it is not present.
     *
     * @param id @PathParam id of transation
     * @return Transaction with requested id.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Transaction get(@PathParam("id") long id) {
        return storage.getTransaction(id);
    }

    /**
     * Method handling HTTP GET requests to base path.
     * Returns transactions with filtering.
     *
     * @param source @QueryParam. If present only transactions with same value of source field will be returned.
     * @param destination @QueryParam. If present only transactions with same value of destination field will
     *                   be returned.
     * @param amount @QueryParam. If present only transactions with same value of amount field will be returned.
     * @return Transactions matching requested criteria.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Transaction> search(@QueryParam("source") Long source,
                                          @QueryParam("destination") Long destination,
                                          @QueryParam("amount") Long amount
                         ) {
        return storage.getTransactions().stream()
                .filter(transaction -> Objects.isNull(source) || source.equals(transaction.getSource()))
                .filter(transaction -> Objects.isNull(destination) || destination.equals(transaction.getDestination()))
                .filter(transaction -> Objects.isNull(amount) || amount.equals(transaction.getAmount()))
                .collect(Collectors.toList());
    }

    /**
     * Method handling HTTP POST requests to initiate/{source}/{destination}/{amount} subpath.
     * Initiates moving positive amount of money from source account to destination account.
     *
     * Source and destination parameters indicate ids of source and destination accounts if positive.
     * If negative they indicate moving money into system or withdrawing money from system.
     * At least one of them should be positive.
     * Put money in source account on hold.
     *
     * @param source @PathParam. Id of account from which money are paid.
     * @param destination @PathParam. Id of account to which money are paid.
     * @param amount @PathParam. Amount of money transferred. Must be positive.
     * @return Id of created transaction if successful. Null otherwise.
     */
    @POST
    @Path("initiate/{source}/{destination}/{amount}")
    @Produces(MediaType.APPLICATION_JSON)
    public Long initiate(@PathParam("source") long source,
                           @PathParam("destination") long destination,
                           @PathParam("amount") long amount) {
        LOGGER.log(Level.INFO,
                "Transaction request to transfer {2} amount of money from {0} to {1}.",
                   new Object[]{ source, destination, amount});
        if (amount <= 0 || !Account.isAccountInternal(source) && !Account.isAccountInternal(destination)) {
            return null;
        }
        DataTransaction dataTransaction = storage.startDataTransaction();
        if (Account.isAccountInternal(source)) {
            dataTransaction.addAccountCondition(source, Account.haveEnoughMoneyPredicate(amount));
            dataTransaction.updateAccout(source, Account.holdMoneyUpdater(amount));
        }
        dataTransaction.insertTransaction(source, destination, amount);
        List<Long> ids = dataTransaction.commit();
        if (ids.size() == 1) {
            return storage.getTransaction(ids.get(0)).id;
        } else {
            return null;
        }
    }

    /**
     * Method handling HTTP POST requests to confirm/{id} subpath.
     * Confirms initiated earlier transaction.
     * Proceed with actual moving money from source account to destination account.
     * If requested transaction isn't in org.checat.storage.Transaction.State.INITIATED state this method does
     * nothing.
     *
     * @param transaction_id @PathParam. Id of transaction to be confirmed.
     * @return true if requested transaction is in org.checat.storage.Transaction.State.CONFIRMED state, false
     *  otherwise.
     */
    @POST
    @Path("confirm/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean confirm(@PathParam("id") long transaction_id) {
        LOGGER.log(Level.INFO, "Confirmation request for transaction {0} received.", transaction_id);
        Transaction confirmed = storage.getTransaction(transaction_id);
        if (confirmed != null) {
            DataTransaction dataTransaction = storage.startDataTransaction();
            dataTransaction.addTransactionCondition(transaction_id, Transaction::isTransactionInitiated);
            dataTransaction.updateTransaction(transaction_id, Transaction::confirmTransaction);
            if (Account.isAccountInternal(confirmed.getSource())) {
                dataTransaction.updateAccout(
                        confirmed.getSource(),
                        Account.transferMoneyUpdater(confirmed.getAmount()));
            }
            if (Account.isAccountInternal(confirmed.getDestination())) {
                dataTransaction.updateAccout(
                        confirmed.getDestination(),
                        Account.addMoneyUpdater(confirmed.getAmount()));
            }
            dataTransaction.commit();
            Transaction result_transaction = storage.getTransaction(transaction_id);
            return result_transaction.getState().equals(Transaction.State.CONFIRMED);
        } else {
            return false;
        }
    }

    /**
     * Method handling HTTP POST requests to cancel/{id} subpath.
     * Cancels initiated earlier transaction.
     * Release money put on hold earlier.
     * If requested transaction isn't in org.checat.storage.Transaction.State.INITIATED state this method does
     * nothing.
     *
     * @param transaction_id @PathParam. Id of transaction to be cancelled.
     * @return true if requested transaction is in org.checat.storage.Transaction.State.CANCELLED state, false
     *  otherwise.
     */
    @POST
    @Path("cancel/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean cancel(@PathParam("id") long transaction_id) {
        LOGGER.log(Level.INFO, "Cancellation request for transaction {0} received.", transaction_id);
        Transaction cancelled = storage.getTransaction(transaction_id);
        if (cancelled != null) {
            DataTransaction dataTransaction = storage.startDataTransaction();
            dataTransaction.addTransactionCondition(transaction_id, Transaction::isTransactionInitiated);
            dataTransaction.updateTransaction(transaction_id, Transaction::cancelTransaction);
            if (Account.isAccountInternal(cancelled.getSource())) {
                dataTransaction.updateAccout(cancelled.getSource(), Account.releaseMoneyUpdater(cancelled.getAmount()));
            }
            dataTransaction.commit();
            Transaction result_transaction = storage.getTransaction(transaction_id);
            return result_transaction.getState().equals(Transaction.State.CANCELED);
        } else {
            return false;
        }
    }
}
