package com.salesforce.storm.spout.sideline.persistence;

import com.salesforce.storm.spout.sideline.kafka.ConsumerState;
import com.salesforce.storm.spout.sideline.trigger.SidelineRequestIdentifier;
import com.salesforce.storm.spout.sideline.trigger.SidelineRequest;
import com.salesforce.storm.spout.sideline.trigger.SidelineType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In memory persistence layer implementation. useful for tests.
 * NOT for production use as all state will be lost between JVM restarts.
 */
public class InMemoryPersistenceManager implements PersistenceManager, Serializable {
    // "Persists" consumer state in memory.
    private Map<String, ConsumerState> storedConsumerState;

    // "Persists" side line request states in memory.
    private Map<SidelineRequestIdentifier, SidelinePayload> storedSidelineRequests;

    @Override
    public void open(Map topologyConfig) {
        // Allow non-destructive re-opening
        if (storedConsumerState == null) {
            storedConsumerState = new HashMap<>();
        }
        if (storedSidelineRequests == null) {
            storedSidelineRequests = new HashMap<>();
        }
    }

    @Override
    public void close() {
        // Cleanup
        storedConsumerState.clear();
        storedSidelineRequests.clear();
    }

    /**
     * Pass in the consumer state that you'd like persisted.
     * @param consumerState - ConsumerState to be persisted.
     */
    @Override
    public void persistConsumerState(String consumerId, ConsumerState consumerState) {
        storedConsumerState.put(consumerId, consumerState);
    }

    /**
     * Retrieves the consumer state from the persistence layer.
     * @return ConsumerState
     */
    @Override
    public ConsumerState retrieveConsumerState(String consumerId) {
        return storedConsumerState.get(consumerId);
    }

    @Override
    public void clearConsumerState(String consumerId) {
        storedConsumerState.remove(consumerId);
    }

    /**
     * @param type - SidelineType (Start or Stop)
     * @param id - unique identifier for the sideline request.
     * @param endingState - The state when we can stop consuming.
     */
    @Override
    public void persistSidelineRequestState(SidelineType type, SidelineRequestIdentifier id, SidelineRequest request, ConsumerState startingState, ConsumerState endingState) {
        storedSidelineRequests.put(id, new SidelinePayload(type, id, request, startingState, null));
    }

    /**
     * Retrieves a sideline request state for the given SidelineRequestIdentifier.
     * @param id - SidelineRequestIdentifier you want to retrieve the state for.
     * @return The ConsumerState that was persisted via persistSidelineRequestState().
     */
    @Override
    public SidelinePayload retrieveSidelineRequest(SidelineRequestIdentifier id) {
        return storedSidelineRequests.get(id);
    }

    @Override
    public void clearSidelineRequest(SidelineRequestIdentifier id) {
        storedSidelineRequests.remove(id);
    }

    @Override
    public List<SidelineRequestIdentifier> listSidelineRequests() {
        return new ArrayList<>(storedSidelineRequests.keySet());
    }
}
