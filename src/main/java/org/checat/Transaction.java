package org.checat;

import org.checat.storage.DataTransaction;
import org.checat.storage.Storage;
import org.checat.storage.StoredAccount;
import org.checat.storage.StoredTransaction;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Root resource (exposed at "transaction" path)
 */
@Path("transaction")
public class Transaction {
    private static final Logger LOGGER = Logger.getLogger( Transaction.class.getName() );
    private static final Handler HANDLER = new ConsoleHandler();
    static {
        LOGGER.addHandler(HANDLER);
    }
    private final Storage storage = new Storage();

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public org.checat.storage.StoredTransaction get(@PathParam("id") long id) {
        return storage.getTransaction(id);
    }

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<StoredTransaction> search(@QueryParam("source") Long source,
                                                @QueryParam("destination") Long destination,
                                                @QueryParam("amount") Long amount
                         ) {
        return storage.getTransactions().stream()
                .filter(transaction -> Objects.isNull(source) || source.equals(transaction.getSource()))
                .filter(transaction -> Objects.isNull(destination) || destination.equals(transaction.getDestination()))
                .filter(transaction -> Objects.isNull(amount) || amount.equals(transaction.getAmount()))
                .collect(Collectors.toList());
    }

    @POST
    @Path("initiate/{source}/{destination}/{amount}")
    @Produces(MediaType.APPLICATION_JSON)
    public StoredTransaction initiate(@PathParam("source") long source,
                           @PathParam("destination") long destination,
                           @PathParam("amount") long amount) {
        LOGGER.log(Level.INFO,
                "StoredTransaction request to transfer {2} amount of money from {0} to {1}.",
                   new Object[]{ source, destination, amount});
        DataTransaction dataTransaction = storage.startDataTransaction();
        dataTransaction.addAccountCondition(source, StoredAccount.haveEnoughMoneyPredicate(amount));
        dataTransaction.insertTransaction(source, destination, amount);
        dataTransaction.updateAccout(source, StoredAccount.holdMoneyUpdater(amount));
        List<Long> ids = dataTransaction.commit();
        if (ids.size() == 1) {
            return storage.getTransaction(ids.get(0));
        } else {
            return null;
        }
    }

    @POST
    @Path("confirm/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean confirm(@PathParam("id") long transaction_id) {
        LOGGER.log(Level.INFO, "Confirmation request for transaction {0} received.", transaction_id);
        StoredTransaction confirmed = storage.getTransaction(transaction_id);
        if (confirmed != null) {
            DataTransaction dataTransaction = storage.startDataTransaction();
            dataTransaction.addTransactionCondition(transaction_id, StoredTransaction::isTransactionInitiated);
            dataTransaction.updateTransaction(transaction_id, StoredTransaction::confirmTransaction);
            dataTransaction.updateAccout(confirmed.getSource(), StoredAccount.transferMoneyUpdater(confirmed.getAmount()));
            dataTransaction.updateAccout(confirmed.getDestination(), StoredAccount.addMoneyUpdater(confirmed.getAmount()));
            dataTransaction.commit();
            StoredTransaction result_transaction = storage.getTransaction(transaction_id);
            return result_transaction.getState().equals(StoredTransaction.State.CONFIRMED);
        } else {
            return false;
        }
    }

    @POST
    @Path("cancel/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean cancel(@PathParam("id") long transaction_id) {
        LOGGER.log(Level.INFO, "Cancellation request for transaction {0} received.", transaction_id);
        StoredTransaction cancelled = storage.getTransaction(transaction_id);
        if (cancelled != null) {
            DataTransaction dataTransaction = storage.startDataTransaction();
            dataTransaction.addTransactionCondition(transaction_id, StoredTransaction::isTransactionInitiated);
            dataTransaction.updateTransaction(transaction_id, StoredTransaction::cancelTransaction);
            dataTransaction.updateAccout(cancelled.getSource(), StoredAccount.releaseMoneyUpdater(cancelled.getAmount()));
            dataTransaction.commit();
            StoredTransaction result_transaction = storage.getTransaction(transaction_id);
            return result_transaction.getState().equals(StoredTransaction.State.CANCELED);
        } else {
            return false;
        }
    }
}
