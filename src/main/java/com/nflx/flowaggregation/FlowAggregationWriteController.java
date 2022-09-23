package com.nflx.flowaggregation;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.nflx.flowaggregation.flowstore.AllFlows;
import com.nflx.flowaggregation.model.FlowRecord;

import org.springframework.web.bind.annotation.ResponseBody;

@RestController
public class FlowAggregationWriteController {

    private final AllFlows flows;

    public FlowAggregationWriteController() {
        flows = AllFlows.getInstance();
    }

    @RequestMapping(path = "/flows", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void storeRecords(@RequestBody final List<FlowRecord> records) {
        flows.addFlowMetrics(records);
    }

}
