package com.nflx.flowaggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.nflx.flowaggregation.model.FlowRecord;

public class FlowAggregationReadWriteTests {
    @Test
    void testEmptyRead() {
        final FlowAggregationReadController readController = new FlowAggregationReadController();
        final FlowAggregationWriteController writeController = new FlowAggregationWriteController();
        List<FlowRecord> records = readController.getMatchingRecords(500);
        // Initially we should get no matches
        assertEquals(0, records.size());
        List<FlowRecord> input = new ArrayList<>();
        input.add(new FlowRecord("src_app", "dest_app", "vpc_id", 100L, 200L, 1));
        writeController.storeRecords(records);
        records = readController.getMatchingRecords(2);
        assertEquals(0, records.size());
    }

    @Test
    void testUniqueRecordsNoAggregation() {
        // Different hours are sent within the same POST call
        final FlowAggregationReadController readController = new FlowAggregationReadController();
        final FlowAggregationWriteController writeController = new FlowAggregationWriteController();
        final List<FlowRecord> input = new ArrayList<>();
        for (int i = 100; i < 150; i++) {
            input.add(new FlowRecord("src_app", "dest_app", "vpc_id", 100L, 200L, i));
        }
        writeController.storeRecords(input);
        for (int i = 100; i < 150; i++) {
            List<FlowRecord> records = readController.getMatchingRecords(i);
            assertEquals(records.size(), 1);
            FlowRecord expected = new FlowRecord("src_app", "dest_app", "vpc_id", 100L, 200L, i);
            assertEquals(expected, records.get(0));
        }
    }

    @Test
    void testFlowAggregation() {
        // Duplicated flows are sent within the same POST, they should still be aggregated
        final FlowAggregationReadController readController = new FlowAggregationReadController();
        final FlowAggregationWriteController writeController = new FlowAggregationWriteController();
        final List<FlowRecord> input = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            input.add(new FlowRecord("src_app1", "dest_app", "vpc_id", 200L, 300L, 750));
            input.add(new FlowRecord("src_app2", "dest_app", "vpc_id", 100L, 200L, 750));
        }
        writeController.storeRecords(input);
        List<FlowRecord> records = readController.getMatchingRecords(750);
        assertEquals(2, records.size());

        List<FlowRecord> expected = new ArrayList<>();
        expected.add(new FlowRecord("src_app1", "dest_app", "vpc_id", 10 * 200L, 10 * 300L, 750));
        expected.add(new FlowRecord("src_app2", "dest_app", "vpc_id", 10 * 100L, 10 * 200L, 750));
        Assertions.assertThat(expected).hasSameElementsAs(records);

    }

    @Test
    void testLargeFlowSizeAggregation() {
        // Very large input values for the bytes_tx, bytes_rx fields.  They should still be summed without overflow 
        final FlowAggregationReadController readController = new FlowAggregationReadController();
        final FlowAggregationWriteController writeController = new FlowAggregationWriteController();
        final List<FlowRecord> input = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            input.add(new FlowRecord("src_app1", "dest_app", "vpc_id", (long)Integer.MAX_VALUE, 300L, 950));
            input.add(new FlowRecord("src_app2", "dest_app", "vpc_id", 100L, 3L * Integer.MAX_VALUE, 950));
        }
        writeController.storeRecords(input);
        List<FlowRecord> records = readController.getMatchingRecords(950);
        assertEquals(2, records.size());

        List<FlowRecord> expected = new ArrayList<>();
        expected.add(new FlowRecord("src_app1", "dest_app", "vpc_id", 10 * ((long)Integer.MAX_VALUE), 10 * 300L, 950));
        expected.add(new FlowRecord("src_app2", "dest_app", "vpc_id", 10 * 100L, 10 * 3L * Integer.MAX_VALUE, 950));
        Assertions.assertThat(expected).hasSameElementsAs(records);

    }

}
