package com.example.backend.graph;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class GraphVisualizer {

    public void displayGraph(OperatorNode root) {
        // Configura o motor de UI para Swing
        System.setProperty("org.graphstream.ui", "swing");

        Graph graph = new SingleGraph("Árvore de Execução da Consulta");

        // CSS embutido para design limpo e moderno dos nós
        graph.setAttribute("ui.stylesheet",
                "node { " +
                        "   size-mode: fit; shape: box; padding: 5px, 10px; fill-color: #2c3e50; " +
                        "   text-color: white; text-size: 14px; text-style: bold; " +
                        "} " +
                        "edge { " +
                        "   fill-color: #7f8c8d; size: 2px; arrow-shape: arrow; arrow-size: 10px, 5px; " +
                        "}");

        // Constrói o grafo visual recursivamente
        traverseAndBuild(graph, root, null);

        // Renderiza e aplica layout automático
        graph.display();
    }

    private void traverseAndBuild(Graph graph, OperatorNode operator, String parentId) {
        // Usa o hash code como ID único do nó
        String nodeId = String.valueOf(operator.hashCode());

        // Adiciona o nó atual se não existir
        if (graph.getNode(nodeId) == null) {
            Node n = graph.addNode(nodeId);
            n.setAttribute("ui.label", operator.getName());
        }

        // Conecta com o pai (Seta indicando o fluxo dos dados: Filho -> Pai)
        if (parentId != null) {
            String edgeId = nodeId + "-" + parentId;
            if (graph.getEdge(edgeId) == null) {
                graph.addEdge(edgeId, nodeId, parentId, true);
            }
        }

        // Percorre os filhos recursivamente
        for (OperatorNode child : operator.getChildren()) {
            traverseAndBuild(graph, child, nodeId);
        }
    }

    // Método Main para testar a integração completa
    public static void main(String[] args) {
        // Simulação da saída da HU1 / Entrada da HU2
        String sql = "SELECT C.nome, P.ValorTotalPedido FROM Cliente C JOIN Pedido P ON C.idCliente = P.Cliente_idCliente WHERE P.ValorTotalPedido > 100";

        // 1. Executa a HU2 (Conversão para Álgebra Relacional)
        // Usamos uma instância mockada ou real do serviço
        com.example.backend.service.ConversionAlgebra algebraService = new com.example.backend.service.ConversionAlgebra();
        String algebraString = algebraService.convertToAlgebra(sql);
        System.out.println("Álgebra Gerada pela HU2: " + algebraString);

        // 2. Executa a HU3 (Construção do Grafo Dinâmico)
        QueryGraphBuilder builder = new QueryGraphBuilder();
        OperatorNode rootNode = builder.buildExecutionTree(algebraString);

        // 3. Renderiza o Grafo
        GraphVisualizer visualizer = new GraphVisualizer();
        visualizer.displayGraph(rootNode);
    }
}