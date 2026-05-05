package com.example.backend.service;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GraphBuilderService {

    public String buildUnoptimizedGraph(String sql) {
        return buildGraph(sql, false);
    }

    public String buildOptimizedGraph(String sql) {
        return buildGraph(sql, true);
    }

    private String buildGraph(String sql, boolean optimized) {
        try {
            String cleanSql = sql.trim().replaceAll("\\s+", " ");
            Statement statement = CCJSqlParserUtil.parse(cleanSql);

            if (!(statement instanceof Select)) return "";
            Select selectStatement = (Select) statement;
            if (!(selectStatement.getSelectBody() instanceof PlainSelect)) return "";

            PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
            
            StringBuilder mermaid = new StringBuilder("graph TD;\n");
            int nodeId = 0;

            // Projection (Root)
            String projectionId = "N" + (nodeId++);
            String cols = plainSelect.getSelectItems().stream().map(Object::toString).collect(Collectors.joining(", "));
            mermaid.append("    ").append(projectionId).append("[\"&pi; (").append(cols).append(")\"];\n");

            // Selection (Where)
            String parentId = projectionId;
            String selectionId = null;
            if (plainSelect.getWhere() != null && !optimized) {
                selectionId = "N" + (nodeId++);
                String condicao = plainSelect.getWhere().toString();
                mermaid.append("    ").append(selectionId).append("[\"&sigma; (").append(condicao).append(")\"];\n");
                mermaid.append("    ").append(parentId).append(" --> ").append(selectionId).append(";\n");
                parentId = selectionId;
            }

            // Tables and Joins
            String baseTable = plainSelect.getFromItem().toString();
            String baseTableId = "N" + (nodeId++);
            mermaid.append("    ").append(baseTableId).append("[\"").append(baseTable).append("\"];\n");

            // For optimized graph, we push the selection down to the base table (simplification)
            String targetForFirstJoinLeft = baseTableId;

            if (optimized && plainSelect.getWhere() != null) {
                String pushedSelectionId = "N" + (nodeId++);
                String condicao = plainSelect.getWhere().toString();
                mermaid.append("    ").append(pushedSelectionId).append("[\"&sigma; (").append(condicao).append(")\"];\n");
                mermaid.append("    ").append(pushedSelectionId).append(" --> ").append(baseTableId).append(";\n");
                targetForFirstJoinLeft = pushedSelectionId; // The first join will point to this selection instead of the base table
            }

            if (plainSelect.getJoins() != null && !plainSelect.getJoins().isEmpty()) {
                String currentJoinParent = null;

                for (Join join : plainSelect.getJoins()) {
                    String joinId = "N" + (nodeId++);
                    String tableName = join.getRightItem().toString();
                    String tableId = "N" + (nodeId++);
                    
                    mermaid.append("    ").append(tableId).append("[\"").append(tableName).append("\"];\n");
                    
                    String condicaoJoin = "";
                    if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
                        condicaoJoin = join.getOnExpressions().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" AND "));
                    }

                    mermaid.append("    ").append(joinId).append("[\"⋈ (").append(condicaoJoin).append(")\"];\n");
                    
                    if (currentJoinParent == null) {
                        // First join connects to the base table (or its pushed down selection in optimized mode)
                        mermaid.append("    ").append(joinId).append(" --> ").append(targetForFirstJoinLeft).append(";\n");
                        mermaid.append("    ").append(joinId).append(" --> ").append(tableId).append(";\n");
                    } else {
                        mermaid.append("    ").append(joinId).append(" --> ").append(currentJoinParent).append(";\n");
                        mermaid.append("    ").append(joinId).append(" --> ").append(tableId).append(";\n");
                    }
                    currentJoinParent = joinId;
                }

                mermaid.append("    ").append(parentId).append(" --> ").append(currentJoinParent).append(";\n");

            } else {
                 mermaid.append("    ").append(parentId).append(" --> ").append(targetForFirstJoinLeft).append(";\n");
            }

            return mermaid.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
