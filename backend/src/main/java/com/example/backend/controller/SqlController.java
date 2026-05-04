package com.example.backend.controller;

import com.example.backend.model.QueryRequest;
import com.example.backend.model.QueryResponse;
import com.example.backend.service.SqlValidatorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sql")
public class SqlController {

    private final SqlValidatorService validatorService;

    public SqlController(SqlValidatorService validatorService) {
        this.validatorService = validatorService;
    }

    @PostMapping("/validate")
    public QueryResponse validateQuery(@RequestBody QueryRequest request) {
        return validatorService.validate(request.getQuery());
    }
}