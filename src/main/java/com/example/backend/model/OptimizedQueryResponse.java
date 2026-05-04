package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta contendo a SQL validada, álgebra relacional, e a árvore otimizada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimizedQueryResponse {
    private boolean valid;
    private String message;
    private String originalSql;           // SQL original
    private String algebra;               // Álgebra relacional antes da otimização
    private String optimizedAlgebra;      // Álgebra relacional após otimização
    private GraphNodeDTO optimizedTree;   // Árvore otimizada em formato JSON
    private String warnings;              // Avisos (ex: produto cartesiano detectado)
}
