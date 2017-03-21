package com.salesforce.storm.spout.sideline.tupleBuffer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.salesforce.storm.spout.sideline.KafkaMessage;
import com.salesforce.storm.spout.sideline.TupleMessageId;
import com.salesforce.storm.spout.sideline.config.SidelineSpoutConfig;
import org.apache.storm.tuple.Values;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Kind of silly.  Basically just testing a FIFO buffer.
 */
public class FIFOBufferTest {
    /**
     * Basically just tests that this does FIFO.
     * Kind of silly.
     */
    @Test
    public void doTest() throws InterruptedException {
        // numberOfVSpoutIds * numberOfMessagesPer should be less than the configured
        // max buffer size.
        final int numberOfVSpoutIds = 5;
        final int numberOfMessagesPer = 1500;
        final int maxBufferSize = (numberOfMessagesPer * numberOfVSpoutIds) + 1;

        // Create config
        Map<String, Object> config = Maps.newHashMap();
        config.put(SidelineSpoutConfig.TUPLE_BUFFER_MAX_SIZE, maxBufferSize);

        // Create buffer & open
        TupleBuffer tupleBuffer = new FIFOBuffer();
        tupleBuffer.open(config);

        // Keep track of our order
        final List<KafkaMessage> submittedOrder = Lists.newArrayList();

        // Create random number generator
        Random random = new Random();

        // Generate messages
        for (int x=0; x<(numberOfMessagesPer * numberOfVSpoutIds); x++) {
            // Generate source spout id
            final String sourceSpoutId = "srcSpoutId" + random.nextInt(numberOfVSpoutIds);
            final int partition = random.nextInt(10);


            KafkaMessage kafkaMessage = new KafkaMessage(
                    new TupleMessageId("my topic", partition, x, sourceSpoutId),
                    new Values("poop" + x));

            // Keep track of order
            submittedOrder.add(kafkaMessage);

            // Add to our buffer
            tupleBuffer.put(kafkaMessage);
        }

        // Validate size
        assertEquals("Size should be known", (numberOfMessagesPer * numberOfVSpoutIds), tupleBuffer.size());

        // Now pop them, order should be maintained
        for (KafkaMessage originalKafkaMsg: submittedOrder) {
            final KafkaMessage bufferedMsg = tupleBuffer.poll();

            // Validate it
            assertNotNull("Should not be null", bufferedMsg);
            assertEquals("Objects should be the same", originalKafkaMsg, bufferedMsg);

            // Validate the contents are the same
            assertEquals("Source Spout Id should be equal", originalKafkaMsg.getTupleMessageId().getSrcConsumerId(), bufferedMsg.getTupleMessageId().getSrcConsumerId());
            assertEquals("partitions should be equal", originalKafkaMsg.getPartition(), bufferedMsg.getPartition());
            assertEquals("offsets should be equal", originalKafkaMsg.getOffset(), bufferedMsg.getOffset());
            assertEquals("topic should be equal", originalKafkaMsg.getTopic(), bufferedMsg.getTopic());
            assertEquals("TupleMessageIds should be equal", originalKafkaMsg.getTupleMessageId(), bufferedMsg.getTupleMessageId());
            assertEquals("Values should be equal", originalKafkaMsg.getValues(), bufferedMsg.getValues());
        }

        // Validate that the next polls are all null
        for (int x=0; x<64; x++) {
            assertNull("Should be null", tupleBuffer.poll());
        }
    }
}