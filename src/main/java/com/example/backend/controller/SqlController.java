package com.example.backend.controller;

import com.example.backend.model.QueryRequest;
import com.example.backend.model.QueryResponse;
import com.example.backend.service.SqlValidatorService;
import com.example.backend.service.ConversionAlgebra; 
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sql")
public class SqlController {

    private final SqlValidatorService validatorService;
    private final ConversionAlgebra conversionAlgebra;


    public SqlController(SqlValidatorService validatorService, ConversionAlgebra conversionAlgebra) {
        this.validatorService = validatorService;
        this.conversionAlgebra = conversionAlgebra;
    }

    // @PostMapping("/validate")
    // public QueryResponse validateQuery(@RequestBody QueryRequest request) {
    //     return validatorService.validate(request.getQuery());
    // }

      @PostMapping("/validate")
    public QueryResponse validateQuery(@RequestBody QueryRequest request) {
        String sql = request.getQuery();
        
        // 1. Manda a string pro validador
        QueryResponse response = validatorService.validate(sql);

        // 2. Se for válida, manda a mesma string pro seu segundo arquivo!
        if (response.isValid()) {
            String algebra = conversionAlgebra.convertToAlgebra(sql);
            response.setAlgebra(algebra);
        }

        // Retorna pro front o resultado (Validação + Álgebra gerada)
        return response;
    }
}