# Plano de Implementação: Tratamento do Fluxo de Atualização de Tarifas (Price Change & Re-validação de Ofertas)

## Status: Concluído com Sucesso e Validado

### 1. Revalidação Automática da Oferta antes do Checkout (Backend / Repository)
- **Duffel API Re-validation (`GET /air/offers/{offer_id}`)**:
  - Antes de executar a pré-autorização no Stripe com `capture_method: 'manual'` e chamar `POST /air/orders`, a oferta é revalidada em tempo real diretamente na Duffel.
  - Se a tarifa e condições forem idênticas (`diff <= 0.02`), o checkout atômico prossegue normalmente sem interrupções.
  - Se a oferta expirou (`expires_at` ultrapassado ou código Duffel `offer_no_longer_available`), busca uma nova tarifa ativa substituta em tempo real para a mesma rota.

### 2. Captura e Mapeamento Transparente do Novo Valor
- **Exceção Estruturada `OfferPriceChangedException`**:
  - Implementada em `FlightRepository.kt` contendo `updatedOffer`, `previousGrandTotalEur`, `newGrandTotalEur`, `currency`, `diffEur` e mensagem clara.
  - Se a Duffel retornar `price_changed` ou `offer_price_changed` antes ou durante a criação da ordem, o limite pré-autorizado no Stripe é liberado imediatamente sem cobrança (`paymentIntents.cancel`), a nova tarifa é extraída em tempo real (`new_total_amount` e `currency`), e os detalhes são repassados ao app.
  - Atualização dos DTOs em `AguiaFlyBackendMiddlewareService.kt` com `BackendPriceChangeDetailsDto` para receber os valores em respostas 409 ou JSON com `PRICE_CHANGED`.

### 3. Interface de Confirmação de Preço (`CheckoutScreen.kt` - Jetpack Compose)
- **Modal Material 3 Amigável (`AlertDialog`)**:
  - Exibe ícone estilizado de avião dourado e resumo do voo (Rota IATA e Companhia).
  - Compara visualmente:
    * **Preço Anterior**: riscado com `TextDecoration.LineThrough`.
    * **Novo Preço Atualizado**: destacado em tamanho grande e cor dourada/azul royal.
    * **Pill de Diferença**: mostra o delta em euros (`+€XX.XX (Acréscimo)` ou `-€XX.XX (Desconto)`).
  - **Botão Principal**: `"Aceitar Novo Preço & Pagar (€XX.XX)"` que conclui a emissão com a nova tarifa.
  - **Botão Secundário**: `"Voltar à Busca"` para cancelar o checkout e escolher outro voo.

### 4. Limpeza e Reset do Ciclo de Vida da Oferta
- **Atualização Dinâmica no `FlightBookingViewModel`**:
  - Ao aceitar o novo preço, `_selectedOffer` e a lista `_rawOffers` são atualizadas dinamicamente com o novo `offerId` e valores netos/totais atualizados.
  - Garante que nenhuma requisição subsequente utilize o ID antigo ou expirado da oferta.

### 5. Validação e Testes
- **Testes Unitários (`DuffelOfferProcessorTest.kt`)**: 11 testes executados e todos aprovados com sucesso (`BUILD SUCCESSFUL in 13s`).
- **Compilação do Applet**: `compile_applet` executado com êxito.
