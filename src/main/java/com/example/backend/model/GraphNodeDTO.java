package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO para representar um nó da árvore de execução em formato JSON.
 * Permite a visualização do grafo otimizado no cliente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNodeDTO {
    private String id;                    // ID único do nó (hash)
    private String type;                  // Tipo do nó: TABLE, PROJECTION, SELECTION, JOIN
    private String label;                 // Label exibido (ex: "σ (age > 18)")
    private String details;               // Detalhes adicionais
    private List<GraphNodeDTO> children;  // Filhos
    private String tableName;             // Nome da tabela (se for TABLE)
    private List<String> columns;         // Colunas (se for PROJECTION)
    private String condition;             // Condição (se for SELECTION ou JOIN)
    private int depth;                    // Profundidade na árvore
}
