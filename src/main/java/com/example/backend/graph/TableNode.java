package com.example.backend.graph;

// Nó Folha: Tabela
public class TableNode extends AbstractOperatorNode {
    private final String tableName;

    public TableNode(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String getName() {
        return "Tabela: " + tableName;
    }
}
