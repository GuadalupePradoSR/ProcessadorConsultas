package com.example.backend.graph;

// Nó Folha: Tabela
public class TableNode extends AbstractOperatorNode {
    private final String tableName;

    public TableNode(String tableName) {
        this.tableName = tableName;
    }

    // --- Método novo necessário para o QueryOptimizer ---
    public String getTableName() {
        return tableName;
    }
    // --------------------------------------------------

    @Override
    public String getName() {
        return "Tabela: " + tableName;
    }
}