# BedWars Architecture — AdvancedSlimePaper World System

## Visão Geral

Este documento define a arquitetura técnica oficial do sistema de mapas do plugin BedWars.
O objetivo é garantir um sistema escalável, isolado e performático, evitando a carga desnecessária de WorldEdit/Schematics durante o tempo de execução (runtime) de partidas.

> ⚠️ **Sistemas de mundos — IMPORTANTE.** O sistema **ativo** em runtime é o baseado em **schematic**: `manager/ArenaManager` + `world/WorldManager` + `world/Schematic`. As arenas são persistidas em `arenas/<nome>.yml`, os mapas como schematics FAWE em `maps/`, e os mundos de partida `bw_<nome>` são recriados via unload/delete verificado + `VoidGenerator` + paste do schematic a cada reset (`ArenaManager.resetArenaMap`). O sistema **Slime/ASP descrito nos diagramas abaixo é uma implementação paralela e NÃO ativa** (`arena/ArenaManager`, `reset/ResetManager`, `slime/*`, `template/*`, `world/SimpleWorldManager`) — não está conectado no `BedWarsPlugin.onEnable()`. Alterações devem priorizar o sistema Schematic; o Slime não deve ser tratado como produção.

## Pilares Tecnológicos

- **AdvancedSlimePaper / SlimeWorld:** Núcleo de gerenciamento de mundos para carregamento/descarregamento rápido de templates.
- **FAWE (FastAsyncWorldEdit):** Restrito exclusivamente à fase de edição administrativa.
- **Isolamento:** Cada partida roda em sua própria instância de mundo Slime.
- **Persistência:** Templates SlimeWorld (.slime) armazenados para instanciamento rápido.

---

## Arquitetura Geral (Fluxo de Dados)

```mermaid
flowchart TD

%% ==========================
%% CRIAÇÃO DO MAPA
%% ==========================

subgraph MAP_CREATION["🛠️ Criação e Persistência de Mapas"]

    Admin["👤 Administrador"]

    Admin -->|" /bw admin create <nome> "| Creator["ArenaCreator"]

    Creator -->|"Cria mundo temporário"| VoidWorld["Void Edit World"]

    VoidWorld -->|"Teleport do admin\nX:0 Y:2 Z:0"| EditorPosition["Área de Construção\nGlass Platform 0,0,0"]

    EditorPosition -->|"FAWE / Construção Manual"| Build["Construção da Arena"]

    Build -->|"Configuração"| Setup["Configuração da Arena\n• Spawn\n• Times\n• Camas\n• Geradores\n• Regras"]

    Setup -->|"/bw admin save"| TemplateManager["TemplateManager"]

    TemplateManager -->|"Serialização"| SlimeStorage["SlimeLoader"]

    SlimeStorage -->|"Persistência"| Template["📦 Template SlimeWorld\nmapa.slime"]

end


%% ==========================
%% GERENCIAMENTO DE ARENAS
%% ==========================

subgraph ARENA_SYSTEM["🎮 Sistema de Arena"]

    Template --> ArenaManager["ArenaManager"]

    ArenaManager --> TemplateCache["Template Cache"]

    TemplateCache -->|"Disponível"| ArenaPool["Arena Pool"]

    ArenaPool --> States["Arena State Machine"]

    States --> OFFLINE["OFFLINE"]
    States --> LOADING["LOADING"]
    States --> READY["READY"]
    States --> STARTING["STARTING"]
    States --> PLAYING["PLAYING"]
    States --> ENDING["ENDING"]
    States --> RESETTING["RESETTING"]

end


%% ==========================
%% MATCHMAKING
%% ==========================

subgraph GAME_FLOW["🏆 Fluxo de Partida"]

    Player["Jogadores"]

    Player --> Queue["QueueManager"]

    Queue -->|"Jogadores suficientes"| GameManager["GameManager"]

    GameManager -->|"Solicita arena"| ArenaManager

    ArenaManager -->|"Seleciona READY"| InstanceManager["SlimeInstanceManager"]

    InstanceManager -->|"Clone Template"| Clone["SlimeWorld Instance"]

    Clone -->|"Generate World"| GameWorld["Mundo da Partida"]

    GameWorld -->|"Chunks preparados"| Teleport["Player.teleportAsync()"]

    Teleport --> Match["Partida Iniciada"]

end


%% ==========================
%% DURANTE O JOGO
%% ==========================

subgraph RUNTIME["⚔️ Runtime da Partida"]

    Match --> Game["Game Controller"]

    Game --> Events["Eventos"]

    Events --> Beds["Sistema de Camas"]

    Events --> Generators["Geradores"]

    Events --> Teams["Times"]

    Events --> Shop["Loja"]

    Events --> Score["Scoreboard"]

end


%% ==========================
%% RESET
%% ==========================

subgraph RESET["♻️ Reset e Reciclagem"]

    Match -->|"Fim da partida"| EndGame["Game End"]

    EndGame --> ResetManager["ResetManager"]

    ResetManager -->|"Remove jogadores"| Cleanup["Cleanup"]

    Cleanup -->|"Unload World"| Unload["Unload Slime Instance"]

    Unload -->|"Delete Instance"| Destroy["Destruir Mundo"]

    Destroy -->|"Novo clone disponível"| ArenaPool

end


%% ==========================
%% PERFORMANCE
%% ==========================

subgraph PERFORMANCE["⚡ Otimização"]

    Cache["Cache Control"]

    Cache --> RAM["RAM Control"]

    Cache --> Limit["max-loaded-instances"]

    Limit --> UnloadPolicy["Auto Unload"]

    UnloadPolicy --> ResetManager

end
```

---

## Estrutura de Código (Package Map)

A organização segue padrões de alta coesão e baixo acoplamento:

```text
dev.sebastianjnuwu.bedwars
├── api/            # Interfaces e Eventos públicos para extensões
├── arena/          # Lógica de Arena e estados de instância
├── command/        # Sistema de comandos (BaseCommand, SubCommand)
├── editor/         # Lógica específica do editor de arenas
├── game/           # Core do jogo e estados de partida
├── lang/           # Internacionalização (lang/pt_BR.yml) via LangManager
├── libs/           # Código embarcado (bStats)
├── listener/       # Eventos do Bukkit (Arena, Game, UI)
├── manager/        # Managers singleton (ArenaManager, GameManager, etc.)
├── model/          # POJOs e modelos de dados
├── queue/          # Gerenciamento de filas de espera
├── reset/          # Lógica de descarte de instâncias após partida
├── session/        # Sessões de edição ativa
├── shop/           # Loja (ShopManager, ShopCategory, ShopItem, ShopGui) e NPCs
├── slime/          # Wrapper para SlimeWorld e carregamento de templates (NÃO ativo)
├── template/       # Gerenciamento de persistência de templates (NÃO ativo)
├── ui/             # Interfaces de usuário (GUIs)
├── util/           # Utilitários (LocationUtil)
└── world/          # Abstrações de Mundo (WorldManager, VoidGenerator, Schematic)
```

---

## Detalhes de Implementação

### 1. Sistema de Edição (`editor/`)

Utiliza `ArenaCreator` para gerar mundos `VOID` sem física.
O `ArenaEditorValidator` garante que a arena possua todos os requisitos (spawns, camas, geradores) antes de permitir o `save`.

### 2. Sistema de Mundos (`world/`, `manager/`)

Sistema **ativo** baseado em schematic:

- `world/WorldManager`: cria mundos void (`bw_<nome>` via `WorldCreator` + `VoidGenerator`), salva/copia templates e descarrega mundos com verificação.
- `world/Schematic`: captura e aplica schematics FAWE (`.schem`/`.bwmap`) em `maps/` — usado apenas no load/save/restore, nunca em runtime.
- `manager/ArenaManager`: `resetArenaMap(name)` executa o ciclo de reset — unload + delete verificados (`WorldManager.deleteWorld`) → novo mundo void → `Schematic.paste` → `flush`. Retorna `boolean`; em falha loga `log.arena_manager.reset_error`.

Implementação paralela **NÃO ativa**: `world/SimpleWorldManager`, `slime/SlimeWorldManager`, `slime/SlimeManager` (ver aviso no início).

### 3. Gerenciamento de Partidas (`game/`, `arena/`)

O `GameManager` coordena a transição entre `GameState` (READY -> STARTING -> PLAYING -> ENDING -> RESETTING).
O `ResetManager` (sistema Slime, não ativo) não limpa blocos: solicita o descarregamento da instância `SlimeWorld` e remove o arquivo temporário. No sistema **ativo**, o reset é feito por `ArenaManager.resetArenaMap(name)` chamado por `Game.forceEnd()`/`endGame()` — recria o mundo do schematic a cada partida, garantindo integridade total para a próxima.

---

## Regras de Desenvolvimento (Obrigatório)

1. **Zero WorldEdit em Runtime:** Proibido usar WE para limpar arena.
2. **Async Operations:** Sempre usar `TeleportAsync` e operações de mundo assíncronas para não travar a thread principal.
3. **Imutabilidade de Template:** Templates são *read-only*. Partidas SEMPRE rodam em cópias (instâncias).
4. **Descarregamento Seguro:** Partidas encerradas devem disparar o ciclo de `RESETTING` imediatamente.
5. **Clean Architecture:** Novas funcionalidades devem preferir a injeção dos Managers existentes.
