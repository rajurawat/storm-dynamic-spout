package com.salesforce.storm.spout.sideline.tupleBuffer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.salesforce.storm.spout.sideline.KafkaMessage;
import com.salesforce.storm.spout.sideline.TupleMessageId;
import com.salesforce.storm.spout.sideline.config.SidelineSpoutConfig;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.storm.tuple.Values;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Kind of silly.  Basically just testing a FIFO buffer.
 */
@RunWith(DataProviderRunner.class)
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
                    new Values("value" + x));

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
            assertEquals("Source Spout Id should be equal", originalKafkaMsg.getTupleMessageId().getSrcVirtualSpoutId(), bufferedMsg.getTupleMessageId().getSrcVirtualSpoutId());
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

    /**
     * Makes sure that we can properly parse long config values on open().
     */
    @Test
    @UseDataProvider("provideConfigObjects")
    public void testConstructorWithConfigValue(Number inputValue) throws InterruptedException {
        // Create config
        Map<String, Object> config = Maps.newHashMap();
        config.put(SidelineSpoutConfig.TUPLE_BUFFER_MAX_SIZE, inputValue);

        // Create buffer
        FIFOBuffer tupleBuffer = new FIFOBuffer();
        tupleBuffer.open(config);

        // Validate
        assertEquals("Set correct", inputValue.intValue(), ((LinkedBlockingQueue)tupleBuffer.getUnderlyingQueue()).remainingCapacity());
    }

    /**
     * Provides various tuple buffer implementation.
     */
    @DataProvider
    public static Object[][] provideConfigObjects() throws InstantiationException, IllegalAccessException {
        return new Object[][]{
                // Integer
                { 200 },

                // Long
                { 2000L }
        };
    }
}