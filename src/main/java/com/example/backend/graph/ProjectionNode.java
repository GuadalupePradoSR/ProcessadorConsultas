package com.example.backend.graph;

import java.util.List;

// Nó Unário: Projeção (Nó Raiz)
public class ProjectionNode extends AbstractOperatorNode {
    private final List<String> columns;

    public ProjectionNode(OperatorNode child, List<String> columns) {
        this.columns = columns;
        this.children.add(child);
    }

    // --- Métodos novos necessários para o QueryOptimizer ---

    public OperatorNode getChild() {
        return this.children.isEmpty() ? null : this.children.get(0);
    }

    public void setChild(OperatorNode child) {
        if (this.children.isEmpty()) {
            this.children.add(child);
        } else {
            // Substitui o filho atual pelo novo filho otimizado
            this.children.set(0, child);
        }
    }
    
    public List<String> getColumns() {
        return columns;
    }

    // ------------------------------------------------------

    @Override
    public String getName() {
        return "π (" + String.join(", ", columns) + ")";
    }
}