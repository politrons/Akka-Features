package io.cloudstate.shopping;

import io.cloudstate.javasupport.CloudState;

/**
 * Main class to register the entity created using [CloudState] instance and the [registerEventSourcedEntity]
 * in this case since the entity is an EventSourcing entity.
 */
public class ShoppingCartMain {

    public static void main(String... args) {
        new CloudState()
                .registerEventSourcedEntity(
                        ShoppingCartEntity.class,
                        ShoppingProtocol.getDescriptor().findServiceByName("ShoppingCartService"),
                        ShoppingDomain.getDescriptor())
                .start();
    }
}