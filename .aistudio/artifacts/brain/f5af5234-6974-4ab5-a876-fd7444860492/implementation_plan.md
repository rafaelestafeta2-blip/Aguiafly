# Plano de Implementação — Melhorias de Layout, Radar Push Global, GPS em Ibiza e Carimbos Automáticos

## 1. Visão Geral do Produto e Diagnóstico

Com base no seu feedback e na captura de tela anexada, identificámos e estruturámos a solução para os quatro pontos relatados:

1. **Correção do Layout e Escala dos Cartões de Voo (`SearchAndResultsScreen.kt`)**:
   - **Causa do problema na captura de tela**: Os cartões de voo e a barra de pesquisa dividiam a largura horizontal entre demasiadas colunas simultâneas (companhia + avião + preço à direita, ou partida + duração + chegada + preço na mesma linha). Em ecrãs com escala de fonte aumentada ou largura compacta, o preço (`€470.80`) e os horários (`16:55`) ficavam espremidos e quebravam letra a letra verticalmente.
   - **Solução**: Reestruturar os cartões de ofertas com **cabeçalho horizontal dedicado** (Companhia + Código do Voo à esquerda e Preço Total destacado em linha única com `maxLines = 1` e `softWrap = false` à direita) e **linha exclusiva de ponta a ponta para os horários** (`Partida ➔ Duração/Escalas ➔ Chegada`), garantindo leitura limpa, sem quebras de texto e com espaçamento compacto e organizado.

2. **Aeroporto de Ibiza (`IBZ`) e Expansão do Catálogo de Geolocalização GPS (`NearestAirportLocator.kt` & `PlaceAutocompleteCatalog.kt`)**:
   - **Causa do problema**: O motor de cálculo de distância GPS (fórmula de Haversine) continha apenas Madrid (`MAD`) e Barcelona (`BCN`) em Espanha. Estando em Ibiza (`38.8729° N, 1.3731° E`), o aeroporto registado mais próximo na lista era Barcelona (~280 km).
   - **Solução**: Adicionar o **Aeroporto de Ibiza (`IBZ`)**, Palma de Maiorca (`PMI`), Menorca (`MAH`), Málaga (`AGP`), Valência (`VLC`), Alicante (`ALC`), Sevilha (`SVQ`), Bilbau (`BIO`), Tenerife (`TFS`) e outros hubs europeus tanto no `NearestAirportLocator` quanto no `PlaceAutocompleteCatalog`, além de refinar a leitura de GPS (`LocationManager`) para detetar `IBZ • Ibiza` imediatamente.

3. **Radar Global Automático de Super Promoções Push (`PushNotificationHelper.kt`, `PromoAlertWorker.kt` & `FlightSearchViewModel.kt`)**:
   - **Como funcionará**: Além dos alertas de rotas específicas, o sistema passará a contar com um **Radar Global Automático de Super Promoções** ativo por padrão.
   - Ao abrir o aplicativo e também em segundo plano (via `WorkManager` periódico), o motor analisa tarifas promocionais partindo do aeroporto mais próximo do utilizador (ex.: `IBZ`, `BCN`, `LIS`, `GRU`) para diferentes países (Espanha, Portugal, França, Itália, Reino Unido, Brasil, Suíça, Países Baixos, Emirados Árabes Unidos, EUA) e dispara notificações Push automáticas sempre que detetar uma queda acentuada ou tarifa relâmpago (ex.: descontos de 25% a 45% abaixo da média), permitindo abrir o aplicativo diretamente na oferta.

4. **Carimbos de Passaporte 100% Automáticos na Compra do Bilhete com até 15 Fotos (`ProfilePassportScreen.kt`, `ProfilePassportModels.kt` & `PassportVisaPageComponent.kt`)**:
   - **Remoção de carimbos manuais**: O botão e diálogo de criação manual de carimbos (`AddPastTripStampDialog`) serão removidos para garantir autenticidade — nenhum utilizador poderá fabricar carimbos sem ter adquirido o bilhete no aplicativo.
   - **Geração automática imediata**: Assim que o utilizador concluir a compra/emissão de um bilhete (`issuedTickets`), o carimbo oficial daquele país/destino é gerado automaticamente no Passaporte do Perfil com o código PNR, número do voo, cidade e bandeira.
   - **Galeria de até 15 fotos por carimbo**: Cada carimbo emitido automaticamente terá um seletor e galeria interativa que permite anexar, visualizar em carrossel/grelha e gerir **até 15 fotos reais da viagem** (`PickMultipleVisualMedia(15)`), com indicador de contagem (`X/15 fotos`) e opção de adicionar mais fotos ou remover fotos individuais.

---

## 2. Arquitetura e Ficheiros a Modificar

- **`app/src/main/java/com/example/domain/location/NearestAirportLocator.kt`**:
  - Inclusão das coordenadas exatas do Aeroporto de Ibiza (`IBZ` — `38.8729, 1.3731`) e demais aeroportos das Ilhas Baleares, Espanha, Europa e América do Sul.
- **`app/src/main/java/com/example/data/model/PlaceAutocompleteCatalog.kt`**:
  - Sincronização do catálogo de pesquisa e resolução IATA com `IBZ` (Ibiza) e novos destinos internacionais.
- **`app/src/main/java/com/example/notifications/PushNotificationHelper.kt` & `PromoAlertWorker.kt`**:
  - Novo motor `GlobalSuperPromoRadar` que seleciona e valida super promoções internacionais para múltiplos países a partir da origem local do utilizador e envia notificações Push automáticas ao iniciar a sessão e em segundo plano.
- **`app/src/main/java/com/example/ui/screens/SearchAndResultsScreen.kt`**:
  - Redesenho responsivo do `FlightOfferCard`, dos chips de filtro e dos cartões do Radar de Promoções:
    - Textos de preço, horários e códigos IATA com `softWrap = false` e `maxLines = 1`.
    - Separação clara entre linha de cabeçalho (Companhia + Preço) e linha de horários (Partida ➔ Duração ➔ Chegada).
    - Redução de espaçamentos excessivos e tamanhos de fonte exagerados para caber perfeitamente em qualquer densidade de ecrã.
- **`app/src/main/java/com/example/data/model/ProfilePassportModels.kt` & `app/src/main/java/com/example/ui/screens/ProfilePassportScreen.kt`**:
  - Remoção da criação manual de carimbos (`manualStamps` deixam de ser criados manualmente; foco exclusivo nos carimbos gerados pelos `issuedTickets`).
  - Atualização do limite de fotos por carimbo de bilhete emitido para **15 fotos** (`MAX_PHOTOS_PER_STAMP = 15`), permitindo adicionar fotos em múltiplos lotes até completar 15 imagens ou remover fotos individualmente.
- **`app/src/main/java/com/example/ui/components/PassportVisaPageComponent.kt`**:
  - Atualização dos cartões visuais de carimbo do passaporte e da linha do tempo para suportar a galeria de até 15 fotos por carimbo emitido.

---

## 3. Critérios de Verificação

1. **Verificação Visual dos Cartões**: Nenhum horário (`16:55`, `18:10`) ou preço (`€470.80`) pode quebrar em duas ou três linhas; todos os cartões devem apresentar leitura imediata e limpa.
2. **Verificação de GPS em Ibiza**: Coordenadas na região de Ibiza (`38.87° N, 1.37° E`) resolvem diretamente para `IBZ - Ibiza (Aeroporto de Ibiza)`.
3. **Verificação do Radar Push Automático**: Ao abrir o app (com permissão de notificação ativa) e via tarefa periódica em segundo plano, o sistema deteta e notifica super promoções para diferentes países sem exigir configuração manual prévia.
4. **Verificação de Carimbos Automáticos e 15 Fotos**: O passaporte exibe apenas carimbos provenientes de bilhetes emitidos na plataforma e permite anexar e gerir até 15 fotografias em cada carimbo.
