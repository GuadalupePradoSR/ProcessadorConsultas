package com.example.backend.graph;

// Nó Unário: Seleção
public class SelectionNode extends AbstractOperatorNode {
    // O 'final' foi removido para permitir que o otimizador atualize a condição (ex: ao dividir os ANDs)
    private String condition;

    public SelectionNode(OperatorNode child, String condition) {
        this.condition = condition;
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

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    // ------------------------------------------------------

    @Override
    public String getName() {
        return "σ (" + condition + ")";
    }
}