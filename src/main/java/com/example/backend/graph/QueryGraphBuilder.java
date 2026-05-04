package com.example.backend.graph;

import java.util.Arrays;

// Construtor do Grafo que recebe a string de álgebra relacional da HU2 e monta a árvore
public class QueryGraphBuilder {

    /**
     * Constrói a árvore de execução a partir da string de Álgebra Relacional gerada pela HU2.
     * @param algebraString Ex: π_(nome, idade)(σ_(idade > 18)((Cliente C ⋈_{C.id = P.cliente_id} Pedido P)))
     * @return O nó raiz (OperatorNode) da árvore gerada.
     */
    public OperatorNode buildExecutionTree(String algebraString) {
        if (algebraString == null || algebraString.trim().isEmpty() || algebraString.startsWith("Erro")) {
            throw new IllegalArgumentException("Álgebra inválida para construção do grafo: " + algebraString);
        }
        return parseExpression(algebraString.trim());
    }

    private OperatorNode parseExpression(String expr) {
        expr = expr.trim();

        // 1. Projeção: π_(colunas)(resto)
        if (expr.startsWith("π_(")) {
            int splitIdx = findClosingParenthesis(expr, 3);
            if (splitIdx != -1 && splitIdx + 1 < expr.length() && expr.charAt(splitIdx + 1) == '(') {
                String colsStr = expr.substring(3, splitIdx);
                String rest = expr.substring(splitIdx + 2, expr.length() - 1);
                OperatorNode child = parseExpression(rest);
                return new ProjectionNode(child, Arrays.asList(colsStr.split(",\\s*")));
            }
        }

        // 2. Seleção: σ_(condicao)(resto)
        if (expr.startsWith("σ_(")) {
            int splitIdx = findClosingParenthesis(expr, 3);
            if (splitIdx != -1 && splitIdx + 1 < expr.length() && expr.charAt(splitIdx + 1) == '(') {
                String condStr = expr.substring(3, splitIdx);
                String rest = expr.substring(splitIdx + 2, expr.length() - 1);
                OperatorNode child = parseExpression(rest);
                return new SelectionNode(child, condStr);
            }
        }

        // 3. Junções: (Esquerda ⋈_{condicao} Direita) ou (Esquerda × Direita)
        if (expr.startsWith("(") && expr.endsWith(")")) {
            String inner = expr.substring(1, expr.length() - 1).trim();
            int level = 0;
            int splitIdx = -1;
            char operator = ' ';
            
            // Procuramos o operador de junção da direita para a esquerda, no nível 0 de aninhamento
            for (int i = inner.length() - 1; i >= 0; i--) {
                char c = inner.charAt(i);
                if (c == ')') level++;
                else if (c == '(') level--;
                else if (level == 0) {
                    if (c == '⋈' || c == '×') {
                        splitIdx = i;
                        operator = c;
                        break;
                    }
                }
            }
            
            if (splitIdx != -1) {
                String leftExpr = inner.substring(0, splitIdx).trim();
                String rightExpr = inner.substring(splitIdx + 1).trim();
                String condition = "";
                
                // Extrai a condição de junção se for um JOIN explícito
                if (operator == '⋈' && rightExpr.startsWith("_{")) {
                    int endCond = rightExpr.indexOf("}");
                    if (endCond != -1) {
                        condition = rightExpr.substring(2, endCond);
                        rightExpr = rightExpr.substring(endCond + 1).trim();
                    }
                }
                
                OperatorNode leftChild = parseExpression(leftExpr);
                OperatorNode rightChild = parseExpression(rightExpr);
                
                return new JoinNode(leftChild, rightChild, condition.isEmpty() ? "Cartesiano" : condition);
            }
            
            // Se tinha parênteses mas não era uma junção (ex: múltiplos parênteses), remove e analisa o interior
            return parseExpression(inner);
        }

        // 4. Se não for nenhuma operação acima, é um Nó Folha (Tabela Base)
        return new TableNode(expr);
    }

    private int findClosingParenthesis(String s, int openIdx) {
        int count = 1; // o parêntese na posição openIdx-1 já foi aberto
        for (int i = openIdx; i < s.length(); i++) {
            if (s.charAt(i) == '(') count++;
            else if (s.charAt(i) == ')') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }
}
