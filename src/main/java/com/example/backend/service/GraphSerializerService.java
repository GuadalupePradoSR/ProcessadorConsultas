package com.example.backend.service;

import com.example.backend.model.GraphNodeDTO;
import com.example.backend.graph.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço que converte a árvore de operadores em DTO JSON.
 */
public class GraphSerializerService {

    /**
     * Serializa uma árvore de operadores em um DTO JSON.
     */
    public GraphNodeDTO serializeTree(OperatorNode root) {
        return serializeNode(root, 0);
    }

    /**
     * Converte um nó de operador em GraphNodeDTO de forma recursiva.
     */
    private GraphNodeDTO serializeNode(OperatorNode node, int depth) {
        if (node == null) {
            return null;
        }

        GraphNodeDTO dto = new GraphNodeDTO();
        dto.setId(String.valueOf(node.hashCode()));
        dto.setDepth(depth);
        dto.setLabel(node.getName());

        // Serializa filhos
        List<GraphNodeDTO> childDtos = new ArrayList<>();
        for (OperatorNode child : node.getChildren()) {
            GraphNodeDTO childDto = serializeNode(child, depth + 1);
            if (childDto != null) {
                childDtos.add(childDto);
            }
        }
        dto.setChildren(childDtos);

        // Tipo e detalhes específicos de cada nó
        if (node instanceof TableNode) {
            TableNode tableNode = (TableNode) node;
            dto.setType("TABLE");
            dto.setTableName(tableNode.getTableName());
            dto.setDetails("Tabela: " + tableNode.getTableName());

        } else if (node instanceof ProjectionNode) {
            ProjectionNode projNode = (ProjectionNode) node;
            dto.setType("PROJECTION");
            dto.setColumns(projNode.getColumns());
            dto.setDetails("π (" + String.join(", ", projNode.getColumns()) + ")");

        } else if (node instanceof SelectionNode) {
            SelectionNode selNode = (SelectionNode) node;
            dto.setType("SELECTION");
            dto.setCondition(selNode.getCondition());
            dto.setDetails("σ (" + selNode.getCondition() + ")");

        } else if (node instanceof JoinNode) {
            JoinNode joinNode = (JoinNode) node;
            dto.setType("JOIN");
            dto.setCondition(joinNode.getCondition());

            if ("Cartesiano".equals(joinNode.getCondition())) {
                dto.setDetails("⋈ (Produto Cartesiano) ⚠️");
            } else {
                dto.setDetails("⋈ (" + joinNode.getCondition() + ")");
            }
        }

        return dto;
    }

    /**
     * Converte a árvore em uma representação em string (Álgebra Relacional).
     */
    public String treeToAlgebraString(OperatorNode node) {
        if (node == null) {
            return "";
        }

        if (node instanceof TableNode) {
            return ((TableNode) node).getTableName();
        }

        if (node instanceof ProjectionNode) {
            ProjectionNode proj = (ProjectionNode) node;
            String cols = String.join(", ", proj.getColumns());
            return "π_(" + cols + ")(" + treeToAlgebraString(proj.getChild()) + ")";
        }

        if (node instanceof SelectionNode) {
            SelectionNode sel = (SelectionNode) node;
            return "σ_(" + sel.getCondition() + ")(" + treeToAlgebraString(sel.getChild()) + ")";
        }

        if (node instanceof JoinNode) {
            JoinNode join = (JoinNode) node;
            String left = treeToAlgebraString(join.getLeft());
            String right = treeToAlgebraString(join.getRight());

            if ("Cartesiano".equals(join.getCondition())) {
                return "(" + left + " × " + right + ")";
            } else {
                return "(" + left + " ⋈_{" + join.getCondition() + "} " + right + ")";
            }
        }

        return "";
    }
}
