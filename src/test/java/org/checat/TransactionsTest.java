package org.checat;

import org.checat.storage.Storage;
import org.checat.storage.Transaction;
import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TransactionsTest {

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        // start the server
        server = Main.startServer();
        // create the client
        Client c = ClientBuilder.newClient();

        target = c.target(Main.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        Storage.STORAGE.reset();
    }

    /**
     * Test to get empty transaction list at the startup.
     */
    @Test
    public void testGetEmptyTransactionList() {
        List<Transaction> responseMsg = target
                .path("transaction")
                .request()
                .get()
                .readEntity(new GenericType<List<Transaction>>() {});
        assertEquals(Collections.emptyList(), responseMsg);
    }
    /**
     * Test to get null for non existing transaction
     */
    @Test
    public void testGetNullNonExistingTransaction() {
        Transaction responseMsg = target
                .path("transaction/1")
                .request()
                .get(Transaction.class);
        assertNull(responseMsg);
    }

    /**
     * Test to get all transactions after a couple of initiates.
     */
    @Test
    public void testGetAllTransactions() {
        target.path("transaction/initiate/-1/123/100000").request().post(Entity.text(""));
        target.path("transaction/initiate/0/1253/100000").request().post(Entity.text(""));
        target.path("transaction/initiate/0/1233/100000").request().post(Entity.text(""));
        target.path("transaction/initiate/0/325/100000").request().post(Entity.text(""));
        List<Transaction> responseMsg = target
                .path("transaction")
                .request().get().readEntity(new GenericType<List<Transaction>>() {});
        assertThat(responseMsg, org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder(
                new Transaction(1, -1, 123, 100000, Transaction.State.INITIATED),
                new Transaction(2, 0, 1253, 100000, Transaction.State.INITIATED),
                new Transaction(3, 0, 1233, 100000, Transaction.State.INITIATED),
                new Transaction(4, 0, 325, 100000, Transaction.State.INITIATED)
        ));
    }

    /**
     * Test transaction search
     */
    @Test
    public void testTransactionsSearch() {
        target.path("transaction/initiate/-1/123/100000").request().post(Entity.text(""));
        target.path("transaction/initiate/0/1253/60000").request().post(Entity.text(""));
        target.path("transaction/initiate/0/1233/10000").request().post(Entity.text(""));
        target.path("transaction/initiate/0/325/6000").request().post(Entity.text(""));
        List<Transaction> responseMsg = target
                .path("transaction")
                .request().get().readEntity(new GenericType<List<Transaction>>() {});
        assertThat(responseMsg, org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder(
                new Transaction(1, -1, 123, 100000, Transaction.State.INITIATED),
                new Transaction(2, 0, 1253, 60000, Transaction.State.INITIATED),
                new Transaction(3, 0, 1233, 10000, Transaction.State.INITIATED),
                new Transaction(4, 0, 325, 6000, Transaction.State.INITIATED)
        ));
        List<Transaction> secondResponseMsg = target
                .path("transaction")
                .queryParam("source", 0)
                .request()
                .get().readEntity(new GenericType<List<Transaction>>() {});
        assertThat(secondResponseMsg, org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder(
                new Transaction(2, 0, 1253, 60000, Transaction.State.INITIATED),
                new Transaction(3, 0, 1233, 10000, Transaction.State.INITIATED),
                new Transaction(4, 0, 325, 6000, Transaction.State.INITIATED)
        ));
        List<Transaction> thirdResponseMsg = target
                .path("transaction")
                .queryParam("destination", 1253)
                .request()
                .get().readEntity(new GenericType<List<Transaction>>() {});
        assertThat(thirdResponseMsg, org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder(
                new Transaction(2, 0, 1253, 60000, Transaction.State.INITIATED)
        ));
        List<Transaction> forthResponseMsg = target
                .path("transaction")
                .queryParam("amount", 6000)
                .request()
                .get().readEntity(new GenericType<List<Transaction>>() {});
        assertThat(forthResponseMsg, org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder(
                new Transaction(4, 0, 325, 6000, Transaction.State.INITIATED)
        ));

        List<Transaction> fifthResponseMsg = target
                .path("transaction")
                .queryParam("destination", 1253)
                .queryParam("amount", 60000)
                .queryParam("source", 0)
                .request()
                .get().readEntity(new GenericType<List<Transaction>>() {});
        assertThat(fifthResponseMsg, org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder(
                new Transaction(2, 0, 1253, 60000, Transaction.State.INITIATED)
        ));

        List<Transaction> sixthResponseMsg = target
                .path("transaction")
                .queryParam("destination", 1253)
                .queryParam("source", -10)
                .request()
                .get().readEntity(new GenericType<List<Transaction>>() {});
        assertTrue(sixthResponseMsg.isEmpty());

    }

    /**
     * Test on confirming transaction.
     */
    @Test
    public void testConfirmTransaction() {
        Long id = target
                .path("transaction/initiate/-1/123/100000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertTrue(target.path("transaction/confirm/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertFalse(target.path("transaction/cancel/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
    }

    /**
     * Test on cancelation.
     */
    @Test
    public void testCancelTransaction() {
        Long id = target
                .path("transaction/initiate/-1/123/100000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertTrue(target.path("transaction/cancel/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertFalse(target.path("transaction/confirm/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
    }

    /**
     * Test on repeated confirmation.
     */
    @Test
    public void testRepeatedConfirmation() {
        Long id = target
                .path("transaction/initiate/-1/123/100000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertTrue(target.path("transaction/confirm/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertTrue(target.path("transaction/confirm/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertTrue(target.path("transaction/confirm/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertTrue(target.path("transaction/confirm/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
    }

    /**
     * Test on repeated cancelation.
     */
    @Test
    public void testRepeatedCancelation() {
        Long id = target
                .path("transaction/initiate/-1/123/100000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertTrue(target.path("transaction/cancel/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertTrue(target.path("transaction/cancel/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertTrue(target.path("transaction/cancel/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
        assertTrue(target.path("transaction/cancel/" + id.toString())
                .request()
                .post(Entity.text(""))
                .readEntity(Boolean.class));
    }

    /**
     * Test on money counting.
     */
    @Test
    public void testMoneyCounting() {
        Long id = target
                .path("transaction/initiate/-1/123/100000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertEquals(0, Storage.STORAGE.getAccount(123).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(123).getHold());
        target.path("transaction/confirm/" + id.toString())
                .request()
                .post(Entity.text(""));
        assertEquals(100000, Storage.STORAGE.getAccount(123).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(123).getHold());
        Long secondTransactionId = target
                .path("transaction/initiate/123/122/10000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertEquals(100000, Storage.STORAGE.getAccount(123).getMoney());
        assertEquals(10000, Storage.STORAGE.getAccount(123).getHold());

        assertEquals(0, Storage.STORAGE.getAccount(122).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(122).getHold());

        target.path("transaction/confirm/" + secondTransactionId.toString())
                .request()
                .post(Entity.text(""));
        assertEquals(90000, Storage.STORAGE.getAccount(123).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(123).getHold());

        assertEquals(10000, Storage.STORAGE.getAccount(122).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(122).getHold());
        Long thirdTransactionId = target
                .path("transaction/initiate/122/123/5000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertEquals(90000, Storage.STORAGE.getAccount(123).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(123).getHold());

        assertEquals(10000, Storage.STORAGE.getAccount(122).getMoney());
        assertEquals(5000, Storage.STORAGE.getAccount(122).getHold());
        target.path("transaction/cancel/" + thirdTransactionId.toString())
                .request()
                .post(Entity.text(""));
        assertEquals(90000, Storage.STORAGE.getAccount(123).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(123).getHold());

        assertEquals(10000, Storage.STORAGE.getAccount(122).getMoney());
        assertEquals(0, Storage.STORAGE.getAccount(122).getHold());

        Long forthTransactionId = target
                .path("transaction/initiate/122/123/50000")
                .request()
                .post(Entity.text(""))
                .readEntity(Long.class);
        assertNull(forthTransactionId);
    }

}
