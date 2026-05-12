package com.example.backend.service;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Join;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class GraphBuilderService {

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

            String parentId = projectionId;

            // Se NÃO for otimizado, pendura o WHERE inteiro direto abaixo da projeção
            if (plainSelect.getWhere() != null && !optimized) {
                String selectionId = "N" + (nodeId++);
                String condicao = plainSelect.getWhere().toString().replaceAll("(?i)\\bAND\\b", "^");
                mermaid.append("    ").append(selectionId).append("[\"&sigma; (").append(condicao).append(")\"];\n");
                mermaid.append("    ").append(parentId).append(" --> ").append(selectionId).append(";\n");
                parentId = selectionId;
            }

            // MAPA DE TABELAS (Nome da tabela -> ID do nó mais alto dela atualmente)
            java.util.Map<String, String> topNodesPerTable = new java.util.HashMap<>();

            // 1. Tabela Base
            String baseTable = plainSelect.getFromItem().toString();
            String baseTableId = "N" + (nodeId++);
            mermaid.append("    ").append(baseTableId).append("[\"").append(baseTable).append("\"];\n");
            topNodesPerTable.put(baseTable.toLowerCase(), baseTableId);

            // 2. Tabelas dos JOINs
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    String tableName = join.getRightItem().toString();
                    String tableId = "N" + (nodeId++);
                    mermaid.append("    ").append(tableId).append("[\"").append(tableName).append("\"];\n");
                    topNodesPerTable.put(tableName.toLowerCase(), tableId);
                }
            }

            // 3. PUSH-DOWN DE SELEÇÃO (Redução de Tuplas - Apenas Otimizado)
            if (optimized && plainSelect.getWhere() != null) {
                String[] condicoes = plainSelect.getWhere().toString().split("(?i) AND ");
                
                for (String condicao : condicoes) {
                    condicao = condicao.trim();
                    String targetTable = null;

                    for (String tName : topNodesPerTable.keySet()) {
                        if (condicao.toLowerCase().contains(tName + ".")) {
                            targetTable = tName;
                            break;
                        }
                    }

                    if (targetTable != null) {
                        String selectionId = "N" + (nodeId++);
                        mermaid.append("    ").append(selectionId).append("[\"&sigma; (").append(condicao).append(")\"];\n");
                        mermaid.append("    ").append(selectionId).append(" --> ").append(topNodesPerTable.get(targetTable)).append(";\n");
                        topNodesPerTable.put(targetTable, selectionId);
                    } else {
                        String selectionId = "N" + (nodeId++);
                        mermaid.append("    ").append(selectionId).append("[\"&sigma; (").append(condicao).append(")\"];\n");
                        mermaid.append("    ").append(selectionId).append(" --> ").append(topNodesPerTable.get(baseTable.toLowerCase())).append(";\n");
                        topNodesPerTable.put(baseTable.toLowerCase(), selectionId);
                    }
                }
            }

            // 3.5. PUSH-DOWN DE PROJEÇÃO (Redução de Campos - Apenas Otimizado)
            if (optimized) {
                java.util.Set<String> requiredColumns = new java.util.HashSet<>();
                
                // Pega colunas do SELECT
                java.util.regex.Matcher mSelect = java.util.regex.Pattern.compile("\\b[a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+\\b").matcher(cols);
                while (mSelect.find()) requiredColumns.add(mSelect.group());

                // Pega colunas dos JOINs
                if (plainSelect.getJoins() != null) {
                    for (Join j : plainSelect.getJoins()) {
                        if (j.getOnExpressions() != null) {
                            String onStr = j.getOnExpressions().stream().map(Object::toString).collect(Collectors.joining(" AND "));
                            java.util.regex.Matcher mOn = java.util.regex.Pattern.compile("\\b[a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+\\b").matcher(onStr);
                            while (mOn.find()) requiredColumns.add(mOn.group());
                        }
                    }
                }

                // Agrupa as colunas por tabela
                java.util.Map<String, java.util.List<String>> columnsPerTable = new java.util.HashMap<>();
                for (String col : requiredColumns) {
                    String tableName = col.split("\\.")[0].toLowerCase();
                    columnsPerTable.putIfAbsent(tableName, new java.util.ArrayList<>());
                    columnsPerTable.get(tableName).add(col);
                }

                // Cria os nós de projeção acima de cada galho
                for (java.util.Map.Entry<String, java.util.List<String>> entry : columnsPerTable.entrySet()) {
                    String tableName = entry.getKey();
                    if (topNodesPerTable.containsKey(tableName)) {
                        String projId = "N" + (nodeId++);
                        String tableCols = String.join(", ", entry.getValue());
                        mermaid.append("    ").append(projId).append("[\"&pi; (").append(tableCols).append(")\"];\n");
                        
                        mermaid.append("    ").append(projId).append(" --> ").append(topNodesPerTable.get(tableName)).append(";\n");
                        // Atualiza o topo para ser a nova projeção
                        topNodesPerTable.put(tableName, projId);
                    }
                }
            }

            // 4. Conectar os JOINs (Usando os topos atualizados, que agora possuem as projeções)
            String targetForFirstJoinLeft = topNodesPerTable.get(baseTable.toLowerCase());
            
            if (plainSelect.getJoins() != null && !plainSelect.getJoins().isEmpty()) {
                String currentJoinParent = null;

                for (Join join : plainSelect.getJoins()) {
                    String tableName = join.getRightItem().toString();
                    String rightTopNode = topNodesPerTable.get(tableName.toLowerCase());
                    String joinId = "N" + (nodeId++);
                    
                    String condicaoJoin = "";
                    if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
                        condicaoJoin = join.getOnExpressions().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" ^ "));
                    }

                    mermaid.append("    ").append(joinId).append("[\"⋈ (").append(condicaoJoin).append(")\"];\n");
                    
                    if (currentJoinParent == null) {
                        mermaid.append("    ").append(joinId).append(" --> ").append(targetForFirstJoinLeft).append(";\n");
                        mermaid.append("    ").append(joinId).append(" --> ").append(rightTopNode).append(";\n");
                    } else {
                        mermaid.append("    ").append(joinId).append(" --> ").append(currentJoinParent).append(";\n");
                        mermaid.append("    ").append(joinId).append(" --> ").append(rightTopNode).append(";\n");
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

    public String buildUnoptimizedGraph(String sql) {
        return buildGraph(sql, false);
    }

    public String buildOptimizedGraph(String sql) {
        return buildGraph(sql, true);
    }

    public String buildExecutionPlanGraph(String sql) {
        String optimizedGraph = buildGraph(sql, true);
        if (optimizedGraph == null || optimizedGraph.isEmpty()) return "";

        try {
            java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.List<String>> children = new java.util.HashMap<>();
            
            String[] lines = optimizedGraph.split("\n");
            
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.contains("[\"")) {
                    String id = trimmed.substring(0, trimmed.indexOf("["));
                    String label = trimmed.substring(trimmed.indexOf("[\"") + 2, trimmed.lastIndexOf("\"];"));
                    labels.put(id, label);
                } else if (trimmed.contains("-->")) {
                    String clean = trimmed.replace(";", "");
                    String[] parts = clean.split(" --> ");
                    if (parts.length == 2) {
                        String parent = parts[0];
                        String child = parts[1];
                        children.computeIfAbsent(parent, k -> new java.util.ArrayList<>()).add(child);
                    }
                }
            }

            java.util.Set<String> allChildren = new java.util.HashSet<>();
            for (java.util.List<String> list : children.values()) {
                allChildren.addAll(list);
            }

            String root = null;
            for (String node : labels.keySet()) {
                if (!allChildren.contains(node)) {
                    root = node;
                    break;
                }
            }
            if (root == null && !labels.isEmpty()) {
                root = labels.keySet().iterator().next(); // fallback to first node
            }

            java.util.List<String> executionOrder = new java.util.ArrayList<>();
            postOrder(root, children, new java.util.HashSet<>(), executionOrder);

            java.util.Map<String, Integer> steps = new java.util.HashMap<>();
            int step = 1;
            for (String node : executionOrder) {
                steps.put(node, step++);
            }

            StringBuilder newMermaid = new StringBuilder("graph TD;\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.equals("graph TD;")) continue;
                
                if (trimmed.contains("[\"")) {
                    String id = trimmed.substring(0, trimmed.indexOf("["));
                    String label = trimmed.substring(trimmed.indexOf("[\"") + 2, trimmed.lastIndexOf("\"];"));
                    Integer nodeStep = steps.get(id);
                    if (nodeStep != null) {
                        newMermaid.append("    ").append(id).append("[\"").append(nodeStep).append(" - ").append(label).append("\"];\n");
                    } else {
                        newMermaid.append("    ").append(trimmed).append("\n"); // Fallback
                    }
                } else {
                    newMermaid.append("    ").append(trimmed).append("\n"); // Edge or other
                }
            }

            return newMermaid.toString();
        } catch (Exception e) {
            return optimizedGraph;
        }
    }

    private void postOrder(String node, java.util.Map<String, java.util.List<String>> children, 
                           java.util.Set<String> visited, java.util.List<String> executionOrder) {
        if (node == null || visited.contains(node)) return;
        visited.add(node);
        if (children.containsKey(node)) {
            for (String child : children.get(node)) {
                postOrder(child, children, visited, executionOrder);
            }
        }
        executionOrder.add(node);
    }
}