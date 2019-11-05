package io.cloudstate.shopping;

import io.cloudstate.connector.Connector;
import io.cloudstate.connector.RestConnectorGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Ignore
public class ConnectorTest {


    @Test
    public void connectorTest() {
        ManagedChannel channel = getManagedChannel();
        RestConnectorGrpc.RestConnectorBlockingStub stub = getRpcServiceStub(channel);

        Connector.RestResponse response = stub.makeRequest(Connector.RestRequest.newBuilder()
                .setUserId("politrons")
                .setMethod(Connector.Method.GET)
                .setHost("www.mocky.io")
                .setUri("/v2/5185415ba171ea3a00704eed")
                .setPort(80)
                .build());

        System.out.println("Connector response:" + response);
        assert (response != null);
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
        return ManagedChannelBuilder.forAddress("localhost", 2981)
                .usePlaintext(true)
                .build();
    }
}
