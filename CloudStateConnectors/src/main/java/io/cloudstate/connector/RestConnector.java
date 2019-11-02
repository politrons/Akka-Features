package io.cloudstate.connector;

import io.cloudstate.javasupport.EntityId;
import io.cloudstate.javasupport.eventsourced.CommandHandler;
import io.cloudstate.javasupport.eventsourced.EventSourcedEntity;

@EventSourcedEntity(persistenceId = "connector-actions", snapshotEvery = 20)
public class RestConnector {


    private final String entityId;

    public RestConnector(@EntityId String entityId) {
        this.entityId = entityId;
    }

    @CommandHandler
    public Connector.Response getRequest(Connector.GetEntity requestCommand) {
        System.out.println("Request to Connector:" + requestCommand.getUrl());
        return Connector.Response.newBuilder().setResponse("hello connector world").build();
    }

}
