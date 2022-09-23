package com.nflx.flowaggregation.flowstore;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.nflx.flowaggregation.model.FlowRecord;

/**
 * AllFlows keeps track of all the flows seen by the service so far.
 * It's efficient to fetch metrics by the hour, and safe to do so concurrently.
 */
public class AllFlows {

   /**
    * This map stores an aggregated metrics for every hour.
    *
    */
   private final ConcurrentHashMap<Integer, HourlyFlows> aggregatedMap;
   private static AllFlows db = new AllFlows();

   public static AllFlows getInstance() {
      return db;
   }

   protected AllFlows() {
      this.aggregatedMap = new ConcurrentHashMap<>();
      // Future-Work: In case we support persisent storage of records, this can update
      // the in memory map on startup
   }

   public List<FlowRecord> getHourly(int hour) {
      aggregatedMap.putIfAbsent(hour, new HourlyFlows());
      HourlyFlows matched = aggregatedMap.get(hour);
      return matched.getRecords(hour);
   }

   public void addFlowMetrics(List<FlowRecord> records) {
      // Pre-aggregation is possible here, before we update the DB
      for (FlowRecord r : records) {
         int hour = r.getHour();
         db.aggregatedMap.putIfAbsent(hour, new HourlyFlows());
         HourlyFlows hf = db.aggregatedMap.get(hour);
         hf.addRecord(r);
      }
      // Future-Work: In case we support persisent storage of records
      // this method should forward the record to be stored asynchronously in some
      // persistent storage
   }
}
