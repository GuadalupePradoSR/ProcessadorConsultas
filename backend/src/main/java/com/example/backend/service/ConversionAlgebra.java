package com.example.backend.service;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ConversionAlgebra {

    public String convertToAlgebra(String sql) {
        try {
            String cleanSql = sql.trim().replaceAll("\\s+", " ");
            Statement statement = CCJSqlParserUtil.parse(cleanSql);

            if (!(statement instanceof Select)) {
                return "Erro: Suporte apenas para comandos SELECT.";
            }

            Select selectStatement = (Select) statement;
            if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
                return "Erro: Consultas complexas (UNION, INTERSECT) ainda não suportadas.";
            }

            PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
            
            // 1. FROM (Relação Base)
            String expressaoAtual = plainSelect.getFromItem().toString();

            // 2. JOINs
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    String tabelaJoin = join.getRightItem().toString();
                    
                    if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
                        String condicaoJoin = join.getOnExpressions().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" ^ "));
                        expressaoAtual = "(" + expressaoAtual + " ⋈_{" + condicaoJoin + "} " + tabelaJoin + ")";
                    } else {
                        expressaoAtual = "(" + expressaoAtual + " × " + tabelaJoin + ")"; 
                    }
                }
            }

            // 3. WHERE (Seleção - σ) - CORRIGIDO PARA CASCATA
            if (plainSelect.getWhere() != null) {
                // Divide as condições pelo AND (ignorando maiúsculas/minúsculas)
                String[] condicoes = plainSelect.getWhere().toString().split("(?i) AND ");
                
                // Aplica uma operação σ para cada condição isolada
                for (String condicao : condicoes) {
                    expressaoAtual = "σ_(" + condicao.trim() + ")(" + expressaoAtual + ")";
                }
            }

            // 4. SELECT (Projeção - π)
            String colunas = plainSelect.getSelectItems().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            
            expressaoAtual = "π_(" + colunas + ")(" + expressaoAtual + ")";

            return expressaoAtual;

        } catch (Exception e) {
            return "Erro ao converter para álgebra: " + e.getMessage();
        }
    }
}