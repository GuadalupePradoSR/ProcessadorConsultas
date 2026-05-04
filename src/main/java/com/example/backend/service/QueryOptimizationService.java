package com.example.backend.service;

import com.example.backend.graph.*;
import com.example.backend.model.OptimizedQueryResponse;
import com.example.backend.model.ComparisonQueryResponse;
import com.example.backend.model.GraphNodeDTO;
import org.springframework.stereotype.Service;

/**
 * Serviço que orquestra todo o pipeline de otimização:
 * 1. Validação SQL
 * 2. Conversão para Álgebra Relacional
 * 3. Construção da árvore de execução
 * 4. Otimização com heurísticas
 * 5. Serialização para JSON
 */
@Service
public class QueryOptimizationService {

    private final SqlValidatorService validatorService;
    private final ConversionAlgebra conversionAlgebra;
    private final GraphSerializerService serializerService;

    public QueryOptimizationService(SqlValidatorService validatorService, 
                                     ConversionAlgebra conversionAlgebra) {
        this.validatorService = validatorService;
        this.conversionAlgebra = conversionAlgebra;
        this.serializerService = new GraphSerializerService();
    }

    /**
     * Pipeline completo de otimização.
     * @param sql Consulta SQL original
     * @return Resposta contendo todas as etapas e a árvore otimizada
     */
    public OptimizedQueryResponse optimizeQuery(String sql) {
        OptimizedQueryResponse response = new OptimizedQueryResponse();
        response.setOriginalSql(sql);

        try {
            // Etapa 1: Validação SQL
            boolean isValid = validatorService.validate(sql).isValid();
            response.setValid(isValid);

            if (!isValid) {
                response.setMessage("SQL inválido");
                return response;
            }

            // Etapa 2: Conversão para Álgebra Relacional
            String algebraString = conversionAlgebra.convertToAlgebra(sql);
            response.setAlgebra(algebraString);

            if (algebraString == null || algebraString.startsWith("Erro")) {
                response.setValid(false);
                response.setMessage("Erro ao converter para Álgebra Relacional");
                return response;
            }

            // Etapa 3: Construção da árvore
            QueryGraphBuilder builder = new QueryGraphBuilder();
            OperatorNode originalTree = builder.buildExecutionTree(algebraString);

            // Etapa 4: Otimização
            QueryOptimizer optimizer = new QueryOptimizer();
            OperatorNode optimizedTree = optimizer.optimize(originalTree);

            // Etapa 5: Serialização para JSON
            GraphNodeDTO treeDTO = serializerService.serializeTree(optimizedTree);
            response.setOptimizedTree(treeDTO);

            // Etapa 6: Converter a árvore otimizada de volta para string
            String optimizedAlgebraString = serializerService.treeToAlgebraString(optimizedTree);
            response.setOptimizedAlgebra(optimizedAlgebraString);

            response.setMessage("Otimização realizada com sucesso");
            response.setValid(true);

        } catch (Exception e) {
            response.setValid(false);
            response.setMessage("Erro durante a otimização: " + e.getMessage());
            response.setWarnings("Exception: " + e.getClass().getSimpleName());
        }

        return response;
    }

    /**
     * Retorna comparação lado-a-lado: árvore original vs otimizada.
     */
    public ComparisonQueryResponse compareQueries(String sql) {
        ComparisonQueryResponse response = new ComparisonQueryResponse();
        response.setOriginalSql(sql);

        try {
            // Etapa 1: Validação SQL
            boolean isValid = validatorService.validate(sql).isValid();
            if (!isValid) {
                response.setOptimizationSummary("SQL inválido");
                return response;
            }

            // Etapa 2: Conversão para Álgebra Relacional
            String algebraString = conversionAlgebra.convertToAlgebra(sql);
            response.setAlgebra(algebraString);

            if (algebraString == null || algebraString.startsWith("Erro")) {
                response.setOptimizationSummary("Erro ao converter para Álgebra Relacional");
                return response;
            }

            // Etapa 3: Construção da árvore original
            QueryGraphBuilder builder = new QueryGraphBuilder();
            OperatorNode originalTree = builder.buildExecutionTree(algebraString);

            // Serializa a árvore original
            GraphNodeDTO originalTreeDTO = serializerService.serializeTree(originalTree);
            response.setOriginalTree(originalTreeDTO);

            // Etapa 4: Otimização
            QueryOptimizer optimizer = new QueryOptimizer();
            OperatorNode optimizedTree = optimizer.optimize(originalTree);

            // Serializa a árvore otimizada
            GraphNodeDTO optimizedTreeDTO = serializerService.serializeTree(optimizedTree);
            response.setOptimizedTree(optimizedTreeDTO);

            // Converte ambas as árvores para string
            String optimizedAlgebraString = serializerService.treeToAlgebraString(optimizedTree);
            response.setOptimizedAlgebra(optimizedAlgebraString);

            // Cria um resumo das otimizações
            response.setOptimizationSummary(generateOptimizationSummary(originalTree, optimizedTree));

        } catch (Exception e) {
            response.setOptimizationSummary("Erro: " + e.getMessage());
        }

        return response;
    }

    /**
     * Apenas retorna a árvore otimizada em formato JSON (sem os detalhes de SQL/Álgebra).
     */
    public GraphNodeDTO getOptimizedTree(String sql) {
        try {
            if (!validatorService.validate(sql).isValid()) {
                return null;
            }

            String algebraString = conversionAlgebra.convertToAlgebra(sql);
            QueryGraphBuilder builder = new QueryGraphBuilder();
            OperatorNode originalTree = builder.buildExecutionTree(algebraString);

            QueryOptimizer optimizer = new QueryOptimizer();
            OperatorNode optimizedTree = optimizer.optimize(originalTree);

            return serializerService.serializeTree(optimizedTree);

        } catch (Exception e) {
            System.err.println("Erro ao gerar árvore otimizada: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gera um resumo das otimizações aplicadas.
     */
    private String generateOptimizationSummary(OperatorNode original, OperatorNode optimized) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Heurísticas Aplicadas:\n");
        summary.append("✓ Pushdown de Seleções (Redução de Tuplas)\n");
        summary.append("✓ Pushdown de Projeções (Redução de Atributos)\n");
        summary.append("✓ Reordenação de JOINs por Seletividade\n");
        
        // Conta cartesianos
        int cartesianCount = countCartesianProducts(optimized);
        if (cartesianCount > 0) {
            summary.append("⚠️ Aviso: " + cartesianCount + " Produto(s) Cartesiano(s) detectado(s)\n");
        } else {
            summary.append("✓ Nenhum Produto Cartesiano detectado\n");
        }
        
        summary.append("\nProfundidade da árvore: ").append(calculateTreeDepth(optimized));
        
        return summary.toString();
    }

    /**
     * Conta o número de produtos cartesianos na árvore.
     */
    private int countCartesianProducts(OperatorNode node) {
        if (node == null) return 0;

        int count = 0;
        if (node instanceof JoinNode) {
            JoinNode join = (JoinNode) node;
            if ("Cartesiano".equals(join.getCondition())) {
                count++;
            }
            count += countCartesianProducts(join.getLeft());
            count += countCartesianProducts(join.getRight());
        } else if (node instanceof SelectionNode) {
            count += countCartesianProducts(((SelectionNode) node).getChild());
        } else if (node instanceof ProjectionNode) {
            count += countCartesianProducts(((ProjectionNode) node).getChild());
        }

        return count;
    }

    /**
     * Calcula a profundidade máxima da árvore.
     */
    private int calculateTreeDepth(OperatorNode node) {
        if (node == null) return 0;

        int maxChildDepth = 0;
        for (OperatorNode child : node.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, calculateTreeDepth(child));
        }

        return 1 + maxChildDepth;
    }
}
