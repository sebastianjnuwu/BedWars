# AGENTS.md

## Projeto

- Plugin **BedWars** para **Paper 1.21.4** (Java 21, Maven 3.9+).
- Identificação Maven: `dev.sebastianjnuwu:sBedWars:1.0.0` → JAR `target/BedWars-1.0.0.jar`.
- Dependências `provided`: `paper-api` 1.21.4-R0.1-SNAPSHOT, `FastAsyncWorldEdit` (Core + Bukkit) 2.15.3, `FancyNpcs` 2.11.0 (opcional — NPCs da loja).
- Dependências *shaded* (via maven-shade-plugin): `AdvancedSlimePaper` api + file-loader 4.1.0.
- Telemetria: bStats (plugin ID 33001).
- Antes de alterar código, consulte:
  - `README.md` — tutorial de uso e lista de comandos.
  - `ARCHITECTURE.md` — arquitetura oficial e regras de desenvolvimento.

## Estrutura do código

Base: `src/main/java/dev/sebastianjnuwu/bedwars`:

| Pacote | Responsabilidade |
|--------|------------------|
| `api/` | Interfaces e eventos públicos (`ArenaManager`, `Game`, eventos em `api/events`, modelos em `api/model`) |
| `arena/` | Lógica de arena e instâncias (sistema Slime — ver gotchas) |
| `command/` | Sistema de comandos (`BaseCommand`/`SubCommand`; subcomandos em `command/admin/...`) |
| `editor/` | Editor de arenas (`ArenaCreator`, validação) |
| `game/` | Core do jogo, `GameState`, ciclos de ticks |
| `lang/` | Internacionalização (`lang/pt_BR.yml`) |
| `libs/` | Código embarcado (bStats) |
| `listener/` | Listeners do Bukkit |
| `manager/` | Managers singletons (`ArenaManager`, `GameManager`, `ConfigManager`) |
| `model/` | POJOs e modelos de dados |
| `queue/` | Filas de espera |
| `reset/` | Descarte de instâncias (sistema Slime — ver gotchas) |
| `session/` | Sessões de edição ativa (`EditorManager`) |
| `shop/` | Loja e NPCs (`ShopNpcManager`) |
| `slime/` | Wrapper SlimeWorld (sistema paralelo — ver gotchas) |
| `template/` | Persistência de templates (sistema paralelo — ver gotchas) |
| `ui/` | GUIs |
| `util/` | Utilitários |
| `world/` | Abstrações de mundo (`WorldManager`, `VoidGenerator`, `Schematic`) |

## Comandos

- Validar build (obrigatório antes de encerrar qualquer tarefa): `mvn -o clean compile -DskipTests` — executa o Checkstyle na fase `validate` e **exige 0 violações** (`violationSeverity=error`, `failOnViolation=true`).
- Empacotar: `mvn clean package` → JAR em `target/BedWars-1.0.0.jar` (shade inclui o ASP).
- `src/test/` é ignorado pelo repositório; **não adicionar testes**.

## Arquitetura — pontos críticos

- **Sistema de mundos ATIVO (runtime):** `manager/ArenaManager` + `world/WorldManager` + `world/Schematic`.
  - Arenas são salvas como YAML em `arenas/<nome>.yml`; mapas como schematics FAWE (`.schem`/`.bwmap`) em `maps/`.
  - Mundos de partida são `bw_<nome>`, criados com `WorldCreator` + `VoidGenerator`, recriados do schematic a cada reset.
  - `ArenaManager.resetArenaMap(name)` (chamado por `Game.forceEnd()`/`endGame()`) faz: unload + delete verificados do mundo (`WorldManager.deleteWorld`) → novo mundo void → `Schematic.paste` → `flush`. Retorna `boolean`; em falha loga `log.arena_manager.reset_error`.
- **Sistema Slime paralelo (NÃO ativo):** `arena/ArenaManager`, `reset/ResetManager`, `slime/*`, `template/*`, `world/SimpleWorldManager` formam uma implementação paralela que **não está conectada** no `BedWarsPlugin.onEnable()`. Não trate como sistema de produção; mudanças devem priorizar o sistema Schematic acima.
- **`Game` usa a `Arena` do cache** (referências de mundo atualizadas em memória via `updateWorldReferences` e persistidas por `flush`).
- Regras do `ARCHITECTURE.md`: zero WorldEdit em runtime, operações de mundo assíncronas (`teleportAsync`) quando possível, templates read-only, partidas sempre em cópias/instâncias, descarregamento seguro no fim de partida.
- Comandos de admin são executados na **main thread**; não criar workloads pesados fora dela sem necessidade.

## Convenção de commits

- Mensagem: `v0.0.1-0XX - <tipo>: <descrição>` (tipos: `fix`, `feat`, `docs`, `refactor`, `chore`...).
- Incrementar o `0XX` a cada commit.
- **Nunca commitar nem fazer push sem pedido explícito** do usuário.

## Estilo de código

- Java 21; seguir os padrões existentes: parâmetros `final`, switch expressions, visibilidade restrita (private sempre que possível).
- **Não adicionar comentários** salvo se solicitado.
- Logs e mensagens ao jogador em **português**; identificadores em **inglês**.
- Mensagens do jogador via `LangManager` (chaves em `lang/pt_BR.yml`) — não hardcoded.
- Checkstyle é parte do build; 0 violações são obrigatórias.

## Qualidade — Codacy

- Após **QUALQUER** edição de arquivo, quando o CLI/MCP do Codacy estiver disponível, rode a análise dos arquivos alterados e corrija os problemas reportados antes de encerrar.
- **NÃO** instalar o Codacy CLI manualmente (brew/npm/npx) — apenas avisar o usuário se não estiver instalado.
- Após **QUALQUER** mudança de dependência (`pom.xml`), rode a análise de segurança (trivy) e resolva vulnerabilidades antes de continuar.
- Não rodar análise buscando duplicação de código, métrica de complexidade ou cobertura.

## Workflow

1. Entenda o fluxo afetado (leia `README.md`/`ARCHITECTURE.md` e o código vizinho) antes de editar.
2. Faça mudanças pequenas e idiomáticas ao padrão existente.
3. Valide com `mvn -o clean compile -DskipTests` (checkstyle incluso).
4. Ao finalizar, ofereça o commit seguindo a convenção (nunca commitar por conta própria).
