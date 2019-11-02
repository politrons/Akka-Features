package io.cloudstate.connector;

import com.google.protobuf.Descriptors;
import io.cloudstate.javasupport.CloudState;

import java.util.concurrent.ExecutionException;

/**
 * Main class to register the entity created using [CloudState] instance and the [registerEventSourcedEntity]
 * in this case since the entity is an EventSourcing entity.
 */
public class RestConnectorMain {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Descriptors.ServiceDescriptor shoppingCartService = Connector.getDescriptor().findServiceByName("RestConnector");
        System.out.println("Protocol descriptor:" + shoppingCartService);
        new CloudState()
                .registerEventSourcedEntity(
                        RestConnector.class,
                        shoppingCartService)
                .start()
                .toCompletableFuture()
                .get();
    }
}