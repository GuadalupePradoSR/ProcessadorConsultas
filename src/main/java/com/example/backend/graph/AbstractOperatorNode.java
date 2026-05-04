package com.example.backend.graph;

import java.util.ArrayList;
import java.util.List;

// Classe abstrata para reaproveitamento do gerenciamento de filhos
public abstract class AbstractOperatorNode implements OperatorNode {
    protected List<OperatorNode> children = new ArrayList<>();

    @Override
    public List<OperatorNode> getChildren() {
        return children;
    }
}
