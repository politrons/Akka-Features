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
        System.out.println("Hello connector");

        Descriptors.ServiceDescriptor restConnector = Connector.getDescriptor().findServiceByName("RestConnector");
        System.out.println("Coudstate Rest Connector:" + restConnector);
        new CloudState()
                .registerCrdtEntity(
                        RestConnector.class,
                        restConnector)
                .start()
                .toCompletableFuture()
                .get();
    }
}