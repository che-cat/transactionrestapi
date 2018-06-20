package org.checat;

import org.checat.storage.Transaction;
import org.glassfish.grizzly.http.server.HttpServer;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    }

    /**
     * Test to get empty transaction list at the startup.
     */
    @Test
    public void testGetEmptyTransactionList() {
        List<Transaction> responseMsg = (ArrayList<Transaction>) target
                .path("transaction")
                .request()
                .get(ArrayList.class);
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
        List<Transaction> responseMsg = (ArrayList<Transaction>) target
                .path("transaction")
                .request().get().readEntity(new GenericType<List<Transaction>>() {});
        for (Transaction response: responseMsg) {
            System.out.println(response.getClass());
        }
        assertEquals(Arrays.asList(
                new Transaction(1, 0, 123, 100000, Transaction.State.INITIATED),
                new Transaction(2, 0, 1253, 100000, Transaction.State.INITIATED),
                new Transaction(3, 0, 1233, 100000, Transaction.State.INITIATED),
                new Transaction(4, 0, 325, 100000, Transaction.State.INITIATED)
        ), responseMsg);
    }
}
