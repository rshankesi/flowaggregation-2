package com.nflx.flowaggregation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nflx.flowaggregation.flowstore.AllFlows;
import com.nflx.flowaggregation.model.FlowRecord;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
public class FlowAggregationReadController {

    private final AllFlows flows;

    public FlowAggregationReadController() {
        flows = AllFlows.getInstance();
    }

    @GetMapping(value = "/flows")
    @ResponseBody
    public List<FlowRecord> getMatchingRecords(@RequestParam(value = "hour") Integer hour) {
        try {
            return flows.getHourly(hour);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Problem with input", e);
        }
    }
}
