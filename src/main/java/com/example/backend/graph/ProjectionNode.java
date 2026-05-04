package com.example.backend.graph;

import java.util.List;

// Nó Unário: Projeção (Nó Raiz)
public class ProjectionNode extends AbstractOperatorNode {
    private final List<String> columns;

    public ProjectionNode(OperatorNode child, List<String> columns) {
        this.columns = columns;
        this.children.add(child);
    }

    @Override
    public String getName() {
        return "π (" + String.join(", ", columns) + ")";
    }
}
