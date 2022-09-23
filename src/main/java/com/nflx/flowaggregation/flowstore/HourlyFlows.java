package com.nflx.flowaggregation.flowstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.nflx.flowaggregation.model.FlowRecord;

/**
 * HourlyFlows keeps an aggregated count of all the bytes_rx, bytes_tx for all
 * the flows for a given hour.
 * This internally uses a concurrent map to make sure adding flow records is
 * safe and very efficient.
 * The metrics themselves are incremented atomically so we don't have race
 * conditions with updates.
 */
public class HourlyFlows {

    public record Flow(String src_app, String dest_app, String vpc_id) {
    };

    public record Metrics(AtomicLong bytes_rx, AtomicLong bytes_tx) {
    };

    private ConcurrentMap<Flow, Metrics> perHourDB = null;

    /**
     * 
     */
    public HourlyFlows() {
        perHourDB = new ConcurrentHashMap<>();
    }

    /**
     * @param hour The hour filter on all the records seen so far
     * @return List of FlowRecords for the given hour, which are sum-aggregated by
     *         Flow (src_app, dest_app, vpc_id)
     */
    public List<FlowRecord> getRecords(int hour) {
        List<FlowRecord> ret = new ArrayList<>();
        for (Entry<Flow, Metrics> e : perHourDB.entrySet()) {
            Flow f = e.getKey();
            Metrics m = e.getValue();
            ret.add(new FlowRecord(f.src_app(), f.dest_app(), f.vpc_id(), m.bytes_tx().get(), m.bytes_rx().get(),
                    hour));
        }
        return ret;
    }

    /**
     * @param record update this hourly flow with the given parameters. Increments
     *               the cumulative bytes_rx and bytes_tx for the given flow.
     */
    public void addRecord(FlowRecord record) {
        Flow f = new Flow(record.getSrc_app(), record.getDest_app(), record.getVpc_id());
        perHourDB.putIfAbsent(f, new Metrics(new AtomicLong(0), new AtomicLong(0)));
        Metrics m = perHourDB.get(f);
        m.bytes_rx.addAndGet(record.getBytes_rx());
        m.bytes_tx.addAndGet(record.getBytes_tx());
    }

}
