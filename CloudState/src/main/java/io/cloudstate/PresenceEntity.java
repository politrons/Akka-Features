package io.cloudstate;

import com.google.protobuf.Empty;
import io.cloudstate.javasupport.CloudState;
import io.cloudstate.javasupport.EntityId;
import io.cloudstate.javasupport.crdt.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A presence entity.
 * <p/>
 * There will be at most one of these on each node per user. It holds the CRDT, which is a Vote CRDT, which
 * allows us to register this nodes vote as to whether the user is currently online. The Vote CRDT then tells
 * us how many nodes have voted, and if at least one of them voted true, we know the user is online.
 */
@CrdtEntity
public class PresenceEntity {
    /**
     * The user who this entity represents.
     */
    private final String username;

    /**
     * The vote CRDT.
     */
    private final Vote vote;

    /**
     * The number of users currently connected to this node. Note, this is only the users on this node, not the users
     * on all nodes together.
     */
    private int currentUsers = 0;

    /**
     * Constructor. The CloudState java support library makes several different types of parameters available both for
     * injection into the constructor, as well as passing into any CommandHandler. Those are the context, the CRDT, and
     * the entity id, which in this case is the user that this entity is for.
     */
    public PresenceEntity(Optional<Vote> vote, CrdtCreationContext ctx, @EntityId String username) {
        this.username = username;
        // If there's an existing CRDT, we use that, otherwise we create a new one.
        this.vote = vote.orElseGet(ctx::newVote);
    }

    /**
     * Handler for the connect command.
     * <p/>
     * A user is online as long as there is an active streamed connection for them to at least one node.
     * <p/>
     * Note the input for this command is the User to monitor. The proxy has already inspected that, extracted the
     * username as the entity id, and directed the request to this entity, so there's no useful information for us
     * to handle here, hence we don't need to declare the command as a parameter to this method. Also, when a user
     * is connected, we don't send them anything in response, so the output of this command handler is void.
     */
    @CommandHandler
    public void connect(StreamedCommandContext<Empty> ctx) {
        currentUsers += 1;
        if (currentUsers == 1) {
            // If the number of users on this node when up from zero, then we change our vote.
            vote.vote(true);
        }

        // Register a callback for when the user disconnects.
        ctx.onCancel(cancelled -> {
            currentUsers -= 1;
            if (currentUsers == 0) {
                // If the number of users on this node went down to zero, then we change our vote.
                vote.vote(false);
            }
        });
    }

    /**
     * Handler for the monitor command.
     */
    @CommandHandler
    public PresenceProtos.OnlineStatus monitor(StreamedCommandContext<PresenceProtos.OnlineStatus> ctx) {
        AtomicBoolean lastStatus = new AtomicBoolean(vote.isAtLeastOne());

        // Register a callback so that whenever the vote changes, we can handle it.
        ctx.onChange(change -> {
            // We should check that the status has actually changed, the notification could just be that the
            // user has connected on another node, or another node has come online.
            if (lastStatus.get() != vote.isAtLeastOne()) {
                lastStatus.set(vote.isAtLeastOne());
                return Optional.of(statusMessage());
            } else {
                return Optional.empty();
            }
        });

        return statusMessage();
    }

    /**
     * Convenience method for building the OnlineStatus message for the current status.
     */
    private PresenceProtos.OnlineStatus statusMessage() {
        return PresenceProtos.OnlineStatus.newBuilder()
                .setOnline(vote.isAtLeastOne())
                .build();
    }

    /**
     * Main method.
     */
    public static void main(String... args) {
        // Register this entity to handle the Presence service.
        CloudState cloudState = new CloudState().registerCrdtEntity(PresenceEntity.class,
                PresenceProtos.getDescriptor().findServiceByName("Presence")
        );
        cloudState.start();
    }
}