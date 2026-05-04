package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta comparando a árvore original com a árvore otimizada.
 * Útil para visualização lado-a-lado das diferenças.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonQueryResponse {
    private String originalSql;           // SQL original
    private String algebra;               // Álgebra relacional antes
    private GraphNodeDTO originalTree;    // Árvore antes da otimização
    private String optimizedAlgebra;      // Álgebra relacional depois
    private GraphNodeDTO optimizedTree;   // Árvore depois da otimização
    private String optimizationSummary;   // Resumo das otimizações aplicadas
}
