# Blindagem de Segurança e Qualidade — AguiaFly Royal Sapphire

Elevação da postura de segurança cibernética, privacidade de dados pessoais (PII/PCI) e confiabilidade transacional do **AguiaFly** para padrão bancário e de companhia aérea internacional, protegendo o utilizador desde a pesquisa até a emissão do bilhete e guarda do passaporte digital.

## User Review & Critical Decisions

> [!IMPORTANT]
> As decisões abaixo refletem exatamente as escolhas confirmadas para a blindagem do aplicativo:

- **Decisão Confirmada 1 — Privacidade de Ecrã e Mascaramento de Passaporte**: Ativação automática de `FLAG_SECURE` na tela de Checkout (impedindo prints, gravações de tela ou espelhamento enquanto dados de cartão e passaporte estão abertos) e mascaramento visual do número de passaporte (`•••••789A`) com botão de revelação temporária sob toque.
- **Decisão Confirmada 2 — Segurança Transacional no Checkout (Luhn, Validade e Idempotência)**: Validação matemática real do número do cartão pelo **Algoritmo de Luhn**, verificação da data de validade do cartão e validade mínima internacional do passaporte, além de chave criptográfica de idempotência (`Idempotency-Key`) por transação para impedir cobranças duplicadas no Stripe ou emissões duplicadas na Duffel em caso de oscilação de rede.
- **Decisão Confirmada 3 — Blindagem de Rede HTTPS/TLS, Cache Criptografado e Auditoria Visual no Perfil**: Configuração estrita de rede (`cleartextTrafficPermitted="false"` aceitando exclusivamente HTTPS TLS 1.2/1.3 para Duffel, Stripe e Firebase), exclusão de caches sensíveis de backups ADB/nuvem não protegidos, criptografia AES-256-GCM (via Android Keystore) para dados sensíveis em cache local e um **Selo + Painel de Auditoria de Segurança Royal Sapphire** na aba Perfil.

---

## 1. Overview & Core Concept

- **O que entrega**: Uma camada completa de proteção em profundidade (*Defense-in-Depth*) cobrindo rede, armazenamento local, privacidade visual contra malware/captura de tela e integridade financeira no momento da compra da passagem aérea.
- **Público-Alvo**: Viajantes internacionais que inserem documentos reais de viagem (passaporte, data de nascimento, programas de milhagem) e realizam pagamentos de passagens aéreas em tempo real.
- **Valor Principal**: Elimina riscos de cobrança duplicada, impede vazamento de documentos por captura de tela ou extração de backup local e oferece transparência total ao utilizador através de um diagnóstico visual de segurança em tempo real.

---

## 2. User Experience & Visual Design

- **Fluxos de Segurança na Experiência do Utilizador**:
  1. **Checkout Protegido (`FLAG_SECURE` + Validador Inteligente)**:
     - Assim que o utilizador entra no Checkout, a janela Android ativa proteção `FLAG_SECURE` (exibindo o selo *"Sessão Blindada PCI-DSS • Captura de Tela Bloqueada"* no topo). Ao sair do Checkout, a permissão normal de captura de tela é restaurada automaticamente.
     - O campo de passaporte permite alternar entre visualização mascarada (`••••••482`) e visível, e exibe feedback imediato caso o passaporte esteja vencido ou próximo de expirar (alerta de regra internacional de 6 meses).
     - O formulário de cartão valida em tempo real a bandeira (Visa, Mastercard, Amex), o dígito verificador matemático via **Algoritmo de Luhn** e se o mês/ano de expiração é válido antes de liberar o botão de pagamento.
  2. **Proteção Anti-Duplicidade (Idempotência Transacional)**:
     - Ao tocar em pagar, o motor gera uma assinatura única determinística para a reserva (`SHA-256` da oferta + passageiro + janela temporal) enviada ao Stripe e controlada localmente para bloquear submissões concorrentes ou repetidas.
  3. **Painel de Auditoria de Segurança no Perfil Passaporte**:
     - Na tela **Perfil & Passaporte**, um cartão compacto **"Blindagem AguiaFly Security Shield (6/6 Ativos)"** mostra o estado verificado em tempo real:
       - Firestore Zero-Trust Rules & Isolamento `UID`
       - Firebase App Check Attestation
       - Conexão Exclusiva HTTPS/TLS 1.3 (`api.duffel.com` & `api.stripe.com`)
       - Cofre Local Criptografado (Android Keystore AES-256)
       - Proteção Anti-Print `FLAG_SECURE` no Checkout
       - Motor Anti-Duplicidade & Validação Luhn PCI

- **Identidade Visual & Tema**:
  - *Estética*: **Royal Sapphire & Champagne Gold** com indicadores de segurança em **Verde Esmeralda (`#10B981`)** e alertas preventivos em **Âmbar Imperial (`#F59E0B`)**.
  - *Hierarquia Visual*: Badges de segurança com ícone de escudo verificado, tipografia monoespaçada para números mascarados de passaporte/cartão e cartões translúcidos em camadas.

---

## 3. Key Product Decisions & Trade-Offs

- **Decisão 1: `FLAG_SECURE` Dinâmico Apenas no Checkout + Mascaramento no Perfil**
  - *Abordagem Escolhida*: Ativar `WindowManager.LayoutParams.FLAG_SECURE` dinamicamente via ciclo de vida Compose (`DisposableEffect`) durante o Checkout e mascarar o número do passaporte no Perfil/Cofre (`•••••1234`).
  - *Porquê*: Protege o momento crítico em que o número completo do cartão, CVV e passaporte estão na tela contra malwares de captura, mas continua permitindo que o utilizador tire print dos seus carimbos de viagem ou de promoções na tela inicial.
  - *Alternativas Consideradas*: Bloquear print no app inteiro impediria o utilizador de compartilhar promoções ou o seu passaporte de carimbos com amigos.

- **Decisão 2: Criptografia Hardware-Backed (Android Keystore AES-256-GCM) + Regras de Backup Estritas**
  - *Abordagem Escolhida*: Criptografar os dados do Cofre do Viajante armazenados no cache local usando chave simétrica `AES/GCM/NoPadding` guardada no `AndroidKeyStore` do aparelho, além de configurar `backup_rules.xml` e `data_extraction_rules.xml` para excluir caches sensíveis de extrações externas.
  - *Porquê*: Mesmo em aparelhos perdidos ou submetidos a extração via cabo USB/ADB, o número do passaporte e dados pessoais nunca ficam em texto puro no disco local, enquanto a recuperação legítima ao reinstalar o app continua funcionando via Firestore autenticado com Google Sign-In.

- **Decisão 3: Validação Matemática de Luhn + Chave de Idempotência no Pagamento**
  - *Abordagem Escolhida*: Validar o cartão localmente com o Algoritmo de Luhn e anexar cabeçalho e trava de idempotência na requisição Stripe/Duffel.
  - *Porquê*: Evita chamadas desnecessárias à API com números de cartão digitados incorretamente e garante que uma falha momentânea de 4G/Wi-Fi jamais gere duas cobranças para o mesmo bilhete.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Diagrama de Arquitetura de Segurança em Camadas

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        AGUIAFLY SECURITY SHIELD (UI)                       │
│  ┌──────────────────────────────┐      ┌────────────────────────────────┐  │
│  │   CheckoutScreen Blindado    │      │  Perfil & Passaporte Digital   │  │
│  │ • Window FLAG_SECURE Ativo   │      │ • Passaporte Mascarado ••••89A │  │
│  │ • Validador Luhn & Expiração │      │ • Painel Auditoria 6/6 Checks  │  │
│  │ • Alerta Validade Passaporte │      │ • Sincronização Cloud Segura   │  │
│  └──────────────┬───────────────┘      └───────────────┬────────────────┘  │
└─────────────────┼──────────────────────────────────────┼───────────────────┘
                  │                                      │
                  ▼                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   CAMADA DE CRIPTOGRAFIA & VALIDAÇÃO                       │
│  ┌──────────────────────────────┐      ┌────────────────────────────────┐  │
│  │  CheckoutSecurityValidator   │      │   KeystoreVaultCipher (AES)    │  │
│  │ • Algoritmo Luhn (Mod 10)    │      │ • AndroidKeyStore AES-256-GCM  │  │
│  │ • Detector de Bandeira       │      │ • Cifra/Decifra PII no Cache   │  │
│  │ • Guardião de Idempotência   │      │ • Zero Texto Puro em Disco     │  │
│  └──────────────┬───────────────┘      └───────────────┬────────────────┘  │
└─────────────────┼──────────────────────────────────────┼───────────────────┘
                  │                                      │
                  ▼                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                 CAMADA DE REDE & PERSISTÊNCIA ZERO-TRUST                   │
│  ┌──────────────────────────────┐      ┌────────────────────────────────┐  │
│  │ Network Security Config XML  │      │ Firestore Enterprise + Rules   │  │
│  │ • cleartextPermitted = false │      │ • Google Sign-In + App Check   │  │
│  │ • HTTPS TLS 1.2/1.3 Estrito  │      │ • Isolamento request.auth.uid  │  │
│  │ • Headers Sanitizados        │      │ • Regras Backup Anti-Extração  │  │
│  └──────────────────────────────┘      └────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Mapeamento Interativo de Componentes e Estados
- **Ativador `FLAG_SECURE` no Checkout**: Quando o utilizador navega para a tela de Checkout, o componente aplica `FLAG_SECURE` na `Window` da Activity e exibe um badge de confirmação no topo; ao navegar de volta ou concluir a compra, o `onDispose` remove a flag imediatamente.
- **Validador de Cartão (Luhn + Data) e Passaporte**: À medida que o utilizador digita no Checkout, o validador verifica:
  1. Número do cartão entre 13 e 19 dígitos aprovando na soma de verificação de **Luhn (Módulo 10)**.
  2. Mês (`01..12`) e ano de expiração não vencidos em relação à data atual.
  3. Data de expiração do passaporte (alertando visualmente se estiver vencido ou com menos de 6 meses de validade).
- **Cifra AES-256-GCM para Cache Offline**: Antes de salvar qualquer dado do Cofre de Viajante em `SharedPreferences`, o texto é cifrado com chave gerada dentro do `AndroidKeyStore` (`AES/GCM/NoPadding` com IV aleatório de 12 bytes) e decifrado de forma transparente na leitura.
- **Guardião de Idempotência no Pagamento**: Gera um identificador único para a tentativa de compra atual e bloqueia reenvios duplicados para a mesma oferta/passageiro enquanto a transação está em processamento ou recém-concluída.
