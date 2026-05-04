package com.example.backend.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class QueryOptimizer {

    /**
     * Aplica as heurísticas de otimização no grafo gerado.
     * Heurísticas implementadas:
     * 1. Pushdown de Seleções (Redução de Tuplas)
     * 2. Pushdown de Projeções (Redução de Colunas)
     * 3. Reordenação de JOINs por seletividade
     * 4. Detecção e avoidance de produto cartesiano
     */
    public OperatorNode optimize(OperatorNode root) {
        // 1. Heurística 1: Pushdown de Seleções (Redução de Tuplas)
        OperatorNode optimizedTree = pushDownSelections(root);

        // 2. Heurística 2: Reordenação de JOINs por seletividade (mais restritivos primeiro)
        optimizedTree = reorderJoinsBySelectivity(optimizedTree);

        // 3. Heurística 3: Detecção de produto cartesiano e aviso
        detectCartesianProduct(optimizedTree);

        // 4. Heurística 4: Pushdown de Projeções (Redução de Colunas)
        if (optimizedTree instanceof ProjectionNode) {
            ProjectionNode rootProj = (ProjectionNode) optimizedTree;
            // Pegamos todas as colunas que o SELECT final precisa
            List<String> requiredColumns = rootProj.getColumns(); 
            // Começamos a empurrar as projeções para os filhos
            rootProj.setChild(pushDownProjections(rootProj.getChild(), requiredColumns));
        }

        return optimizedTree;
    }

    /**
     * Empurra as operações de Seleção (WHERE) o mais para baixo possível na árvore.
     */
    private OperatorNode pushDownSelections(OperatorNode node) {
        if (node == null) return null;

        // Se o nó atual for uma Projeção, apenas repassamos para o filho
        if (node instanceof ProjectionNode) {
            ProjectionNode proj = (ProjectionNode) node;
            proj.setChild(pushDownSelections(proj.getChild()));
            return proj;
        }

        // Se o nó atual for uma Seleção, tentamos empurrá-la para baixo
        if (node instanceof SelectionNode) {
            SelectionNode selNode = (SelectionNode) node;
            OperatorNode child = selNode.getChild();

            // Se o filho da seleção for um JOIN, é aqui que a mágica acontece!
            if (child instanceof JoinNode) {
                JoinNode joinNode = (JoinNode) child;
                String fullCondition = selNode.getCondition(); 

                // Divide as condições múltiplas separadas por AND ou ^
                String[] conditions = fullCondition.split("(?i)\\s+AND\\s+|\\s*\\^\\s*");

                List<String> leftConditions = new ArrayList<>();
                List<String> rightConditions = new ArrayList<>();
                List<String> keepConditions = new ArrayList<>();

                // Analisa cada condição para ver a qual tabela ela pertence
                for (String cond : conditions) {
                    if (belongsToTree(cond, joinNode.getLeft())) {
                        leftConditions.add(cond);
                    } else if (belongsToTree(cond, joinNode.getRight())) {
                        rightConditions.add(cond);
                    } else {
                        // Se envolver as duas tabelas, tem que ficar acima do JOIN (ou virar condição do ON)
                        keepConditions.add(cond);
                    }
                }

                // Cria novas seleções acima das folhas, se houver condições para elas
                OperatorNode leftChild = joinNode.getLeft();
                if (!leftConditions.isEmpty()) {
                    String combinedLeftCond = String.join(" AND ", leftConditions);
                    leftChild = new SelectionNode(leftChild, combinedLeftCond);
                }

                OperatorNode rightChild = joinNode.getRight();
                if (!rightConditions.isEmpty()) {
                    String combinedRightCond = String.join(" AND ", rightConditions);
                    rightChild = new SelectionNode(rightChild, combinedRightCond);
                }

                // Atualiza os filhos do JOIN com (possivelmente) os novos nós de Seleção
                joinNode.setLeft(pushDownSelections(leftChild));
                joinNode.setRight(pushDownSelections(rightChild));

                // Se sobrou alguma condição que não pôde descer, mantemos um nó de Seleção no topo do JOIN
                if (!keepConditions.isEmpty()) {
                    selNode.setCondition(String.join(" AND ", keepConditions));
                    selNode.setChild(joinNode);
                    return selNode;
                }

                // Se todas as condições desceram, o JOIN vira o novo topo desta subárvore!
                return joinNode;
            } else {
                // Se o filho não for JOIN (ex: for uma Tabela), apenas continua descendo
                selNode.setChild(pushDownSelections(selNode.getChild()));
                return selNode;
            }
        }

        // Se for um JOIN simples (sem seleção em cima), apenas otimiza os filhos
        if (node instanceof JoinNode) {
            JoinNode join = (JoinNode) node;
            join.setLeft(pushDownSelections(join.getLeft()));
            join.setRight(pushDownSelections(join.getRight()));
            return join;
        }

        return node; // Retorna TableNode intacto
    }

    /**
     * Empurra as projeções para o mais perto possível das tabelas.
     */
    private OperatorNode pushDownProjections(OperatorNode node, List<String> requiredColumns) {
        if (node == null) return null;

        if (node instanceof JoinNode) {
            JoinNode joinNode = (JoinNode) node;
            
            // Precisamos garantir que os campos usados na condição do JOIN também sejam projetados!
            List<String> allRequired = new ArrayList<>(requiredColumns);
            String joinCond = joinNode.getCondition();
            if (joinCond != null && !joinCond.equals("Cartesiano")) {
                String[] terms = joinCond.split("(?i)\\s*=\\s*|\\s+AND\\s+");
                for (String term : terms) {
                    allRequired.add(term.trim());
                }
            }

            // Descemos recursivamente para os filhos da esquerda e da direita
            OperatorNode optimizedLeft = pushDownProjections(joinNode.getLeft(), allRequired);
            OperatorNode optimizedRight = pushDownProjections(joinNode.getRight(), allRequired);

            // Filtramos apenas as colunas que pertencem à tabela da esquerda
            List<String> leftCols = filterColumnsForTree(allRequired, optimizedLeft);
            if (!leftCols.isEmpty()) {
                // Adiciona um nó de Projeção ACIMA do filho da esquerda
                optimizedLeft = new ProjectionNode(optimizedLeft, leftCols);
            }

            // Filtramos apenas as colunas que pertencem à tabela da direita
            List<String> rightCols = filterColumnsForTree(allRequired, optimizedRight);
            if (!rightCols.isEmpty()) {
                // Adiciona um nó de Projeção ACIMA do filho da direita
                optimizedRight = new ProjectionNode(optimizedRight, rightCols);
            }

            joinNode.setLeft(optimizedLeft);
            joinNode.setRight(optimizedRight);
            return joinNode;
            
        } else if (node instanceof SelectionNode) {
            // Se for uma seleção, apenas repassa para o filho, mas adiciona as colunas do WHERE na lista de necessárias
            SelectionNode selNode = (SelectionNode) node;
            List<String> allRequired = new ArrayList<>(requiredColumns);
            
            String[] terms = selNode.getCondition().split("(?i)\\s*=\\s*|\\s*<>\\s*|\\s*>\\s*|\\s*<\\s*|\\s*>=\\s*|\\s*<=\\s*|\\s+AND\\s+");
            for (String term : terms) {
                // Ignora números soltos (ex: o '1' em cliente.tipo = 1)
                if (!term.matches("\\d+")) { 
                    allRequired.add(term.trim());
                }
            }
            
            selNode.setChild(pushDownProjections(selNode.getChild(), allRequired));
            return selNode;
        }

        return node; 
    }

    /**
     * Função auxiliar para descobrir se uma condição ou coluna pertence à subárvore de um nó.
     */
    private boolean belongsToTree(String conditionOrCol, OperatorNode node) {
        if (node instanceof TableNode) {
            String tableName = ((TableNode) node).getTableName().toLowerCase();
            String prefix = conditionOrCol.split("\\.")[0].trim().toLowerCase();
            return prefix.equals(tableName) || tableName.startsWith(prefix);
        } else if (node instanceof JoinNode) {
            JoinNode join = (JoinNode) node;
            return belongsToTree(conditionOrCol, join.getLeft()) || belongsToTree(conditionOrCol, join.getRight());
        } else if (node instanceof SelectionNode) {
            return belongsToTree(conditionOrCol, ((SelectionNode) node).getChild());
        } else if (node instanceof ProjectionNode) {
            return belongsToTree(conditionOrCol, ((ProjectionNode) node).getChild());
        }
        return false;
    }

    /**
     * Função auxiliar para filtrar quais colunas pertencem a qual lado da árvore
     */
    private List<String> filterColumnsForTree(List<String> columns, OperatorNode node) {
        List<String> filtered = new ArrayList<>();
        for (String col : columns) {
            if (belongsToTree(col, node) && !filtered.contains(col)) {
                filtered.add(col);
            }
        }
        return filtered;
    }

    /**
     * Heurística 3: Reordena JOINs colocando os mais restritivos primeiro.
     * JOINs com condições de igualdade são mais seletivos que produtos cartesianos.
     */
    private OperatorNode reorderJoinsBySelectivity(OperatorNode node) {
        if (node == null) return null;

        if (node instanceof ProjectionNode) {
            ProjectionNode proj = (ProjectionNode) node;
            proj.setChild(reorderJoinsBySelectivity(proj.getChild()));
            return proj;
        }

        if (node instanceof SelectionNode) {
            SelectionNode sel = (SelectionNode) node;
            sel.setChild(reorderJoinsBySelectivity(sel.getChild()));
            return sel;
        }

        if (node instanceof JoinNode) {
            JoinNode join = (JoinNode) node;
            
            // Otimiza recursivamente os filhos
            join.setLeft(reorderJoinsBySelectivity(join.getLeft()));
            join.setRight(reorderJoinsBySelectivity(join.getRight()));

            // Verifica se ambos os filhos são JOINs para possível reordenação
            if (join.getLeft() instanceof JoinNode && join.getRight() instanceof JoinNode) {
                JoinNode leftJoin = (JoinNode) join.getLeft();
                JoinNode rightJoin = (JoinNode) join.getRight();

                int leftSelectivity = calculateSelectivity(leftJoin.getCondition());
                int rightSelectivity = calculateSelectivity(rightJoin.getCondition());

                // Se o JOIN da direita é mais seletivo, reordena
                if (rightSelectivity > leftSelectivity) {
                    OperatorNode temp = join.getLeft();
                    join.setLeft(join.getRight());
                    join.setRight(temp);
                }
            }

            return join;
        }

        return node;
    }

    /**
     * Calcula a seletividade de uma condição de JOIN.
     * Quanto maior o score, mais seletivo é o JOIN.
     */
    private int calculateSelectivity(String condition) {
        if (condition == null || condition.equals("Cartesiano")) {
            return 0; // Produto cartesiano tem seletividade mínima
        }

        int score = 1; // Base para JOIN com condição

        // JOINs com igualdade são mais seletivos
        if (condition.contains("=")) {
            score += 10;
        }

        // JOINs com múltiplas condições são mais restritivos
        int andCount = condition.split("(?i)\\s+AND\\s+").length - 1;
        score += andCount * 5;

        // Condições < ou > são mais seletivas que =
        if (condition.matches(".*[<>].*")) {
            score += 3;
        }

        return score;
    }

    /**
     * Heurística 4: Detecta produtos cartesianos na árvore.
     * Produtos cartesianos devem ser evitados sempre que possível.
     */
    private void detectCartesianProduct(OperatorNode node) {
        if (node == null) return;

        if (node instanceof JoinNode) {
            JoinNode join = (JoinNode) node;

            if (join.getCondition() != null && join.getCondition().equals("Cartesiano")) {
                System.out.println("⚠️  AVISO: Produto Cartesiano detectado!");
                System.out.println("    Esquerda: " + getTableNames(join.getLeft()));
                System.out.println("    Direita: " + getTableNames(join.getRight()));
            }

            detectCartesianProduct(join.getLeft());
            detectCartesianProduct(join.getRight());
        } else if (node instanceof SelectionNode) {
            detectCartesianProduct(((SelectionNode) node).getChild());
        } else if (node instanceof ProjectionNode) {
            detectCartesianProduct(((ProjectionNode) node).getChild());
        }
    }

    /**
     * Retorna os nomes das tabelas presentes em um nó e seus filhos.
     */
    private String getTableNames(OperatorNode node) {
        if (node instanceof TableNode) {
            return ((TableNode) node).getTableName();
        } else if (node instanceof JoinNode) {
            JoinNode join = (JoinNode) node;
            return getTableNames(join.getLeft()) + ", " + getTableNames(join.getRight());
        } else if (node instanceof SelectionNode) {
            return getTableNames(((SelectionNode) node).getChild());
        } else if (node instanceof ProjectionNode) {
            return getTableNames(((ProjectionNode) node).getChild());
        }
        return "Desconhecido";
    }
}