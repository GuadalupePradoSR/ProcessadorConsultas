package com.example.backend.graph;

// Nó Unário: Seleção
public class SelectionNode extends AbstractOperatorNode {
    private final String condition;

    public SelectionNode(OperatorNode child, String condition) {
        this.condition = condition;
        this.children.add(child);
    }

    @Override
    public String getName() {
        return "σ (" + condition + ")";
    }
}
