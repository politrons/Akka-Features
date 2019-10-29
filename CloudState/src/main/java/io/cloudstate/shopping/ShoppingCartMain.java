package io.cloudstate.shopping;

import com.google.protobuf.Descriptors;
import io.cloudstate.javasupport.CloudState;
import io.cloudstate.shopping.domain.Domain;

import java.util.concurrent.ExecutionException;

/**
 * Main class to register the entity created using [CloudState] instance and the [registerEventSourcedEntity]
 * in this case since the entity is an EventSourcing entity.
 */
public class ShoppingCartMain {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Descriptors.ServiceDescriptor shoppingCartService = Protocol.getDescriptor().findServiceByName("ShoppingCartService");
        Descriptors.FileDescriptor descriptor = Domain.getDescriptor();
        System.out.println("Protocol descriptor:" + shoppingCartService);
        System.out.println("Domain descriptor:" + descriptor);
        new CloudState()
                .registerEventSourcedEntity(
                        ShoppingCartEntity.class,
                        shoppingCartService,
                        descriptor)
                .start()
                .toCompletableFuture()
                .get();
    }
}