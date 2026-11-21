package br.edu.atitus.productapi.components;

import br.edu.atitus.productapi.dtos.StockItemRequest;
import br.edu.atitus.productapi.entities.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockRulesTest {
    private static ProductEntity produto(long id, long loja, int estoque, boolean ativo) {
        ProductEntity p = new ProductEntity();
        p.setId(id); p.setName("P" + id); p.setPrice(10); p.setCurrency("BRL");
        p.setEstablishmentId(loja); p.setProviderId(loja + 100); p.setStock(estoque); p.setActive(ativo);
        return p;
    }

    @Test
    void reserveBaixaEstoque() {
        var produtos = Map.of(1L, produto(1, 1, 5, true), 2L, produto(2, 1, 3, true));
        var itens = StockRules.reserve(StockRules.merge(List.of(new StockItemRequest(1L, 2), new StockItemRequest(2L, 3))), produtos);
        assertEquals(3, produtos.get(1L).getStock());
        assertEquals(0, produtos.get(2L).getStock());
        assertEquals(2, itens.size());
        assertEquals(1L, itens.get(0).establishmentId());
    }

    @Test
    void reserveSomaProdutoRepetido() {
        var produtos = Map.of(1L, produto(1, 1, 4, true));
        var wanted = StockRules.merge(List.of(new StockItemRequest(1L, 2), new StockItemRequest(1L, 3)));
        assertEquals(5, wanted.get(1L));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> StockRules.reserve(wanted, produtos)).getStatusCode().value());
        assertEquals(4, produtos.get(1L).getStock());
    }

    @Test
    void reserveRecusaLojasDiferentes() {
        var produtos = Map.of(1L, produto(1, 1, 5, true), 2L, produto(2, 2, 5, true));
        var wanted = StockRules.merge(List.of(new StockItemRequest(1L, 1), new StockItemRequest(2L, 1)));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> StockRules.reserve(wanted, produtos)).getStatusCode().value());
        assertEquals(5, produtos.get(1L).getStock());
        assertEquals(5, produtos.get(2L).getStock());
    }

    @Test
    void reserveRecusaProdutoInativoOuAusente() {
        var produtos = Map.of(1L, produto(1, 1, 5, false));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> StockRules.reserve(StockRules.merge(List.of(new StockItemRequest(1L, 1))), produtos)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> StockRules.reserve(StockRules.merge(List.of(new StockItemRequest(9L, 1))), produtos)).getStatusCode().value());
    }

    @Test
    void mergeRecusaQuantidadeInvalidaOuListaVazia() {
        assertThrows(ResponseStatusException.class, () -> StockRules.merge(List.of(new StockItemRequest(1L, 0))));
        assertThrows(ResponseStatusException.class, () -> StockRules.merge(List.of()));
    }

    @Test
    void mergeRecusaSomaQueEstoura() {
        var itens = List.of(new StockItemRequest(1L, Integer.MAX_VALUE), new StockItemRequest(1L, Integer.MAX_VALUE));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> StockRules.merge(itens)).getStatusCode().value());
    }

    @Test
    void releaseDevolveEstoque() {
        var produtos = Map.of(1L, produto(1, 1, 1, true));
        StockRules.release(Map.of(1L, 4), produtos);
        assertEquals(5, produtos.get(1L).getStock());
    }
}
