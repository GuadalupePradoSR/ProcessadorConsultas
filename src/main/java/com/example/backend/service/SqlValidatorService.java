package com.example.backend.service;

import com.example.backend.model.QueryResponse;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

import net.sf.jsqlparser.util.TablesNamesFinder;
// import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.*;

@Service
public class SqlValidatorService {

    // Dicionário de Dados Baseado no PDF
    private static final Map<String, List<String>> METADATA = new HashMap<>();

    static {
        METADATA.put("categoria", Arrays.asList("idcategoria", "descricao"));
        METADATA.put("produto", Arrays.asList("idproduto", "nome", "descricao", "preco", "quantestoque", "categoria_idcategoria"));
        METADATA.put("tipocliente", Arrays.asList("idtipocliente", "descricao"));
        METADATA.put("cliente", Arrays.asList("idcliente", "nome", "email", "nascimento", "senha", "tipocliente_idtipocliente", "dataregistro"));
        METADATA.put("tipoendereco", Arrays.asList("idtipoendereco", "descricao"));
        METADATA.put("endereco", Arrays.asList("idendereco", "enderecopadrao", "logradouro", "numero", "complemento", "bairro", "cidade", "uf", "cep", "tipoendereco_idtipoendereco", "cliente_idcliente"));
        METADATA.put("telefone", Arrays.asList("numero", "cliente_idcliente"));
        METADATA.put("status", Arrays.asList("idstatus", "descricao"));
        METADATA.put("pedido", Arrays.asList("idpedido", "status_idstatus", "datapedido", "valortotalpedido", "cliente_idcliente"));
        METADATA.put("pedido_has_produto", Arrays.asList("idpedidoproduto", "pedido_idpedido", "produto_idproduto", "quantidade", "precounitario"));
    }

    public QueryResponse validate(String sql) {
        try {
            // Ignorar diferença entre maiúsculas e minúsculas e espaços extras
            String cleanSql = sql.trim().replaceAll("\\s+", " ");
            
            // Faz o parsing usando JSqlParser
            Statement statement = CCJSqlParserUtil.parse(cleanSql);
            
            if (!(statement instanceof Select)) {
                return new QueryResponse(false, "Sistema suporta apenas comandos SELECT.");
            }

            Select selectStatement = (Select) statement;

            // 1. Extrair tabelas e verificar se existem
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(selectStatement);

            Set<String> validColumns = new HashSet<>();
            
            for (String tableName : tableList) {
                String cleanTableName = tableName.toLowerCase(); // Remover backticks ou aspas se houver
                cleanTableName = cleanTableName.replaceAll("`", "").replaceAll("\"", "");
                
                if (!METADATA.containsKey(cleanTableName)) {
                    return new QueryResponse(false, "Erro: A tabela '" + cleanTableName + "' não existe.");
                }
                // Adiciona colunas válidas
                validColumns.addAll(METADATA.get(cleanTableName));
            }

            // 2. Extrair colunas da query
            ColumnExtractor columnExtractor = new ColumnExtractor();
            if (selectStatement.getSelectBody() != null) {
                selectStatement.getSelectBody().accept(columnExtractor);
            }

            // 3. Validar se todas as colunas existem nas tabelas relacionadas
            for (String col : columnExtractor.getColumns()) {
                String cleanCol = col.toLowerCase().replaceAll("`", "").replaceAll("\"", "");
                // Ignorar colunas com wildcard como * 
                if (cleanCol.equals("*")) continue;
                
                // Tratar atributos com nome da tabela, ex: cliente.nome
                if (cleanCol.contains(".")) {
                    cleanCol = cleanCol.substring(cleanCol.lastIndexOf(".") + 1);
                }

                if (!validColumns.contains(cleanCol)) {
                    return new QueryResponse(false, "Erro: O atributo '" + col + "' não existe nas tabelas informadas.");
                }
            }

            return new QueryResponse(true, "Consulta válida sintaticamente e atributos confirmados!");
            
        } catch (Exception e) {
            return new QueryResponse(false, "Erro de Sintaxe SQL: " + e.getMessage());
        }
    }
}