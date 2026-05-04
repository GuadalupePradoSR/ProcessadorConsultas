package com.example.backend.graph;

// Nó Binário: Junção
public class JoinNode extends AbstractOperatorNode {
    private final String condition;

    public JoinNode(OperatorNode leftChild, OperatorNode rightChild, String condition) {
        this.condition = condition;
        this.children.add(leftChild);
        this.children.add(rightChild);
    }

    @Override
    public String getName() {
        return "⋈ (" + condition + ")";
    }
}
