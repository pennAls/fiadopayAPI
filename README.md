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
* **Workflows (`PaymentsWorkflow`, `RefundsWorkflow`):** Os "Maestros". Coordenam a chamada dos UseCases e gerenciam as Threads, mas não conhecem a regra de negócio detalhada 
* **UseCases:** Unidades atômicas.
* **`AuthorizePaymentUseCase`**: Executado de forma assíncrona, é responsável por efetivar a cobrança junto à estratégia de pagamento (Gateway). Possui tratamento de falhas robusto para garantir que o pagamento nunca permaneça em estado `PENDING` indefinidamente.
* **`CreatePendingPaymentUseCase`**: Executado de forma síncrona no início do fluxo. Responsável por criar o registro inicial do pagamento, calcular juros de parcelamento e validar a chave de idempotência.
* **`CreateWebhookUseCase`**: Atua como o "produtor" do sistema de notificações. Ele busca o estado mais recente ("fresco") do pagamento no banco, gera o payload seguro (HMAC) e agenda o envio na tabela de entregas.
* **`DispatchWebhookUseCase`**: Atua como o "consumidor" ou trabalhador. É executado em uma thread isolada para realizar o envio HTTP real do webhook, gerenciando timeouts e políticas de retentativa sem bloquear o fluxo principal.
* **`GetPaymentUseCase`**: Responsável pela operação de leitura, recuperando os dados detalhados de um pagamento específico através de seu ID.
* **`RefundPaymentUseCase`**: Gerencia a lógica de negócio síncrona para a realização de estornos e reembolsos de transações aprovadas.
* **`ValidateMerchantAuthUseCase`**: Um componente auxiliar que centraliza a lógica de autenticação do Merchant via token. É reutilizado tanto no fluxo de criação quanto no fluxo de reembolso para garantir o princípio DRY (Don't Repeat Yourself).
### 2. Concorrência e Assincronismo
Para resolver problemas de latência e escalabilidade, abandonamos o processamento linear na thread HTTP.
* **`ExecutorService` (FixedThreadPool):** O processamento pesado (Gateway e Webhook) é submetido a um pool de threads gerenciado.
* **Idempotência (`isNew`):** Implementamos uma verificação atômica para garantir que cliques duplos não disparem duas threads de processamento simultâneas.

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

## 🧩 Engenharia da Factory (Pipeline OCP)

Um dos pontos altos da refatoração foi a implementação de um **Pipeline de Montagem Dinâmico**, eliminando condicionais rígidas (`if/else`) e garantindo o **Princípio Aberto/Fechado (OCP)**.

A arquitetura utiliza o padrão **Chain of Responsibility** para transformar metadados (Anotações) em comportamento (Decorators) de forma desacoplada:

### 1. Identificação e Registro (Allowlist)
O processo inicia com a validação da anotação **`@PaymentMethod`**.
* Ao inicializar, o Spring injeta todas as classes que implementam `PaymentStrategy`.
* A Factory itera sobre elas e verifica a presença de `@PaymentMethod`.
* **A Regra:** Se a classe não possuir essa anotação, ela é **ignorada/rejeitada** imediatamente. Isso atua como uma autenticação interna: apenas estratégias explicitamente marcadas com esse "crachá" são registradas no sistema.

### 2. O Fluxo de Montagem (The Pipeline)
Para as estratégias aceitas (ex: `CardStrategy`), inicia-se o processo de "decoração" através dos **Providers**:

1.  **Coleta:** A Factory recebe uma lista injetada de `DecoratorProvider` (ex: `AntiFraudProvider`, `RandomFailureProvider`).
2.  **Execução da Cadeia:** A estratégia base é passada sequencialmente por cada Provider.
    * O **Provider** inspeciona a classe original via **Reflexão**.
    * **Se a anotação de regra estiver presente** (ex: `@AntiFraud`): O Provider instancia o Decorator específico, "embrulha" a estratégia atual dentro dele e retorna o novo objeto composto.
    * **Se não:** Retorna a estratégia inalterada para o próximo passo.

### 3. Resultado Técnico
Ao final do pipeline, a Factory armazena no mapa (sob a chave definida em `@PaymentMethod`) um objeto complexo pronto para uso: pode ser uma estratégia pura ou uma "cebola" de decorators (ex: `AntiFraud(Random(Card))`).

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

<img width="861" height="753" alt="image" src="https://github.com/user-attachments/assets/8b9bfa7c-fd49-49cf-a702-3391ab51504a" />


### 2. Refunds Workflow (Reuso de Código)
*Evidência do reuso dos UseCases de Autenticação e Webhook em um fluxo diferente.*

<img width="764" height="614" alt="image" src="https://github.com/user-attachments/assets/bbfe5140-aeed-4890-8b8d-63e314f479eb" />

### 3. Payment Strategy Factory (Lógica OCP)
*O coração da engenharia: montagem dinâmica baseada na lista de providers injetada.*

<img width="967" height="610" alt="image" src="https://github.com/user-attachments/assets/09ca706f-18e4-4f3b-844c-b8c5800bb0ec" />

### 4. Annotations

<img width="340" height="133" alt="image" src="https://github.com/user-attachments/assets/8d2a401a-f3df-4a9d-9a31-e6f69f617f3d" />
<img width="340" height="133" alt="image" src="https://github.com/user-attachments/assets/07896da3-c43d-4966-b479-baa485ce79d2" />
<img width="340" height="133" alt="image" src="https://github.com/user-attachments/assets/32e2469e-bee4-4413-9468-de00ab852b11" />

### 5. Strategies (Implementações Concretas)

Classes que contêm a lógica de comunicação com o Gateway (Mock), marcadas com @PaymentMethod.

<img width="453" height="217" alt="image" src="https://github.com/user-attachments/assets/997b5bc8-eddc-4196-b96d-84c21860a8df" />
<img width="453" height="217" alt="image" src="https://github.com/user-attachments/assets/70d983d0-a414-43f5-9b6b-1bb12b6690f9" />
<img width="453" height="217" alt="image" src="https://github.com/user-attachments/assets/09be3ad5-5519-4e20-9147-b6cec95ae9f5" />
<img width="453" height="217" alt="image" src="https://github.com/user-attachments/assets/97f48e9d-4a22-47d8-b3fc-ecfffffe29d6" />

### 6. Decorators

A implementação isolada das regras de negócio (ex: AntiFraude) que são plugadas nas strategies.

<img width="690" height="359" alt="image" src="https://github.com/user-attachments/assets/d8faec82-b8ee-4b96-a809-3984da549947" />
<img width="652" height="320" alt="image" src="https://github.com/user-attachments/assets/0132479b-2793-4e20-9a6d-d4e438a616b1" />

### 7. Providers

Componentes especialistas que encapsulam a lógica de leitura de anotações e montagem dos Decorators, permitindo que a Factory siga o princípio OCP.

<img width="652" height="320" alt="image" src="https://github.com/user-attachments/assets/72ab77d4-7952-4e9f-93f6-6c329b140c8d" />
<img width="804" height="320" alt="image" src="https://github.com/user-attachments/assets/0e77b8d6-5418-427b-98e7-f5071861696a" />


# 💻 Como Rodar (FiadoPay Simulator)

Gateway de pagamento **FiadoPay** para a disciplina de POO Avançado.
Substitui PSPs reais com um backend em memória (H2).

## Instalação e Execução
```bash
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```

## Pré-requisitos

Para executar este projeto, certifique-se de ter o ambiente configurado com:

Java: JDK 21 ou superior.

Maven: 3.9.x ou superior.


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
