# Calculadora de materiais — estratégia

Módulo: `backend/src/main/java/com/craftbook/calculator` (Java puro, sem Spring/JPA/HTTP).

## Fórmulas
- `necessário_total = soma das quantidades das etapas × buildQuantity`
- `faltante = max(0, necessário − inventário)`
- `operações = ceil(faltante / saída_da_receita)`; `excedente = operações × saída − faltante`
  (ex.: receita de 4 unidades, faltam 5 → 2 operações, 8 produzidas, 3 de excedente)

## Inventário sem desconto duplicado
O grafo produto → ingredientes é ordenado topologicamente (Kahn). Cada material é processado uma única vez,
já com a demanda somada de todos os consumidores; o inventário é descontado nesse ponto e só então o
faltante vira operações de receita, cujos ingredientes entram na demanda dos materiais seguintes.

## Decisões e limitações
- Só materiais com `craftable = true` são expandidos. Receitas reversíveis (ex.: lingote ↔ bloco) devem
  ficar em materiais não fabricáveis; se ambos forem fabricáveis, o ciclo é detectado e o cálculo falha
  com `RecipeCycleException` (nunca entra em loop).
- Receita ausente ou incompatível com edição/versão → `Issue` + `exact = false`; o faltante vira "a obter".
  Nenhuma receita ou quantidade é inventada.
- Excedentes de receita são apenas informados; não são reaproveitados em outras demandas (exigiria
  otimização global).
- Se houver várias receitas compatíveis, usa-se a primeira por id (determinístico). Escolha entre variantes
  fica para evolução futura.
- Limites: 1–1000 construções; 1–10.000.000 por item; overflow vira erro de validação.

## API disponível

- `GET /api/health`: estado do serviço.
- `GET /api/catalog/materials`: materiais do catálogo. Filtros opcionais: `query` (nome ou id) e `category`.
- `GET /api/catalog/recipes`: receitas, ingredientes, edições e intervalo de versões.
- `POST /api/calculator`: recebe o plano e retorna a lista consolidada.

Exemplo de requisição:

```json
{
  "edition": "JAVA",
  "version": "1.21.4",
  "buildQuantity": 2,
  "requirements": [
    { "materialId": "chest", "quantity": 3 }
  ],
  "inventory": {
    "chest": 1,
    "oak_planks": 4
  },
  "expandRecipes": true
}
```

`edition` aceita `JAVA` ou `BEDROCK`. A quantidade em `requirements` é por construção;
`buildQuantity` multiplica essa demanda. `inventory` pode incluir tanto itens da lista
quanto ingredientes gerados por receitas. A resposta contém `lines`, `issues` e `exact`;
cada linha mostra necessário, inventário, faltante, operações de fabricação e excedente.
Entradas inválidas retornam HTTP 400 com `message` e `errors`.

## Catálogo inicial

A migração `V2__seed_demo_catalog.sql` carrega um conjunto pequeno, revisável de onze
materiais e sete receitas de demonstração (madeira, baú, fornalha, tochas e picareta).
Ele cobre o fluxo ponta a ponta, mas não é um catálogo completo; expansão e validação
abrangente das receitas são etapas futuras.
