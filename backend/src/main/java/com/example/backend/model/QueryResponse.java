package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueryResponse {
    private boolean valid;
    private String message;
    private String algebra;
    private String unoptimizedGraph;
    private String optimizedGraph;

    public QueryResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
}