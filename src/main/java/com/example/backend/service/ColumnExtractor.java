package com.example.backend.service;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;

import java.util.HashSet;
import java.util.Set;

public class ColumnExtractor extends SelectVisitorAdapter {
    private final Set<String> columns = new HashSet<>();

    public Set<String> getColumns() {
        return columns;
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem item : plainSelect.getSelectItems()) {
                item.accept(new SelectItemVisitorAdapter() {
                    @Override
                    public void visit(SelectExpressionItem item) {
                        if (item.getExpression() != null) {
                            item.getExpression().accept(new ExpressionVisitorAdapter() {
                                @Override
                                public void visit(Column column) {
                                    columns.add(column.getColumnName().toLowerCase());
                                }
                            });
                        }
                    }
                });
            }
        }

        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {
                    columns.add(column.getColumnName().toLowerCase());
                }
            });
        }
        
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                if (join.getOnExpressions() != null) {
                    for (net.sf.jsqlparser.expression.Expression expr : join.getOnExpressions()) {
                        expr.accept(new ExpressionVisitorAdapter() {
                            @Override
                            public void visit(Column column) {
                                columns.add(column.getColumnName().toLowerCase());
                            }
                        });
                    }
                }
            }
        }
    }
}
