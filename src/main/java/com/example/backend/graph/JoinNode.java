package com.example.backend.graph;

// Nó Binário: Junção
public class JoinNode extends AbstractOperatorNode {
    // O 'final' foi removido para permitir edições pelo otimizador
    private String condition;

    public JoinNode(OperatorNode leftChild, OperatorNode rightChild, String condition) {
        this.condition = condition;
        // O filho da esquerda fica no índice 0
        this.children.add(leftChild);
        // O filho da direita fica no índice 1
        this.children.add(rightChild);
    }

    // --- Métodos necessários para o QueryOptimizer ---

    public OperatorNode getLeft() {
        return this.children.isEmpty() ? null : this.children.get(0);
    }

    public void setLeft(OperatorNode leftChild) {
        if (this.children.isEmpty()) {
            this.children.add(leftChild);
        } else {
            this.children.set(0, leftChild); // Substitui o da esquerda
        }
    }

    public OperatorNode getRight() {
        return this.children.size() > 1 ? this.children.get(1) : null;
    }

    public void setRight(OperatorNode rightChild) {
        // Garante que a lista tenha espaço até o índice 1
        while (this.children.size() < 2) {
            this.children.add(null);
        }
        this.children.set(1, rightChild); // Substitui o da direita
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    // ------------------------------------------------

    @Override
    public String getName() {
        return "⋈ (" + condition + ")";
    }
}