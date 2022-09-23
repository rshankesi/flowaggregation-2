# Flow Aggregation Service

This project  implements a fast, concurrent, in-memory REST service for aggregated network traffic flows by the hour.  For this service, the total bytes sent and recieved are aggregated on a per-flow basis, for every hour.  The users of the service can sent the individual flow records as a tuples of `(src_ip, dest_ip, vpc, bytes_rx, bytes_tx, hour)`.  The clients can query the aggregated records for any given hour, and the service will return an aggregated list of records for all the flows in that hour. A flow here is the tuple `(src_ip, dest_ip, vpc_id)`.  

Note that we can have a large number instances reporting these individual flow records, and potentially a large number of 

## Quick start
This is a microservice based on Spring Boot framework, and uses maven build automation.
### Run
``mvn clean spring-boot:run``
This would get the service up and running on the localhost:8080
### Testing
``mvn test``
### Manual testing
There are two methods supported.  
* A GET method, which requires the hour Query parameter.
```
curl "http://localhost:8080/flows?hour=5"
```
* A POST method, which can send a list of all the flow records.  There's no expectation that these flow records in a single call are pre-aggregated in any manner.
```
curl -X POST "http://localhost:8080/flows" \
-H 'Content-Type: application/json' \
-d '[{"src_app": "foo", "dest_app": "bar", "vpc_id": "vpc", "bytes_tx":100, "bytes_rx":500, "hour": 20}]'
```

## System Overview
Our  system's data is an append-only list of records.  Input records are added (concurrently) from various client end hosts, but they are never deleted.  
Our requirement is to return the 'aggregated' `List<Record>` given the hour.

A logical way of organizing this is to have the following structure
```
Write/POST endpoint: Given a List<Record> 
   Store it in Map<Hour, List<Record>>
Read/GET endpoint:  Given the hour 
    Fetch the List<Record> for that hour (from the map)
    For all the records in the list:
         Aggregate: Flow(src, dest, vpc)  -> Metrics(bytes_rx, bytes_tx)
            (sum all the matching bytes_rx, bytes_rx) per flow
```
An straightforward way to handle it is to implement just this, with appropriate synchronized barriers to make sure that the map is not corrupted due to concurrent updates.

When we look at the implementation and scalabiility of this solution, we see the following issues:
* The 'hour' key is going to be very 'hot' and temporal.   This particular key in our map will be a cause of blocking as most instances will likely be updating it for the same hour.  A synchronized map will be very slow.
* Append it to a list for a given hour will also make all the instances (for all the flows) block on a single list 

### Solution
To solve both of the issues, we adopt the following structure
```
ConcurrentMap of <Hour ->  HourlyFlow>:
    This will be generally faster than using a blocking synchronized map.  Especially when mutiple overlapping hours are being updated. 

HourlyFlow ConcurrentMap of <Flow -> Metrics>
    This will further reduce contention between different updates.  Each Flow, will only potentially be blocked on their update by records of the same flow (and not for all of the records in that hour)

    Metrics itself stores the tx/rx values as atomic numbers, so they can be updated safely in parallel.    

```
#### Runtime characteristics
* Updates will be amortized O(x), where 'x' is the number of unique flows that are present in the input record list.  
* Reads will also be amortied O(x), where 'x is the number of unique flows that are present for any given hour.  (Caveat:  Note however, that in the extreme case it's possible for all the input requests to arrive at the same time and try to create a new HourlyFlow object.  This is very dependent on the traffic patterns)

## Future work
* Read/Write end points are currently separated and can be scaled independently.  However, a further optimization would be partition the data by hour, so that we don't have the load/memory on a single table.
* Currently there's no persistence of the data.  A failed instance would lead to a loss of data.  The update flow can be modified so that we can send the records to a persistent store in an asynchronous manner.  This will 