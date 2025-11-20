# 🚀 FiadoPay - Refatoração de Backend (Foco em Engenharia)

> **Projeto de POO Avançado**
>
> *De uma "God Class" monolítica para uma arquitetura distribuída, assíncrona e orientada a Padrões de Projeto (SOLID).*

---

## 🎯 Contexto e O Problema
O sistema original do **FiadoPay** sofria de alto acoplamento e baixa coesão. Toda a lógica crítica residia em uma única classe (`PaymentService`), que misturava:

1. Criação de Merchants.
2. Autenticação de Merchants.
3. Criação e Refund de Pagamentos.
4. Integração (Gateways de pagamento).
5. Persistência (Banco de Dados).
6. Notificações (Webhooks com retries bloqueantes).

**O Objetivo:** Refatorar o núcleo do sistema mantendo o contrato da API inalterado, mas aplicando engenharia de software robusta "por baixo do capô".

---

## 🏗️ Arquitetura e Decisões de Design

A nova arquitetura foi desenhada em camadas com responsabilidades estritas (SRP):

### 1. Fluxo de Controle (Orquestração vs. Execução)
* **Controller:** Atua apenas como porta de entrada (`@Valid` DTOs). Não contém regras.
* **Workflows (`PaymentsWorkflow`, `RefundsWorkflow`):** Os "Maestros". Coordenam a chamada dos UseCases e gerenciam as Threads, mas não conhecem a regra de negócio detalhada (ex: cálculo de juros ou integração com Cielo).
* **UseCases:** Unidades atômicas.
    * `CreatePendingPaymentUseCase`: Síncrono. Garante persistência inicial e cálculo de preços.
    * `AuthorizePaymentUseCase`: Assíncrono. Executa a cobrança e garante que o pagamento saia do estado `PENDING` (tratamento de falhas com *Fail Safe*).
    * `ValidateMerchantAuthUseCase`: Reutilizável em múltiplos fluxos para centralizar a autenticação.

### 2. Concorrência e Assincronismo
Para resolver problemas de latência e escalabilidade, abandonamos o processamento linear na thread HTTP.
* **`ExecutorService` (FixedThreadPool):** O processamento pesado (Gateway e Webhook) é submetido a um pool de threads gerenciado.
* **Idempotência (`isNew`):** Implementamos uma verificação atômica para garantir que cliques duplos não disparem duas threads de processamento simultâneas.

---

## 🧩 Engenharia da Factory (Pipeline OCP)

Um dos pontos altos da refatoração foi a eliminação de condicionais (`if/else`) na criação das estratégias, garantindo o **Princípio Aberto/Fechado (Open/Closed Principle)**.

Utilizamos o padrão **Chain of Responsibility** combinado com a **Injeção de Dependência do Spring**:

1.  **A Interface `DecoratorProvider`:** Define um contrato para componentes que sabem "aplicar" um Decorator (ex: Fraude, Log, Falha).
2.  **Injeção de Lista (`List<DecoratorProvider>`):** O Spring escaneia o projeto e injeta automaticamente na Factory todos os componentes (`@Component`) que implementam essa interface.
3.  **Pipeline Dinâmico:** A `PaymentStrategyFactory` itera sobre essa lista e passa a estratégia base por cada provider.
    * *Resultado:* Para adicionar uma nova regra (ex: Log de Auditoria), basta criar uma nova classe `LogProvider`. **Não é necessário alterar uma única linha de código na Factory.**

---

## 🛠️ Padrões de Projeto Aplicados (Design Patterns)

A refatoração foi fortemente baseada nos padrões do GoF.

| Padrão | Aplicação no FiadoPay | Benefício |
| :--- | :--- | :--- |
| **Strategy** | `PaymentStrategy` (Interface) e implementações (`CardStrategy`, `PixStrategy`). | Permite adicionar novos meios de pagamento sem alterar o código dos fluxos existentes. |
| **Factory** | `PaymentStrategyFactory`. | Centraliza a montagem dos objetos. Transforma anotações em objetos complexos prontos para uso. |
| **Decorator** | `AntiFraudDecorator`, `RandomFailureDecorator`. | Adiciona comportamentos (segurança, simulação de erro) dinamicamente em cima das estratégias, sem que elas saibam ("Onion Architecture"). |
| **Mapper** | `PaymentMapper`. | Isola a lógica de conversão Entidade-DTO, seguindo o princípio DRY. |

---

## 🧩 Metaprogramação (Anotações Customizadas)

O sistema é configurado declarativamente através de anotações criadas especificamente para o domínio:

* **`@PaymentMethod(type="...")`**: Marca uma classe como uma estratégia de pagamento elegível. A Factory escaneia essas classes na inicialização.
* **`@AntiFraud(threshold=...)`**: Ativa o Decorator de segurança (Provider: `AntiFraudProvider`). Se o valor da transação exceder o `threshold`, a estratégia real nem sequer é chamada (Fail Fast).
* **`@RandomicFailureRate(failureRate=...)`**: Ativa o Decorator de caos (Provider: `RandomFailureProvider`).

---

## 📡 Sistema de Notificações (Webhooks)

A lógica de Webhooks foi desacoplada para evitar o bloqueio das threads de pagamento.

1.  **O Agendador (`CreateWebhookUseCase`):** Roda no fluxo principal (bloco `finally` do Workflow). Busca o estado atualizado ("fresco") do pagamento no banco, gera o payload seguro (HMAC) e salva o registro.
2.  **O Trabalhador (`DispatchWebhookUseCase`):** Roda em thread isolada. Realiza o I/O de rede e gerencia a política de retentativas.

---

## 📸 Evidências da Refatoração

### 1. Payments Workflow (Orquestração Assíncrona)
*Visualização do Maestro coordenando criação, autorização (thread pool) e webhook.*

[INSIRA PRINT DA CLASSE PAYMENTS WORKFLOW AQUI]

### 2. Refunds Workflow (Reuso de Código)
*Evidência do reuso dos UseCases de Autenticação e Webhook em um fluxo diferente.*

[INSIRA PRINT DA CLASSE REFUNDS WORKFLOW AQUI]

### 3. Payment Strategy Factory (Lógica OCP)
*O coração da engenharia: montagem dinâmica baseada na lista de providers injetada.*

[INSIRA PRINT DA CLASSE PAYMENT STRATEGY FACTORY AQUI]

### 4. Providers & Decorators
*A implementação isolada das regras de negócio (ex: AntiFraude) que são plugadas na Factory.*

[INSIRA PRINT DE UM DECORATOR PROVIDER E SEU DECORATOR AQUI]

---

# 💻 Como Rodar (FiadoPay Simulator)

Gateway de pagamento **FiadoPay** para a disciplina de POO Avançado.
Substitui PSPs reais com um backend em memória (H2).

## Instalação e Execução
```bash
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```

h2 console: http://localhost:8080/h2  
Swagger UI: http://localhost:8080/swagger-ui.html

## Fluxo

1) **Cadastrar merchant**
```bash
curl -X POST http://localhost:8080/fiadopay/admin/merchants   -H "Content-Type: application/json"   -d '{"name":"MinhaLoja ADS","webhookUrl":"http://localhost:8081/webhooks/payments"}'
```

2) **Obter token**
```bash
curl -X POST http://localhost:8080/fiadopay/auth/token   -H "Content-Type: application/json"   -d '{"client_id":"<clientId>","client_secret":"<clientSecret>"}'
```

3) **Criar pagamento**
```bash
curl -X POST http://localhost:8080/fiadopay/gateway/payments   -H "Authorization: Bearer FAKE-<merchantId>"   -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000"   -H "Content-Type: application/json"   -d '{"method":"CARD","currency":"BRL","amount":250.50,"installments":12,"metadataOrderId":"ORD-123"}'
```

4) **Consultar pagamento**
```bash
curl http://localhost:8080/fiadopay/gateway/payments/<paymentId>
```