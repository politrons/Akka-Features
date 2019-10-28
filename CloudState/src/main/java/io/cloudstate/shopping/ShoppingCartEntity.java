package io.cloudstate.shopping;

import com.google.protobuf.Empty;
import io.cloudstate.javasupport.EntityId;
import io.cloudstate.javasupport.eventsourced.*;
import io.cloudstate.shopping.domain.Domain;

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

    @CommandHandler
    public Protocol.Cart getCart() {
        return Protocol.Cart.newBuilder().addAllItems(cart.values()).build();
    }


    /**
     * A command handler may emit an event by taking in a CommandContext parameter, and invoking the emit method on it.
     * Invoking emit will immediately invoke the associated event handler for that event
     * <p>
     * [ShoppingDomain] is the factory class responsible for the creation of events. Here using for instance [ItemAdded.newBuilder()]
     * we use builder pattern to create a new Event using the command info.
     *
     * @param item Command to transform into event
     * @param ctx  of command to link the Command and Event Handler.
     * @return Empty
     */
    @CommandHandler
    public Empty addItem(Protocol.AddLineItem item, CommandContext ctx) {
        if (item.getQuantity() <= 0) {
            ctx.fail("Cannot add negative quantity of to item" + item.getProductId());
        }
        ctx.emit(Domain.ItemAdded.newBuilder()
                .setItem(Domain.LineItem.newBuilder()
                        .setProductId(item.getProductId())
                        .setName(item.getName())
                        .setQuantity(item.getQuantity())
                        .build())
                .build());
        return Empty.getDefaultInstance();
    }

    // HANDLERS
    //-------------

    /**
     * Handle method that is invoked once a Command create an event of type [ItemAdded] and ise emit into
     * the [CommandContext]
     */
    @EventHandler
    public void itemAdded(Domain.ItemAdded itemAdded) {
        Protocol.LineItem item = cart.get(itemAdded.getItem().getProductId());
        if (item == null) {
            item = convert(itemAdded.getItem());
        } else {
            item = item.toBuilder()
                    .setQuantity(item.getQuantity() + itemAdded.getItem().getQuantity())
                    .build();
        }
        cart.put(item.getProductId(), item);
    }

    private Protocol.LineItem convert(Domain.LineItem item) {
        return Protocol.LineItem.newBuilder()
                .setProductId(item.getProductId())
                .setName(item.getName())
                .setQuantity(item.getQuantity())
                .build();
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
                .addAllItems(cart.values().stream().map(this::convert).collect(Collectors.toList()))
                .build();
    }

    private Domain.LineItem convert(Protocol.LineItem item) {
        return Domain.LineItem.newBuilder()
                .setProductId(item.getProductId())
                .setName(item.getName())
                .setQuantity(item.getQuantity())
                .build();
    }

    /**
     * When the entity is REYDRATE again, the snapshot will first be loaded before any other events are received, and passed to a snapshot handler
     *
     * @param cart
     */
    @SnapshotHandler
    public void handleSnapshot(Domain.Cart cart) {
        this.cart.clear();
        for (Domain.LineItem item : cart.getItemsList()) {
            this.cart.put(item.getProductId(), convert(item));
        }
    }
}
