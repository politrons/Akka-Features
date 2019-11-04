package io.cloudstate.shopping;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

//@Ignore
public class ShoppingCartTest {


    @Test
    public void AddItemAndGetCart() throws InterruptedException, ExecutionException, TimeoutException {
        ManagedChannel channel = getManagedChannel();
        ShoppingCartGrpc.ShoppingCartBlockingStub stub = getRpcServiceStub(channel);

        stub.addItem(Protocol.AddLineItem.newBuilder()
                .setUserId("politrons")
                .setName("coca-cola")
                .setProductId("productId")
                .setQuantity(1)
                .build());

        Protocol.Cart cart = stub.getCart(Protocol.GetShoppingCart.newBuilder()
                .setUserId("politrons")
                .build());

        System.out.println("Shopping response:" + cart);
        assert (cart != null);
        assert (cart.getItemsCount() > 0);
        channel.shutdown();
    }

    /**
     * From the contract of the proto we create the FutureStub. There´re other strategies as Sync communication.
     *
     * @return
     */
    private static ShoppingCartGrpc.ShoppingCartBlockingStub getRpcServiceStub(ManagedChannel channel) {
        return ShoppingCartGrpc.newBlockingStub(channel);
    }

    /**
     * ManagedChannel is communication channel for the RPC
     */
    private static ManagedChannel getManagedChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 1981)
                .usePlaintext(true)
                .build();
    }
}
