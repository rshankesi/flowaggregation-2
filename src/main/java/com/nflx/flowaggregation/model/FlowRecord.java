package com.nflx.flowaggregation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class FlowRecord {
    @NonNull
    String src_app; 
    @NonNull
    String dest_app;
    @NonNull
    String vpc_id;
    @NonNull
    Long bytes_tx;
    @NonNull
    Long bytes_rx;
    @NonNull
    Integer hour;
}