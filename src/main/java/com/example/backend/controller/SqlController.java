package com.example.backend.controller;

import com.example.backend.model.QueryRequest;
import com.example.backend.model.QueryResponse;
import com.example.backend.model.OptimizedQueryResponse;
import com.example.backend.model.ComparisonQueryResponse;
import com.example.backend.model.GraphNodeDTO;
import com.example.backend.service.SqlValidatorService;
import com.example.backend.service.ConversionAlgebra;
import com.example.backend.service.QueryOptimizationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sql")
public class SqlController {

    private final SqlValidatorService validatorService;
    private final ConversionAlgebra conversionAlgebra;
    private final QueryOptimizationService optimizationService;

    public SqlController(SqlValidatorService validatorService, 
                        ConversionAlgebra conversionAlgebra,
                        QueryOptimizationService optimizationService) {
        this.validatorService = validatorService;
        this.conversionAlgebra = conversionAlgebra;
        this.optimizationService = optimizationService;
    }

    /**
     * Valida SQL e retorna a álgebra relacional.
     */
    @PostMapping("/validate")
    public QueryResponse validateQuery(@RequestBody QueryRequest request) {
        String sql = request.getQuery();
        
        // 1. Valida SQL
        QueryResponse response = validatorService.validate(sql);

        // 2. Se for válida, converte para Álgebra Relacional
        if (response.isValid()) {
            String algebra = conversionAlgebra.convertToAlgebra(sql);
            response.setAlgebra(algebra);
        }

        // Retorna pro front o resultado (Validação + Álgebra gerada)
        return response;
    }

    /**
     * Pipeline completo de otimização: valida, converte para álgebra, 
     * constrói a árvore, aplica heurísticas e retorna tudo em JSON.
     */
    @PostMapping("/optimize")
    public OptimizedQueryResponse optimizeQuery(@RequestBody QueryRequest request) {
        return optimizationService.optimizeQuery(request.getQuery());
    }

    /**
     * Retorna apenas a árvore otimizada em formato JSON.
     */
    @PostMapping("/optimize/tree")
    public GraphNodeDTO getOptimizedTree(@RequestBody QueryRequest request) {
        GraphNodeDTO tree = optimizationService.getOptimizedTree(request.getQuery());
        
        if (tree == null) {
            // Retorna um nó vazio com mensagem de erro
            GraphNodeDTO errorNode = new GraphNodeDTO();
            errorNode.setType("ERROR");
            errorNode.setLabel("Erro ao processar a consulta");
            return errorNode;
        }
        
        return tree;
    }

    /**
     * Retorna comparação lado-a-lado: árvore original vs otimizada.
     * Útil para visualização educativa das otimizações.
     */
    @PostMapping("/optimize/compare")
    public ComparisonQueryResponse compareOptimization(@RequestBody QueryRequest request) {
        return optimizationService.compareQueries(request.getQuery());
    }
}