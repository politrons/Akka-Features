package io.cloudstate.shopping;

import com.google.protobuf.Empty;
import io.cloudstate.connector.Connector;
import io.cloudstate.connector.RestConnectorGrpc;
import io.cloudstate.javasupport.EntityId;
import io.cloudstate.javasupport.eventsourced.*;
import io.cloudstate.shopping.domain.Domain;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entity ready to be used for Event Sourcing
 */
@EventSourcedEntity(persistenceId = "shopping-cart", snapshotEvery = 20)
public class ShoppingCartEntity {

    private final String entityId;

    private final Map<String, Protocol.LineItem> cart = new LinkedHashMap<>();

    public ShoppingCartEntity(@EntityId String entityId) {
        this.entityId = entityId;
    }

    // COMMANDS
    //-----------

    /**
     * Query handler to return the Cart of the Shopping.
     * We will find in the Map all values items for a user
     *
     * @return Cart
     */
    @CommandHandler
    public Protocol.Cart getCart(Protocol.GetShoppingCart getShoppingCartQuery) {
        System.out.println("Get current Cart state for user:" + getShoppingCartQuery.getUserId());
        connectorTest();
        return Protocol.Cart.newBuilder().addAllItems(cart.values()).build();
    }


    /**
     * A command handler may emit an event by taking in a CommandContext parameter, and invoking the emit method on it.
     * Invoking emit will immediately invoke the associated event handler for that event
     * <p>
     * [ShoppingDomain] is the factory class responsible for the creation of events. Here using for instance [ItemAdded.newBuilder()]
     * we use builder pattern to create a new Event using the command info.
     *
     * @param addItemCommand Command to transform into event
     * @param ctx            of command to link the Command and Event Handler.
     * @return Empty
     */
    @CommandHandler
    public Empty addItem(Protocol.AddLineItem addItemCommand, CommandContext ctx) {
        System.out.println("Add item command:" + addItemCommand);
        if (addItemCommand.getQuantity() <= 0) {
            ctx.fail("Cannot add negative quantity of to addItemCommand" + addItemCommand.getProductId());
        }
        ctx.emit(Domain.ItemAdded.newBuilder()
                .setItem(Domain.LineItem.newBuilder()
                        .setProductId(addItemCommand.getProductId())
                        .setName(addItemCommand.getName())
                        .setQuantity(addItemCommand.getQuantity())
                        .build())
                .build());
        return Empty.getDefaultInstance();
    }

    /**
     * Command handle method to transform the productId from the command in the ItemRemoved event which it could be persisted,
     * to be reused in a rehydrate a Cart.
     * We create the event and we pass to the Event handle to apply the action of that event.
     *
     * @param removeLineItemCommand Command to transform into event
     * @param ctx                   of command to link the Command and Event Handler.
     * @return Empty
     */
    @CommandHandler
    public Empty removeItem(Protocol.RemoveLineItem removeLineItemCommand, CommandContext ctx) {
        System.out.println("Remove item command:" + removeLineItemCommand);
        ctx.emit(Domain.ItemRemoved.newBuilder()
                .setProductId(removeLineItemCommand.getProductId())
                .build());
        return Empty.getDefaultInstance();
    }

    @CommandHandler
    public Protocol.LineItem getItem(Protocol.GetLineItem getItemQuery) {
        System.out.println("Get Item by productId:" + getItemQuery.getProductId());
        return cart.get(getItemQuery.getProductId());
    }

    // HANDLERS
    //-------------

    /**
     * Handle method that is invoked once a Command create an event of type [ItemAdded] and ise emit into
     * the [CommandContext]
     */
    @EventHandler
    public void itemAdded(Domain.ItemAdded event) {
        System.out.println("Processing ItemAdded event:" + event);
        Protocol.LineItem item = cart.get(event.getItem().getProductId());
        item = item == null ?
                transformDomainItemToProtocol(event.getItem()) :
                updateItem(event, item);
        cart.put(item.getProductId(), item);
    }

    private Protocol.LineItem updateItem(Domain.ItemAdded event, Protocol.LineItem item) {
        return item.toBuilder()
                .setQuantity(item.getQuantity() + event.getItem().getQuantity())
                .build();
    }

    /**
     * Event handle function responsible to receive the Event and apply the remove of the item from the cart
     */
    @EventHandler
    public void itemRemoved(Domain.ItemRemoved event) {
        System.out.println("Processing ItemRemoved event:" + event);
        cart.remove(event.getProductId());
    }

    // SNAPSHOT
    //----------

    /**
     * Snapshots are an important optimisation for event sourced entities that may contain many events,
     * to ensure that they can be loaded quickly even when they have very long journals
     */
    @Snapshot
    public Domain.Cart snapshot() {
        return Domain.Cart.newBuilder()
                .addAllItems(cart.values().stream().map(this::transformProtocolItemToDomain).collect(Collectors.toList()))
                .build();
    }

    /**
     * When the entity is rehydrate again, the snapshot will first be loaded before any other events are received, and passed to a snapshot handler
     **/
    @SnapshotHandler
    public void handleSnapshot(Domain.Cart cart) {
        System.out.println("Rehydrate shopping cart from Data store:" + cart);
        this.cart.clear();
        for (Domain.LineItem item : cart.getItemsList()) {
            this.cart.put(item.getProductId(), transformDomainItemToProtocol(item));
        }
    }

    // UTILS
    //----------

    private Protocol.LineItem transformDomainItemToProtocol(Domain.LineItem item) {
        return Protocol.LineItem.newBuilder()
                .setProductId(item.getProductId())
                .setName(item.getName())
                .setQuantity(item.getQuantity())
                .build();
    }

    private Domain.LineItem transformProtocolItemToDomain(Protocol.LineItem item) {
        return Domain.LineItem.newBuilder()
                .setProductId(item.getProductId())
                .setName(item.getName())
                .setQuantity(item.getQuantity())
                .build();
    }



    private void connectorTest() {
        ManagedChannel channel = getManagedChannel();
        RestConnectorGrpc.RestConnectorBlockingStub stub = getRpcServiceStub(channel);

        Connector.Response response = stub.getRequest(Connector.GetEntity.newBuilder()
                .setUserId("politrons")
                .setUrl("url")
                .build());

        System.out.println("Connector response:" + response);
        channel.shutdown();
    }

    /**
     * From the contract of the proto we create the FutureStub. There´re other strategies as Sync communication.
     *
     * @return
     */
    private static RestConnectorGrpc.RestConnectorBlockingStub getRpcServiceStub(ManagedChannel channel) {
        return RestConnectorGrpc.newBlockingStub(channel);
    }

    /**
     * ManagedChannel is communication channel for the RPC
     */
    private static ManagedChannel getManagedChannel() {
        return ManagedChannelBuilder.forAddress("cloudstate-rest-connector-service.cloudstate", 2981)
                .usePlaintext(true)
                .build();
    }

}
