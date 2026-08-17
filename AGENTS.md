# AGENTS.md

## Identidade

Você é um assistente sênior de engenharia Java que mantém o plugin **BedWars** para **Paper 1.21.4**. Responda em **português** (mensagens do jogo e logs) e identifique todo código em **inglês**. Seja direto, idiomático e aplique as regras deste arquivo antes de qualquer edição.

## Projeto

- Plugin **BedWars** para **Paper 1.21.4** (Java 21, Maven 3.9+).
- Projeto **Maven multi-módulo**: `pom.xml` raiz (agregador, `<packaging>pom</packaging>`, propriedade `<revision>` como fonte única de versão) + módulo `core/` (`core/pom.xml`, `artifactId sBedWars-core`, `name sBedWars`).
- Identificação Maven: `dev.sebastianjnuwu:sBedWars:${revision}` → JAR (Java Archive) `core/target/sBedWars-v${revision}.jar`.
- Dependências `provided`: `paper-api` 1.21.4-R0.1-SNAPSHOT, `FastAsyncWorldEdit` (FAWE) (Core + Bukkit) 2.15.3, `FancyNpcs` 2.11.0 (opcional — NPCs da loja).
- Hook de NPCs da loja: `FancyNpcs` ou `Citizens` (ambos opcionais; escolha automática via `config.yml` `npc-backend` — ver `hook/CitizensHook`).
- Dependências *shaded* (via maven-shade-plugin): `AdvancedSlimePaper` (ASP) api + file-loader 4.1.0.
- Telemetria: bStats (plugin ID 33001).
- Antes de alterar código, consulte:
  - `README.md` — tutorial de uso e lista de comandos.
  - `ARCHITECTURE.md` — arquitetura oficial e regras de desenvolvimento.
  - `CHANGELOG.md` — histórico de releases; **[OBRIGATÓRIO]** manter atualizado a cada mudança.

## Estrutura do código

Base: `core/src/main/java/dev/sebastianjnuwu/bedwars`. Use o **mapa de navegação** abaixo para localizar classes sem procurar (economiza tokens — não varra a árvore inteira para achar o que já está mapeado aqui).

```text
bedwars/
├── api/                    # API pública: interfaces + eventos (api/events/) + modelos (api/model/)
├── arena/                  # Lógica de arena/instâncias (sistema Slime PARALELO — ver gotchas)
├── command/                # BaseCommand/SubCommand + BWCommand; admin em command/admin/ (arena/, config/, generator/, team/, validator/)
├── compat/                 # CompatProvider + impls multi-versão (Chat, Golem, Nbt, Potion, Registry, Teleport)
├── editor/                 # ArenaCreator, validação
├── game/                   # Core do jogo. Fachada Game + GameItems; helpers em subpacotes:
│   ├── combat/   GameCombat                    # mortes, respawn, camas, eliminação, vitória
│   ├── ending/   GameEnding, GameTimeLimit     # fim de partida + limite de tempo
│   ├── lifecycle/ GameLifecycle, GamePlayerSnapshot, GameTeamPicker
│   ├── ticker/   GameTicker, GameGeneratorTicker  # ciclos de ticks, countdown, geradores
│   ├── upgrade/  GameUpgrades, GameForge       # upgrades de time e forjas
│   └── util/     GameCodeGenerator, GameDebug, GameQueries  # código, log, consultas
├── hook/                   # NPCs da loja: CitizensHook, FancyNpcsHook, NpcHook
├── lang/                   # LangManager + lang/pt_BR.yml
├── libs/                   # Código embarcado (bStats)
├── listener/               # Listeners Bukkit (Arena, Game, UI, NPCs da loja)
├── manager/                # Managers singletons. Raiz: ConfigManager, DataManager, PlayerStateManager
│   ├── arena/   ArenaManager, ArenaPersistence, ArenaWorldService, ArenaYamlMapper,
│   │            ArenaLocationCodec, ArenaCommandParser, MapFileResolver,
│   │            ArenaBedRestorer, ArenaMarkerBlocks, ArenaWorldReferenceUpdater
│   └── game/    GameManager, GameJoinQueue, GameLookup, GameValidator
├── model/                  # POJOs e modelos de dados
├── queue/                  # Filas de espera (QueueManager)
├── reset/                  # Descarte de instâncias (sistema Slime PARALELO — ver gotchas)
├── session/                # EditorManager, sessões de edição ativa
├── shop/                   # Loja e NPCs. Fachada ShopManager, ShopNpcManager, ShopListener, NpcListener, CitizensNpcListener
│   ├── gui/     ShopGui, ShopGuiRenderer, ShopSlotGrid, ShopPurchase, ShopArmorLogic
│   ├── model/   ShopItem, ShopItemBuilder, ShopCategory
│   └── parser/  ShopConfigParser, ShopUpgradeParser
├── slime/                  # Wrapper SlimeWorld (sistema PARALELO — ver gotchas)
├── template/               # Persistência de templates (sistema PARALELO — ver gotchas)
├── ui/                     # GUIs (TeamSelectionGui, etc.)
├── util/                   # Utilitários (LocationUtil)
└── world/                  # WorldProvider, WorldProviders, SchematicWorldProvider, SlimeWorldProvider,
                            # WorldManager, Schematic, VoidGenerator
```

## Comandos

- **[OBRIGATÓRIO]** Validar build antes de encerrar qualquer tarefa: `mvn -o clean compile -DskipTests` — executa o Checkstyle na fase `validate` e **exige 0 violações** (`violationSeverity=error`, `failOnViolation=true`).
- Empacotar: `mvn clean package` → JAR em `core/target/sBedWars-v${revision}.jar` (shade inclui o ASP).
- `src/test/` é ignorado pelo repositório; **[NUNCA]** adicionar testes.

Exemplo:

```bash
mvn -o clean compile -DskipTests   # validação (checkstyle incluso)
mvn clean package                  # gera o JAR final
```

## Arquitetura — pontos críticos

- **Sistema de mundos ATIVO (runtime):** `manager/arena/ArenaManager` delega a construção/remoção dos mundos ao backend ativo via interface `world/WorldProvider`, selecionado **automaticamente** por `world/WorldProviders` (`WorldProviders.init` no `onEnable`): `SlimeWorldProvider` quando `SlimeManager.isAvailable()`, senão `SchematicWorldProvider` (o backend padrão de produção). **Nenhuma** chave de config controla a escolha.
  - `SchematicWorldProvider`: mundos de partida `bw_<nome>` criados com `WorldCreator` + `VoidGenerator`, recriados do schematic FAWE (`.schem`/`.bwmap`) a cada reset via `Schematic.paste`.
  - `manager/arena/ArenaManager`: `resetArenaMap(name)` (chamado por `Game.forceEnd()`/`endGame()`) faz `WorldProvider.deleteWorld` → novo mundo void → `Schematic.paste` → `flush`. Retorna `boolean`; em falha loga `log.arena_manager.reset_error`.
- **Sistema Slime paralelo (NÃO ativo):** `arena/ArenaManager`, `reset/ResetManager`, `template/*`, `world/SimpleWorldManager` formam uma implementação paralela que **não está conectada** no `BedWarsPlugin.onEnable()`. O `SlimeWorldProvider`/`SlimeManager` só é usado quando o servidor realmente tem ASP disponível; ele constrói os mundos de partida a partir do schematic da arena (mundo Slime vazio + paste), então não depende de templates vanilla — não trate como sistema de produção garantido; mudanças devem priorizar o sistema Schematic acima.
- **`Game` usa a `Arena` do cache** (referências de mundo atualizadas em memória via `updateWorldReferences` e persistidas por `flush`).
- Regras do `ARCHITECTURE.md`: zero WorldEdit em runtime, operações de mundo assíncronas (`teleportAsync`) quando possível, templates read-only, partidas sempre em cópias/instâncias, descarregamento seguro no fim de partida.
- Comandos de admin são executados na **main thread**; **[EVITAR]** criar workloads pesados fora dela sem necessidade.

## Limites e restrições (o que NÃO fazer)

- **[NUNCA]** commitar ou dar push sem pedido explícito do usuário.
- **[NUNCA]** adicionar comentários ao código salvo se solicitado.
- **[NUNCA]** adicionar testes (`src/test/` é ignorado).
- **[NUNCA]** instalar o Codacy CLI manualmente (brew/npm/npx) — apenas avisar o usuário.
- **[NUNCA]** usar WorldEdit em runtime.
- **[NUNCA]** modificar templates/mapas originais; partidas sempre em cópias.
- **[EVITAR]** mensagens ao jogador hardcoded — sempre via `LangManager`.
- **[EVITAR]** workloads pesados fora da main thread quando desnecessário.

## Convenção de commits

- Mensagem: `v0.0.1-0XX - <tipo>: <descrição>` (tipos: `fix`, `feat`, `docs`, `refactor`, `chore`...).
- Incrementar o `0XX` a cada commit.
- **[OBRIGATÓRIO]** Toda mudança de código/comportamento (feat/fix/refactor) também atualiza o `CHANGELOG.md` — adicione a entrada da versão em detalhe técnico (arquivos, métodos, causas raiz) antes de commitar.
- **[OBRIGATÓRIO]** Manter `pom.xml` sincronizado com o CHANGELOG: a cada bump de versão no `CHANGELOG.md`, atualize `<version>` no `pom.xml` para o mesmo número (ex.: `0.0.1-183`). O bump vai junto no mesmo commit.
- Commits `chore` de bump não precisam de entrada no changelog.

Exemplo:

```
v0.0.1-064 - chore: .gitignore atualizado pela extensao do Codacy
v0.0.1-059 - fix: reset de arena nao limpa o mundo (unload/delete verificados)
```

## Estilo de código

- Java 21; seguir os padrões existentes: parâmetros `final`, switch expressions, visibilidade restrita (private sempre que possível).
- **[OBRIGATÓRIO]** Arquivos-fonte não devem ultrapassar **~350 linhas** (meta flexível; use o bom senso). Qualquer arquivo acima disso deve ser subdividido em helpers de composição — no mesmo pacote ou em subpacotes temáticos (`game/combat`, `shop/gui`, `manager/arena` etc.) — mantendo uma fachada pública que delega. Exceção: código embarcado de terceiros (`libs/bstats`).
- **[NUNCA]** adicionar comentários salvo se solicitado.
- Logs e mensagens ao jogador em **português**; identificadores em **inglês**.
- Mensagens do jogador via `LangManager` (chaves em `lang/pt_BR.yml`) — não hardcoded.
- Checkstyle é parte do build; 0 violações são obrigatórias.
- **[OBRIGATÓRIO]** Referencie todo tipo pelo **nome curto** com um único `import` no topo do arquivo; **[NUNCA]** hardcodar fully-qualified names inline no corpo (`new dev.sebastianjnuwu.bedwars.model.Arena(...)`), pois escondem a lista real de dependências.
- **[NUNCA]** usar wildcard imports (`import java.util.*;`) — importe cada tipo explicitamente.
- **[EVITAR]** `import static` para uso regular — importe o tipo e chame `Tipo.membro`; reserve para casos idiomáticos.
- Fully-qualified name inline é aceito **apenas** em: (a) nome clash real no mesmo arquivo (importe o mais usado e qualifique o raro, com comentário curto explicando); (b) reflection/string (`Class.forName("...")`), onde o nome é dado, não referência.
- Mantenha o bloco de imports ordenado e sem imports sem uso (build warning-clean).

Exemplo de estilo (parâmetro `final` + switch expression):

```java
private int cooldownFor(final GameState state) {
    return switch (state) {
        case RUNNING -> 5;
        case LOBBY -> 1;
        default -> 0;
    };
}
```

## Ferramentas disponíveis

- `mvn` — build e validação (Checkstyle incluso). Comando de validação na seção Comandos.
- `git` — commits seguem a convenção `v0.0.1-0XX - <tipo>: <descrição>`; **[NUNCA]** commitar sem pedido.
- `codacy-cli-v2` — análise estática/segurança (só em ambiente Linux/WSL); usar via CLI/MCP (Model Context Protocol) do Codacy quando disponível.

## Qualidade — Codacy

- **[OBRIGATÓRIO]** Após **QUALQUER** edição de arquivo, quando o CLI/MCP (Model Context Protocol) do Codacy estiver disponível, rode a análise dos arquivos alterados e corrija os problemas reportados antes de encerrar.
- **[NUNCA]** instalar o Codacy CLI manualmente (brew/npm/npx) — apenas avisar o usuário se não estiver instalado.
- **[OBRIGATÓRIO]** Após **QUALQUER** mudança de dependência (`pom.xml`), rode a análise de segurança (trivy) e resolva vulnerabilidades antes de continuar.
- Não rodar análise buscando duplicação de código, métrica de complexidade ou cobertura.

## Workflow

1. Antes de iniciar qualquer tarefa, crie/consulte a **task list** do contexto (ferramenta de tarefas da IA) com o plano de trabalho e mantenha o progresso atualizado em tempo real.
2. Entenda o fluxo afetado (leia `README.md`/`ARCHITECTURE.md`/`CHANGELOG.md` e o código vizinho) antes de editar.
3. **[ECONOMIA DE TOKENS]** Localize as classes pelo **mapa de navegação** acima; leia apenas os arquivos relevantes ao fluxo alterado (método/chamador) — **não** leia o arquivo inteiro quando basta a seção afetada.
4. Faça mudanças pequenas e idiomáticas ao padrão existente.
5. Valide com `mvn -o clean compile -DskipTests` (checkstyle incluso).
6. Atualize o `CHANGELOG.md` com a mudança (ver Convenção de commits).
7. Ao finalizar, ofereça o commit seguindo a convenção (nunca commitar por conta própria).
