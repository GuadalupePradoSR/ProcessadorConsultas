package com.example.backend.graph;

import java.util.List;

// Interface base para todos os nós
public interface OperatorNode {
    String getName();
    List<OperatorNode> getChildren();
}
