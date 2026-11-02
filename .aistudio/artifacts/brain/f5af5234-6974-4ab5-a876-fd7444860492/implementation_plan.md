# Plano de Implementacao — Aba Perfil & Passaporte Digital + Upgrade do Concierge IA

## 1. Correcao e Upgrade do Concierge IA AguiaFly
- **Identidade Oficial AguiaFly e Explicacao Clara**:
  - Atualizar a saudacao e o cabecalho do Concierge IA (removendo o antigo nome "Aether") para **AguiaFly Concierge IA**, explicando claramente na interface que basta pedir qualquer voo ou destino em linguagem natural para ele preparar a busca real na Duffel.
- **Suporte Completo a Todos os Aeroportos (incluindo Ibiza `IBZ`, Porto `OPO`, Brasil, Europa, EUA e Dubai)**:
  - Conectar o Concierge IA ao catalogo global `PlaceAutocompleteCatalog.GLOBAL_AIRPORTS` (46+ aeroportos internacionais, como `IBZ`, `OPO`, `LIS`, `GRU`, `GIG`, `CDG`, `MAD`, `BCN`, `LHR`, `FCO`, `DXB`, `JFK`, `MIA`).
- **Zero Timeout de 60s + Motor Hibrido Inteligente**:
  - Cancelar consultas anteriores caso o usuario envie uma nova mensagem seguida (como quando enviou a rota de Ibiza para o Porto e logo depois "oi").
  - Reduzir o tempo limite da chamada ao Gemini e adicionar um interpretador semantico instantaneo de rotas, datas/meses (ex.: *"dezembro" -> `2026-12-10`*, *"mais barato" -> `economy`*) e aeroportos para garantir resposta imediata no chat em menos de 2 segundos, mesmo se a rede oscilar.
  - Exibir sempre no chat o **Card Executivo da Rota Recomendada** (`Origem -> Destino`, data sugerida, classe e aeronave recomendada) com o botao **"Buscar Voos Reais Agora"** para abrir diretamente as ofertas reais da Duffel com 1 toque, além de chips rapidos clicaveis (incluindo *"Ibiza (IBZ) -> Porto (OPO) em Dezembro"*).

---

## 2. Nova Aba "Perfil" e Configuracoes Completas do Viajante
- **Novo Botao "Perfil" na Barra de Navegacao Inferior**:
  - Adicionar a 4a aba **`Perfil`** (`MainDestination.PROFILE_PASSPORT`) na barra de navegacao principal do AguiaFly.
- **Foto de Perfil e Foto de Capa Executiva**:
  - Permitir escolher a **Foto de Perfil** e a **Foto de Capa** diretamente da **Galeria do Telemovel** usando o seletor oficial do Android (`ActivityResultContracts.PickVisualMedia`, sem exigir permissoes invasivas) ou escolher entre **Capas Executivas AguiaFly** (Cabine First Class, Pôr do Sol na Asa do Airbus A350, Lounge Executivo Dourado e Horizonte Noturno).
- **Configuracoes do Usuario e Documento / Passaporte Sincronizados**:
  - Painel editavel com Nome, Sobrenome, Numero do Passaporte / Documento, Validade, Data de Nascimento, E-mail, Telemovel, Programa de Milhas, Moeda Preferida (`EUR`, `BRL`, `USD`) e chave de **Check-in Automatico 24h**, sincronizado com o Cofre do Viajante (`SavedTravelerProfile` no Firestore + persistencia local das personalizacoes visuais e fotos).

---

## 3. Passaporte Digital AguiaFly — Galeria e Linha do Tempo com Carimbos
- **Livro de Passaporte Digital Premium**:
  - Secao visual desenhada como um **Passaporte Oficial AguiaFly Executive** com capa azul-marinho e dourada, brasao da aguia, codigo MRZ estilo passaporte na base do cartao de identificacao e contador de **Paises Visitados**, **Carimbos de Imigracao** e **Milhas / Km Voados**.
- **Carimbos de Viagem Autenticos + Linha do Tempo**:
  - **Geracao Automatica dos Bilhetes**: Todo bilhete emitido no AguiaFly gera automaticamente uma pagina/carimbo oficial no passaporte com o codigo do aeroporto (`OPO`, `CDG`, `IBZ`, `LIS`, `GRU`, etc.), cidade, **bandeira/icone do pais**, data do pouso, numero do voo e carimbo de imigracao estilizado (formato circular/oval ou retangular com tinta azul, vermelha ou dourada e angulo autentico de carimbo).
  - **Botao "+ Carimbar Viagem Passada"**: Permite ao usuario registar lugares para onde ja viajou anteriormente (escolhendo o destino/aeroporto no catalogo, data da viagem, nota/memoria e **foto da viagem da galeria do telemovel** ou postal ilustrado do destino).
  - **Galeria e Linha do Tempo Interativa**: Alternancia entre a vista **Folha de Carimbos do Passaporte** e a **Linha do Tempo Cronologica com Fotos**, permitindo tocar em qualquer carimbo para abrir o cartao completo da memoria de viagem ou adicionar/trocar a foto daquele destino.
