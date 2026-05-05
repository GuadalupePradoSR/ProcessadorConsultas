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
            // Limpa a string da mesma forma que o validador
            String cleanSql = sql.trim().replaceAll("\\s+", " ");
            
            // Realiza o parse novamente
            Statement statement = CCJSqlParserUtil.parse(cleanSql);

            // Validações básicas de segurança para a conversão
            if (!(statement instanceof Select)) {
                return "Erro: Suporte apenas para comandos SELECT.";
            }

            Select selectStatement = (Select) statement;
            if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
                return "Erro: Consultas complexas (UNION, INTERSECT) ainda não suportadas.";
            }

            PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
            
            // 1. FROM (Relação Base)
            // Pega a tabela principal da consulta
            String expressaoAtual = plainSelect.getFromItem().toString();

            // 2. JOINs (Junções - ⋈ ou Produto Cartesiano - ×)
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    String tabelaJoin = join.getRightItem().toString();
                    
                    if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
                        String condicaoJoin = join.getOnExpressions().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" AND "));
                        // Aplica o símbolo de Join com a condição
                        expressaoAtual = "(" + expressaoAtual + " ⋈_{" + condicaoJoin + "} " + tabelaJoin + ")";
                    } else {
                        // Se não houver ON, é um produto cartesiano
                        expressaoAtual = "(" + expressaoAtual + " × " + tabelaJoin + ")"; 
                    }
                }
            }

            // 3. WHERE (Seleção - σ)
            if (plainSelect.getWhere() != null) {
                String condicaoWhere = plainSelect.getWhere().toString();
                // Envolve a expressão atual com a operação de Seleção
                expressaoAtual = "σ_(" + condicaoWhere + ")(" + expressaoAtual + ")";
            }

            // 4. SELECT (Projeção - π)
            String colunas = plainSelect.getSelectItems().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            
            // Sempre aplicamos a Projeção (π) para efeito visual no frontend
            expressaoAtual = "π_(" + colunas + ")(" + expressaoAtual + ")";

            return expressaoAtual;

        } catch (Exception e) {
            return "Erro ao converter para álgebra: " + e.getMessage();
        }
    }
}