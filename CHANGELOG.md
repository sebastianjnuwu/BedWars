# Changelog

Todas as mudanças notáveis do plugin **BedWars** (Paper 1.21.4, Java 21) são documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões pares de "bump" (apenas atualização do número no `pom.xml`) são omitidas.

## [0.0.1-224] - 2026-08-17

### Adicionado
- **Integração com PlaceholderAPI**: novo `hook/PlaceholderApiHook.java` estendendo `PlaceholderExpansion` (identificador `bedwars`, `persist()`), registrado no `onEnable` do `BedWarsPlugin` somente quando o plugin está presente (softdepend). Dependência `me.clip:placeholderapi:2.11.6` (scope `provided`) adicionada no `core/pom.xml` com o repositório ExtendedClip no POM raiz; `plugin.yml` ganhou `PlaceholderAPI` na lista de softdepend.
- **Placeholders globais**: `%bedwars_all_players%` (jogadores + espectadores em todas as partidas), `%bedwars_playing%`, `%bedwars_waiting%` (partidas em WAITING/STARTING), `%bedwars_spectators%`, `%bedwars_games%`, `%bedwars_games_playing%`, `%bedwars_games_waiting%` e `%bedwars_arena_<nome>%` (todos os jogadores nas instâncias de uma arena).
- **Placeholders do jogador**: `%bedwars_in_game%` (1/0), `%bedwars_state%`, `%bedwars_team%`, `%bedwars_code%` e `%bedwars_arena%` — resolvidos pela partida do jogador solicitante (via UUID).
- **`manager/game/GameManager.java`**: novos métodos públicos `getActiveGames()` (view de `games`) e overloads `getPlayerGame(UUID)`/`isInGame(UUID)` sobre `playerGames`, usados pelo hook.
- **`lang/pt_BR.yml`**: chaves `startup.placeholderapi_hook` e `startup.placeholderapi_unavailable`.

### Documentação
- **`README.md`**: nova seção "16. Placeholders (PlaceholderAPI)" com tabela completa dos placeholders `%bedwars_*%`; PlaceholderAPI incluído em "Requisitos" e na lista de instalação; tabela "Modos e times" ganhou a coluna "Mapa com 6 camas"; seção "Comandos" reescrita em duas tabelas (Jogador `bw.player` e Administrador `bw.admin`), incluindo o comando `/spawn`.

## [0.0.1-223] - 2026-08-17

### Documentação
- **`AGENTS.md`**: seção "Estrutura do código" substituída por um **mapa de navegação** em árvore (pacotes + subpacotes + classes por linha) refletindo a reorganização da v222 (`game/{combat,ending,lifecycle,ticker,upgrade,util}`, `shop/{gui,model,parser}`, `manager/{arena,game}`), economizando tokens ao localizar classes sem varrer a árvore. Novas regras: "ECONOMIA DE TOKENS" no Workflow (localizar via mapa e ler apenas a seção afetada, não o arquivo inteiro) e referências de caminho atualizadas (`manager/ArenaManager` → `manager/arena/ArenaManager`).
- **`ARCHITECTURE.md`**: "Estrutura de Código (Package Map)" atualizada com a visão por pacote e o mapa completo delegado ao `AGENTS.md`; referências de caminho corrigidas nas seções 2 (`world/`, `manager/arena/`), 3 (`game/`, `manager/game/`), 5 (`GameTimeLimit` em `game/ending/`), no aviso do topo e no diagrama Mermaid.

## [0.0.1-222] - 2026-08-17

### Refatorado
- **Reorganização em subpacotes temáticos** (mantendo comportamento idêntico): a regra do `AGENTS.md` "helpers no mesmo pacote" passa a permitir subpacotes temáticos, mantendo as fachadas públicas que delegam.
- **`game/` subdividido**: `game/combat/GameCombat.java`, `game/ending/GameEnding.java` + `GameTimeLimit.java`, `game/lifecycle/GameLifecycle.java` + `GamePlayerSnapshot.java` + `GameTeamPicker.java`, `game/ticker/GameTicker.java` + `GameGeneratorTicker.java`, `game/upgrade/GameUpgrades.java` + `GameForge.java`, `game/util/GameCodeGenerator.java` + `GameDebug.java` + `GameQueries.java`. `Game.java` permanece como fachada (campos de estado e accessors `public`), com imports cruzados entre subpacotes via `Game`.
- **`shop/` subdividido**: `shop/gui/` (ShopGui, ShopGuiRenderer, ShopSlotGrid, ShopPurchase, ShopArmorLogic), `shop/model/` (ShopItem, ShopItemBuilder, ShopCategory), `shop/parser/` (ShopConfigParser, ShopUpgradeParser). A raiz mantém ShopManager, ShopNpcManager, ShopListener, NpcListener e CitizensNpcListener.
- **`manager/` subdividido**: `manager/arena/` (ArenaManager, ArenaPersistence, ArenaWorldService, ArenaYamlMapper, ArenaLocationCodec, ArenaCommandParser, MapFileResolver, ArenaBedRestorer, ArenaMarkerBlocks, ArenaWorldReferenceUpdater) e `manager/game/` (GameManager, GameJoinQueue, GameLookup, GameValidator). A raiz mantém ConfigManager, DataManager e PlayerStateManager.
- Imports atualizados nos 45 arquivos externos (`manager.ArenaManager` → `manager.arena.ArenaManager`, `manager.GameManager` → `manager.game.GameManager`, `shop.ShopGui` → `shop.gui.ShopGui`).

### Corrigido
- Import `de.oliver.fancynpcs.api.events.NpcInteractEvent` restaurado em `shop/NpcListener.java` (não pertence a nenhum grupo do `ImportOrder`).
- FQN inline de `manager.ArenaManager` em `session/EditorManager.java` substituído por import + nome curto.

## [0.0.1-221] - 2026-08-17

### Refatorado
- **Nova regra de tamanho no `AGENTS.md`** (seção "Estilo de código"): arquivos-fonte não devem ultrapassar **~350 linhas** (meta flexível, bom senso); acima disso, o arquivo deve ser subdividido em helpers de composição no mesmo pacote, mantendo uma fachada que delega. Exceção: código embarcado de terceiros (`libs/bstats`).
- **`game/Game.java` (412 → 341)**: extraídos `game/GameQueries.java` (consultas de jogadores/estado: `isSpectator`, `isBedless`, `isEliminated`, `getPlayerTeam`, `getGamePlayer`, `isPlaying`, `trackPlacedBlock`/`isPlacedBlock`, `getPlayerCount`, `isFull`, `getGamePlayers`, `getPlayers`, `getSpectatorPlayers`, `broadcast`, `blockKey`), `game/GameCodeGenerator.java` (código público de 6 caracteres) e `game/GameDebug.java` (log de depuração). A fachada mantém os delegadores públicos.
- **`manager/GameManager.java` (403 → 333)**: extraídos `manager/GameLookup.java` (buscas de partida: `findFirstByArenaName`, `findGameByCode`, `findOpenGame`, `listOpenCodes`) e `manager/GameValidator.java` (validação de arena: spawn, times mínimos, camas e forjas por time).
- **`game/GameLifecycle.java` (387 → 349)**: extraídos `game/GameTeamPicker.java` (seleção de times: `findSmallestTeam`, `findNamedTeam`, `maxTeamSlots`, `largestValidMode`) e `game/GamePlayerSnapshot.java` (backup/restauração de inventário e reexibição de jogadores de outras partidas).
- **`shop/ShopGuiRenderer.java` (378 → 199)**: extraído `shop/ShopSlotGrid.java` (cálculo de grade: `computeSlots`, `placeItem`, `centerGrid` e constantes `ITEMS_START`/`ITEMS_END`/`ITEMS_PER_PAGE`).
- **`shop/ShopPurchase.java` (371 → 210)**: extraído `shop/ShopArmorLogic.java` (lógica de armaduras: `alreadyHasArmor`, `collectArmorPieces`, `equippedArmor`, `effectivePoints`, `equipTeamArmor`, `leatherFor`, `armorPoints`, `armorToughness` e proteção do time).
- **`manager/ArenaWorldService.java` (368 → 326)**: extraído `manager/ArenaWorldReferenceUpdater.java` (re-sincronização de spawns, camas, geradores e NPCs para o mundo recém-construído).
- **`manager/ArenaYamlMapper.java` (409 → 331)**: extraídos `manager/ArenaLocationCodec.java` (serialização/parsing de localizações `mundo,x,y,z,yaw,pitch`, incluindo `rebase` para instâncias) e `manager/ArenaCommandParser.java` (normalização de `enabled-commands`).
- **`game/GameEnding.java` (353 → 277)**: extraído `game/GameTimeLimit.java` (limite de tempo: avisos em marcos, contagem final, desempate por jogadores vivos/berço/kills e vitória/empate).

### Corrigido
- Imports não usados (`ArenaTeam`, `ArenaMode`, `Bukkit`, `Component`, `CompatProvider`, `Collectors`, `Objects`, `MiniMessage`, `GameItems`, `Color`, `Attribute`, etc.) removidos das fachadas durante as extrações (checkstyle 0 violações).
- `ShopPurchase.deliverItem` re-adicionado após remoção acidental durante a extração de `ShopArmorLogic` (o build detectou os símbolos faltantes).

## [0.0.1-220] - 2026-08-17

### Refatorado
- **`listener/ArenaListener.java` reduzido → fachada**: extraído `listener/ArenaBlockRestorer.java` com a restauração de marcadores de edição quebrados (`tryRestoreArenaSpawn`/`tryRestoreTeamSpawn`/`tryRestoreBed`/`tryRestoreGenerator`, além de `getBedFootLocation` e `isSameBlock`). O listener mantém os handlers e delega via `blockRestorer`; dependências `Location`/`Bukkit.createBlockData` movidas para o helper.
- **`shop/ShopItem.java` reduzido**: o builder fluente interno (≈120 linhas) extraído para `shop/ShopItemBuilder.java` (classe própria no mesmo pacote, com getters package-private); `ShopItem` passou a expor construtor package-private `ShopItem(ShopItemBuilder)` e o `shop/ShopConfigParser` agora usa `ShopItemBuilder`.
- **`arena/ArenaManager.java` (sistema Slime paralelo) reduzido → fachada**: extraído `arena/ArenaFileStore.java` com a persistência YAML das arenas (`load`/`save`, serialização de localizações, `setIfNotNull`). O manager mantém instâncias, templates, criação/reset/deleção e delega a persistência ao `fileStore`.
- **`slime/SlimeManager.java` reduzido**: extraídos `slime/SlimeWorldSettings.java` (aplicação de dificuldade/tempo/clima/regras do mundo) e `slime/SlimeFileUtil.java` (cópia recursiva de mundos, exclusão de pastas, filtro de arquivos ignorados). Aplicação de settings em `createInstance` passou a chamar `SlimeWorldSettings.apply`.

### Corrigido
- **`arena/ArenaFileStore`**: corrigido o import inválido (`as ModelArena`) durante a extração — o conflito de nome entre `api.model.Arena` e `model.Arena` é resolvido importando o `api` e usando fully-qualified name inline no `model` (regra de name clash do AGENTS.md).

## [0.0.1-219] - 2026-08-17

### Corrigido
- **Mensagem de moeda insuficiente mostrava o material cru** (`shop/ShopPurchase.purchaseItem`): ao comprar sem saldo, o `shop.not_enough` recebia `currencyMaterial.name().toLowerCase()` (ex.: "gold_ingot") em vez do nome da moeda. Agora usa `gui.currencyName(currencyMaterial)` (ex.: "Ouro", "Esmeralda") — mapeamento que cobre `IRON_INGOT`, `GOLD_INGOT`, `DIAMOND` e `EMERALD` via `lang/pt_BR.yml` (`shop.currency_iron/gold/diamond/emerald`).

## [0.0.1-218] - 2026-08-17

### Refatorado
- **`game/Game.java` reduzido de 1794 → 354 linhas** (padrão `ChatManager`): extraídos `game/GameItems.java` (constantes de item, cores de lã/armadura, helpers estáticos), `game/GameLifecycle.java` (criação, `killPlayer`, `becomeSpectator`, respawns pendentes, `endGame`/`forceEnd`), `game/GameUpgrades.java` (upgrades de time, níveis de afiação/proteção/forja, `GameUpgrades` com `forgeKey`/`isSword`), `game/GameTicker.java` (ciclo de ticks, contagem regressiva, tempo limite com marcos, `countdownPitch`), `game/GameCombat.java` (dano entre jogadores, `GamePlayerDamageByPlayerEvent`) e `game/GameEnding.java` (anúncio de fim de partida, stats, reset). O `Game` virou fachada: campos de estado package-private acessados pelos helpers; chamadas cruzadas via `game.items()`, `game.lifecycle()`, `game.upgrades()`, `game.ticker()`, `game.combat()`, `game.ending()`. `Game.getWoolColor`/`getArmorColor` (static) movidos para `GameItems`; `shop/ShopGui` atualizado para usá-los.
- **`manager/ArenaManager.java` reduzido de ~1180 → fachada**: extraídos `manager/ArenaPersistence.java` (load/save/flush/serialização de localizações, comandos habilitados, busca de arquivos de arena) e `manager/ArenaWorldService.java` (backend de mundos: `ensureArenaReady`, `resetArenaMap`, criação/remoção de instâncias, `updateWorldReferences`, `restoreBeds`, marcadores). O `ArenaManager` virou fachada com campos package-private (`plugin`, `arenas`, `arenasFolder`, `mapsFolder`, `worldProvider`, `lang`, `diskConfigs`, `cleanWorlds`, `instanceCounters`) e `persistence()`/`worldService()` privados com accessor `persistence()`.
- **`listener/GameListener.java` reduzido de 1131 → fachada de registro**: extraídos `listener/GameCombatListener.java` (morte, respawn, dano no vazio, dano geral, friendly-fire), `listener/GameItemListener.java` (fireball, ovo de ponte, drop, proteção de armadura no inventário, pickup, craft), `listener/GameGolemListener.java` (golem: convocação, dano amigável, alvo, IA e tick de limpeza com mapa `golemOwners` privado) e `listener/GamePlayerListener.java` (quebra/colocação de blocos, camas, explosões, comandos bloqueados, quit/kick). `GameListener.registerAll()` registra os quatro; `BedWarsPlugin.onEnable` passou a chamá-lo.
- **`shop/ShopGui.java` reduzido de 976 → fachada de estado**: extraídos `shop/ShopGuiRenderer.java` (renderização: título, borda, categorias, produtos, paginação, grade de slots, centralização) e `shop/ShopPurchase.java` (compra: validação de saldo, armaduras, upgrades, kits recursivos, `PlayerPurchaseEvent`). Helpers no mesmo pacote acessam o estado package-private da fachada; `ShopGui.handleClick` orquestra navegação/delegação.

### Corrigido
- Imports não usados e acessos privados em `Game`, `GameLifecycle`, `GameTicker`, `ArenaManager` e `ShopGuiRenderer`/`ShopPurchase` detectados pelo checkstyle/build durante a extração (resolvidos).

## [0.0.1-217] - 2026-08-16

### Alterado
- **Avisos de tempo limite em múltiplos marcos** (`game/Game.handleTimeLimit`): o aviso de fim de partida agora é enviado ao chat nos segundos 60, 45, 30, 15 e 10 restantes (uma vez cada, rastreado por `Set<Integer> timeLimitWarningsSent`), em vez de apenas aos 30s. O gatilho deixou de depender de `remaining == 30` (que nunca disparava com limites menores que 30s, ex.: 15s) e passa a usar os marcos fixos. Mantém o título 5, 4, 3, 2, 1 com subtítulo nos últimos 5s e o som de bip.
- **Cor dos avisos de tempo limite** (`lang/pt_BR.yml`): `time_limit_warning`, `time_limit_warning_once` e `time_limit_ending` alteradas de `§c` (vermelho) para `§e` (amarelo); o envio no chat usa `NamedTextColor.GOLD`. Nova chave de debug `debug.time_limit_warning_sent` para diagnóstico no console.

## [0.0.1-216] - 2026-08-16

### Corrigido
- **Contagem regressiva do tempo limite spammava o chat** (`game/Game.handleTimeLimit`): a cada 5 segundos nos últimos 20% da partida era enviada a mensagem `game.time_limit_warning` ("O tempo limite será atingido em {0} segundo(s)!") no chat de todos os jogadores. Agora o aviso é único: a 30 segundos do fim o chat recebe `game.time_limit_warning_once` ("A partida termina em {0} segundos!") uma vez; nos últimos 5 segundos o título mostra a contagem 5, 4, 3, 2, 1 com o subtítulo `game.time_limit_ending` ("A partida vai acabar!") e som de bip — sem mensagens repetidas no chat.
- `shop/ShopGui`: removidos os warnings de deprecação de `ItemStack#setType` ao trocar o item maxed para `RED_STAINED_GLASS_PANE` (agora usa `new ItemStack(...)` com a mesma quantidade).

## [0.0.1-215] - 2026-08-16

### Adicionado
- **Upgrades de time configuráveis na loja** (`shop.yml`): nova subseção `upgrade-config:` nos itens com `upgrade:` (fornalha, afiação e proteção), com `max-level`, `level-default` (opcional, só forja) e `levels.<n>.upgrade.price`/`levels.<n>.upgrade.material` — e, para a forja, `<material>.interval`. A configuração agora é global (igual em todas as arenas) e lida pela partida a partir da loja da arena (`arena.getShop()`, fallback `default`).

### Alterado
- **Novo modelo** `api/model/UpgradeConfig` (`record UpgradeConfig(int maxLevel, int levelDefault, List<ForgeLevel> levels)` com overload `(maxLevel, levels)` e método `nextLevel(currentLevel)`): unifica a configuração de níveis dos três upgrades de time reutilizando o `ForgeLevel` existente; substitui o `TeamUpgradeLevel` (removido).
- `shop/ShopItem`: novo campo `upgradeConfig` (getter + builder `upgradeConfig(...)`).
- `shop/ShopManager`: `parseItem` agora lê a subseção `upgrade-config` (novos helpers `parseUpgradeConfig`/`parseUpgradeLevel`/`parseUpgradeMaterial`/`parsePositiveInt`/`parsePositiveLong`); novo método público `getUpgradeConfig(shopName, upgrade)` com busca recursiva nas categorias (`findUpgradeConfig`).
- `game/Game`: `getForgeUpgradeLevel`, `getForgeMaxLevel`, `getForgeDefaultLevel` (novo helper) e `putForgeTicks`/`initForgeTicks` passam a ler a configuração da loja via `upgradeConfig("forge")` (novo helper que acessa o `ShopManager` pelo nome da loja da arena); `getSharpnessUpgradeLevel`/`getProtectionUpgradeLevel`/`getMaxSharpnessLevel`/`getMaxProtectionLevel` usam `upgradeConfig("sharpness")`/`upgradeConfig("protection")` e retornam `ForgeLevel`; removidos `findUpgradeLevel` e o import de `TeamUpgradeLevel`.
- `shop/ShopGui`: `teamUpgradeLevel(String)` agora retorna `ForgeLevel` (novo tipo devolvido pelos métodos do `Game`); quando o upgrade está no nível máximo, o item da loja vira `RED_STAINED_GLASS_PANE` (painel de vidro vermelho), mantendo nome/lore.
- `api/model/Arena` + `model/Arena`: removidos os métodos/campos de upgrades de time — `get/setForgeMaxLevel`, `get/setForgeDefaultLevel`, `get/setForgeLevels`, `get/setSharpnessMaxLevel`, `get/setSharpnessLevels`, `get/setProtectionMaxLevel`, `get/setProtectionLevels` (config movida para a loja).
- `manager/ArenaManager`: removida a persistência de forge/upgrades da arena (`save`/`loadArenaFromFile`/`create`) e os helpers `writeTeamUpgradeLevels`, `readTeamUpgradeLevels`, `defaultTeamUpgradeLevels` e `forgeCurrencyMaterial`.
- `command/admin/config/StatusCommand`: removida a listagem de níveis da fornalha (`arena.getForgeLevels()`), que não existe mais na arena.
- `shop.yml`: os itens de upgrade ganharam `upgrade-config` — afiação/proteção com 3 níveis (2/4/8 diamante) e fornalha com 10 níveis (`level-default: 1`) e os intervalos de geração por material que antes ficavam na arena.
- `arenas/example.yml`: removidas as seções `forge`, `sharpness` e `protection` (agora configuradas na loja); adicionada nota apontando para o `shop/<loja>.yml`; comentários reescritos de forma objetiva e direta (cada chave explica seu valor e formato sem redundância).

### Corrigido
- **Intervalos da fornalha nunca eram aplicados** (`shop/ShopManager.parseUpgradeConfig`/`parseUpgradeLevel`): as chaves numéricas do YAML (`levels: 1:`, `2:`...) são lidas pelo SnakeYAML como `Integer`, e o cast `(Map<String, Object>)` lançava `ClassCastException` dentro do `parseItem`, que descartava o item (retornando `null`). Resultado: `upgradeConfig("forge")` era `null` e o `Game.putForgeTicks` logava "intervalos nulos ou vazios, forgeLevels=null" em toda partida. Corrigido iterando com `Map<?, ?>` e convertendo a chave via `String.valueOf(...)`.

## [0.0.1-214] - 2026-08-16

### Adicionado
- **Upgrades de time configuráveis no YML** (afiação e proteção) seguindo a mesma estrutura da fornalha: nova seção `sharpness` e `protection` no `arenas/<nome>.yml`, com `max-level` e `levels.<n>.upgrade.price`/`levels.<n>.upgrade.material` (moeda: iron | gold | diamond | emerald). O preço e a moeda de cada nível agora vêm da configuração da arena, não mais do preço fixo do item na loja (`price * level`).

### Alterado
- **Novo modelo** `api/model/TeamUpgradeLevel` (`record TeamUpgradeLevel(int level, int upgradePrice, Material upgradeMaterial)` com overload sem upgrade): espelha o `ForgeLevel` para upgrades de time.
- `api/model/Arena` + `model/Arena`: novos getters/setters `get/setSharpnessMaxLevel`, `get/setSharpnessLevels`, `get/setProtectionMaxLevel`, `get/setProtectionLevels`; defaults (3 níveis, diamante) no construtor e propagados no `copy()`.
- `manager/ArenaManager`: `save` grava as seções `sharpness.*`/`protection.*` (via novo helper `writeTeamUpgradeLevels`), `loadArenaFromFile` as lê (via novo helper `readTeamUpgradeLevels`, reutilizando `forgeCurrencyMaterial`), e `create` popula os defaults (`defaultTeamUpgradeLevels` — 3 níveis: 2/4/8 diamante).
- `game/Game`: removida a constante `MAX_TEAM_UPGRADE_LEVEL`; `getMaxSharpnessLevel`/`getMaxProtectionLevel` agora leem a arena; novos métodos públicos `getSharpnessUpgradeLevel(team)`/`getProtectionUpgradeLevel(team)` retornam o `TeamUpgradeLevel` do próximo nível (via `findUpgradeLevel`); `getMaxUpgradeLevel` passou a delegar para a afiação (mantido para compatibilidade).
- `shop/ShopGui`: `createDisplayItem` e `purchaseItem` usam o `TeamUpgradeLevel` configurado (preço + moeda por nível) para afiação/proteção, com a mesma lógica de "maxed" da fornalha; removidos os helpers `isTeamUpgradeMaxed`, `currentUpgradeLevel` e `upgradePrice` em favor de `teamUpgradeLevel(String)`.
- `arenas/example.yml`: documentadas as seções `sharpness` e `protection` (estrutura, regras e 3 níveis de exemplo com diamante).

## [0.0.1-213] - 2026-08-15

### Adicionado
- **Arquitetura de providers de mundo** (`world/`): nova interface `WorldProvider` (contrato do ciclo de vida dos mundos de partida — `id`, `isAvailable`, `buildWorld`, `buildWorldAsync`, `deleteWorld`, `applyWorldSettings`, `templateExists`, `saveTemplate`, `getTemplateFolder`) com duas implementações independentes:
  - `SchematicWorldProvider` (backend ativo): movido do `manager/ArenaManager` o fluxo de criar/colar/finalizar mundo (WorldCreator + VoidGenerator + Schematic/FAWE), incluindo `createOrLoadWorld`, `pasteSchematic`, `finalizeWorld`, `clearWorldEntities` e `applyWorldSettings`; `buildWorldAsync` cria o void na main, cola o schematic async e conclui na main (callback via scheduler do plugin).
  - `SlimeWorldProvider` (sistema paralelo): estende o `SchematicWorldProvider`, herdando o fluxo de criar/colar/finalizar, mas cria os mundos de partida como mundos Slime vazios gerenciados pelo `SlimeManager` (`createEmptySlimeWorld` via `createVoidSlimeWorld` + `loadWorld`). Se a criação do mundo Slime falhar, cai no mundo void padrão (WorldCreator) para não impedir a partida. `applyWorldSettings` herdado. O `id()`/`isAvailable()` refletem o ASP.
  - `WorldProviders` (ponto único, no padrão do `CompatProvider`): `init(plugin, lang)` no `onEnable` escolhe o backend **automaticamente** por `SlimeManager.isAvailable()` — Slime quando disponível, senão Schematic; `world()` expõe o singleton com guard de inicialização. **Sem** nova chave de config (`world-backend` não foi adicionado).
- **Log de backend no startup** (`lang/pt_BR.yml`, seção `startup`): novas chaves `world_backend_schematic` e `world_backend_slime` usadas pelo `WorldProviders.init`.

### Corrigido
- **Partidas não iniciavam em servidores com AdvancedSlimePaper** (`world/SlimeWorldProvider`): a seleção automática escolhia o backend Slime (ASP presente), mas o pipeline de template Slime estava incompleto — o `loadTemplate` exigia `level.dat`, que o formato de mundo do Paper 1.21.4 (`paper-world.yml`, sem `level.dat`) não possui, então `readVanillaWorld` falhava e o `/bw start` lançava "Template não encontrado: <arena>". Corrigido o `SlimeWorldProvider` para **construir os mundos de partida a partir do schematic da arena** (cria um mundo Slime vazio via ASP e cola o `.schem`/`.bwmap`), reutilizando o fluxo comprovado do `SchematicWorldProvider` (via herança) — o que torna o backend Slime funcional para iniciar partidas sem depender do template vanilla incompatível. `createOrLoadWorld` virou `protected` no `SchematicWorldProvider` para permitir a sobrescrita.
- **VersionChecker mostrava "v${revision}" na versão publicada** (`util/VersionChecker.parseVersion`): o `pom.xml` raiz usa versão CI-friendly (`<version>${revision}</version>`), então o primeiro `<version>` do arquivo no GitHub era o placeholder `${revision}`, exibido literalmente na comparação. Corrigido o `parseVersion` para resolver o placeholder: quando o valor lido é `${<prop>}`, ele lê a tag `<prop>` correspondente (ex.: `<revision>0.0.1-213</revision>`) e usa o valor resolvido.

### Alterado
- `manager/ArenaManager`: o campo `WorldManager` foi substituído por `WorldProvider` (constructor agora `(plugin, WorldProvider, mapsFolder)`). `resetArenaMap`, `createInstance`, `createInstanceAsync`, `ensureWorldLoaded` e `delete` passam a delegar a construção/remoção ao provider; `applyWorldSettings` virou delegate público; adicionado método público `deleteWorld(String)` (usado pelo `EditCommand`); removidos `getWorldManager` e os métodos/imports de `Schematic`/`VoidGenerator`/`WorldCreator`/`Entity`/`Player`/`Difficulty`/`GameRule` que ficaram sem uso.
- `command/admin/arena/EditCommand`: `arenaManager.getWorldManager().deleteWorld(...)` → `arenaManager.deleteWorld(...)` (o fluxo de edição com WorldCreator+VoidGenerator direto permanece inalterado).
- `BedWarsPlugin.onEnable`: `WorldProviders.init(this, this.lang)` no lugar de `new WorldManager(this)`; `new ArenaManager(this, WorldProviders.world(), mapsFolder)`.

## [0.0.1-212] - 2026-08-15

### Corrigido
- **Espada nova comprada na loja vinha sem a Afiação do time** (`shop/ShopGui`): o `deliverItem` aplicava a cor da time e entregava o item, mas nunca aplicava o encantamento de Afiação — ao contrário da armadura, que já recebia a Proteção do time (`equipTeamArmor` → `applyTeamProtection`). Adicionado `applyTeamSharpness(ItemStack)` no `deliverItem`: se o item é uma espada (`*_SWORD`) e o time tem upgrade de Afiação (`game.getSharpnessLevel(team)`), aplica `Enchantment.SHARPNESS` no nível do time antes de dar/dropar. Agora a espada comprada (inclusive após já estar no nível máximo) nasce com a Afiação correta.

## [0.0.1-211] - 2026-08-15

### Corrigido
- **Limpeza de warnings de deprecação/compilação** (build limpo no IDE/`mvn -o clean compile`):
  - `compat/NbtCompatImpl` e `compat/RegistryCompatImpl`: `@SuppressWarnings("deprecation")` nos usos intencionais de `Bukkit.getUnsafe()` e `Registry.ENCHANTMENT`.
  - `compat/PotionCompatLegacy`: `@SuppressWarnings("removal")` para `setBasePotionData(PotionData)`/`PotionData` (marcados para remoção desde 1.20.6 — o `"deprecation"` não os cobre).
  - `shop/ShopItem`: removidos `@SuppressWarnings("deprecation")` desnecessários em `applyTag`/`applyDisplayMeta`.
  - `game/Game.sendEndRanking`: `Comparator.comparingInt((GamePlayer gp) -> gp.getKills())` com parâmetro tipado no lugar do method reference, eliminando o aviso de null type safety do JDT (e a falha de inferência de tipo).

## [0.0.1-210] - 2026-08-15

### Corrigido
- **Loja bloqueava compra de armadura de ferro/ouro/diamante quando o time tinha upgrade de Proteção** (`shop/ShopGui`): o `effectivePoints` somava o encantamento de Proteção da armadura equipada (`protectionWeight`), então a couro do time encantada (Proteção I–III) passava a valer mais pontos que ferro/ouro/diamante e o `alreadyHasArmor` retornava "você já tem uma armadura melhor", impedindo o upgrade de material. Como a Proteção é upgrade de time (aplica-se a qualquer armadura), ela foi removida da comparação de pontos (`effectivePoints` agora considera apenas material + atributo `bw_armor`). Além disso, a peça recém-equipada agora recebe a Proteção do time (`applyTeamProtection` no `equipTeamArmor`), mantendo o upgrade ao trocar de couro para ferro/ouro/diamante. Removido o helper `protectionWeight` (sem uso).

## [0.0.1-209] - 2026-08-15

### Alterado
- **Upgrades `sharpness`/`protection` agora seguem o mesmo fluxo da fornalha** (`shop/ShopGui`): preço escala por nível (`price` base × nível), lore mostra "Preço" + "Próximo nível" (chave `shop.forge_next_level` reaproveitada) e, no nível máximo, a compra é bloqueada com a mensagem `shop.upgrade_maxed` antes de cobrar. Removida a chave `shop.upgrade_level` (nível atual) em favor do fluxo "próximo nível". `Game.getMaxUpgradeLevel()` exposto publicamente para a loja.

## [0.0.1-208] - 2026-08-15

### Corrigido
- **Upgrades de time `sharpness` e `protection` não funcionavam** (`shop/ShopGui`): o `handleUpgrade` só implementava o upgrade `forge`; `sharpness`/`protection` caíam no `default` vazio e o jogador pagava sem receber efeito. Implementados como upgrades por time (níveis I–III), seguindo o padrão do forge:
  - `game/Game`: novos níveis por time (`sharpnessLevels`/`protectionLevels`), métodos `getSharpnessLevel`/`upgradeSharpness`/`getProtectionLevel`/`upgradeProtection` (limite `MAX_TEAM_UPGRADE_LEVEL = 3`) e `applyTeamUpgrades` (aplica `Enchantment.SHARPNESS` nas espadas e `Enchantment.PROTECTION` nas armaduras do time, inclusive no respawn e no início da partida).
  - `shop/ShopGui`: `handleUpgrade` agora roteia `forge`/`sharpness`/`protection`; `purchaseItem` bloqueia a compra quando o upgrade do time já está no nível máximo (antes cobrava e não aplicava); `createDisplayItem` mostra o nível atual via nova chave `shop.upgrade_level`; novas chaves `shop.upgrade_maxed`.
- **NPE ao comprar item comum na loja** (`shop/ShopGui.purchaseItem`): a checagem `isTeamUpgradeMaxed` era chamada com `upgrade == null` para itens sem upgrade (o switch em null lançava `NullPointerException` no `InventoryClickEvent`). Corrigido com guarda `upgrade != null`.

## [0.0.1-207] - 2026-08-09

### Adicionado
- **Camada de compatibilidade multi-versão** (`compat/`): interfaces `ChatCompat`, `GolemCompat`, `NbtCompat`, `PotionCompat`, `RegistryCompat` e `TeleportCompat` + implementações nativas (`*Impl`) + `CompatProvider` (ponto único de acesso com `init()` no `onEnable`, detecção de `minorVersion()`/`isPaper()`). O core agora depende apenas de API pública estável, removendo chamadas diretas a APIs versionadas do Paper, para permitir builds por faixa de versão (incluindo Spigot legado).
- **Implementações legadas por faixa de versão** (`compat/*Legacy`): o `CompatProvider` agora seleciona a impl pela versão do servidor — nativa quando a API existe, legada caso contrário:
  - `TeleportCompatLegacy`: usa `player.teleport(...)` (síncrono) em versões < 1.20, onde `teleportAsync` não existe;
  - `RegistryCompatLegacy`: usa `Enchantment.getByName(...)` em versões < 1.19.4, onde `Registry.ENCHANTMENT` não existe;
  - `PotionCompatLegacy`: usa `PotionMeta#setBasePotionData(PotionData)` em versões < 1.20.5, onde `setBasePotionType` não existe;
  - `GolemCompatLegacy`: sem a Mob Goal API (< 1.20.6), reaplica `setTarget(...)` periodicamente (a cada 4 ticks) com o mesmo resolver de time, deixando a IA vanilla do golem (criado com `setPlayerCreated(false)`) cuidar da perseguição e dano. O dano de `attack` cai para `LivingEntity.damage` usando o atributo `ATTACK_DAMAGE`.
  - `ChatCompat` e `NbtCompat` seguem nativas em todas as versões (Adventure nativo do Paper desde 1.16.5 e `Bukkit.getUnsafe().modifyItemStack` disponível em todo o Paper).
  - Limiares: golem `>= 1.20.6`, poção `>= 1.20.5`, registry `>= 1.19.4`, teleporte `>= 1.20` (novo `CompatProvider.isAtLeast(minor, patch)`).

### Alterado
- **`plugin.yml`**: `api-version` baixado de `1.21` para `1.16` — com a camada de compat usando apenas API estável, o plugin pode carregar em servidores Paper/Purpur 1.16.5+ sem o Paper recusar o load por `api-version` alto.
- **Build/packaging** (`pom.xml` raiz + `core/pom.xml`): versão única via propriedade CI-friendly `${revision}` definida no POM raiz e herdada pelo módulo (bump agora é feito em um único lugar). `finalName` passou a `sBedWars-v${project.version}` (JAR sem o sufixo `-core`); o shade usa `outputFile` (não gera mais `original-*.jar`) e `createDependencyReducedPom=false` (não gera mais `dependency-reduced-pom.xml`). Resultado: `mvn clean package` → um único `core/target/sBedWars-v${revision}.jar`.
- **Docs/CI**: `README.md` e `AGENTS.md` atualizados para a estrutura multi-módulo (`core/target/sBedWars-v${revision}.jar`); `.gitignore` passou a ignorar `core/target/`; `build.yml` publica o artifact com nome `sBedWars-v${revision}` (versão resolvida via `help:evaluate`).

### Reestruturado
- `listener/GameListener`: a IA customizada do golem (`GolemAttackGoal`, classes internas `Goal`/`GoalKey`/`GoalType` do `com.destroystokyo.paper.entity.ai`) foi extraída para `compat/GolemCompat` + `GolemCompatImpl`, mantendo a mesma lógica de alvo, perseguição e cooldown. `GameListener` agora registra a goal via `CompatProvider.golem().registerAttackGoal(...)` com o resolver `findNearestEnemyForGoal`.
- `shop/ShopItem`: `PotionType.valueOf` → `CompatProvider.potion().applyPotionType(...)`; `Registry.ENCHANTMENT.get(...)` → `CompatProvider.registry().getEnchantment(...)`; `Bukkit.getUnsafe().modifyItemStack(...)` → `CompatProvider.nbt().modifyItemStack(...)`.
- `util/LocationUtil`: `player.teleportAsync(...)` → `CompatProvider.teleport().teleportAsync(...)`.
- Chat (envio de `Component`/títulos): ~40 arquivos migrados de `sendMessage(Component)`/`showTitle`/`clearTitle` diretos para `CompatProvider.chat()`, centralizando o caminho de envio no `ChatCompat` (assinatura `CommandSender` para cobrir comandos). Envios de `String` (`lang.raw`) foram mantidos, pois funcionam em qualquer plataforma.

## [0.0.1-206] - 2026-08-09

### Reestruturado
- **Projeto convertido para Maven multi-módulo** (base da migração multi-versão): o `pom.xml` raiz virou agregador (`<packaging>pom</packaging>`, `groupId`/`version` herdados) e todo o código foi movido para o módulo `core/` (`core/pom.xml` com `artifactId sBedWars-core`, mantendo o `name` `sBedWars` para preservar o nome do plugin). O `checkstyle.xml` permanece na raiz e é referenciado via `${maven.multiModuleProjectDirectory}`. Build e empacotamento continuam iguais (`mvn clean package` → JAR em `core/target/`), validado com `mvn -o clean compile -DskipTests`.

## [0.0.1-205] - 2026-08-09

### Corrigido
- **Golem aliado perseguia jogador do mesmo time após dano de fireball** (`listener/GameListener`): o `onGolemDamage` só reconhecia `IronGolem`/`Player` como agressor — projéteis como a fireball da loja passavam direto, o golem tomava dano do aliado e a IA vanilla (`HurtByTarget`) o fazia mirar e perseguir o atirador. O dano amigável agora é bloqueado também para projéteis (`attackerOf` resolve o atirador do projétil), e um novo handler `onGolemTarget` cancela qualquer tentativa da IA de definir um aliado como alvo. Extraído `isSameTeam` para comparar time entre golem e jogador.

## [0.0.1-204] - 2026-08-09

### Corrigido
- **Golems não atacavam os golems de outros times** (`listener/GameListener`): o alvo da `GolemAttackGoal` era tipado como `Player`, e o `findNearestEnemy` só iterava `game.getPlayers()` — golems adversários ficavam fora do alcance de alvo e os times se ignoravam no confronto de golems. O alvo agora é `LivingEntity` e o `findNearestEnemy` também considera golems registrados no `golemOwners` de times adversários (mesmo mundo e válidos). Além disso, o `onGolemDamage` passou a bloquear dano entre golems do **mesmo time** (antes só cobria golem↔jogador).

## [0.0.1-203] - 2026-08-09

### Corrigido
- **Golems de ferro não atacavam nem perseguiam o time adversário** (`listener/GameListener`): o golem era marcado como criado por jogador (`setPlayerCreated(true)`), e no vanilla 1.21 golems assim têm `canTarget(Player)` falso — nunca perseguem jogadores, e o `setTarget()` do `tickIronGolems` era silenciosamente rejeitado. A IA foi reescrita com uma goal customizada da **Mob Goal API** do Paper (`GolemAttackGoal`, prioridade 0, tipos `MOVE`/`LOOK`/`TARGET`): localiza o inimigo vivo mais próximo (mesmo filtro de `findNearestEnemy`), move o golem com `Pathfinder.moveTo` e aplica dano com `LivingEntity.attack` respeitando cooldown de 20 ticks — sem depender do alvo vanilla. O `tickIronGolems` agora só faz cleanup do mapa de donos.

## [0.0.1-202] - 2026-08-08

### Adicionado
- **Ranking de kills no fim da partida** (`game/Game.sendEndRanking`): ao encerrar a partida (`endGame`), o chat exibe o top 3 de jogadores por kills (posição, nome, time, kills e mortes), usando novas chaves `game.rank_*` em `lang/pt_BR.yml`.

## [0.0.1-201] - 2026-08-08

### Corrigido
- **Poções da loja continuavam virando água** (`shop/ShopItem.applyTag`): o `modifyItemStack` do Paper converte NBT antigo e não aplica o componente `potion_contents` do sistema de componentes (1.20.5+). Poções agora têm tratamento dedicado: a tag `{potion_contents:{potion:"minecraft:poison"}}` é interpretada e aplicada via `PotionMeta.setBasePotionType(PotionType)` — a API idiomática do Paper 1.21.4. Tags não-poção continuam via `modifyItemStack`.

## [0.0.1-200] - 2026-08-08

### Corrigido
- **Poções da loja viravam água** (`shop/ShopItem.createItemStack`): a tag SNBT nunca era aplicada ao ItemStack — o código tentava deserializar a tag como componente GSON (formato errado para SNBT) e, no `catch`, ainda resetava o stack com `ItemStack.of(...)`, perdendo também display-name/lore. Agora a tag é aplicada via `Bukkit.getUnsafe().modifyItemStack(stack, tag)` (Paper), e o `applyDisplayMeta` só roda quando há nome/lore/encantamentos, evitando sobrescrever a tag. O YAML `shop.yml` já usava o formato correto `{potion_contents:{potion:"minecraft:poison"}}`.

## [0.0.1-199] - 2026-08-08

### Corrigido
- **Arenas criadas por comando nasciam com valores zerados** (`manager/ArenaManager.create`): ao criar uma arena com `/bw admin create`, os campos `minPlayersPerTeam`, `minTeamsToStart`, `cycleDay`, `cycleWeather`, `spawnMobs` e `spawnAnimals` ficavam em 0/false e eram gravados assim no `arenas/<nome>.yml` — os defaults do loader (`getInt`/`getBoolean` com fallback) nunca eram aplicados porque as chaves já existiam no arquivo. Agora o `create()` aplica explicitamente os defaults (`1`, `2`, `true`, `true`, `true`, `true`, `enabled=false`, `shop=default`) antes dos defaults de forge/geradores.

## [0.0.1-195] - 2026-08-08

### Corrigido
- **Autocomplete de times nos comandos de arena** (`command/BWCommand`): os subcomandos `removeteam`, `setspawn` e `setbed` sugeriam uma lista fixa de cores (`azul`, `vermelho`, ...), mesmo que o time não existisse na arena. Agora mostram apenas os times cadastrados, como já fazia o `addgenerator forge`. `addteam` mantém a lista de cores por ser o comando que cria o time.

## [0.0.1-194] - 2026-08-08

### Corrigido
- **Sufocamento ao renascer com spawn ao lado de parede** (`util/LocationUtil.findSafeRespawn`): a busca só procurava para cima na mesma coluna e mantinha o `x/z` fracionário do spawn. Com o spawn perto de um bloco, o jogador renascia com o corpo atravessando a parede e tomava dano de sufocação. Agora a busca é em espiral (raio até 4 blocos), priorizando o ponto mais próximo, e o retorno é centralizado no bloco (`x+0.5`, `z+0.5`).

## [0.0.1-193] - 2026-08-08

### Corrigido
- **Itens perdidos ao arrastar na GUI da loja** (`shop/ShopListener`): a loja só tratava `InventoryClickEvent`; arrastar (drag) um item sobre os slots da loja movia o item para a GUI e ele se perdia (sumia ao reorganizar/renderizar). Adicionado `onInventoryDrag`: cancela qualquer drag que toque slots da loja (`< 54`), mantendo livre a organização de itens no inventário do jogador.

## [0.0.1-192] - 2026-08-08

### Corrigido
- **Inventário travado com a loja aberta** (`shop/ShopGui.handleClick`): o `setCancelled(true)` era aplicado antes de validar o slot, cancelando também os cliques no inventário do jogador (slots ≥ 54, ex.: ao organizar itens comprados). Agora o evento só é cancelado para os slots da GUI da loja (0-53); cliques no inventário do jogador são liberados. `ShopListener` identifica a loja pelo holder do inventário top da view.

## [0.0.1-191] - 2026-08-08

### Corrigido
- **Itens da loja na mão secundária (offhand) não funcionavam** (`listener/GameListener`): o ovo de ponte, a bola de fogo e o ovo do golem de ferro só eram detectados na mão principal — ao usar no slot do escudo, o item sumia/comportamento vanilla sem consumo. Adicionados os helpers `usedItem` (lê o slot conforme `event.getHand()`, incluindo `EquipmentSlot.OFF_HAND`) e `consumeUsedItem` (consome no slot correto). Os handlers `onFireballUse`, `onBridgeEggUse` e `onIronGolemUse` agora usam os helpers.

## [0.0.1-190] - 2026-08-08

### Adicionado
- **Golem de Ferro de defesa (estilo Hypixel)** (`listener/GameListener`, `shop.yml`):
  - Novo item `IRON_GOLEM_SPAWN_EGG` na loja (seção Utilidades, `5 iron`).
  - `onIronGolemUse`: ao usar o item em partida ativa (PLAYING), convoca um `IronGolem` na posição do jogador, com nome colorido do time (`game.iron_golem_name`), e registra o dono em `golemOwners` (Map `UUID -> ArenaTeam`). Consome o item; mensagem `game.iron_golem_spawned`.
  - `onGolemDeath`: remove o golem do registro e limpa os drops (não solta ferro/papoilas).
  - `onGolemDamage`: bloqueia dano amigável em ambos os sentidos — golem não fere o dono/aliados e aliados não ferem o golem.
  - `tickIronGolems` (agendada a cada 10 ticks no construtor do listener): IA de defesa — procura o inimigo vivo mais próximo dentro de `IRON_GOLEM_RANGE = 20` blocos e define `setTarget`; ignora aliados e espectadores (`game.isPlaying`); limpa entradas órfãs de partidas encerradas.
  - `findNearestEnemy`: seleciona o jogador vivo de time adversário mais próximo (por `distanceSquared`).

## [0.0.1-189] - 2026-08-08

### Alterado
- **Ovo de ponte com projétil real** (`listener/GameListener`): o `EGG` agora é lançado como projétil (`Egg`) na direção do olhar. Ao pousar (`onBridgeEggHit`), a ponte de lã é interpolada **do atirador até o ponto de impacto** (até 16 blocos), acompanhando a altura da trajetória — se você mira para cima, a ponte sobe até onde o ovo cai. Removidos `floorLevel`/`trackLevel` (ponte horizontal antiga). Import de `Egg` restaurado.

## [0.0.1-188] - 2026-08-08

### Corrigido
- **Ovo de ponte criava ponte desconectada no vazio** (`listener/GameListener`): a altura base usava `player.getLocation().getBlockY()` (bloco do pé, que flutua) e `groundLevel` procurava até 6 blocos acima/abaixo, jogando a ponte numa altura errada ao cruzar o vazio. Agora:
  - `floorLevel` acha a superfície do chão sob o jogador (descendo até 10 blocos) — a ponte sai da ilha, não do ar.
  - `trackLevel` acompanha o terreno suavemente (entre -2 e +3 blocos); sem chão (vazio), mantém o nível atual e cruza reto.
  - Renomeado `groundLevel` → `floorLevel`/`trackLevel`.

## [0.0.1-187] - 2026-08-08

### Alterado
- **Aviso do `time-limit` sem spam** (`game/Game.handleTimeLimit`): mensagem e som agora disparam apenas quando os segundos restantes são múltiplos de 5 (ex.: 60, 55, 50...) dentro dos últimos 20% do tempo, em vez de a cada segundo. Mantido o guard `timeLimitWarning` para evitar duplicatas.

## [0.0.1-186] - 2026-08-08

### Alterado
- **Ovo de Ponte reformulado (estilo Hypixel)** (`listener/GameListener`): trocado `onBridgeEggHit` (criava no impacto do projétil, ponte reta no mesmo Y) por `onBridgeEggUse` — ao usar o `EGG` em partida, o evento é cancelado e a ponte é criada **imediatamente na direção do olhar**, seguindo a altura do chão a cada bloco (`groundLevel` sobe/desce até 6 blocos para acompanhar o terreno). Comprimento aumentado para `BRIDGE_EGG_LENGTH = 16`. Consome o ovo na hora. Import de `Egg` removido.

## [0.0.1-185] - 2026-08-08

### Corrigido
- **Ovo de Ponte não criava a ponte** (`listener/GameListener.onBridgeEggHit`): no impacto do `Egg`, a velocidade horizontal costumava ser ~0 (o projétil cai em Y), fazendo o guard `dir.lengthSquared() < 0.0001` abortar a ponte. Agora usa a direção do atirador (`shooter.getLocation().getDirection()`) como fallback; bloco de impacto também cai para `egg.getLocation().getBlock()` quando `getHitBlock()` é null.
- **Impulso da fireball** (`GameListener`): reforçado estilo "vento"/Wind Charge do Hypixel:
  - `knockbackVictim` (acertou jogador): impulso horizontal maior (`2.2`) + vertical (`1.0`).
  - Novo `windBlast` (acertou bloco): empurra radialmente **todos** os jogadores da partida num raio de 5 blocos para longe do impacto (força decresce com a distância), com elevação; substitui o antigo `boostShooter` (super pulo só do atirador).

## [0.0.1-184] - 2026-08-08

### Adicionado
- **Ovo de Ponte** (`listener/GameListener.onBridgeEggHit`): novo item `EGG` na loja (seção de armas, `2 gold`). Ao ser lançado e atingir um bloco durante uma partida ativa, estende uma ponte horizontal de lã na direção do lançamento (comprimento fixo `BRIDGE_EGG_LENGTH = 8`), usando a cor do time do atirador (`getWoolColor`). Cada bloco é rastreado com `trackPlacedBlock` para ser limpo no reset da arena.

## [0.0.1-183] - 2026-08-08

### Adicionado
- **Super pulo da bola de fogo** (`listener/GameListener.boostShooter`): quando a `SmallFireball` acerta um bloco, o atirador é lançado para o alto (`1.4` no eixo Y), efeito "rocket jump" estilo Hypixel. Refatorado `onFireballHit` em dois casos: `knockbackVictim` (acertou jogador — impulso horizontal + vertical) e `boostShooter` (acertou bloco — pulo vertical). Ambos apenas em partida ativa (PLAYING).
- **Impulso da bola de fogo** (`listener/GameListener.knockbackVictim`): novo handler de `ProjectileHitEvent` — quando a `SmallFireball` (item `FIRE_CHARGE` da loja) acerta um jogador em partida ativa, a vítima é lançada horizontalmente na direção do projétil (`1.6`) e para o alto (`1.1`), efeito "quase voar" estilo Hypixel. Não afeta fora do estado PLAYING nem quando não há componente horizontal.

## [0.0.1-181] - 2026-08-08

### Corrigido
- **Exceção `World unloaded` em `handleForgeTicks`** (`game/Game.java`): quando o mundo da partida era descarregado (reset de arena) com a partida ainda ativa, `Location.getBlock()` lançava `IllegalArgumentException: World unloaded` a cada tick. Adicionado `Game.isMatchWorldLoaded()` (resolve o mundo por nome via `Bukkit.getWorld`) e guard no início de `gameTick`: se o mundo sumiu com a partida em STARTING/PLAYING, a partida é encerrada via `forceEnd()` antes de tocar qualquer `Location`.
- Avisos do VS Code/IDE (não quebravam build):
  - `manager/GameManager.removeFromPendingJoins`: `List::isEmpty` → lambda `queue -> queue.isEmpty()` (null type safety do `Predicate`).
  - `shop/ShopGui.protectionWeight`: `Registry.ENCHANTMENT.get(...)` (deprecated desde 1.21) → `Enchantment.PROTECTION`; import de `Registry` removido.


### Adicionado
- **`ChatManager`** (`game/ChatManager.java`): centraliza envio de mensagens, títulos e sons da partida com javadocs — `sendToPlayers`, `broadcast`, `showTitle`, `clearTitle`, `playSound`, `broadcastWithSound` e `getPresentPlayers`. Alcança apenas jogadores da partida (players + espectadores), nunca o servidor todo.
- `Game.getSpectatorPlayers()` e campo `chat` integrado.
- Sons em eventos: cama destruída (`ENTITY_WITHER_BREAK_BLOCK`), eliminação de time (`ENTITY_LIGHTNING_BOLT_THUNDER`) e vitória (`UI_TOAST_CHALLENGE_COMPLETE`).

### Alterado
- `breakBed`, `eliminateTeam`, `endGame`, `started`, `countdown_cancelled`, `join_broadcast`, `leave_broadcast` e avisos de `time-limit` agora usam `ChatManager` — mensagens vão **só para quem está na partida**, não para todo o servidor (removidos os `Bukkit.getOnlinePlayers()` de chat; restaram apenas os de visibilidade entre partidas).
- Countdown de início e `time-limit` usam `chat.playSound` com o mesmo pitch crescente (extraído para `Game.countdownPitch`).

## [0.0.1-180] - 2026-08-08

### Adicionado
- Som de countdown nos últimos **20% do tempo** (countdown de início e `time-limit`): `Game.playCountdownSound(int, int)` toca `BLOCK_NOTE_BLOCK_PLING` com pitch crescente conforme o tempo acaba.
- Mensagens de aviso do `time-limit` agora vão **apenas para os jogadores em partida** (novo `Game.sendToPlayers(Component)`), sem espectadores.

## [0.0.1-179] - 2026-08-07

### Adicionado
- **Início de partida por times ativos** (substitui o mínimo global de jogadores):
  - Novas chaves no YAML da arena: `teams.min-players` (mínimo p/ time ser ativo, padrão `1`), `teams.max-players` (teto por time, `0` = derivar do modo, padrão `0`) e `teams.min-teams` (nº de times ativos p/ iniciar o countdown, padrão `2`).
  - Campos `minPlayersPerTeam`, `maxPlayersPerTeam`, `minTeamsToStart` + getters/setters em `model/Arena` e `api/model/Arena`, incluído no `copy()`.
  - Persistência em `manager/ArenaManager` e `arena/ArenaManager` (sistema Slime): save/load das 3 chaves; load de times ignora `min-players`/`max-players`/`min-teams` (não viram times).
  - `Game.updateCountdownState` agora usa `countActiveTeams() >= getMinTeamsToStart()` (time ativo = `size() >= minPlayersPerTeam`); `Game.start()` exige `hasEnoughActiveTeams()`; `forceStart()` continua ignorando.
  - `Game.maxTeamSlots()` respeita `max-players` da arena; partida livre sem teto deriva do **maior modo válido** do mapa (`largestValidMode`).
  - `GameManager.validateArena` valida `min-teams <= nº de times` (`game.validate_min_teams`); join rejeita modo com `teamSize > max-players` (`game.mode_exceeds_team_limit`).
  - `StatusCommand` exibe min/max por time e min-teams (`admin.arena.status_team_limits`).
- **Progressão de níveis dos geradores base por tempo de partida:**
  - `GeneratorConfig` virou `record GeneratorConfig(Material material, Map<Integer, Long> levels)` com `intervalForLevel(int)` (resolução: nível exato → maior nível ≤ pedido → menor nível configurado). O campo `interval` fixo foi removido (formato quebrado).
  - Nova chave `generator_config.<tipo>.levels.<nivel>` (intervalo em ticks por nível), substituindo o antigo `generator_config.<tipo>.interval`.
  - `Game.currentGeneratorLevel()` resolve o nível pelo tempo decorrido (minutos); `handleGeneratorTicks`/`initGeneratorTicks` usam o intervalo do nível atual dinamicamente.
  - Defaults em `manager/ArenaManager.create()`: 5 níveis por tipo (iron 40→20, gold 120→40, diamond 600→200, emerald 1200→400).
  - Persistência em `manager/ArenaManager` (save/load) de `generator_config.*.levels.*`.

### Removido
- Campo `minPlayers` removido por completo: `model/Arena`, `api/model/Arena`, persistência (save/load), `CreateCommand`, `create()` dos dois `ArenaManager`, comando `/bw admin arena setminplayers` (`SetMinPlayersCommand` deletado, registro em `ArenaRouter` e tab-complete em `BWCommand`), chaves de lang `setminplayers_*`/`status_minplayers` e documentação (`example.yml`, `README.md`).
- Seção `level-times` do `example.yml` (sem ela os geradores base permanecem no nível 1).

### Alterado
- `min-teams` movido de `game.min-teams` para `teams.min-teams` no YAML da arena (save/load nos dois `ArenaManager`; load de times ignora a chave; lang e docs atualizados).

### Documentação
- `example.yml`: bloco "INÍCIO DA PARTIDA (por time)" com `teams.min-teams` e `teams.min-players`/`max-players` dentro da seção `teams`; `generator_config` reescrita com `levels` por tipo; removido `min_players` e `level-times`.
- `README.md`: seções 13.2 e 14.2 atualizadas com a regra de times ativos e teto por time; descrição de `generator_config` e `teams.min-teams`; removido `setminplayers` do tutorial, da tabela de comandos e da referência do YAML.

## [0.0.1-178] - 2026-08-07

### Documentação
- `ARCHITECTURE.md`: diagramas Mermaid atualizados do sistema **Slime/ASP (não ativo)** para o sistema **Schematic (ativo)** — criação/save via `Schematic.save` para `maps/`, partidas via `WorldCreator` + `VoidGenerator` + `Schematic.paste`, reset via `ArenaManager.resetArenaMap` (unload/delete verificados → mundo void → repaste → flush), runtime com "Tempo Limite" e loja via `NpcHook` (FancyNpcs/Citizens).
- `ARCHITECTURE.md`: "Pilares Tecnológicos" atualizados (schematic FAWE como núcleo de persistência, YAML + schematic, sem templates Slime em produção); seção 3 reescrita com o fluxo de reset ativo.

## [0.0.1-175] - 2026-08-07

### Documentação
- `CHANGELOG.md` criado com todo o histórico do plugin, detalhando as mudanças de cada release.

## [0.0.1-173] - 2026-08-07

### Refatoração
- Aplicada a regra de estilo de imports em todo o código: **imports no topo do arquivo + nome curto no corpo**, eliminando fully-qualified names inline (`new dev.sebastianjnuwu.bedwars.model.Arena(...)` → import único + `new Arena(...)`).
- Arquivos afetados: `BedWarsPlugin` (imports de `NpcListener`/`CitizensNpcListener`), `ReloadCommand`, `CreateCommand`, `EditCommand`, `LoadCommand`, `SaveCommand` (imports de `JavaPlugin`/`BedWarsPlugin`/`VoidGenerator`), `TeamAddCommand` (`model.ArenaTeam`), `GeneratorAddCommand` (`model.ArenaGenerator`), `Game` (import de `org.bukkit.entity.Item`), `ShopGui`, `arena/ArenaManager` e `manager/ArenaManager`.
- FQN inline mantido **apenas** nos casos de conflito real de nome entre a interface `api.model.*` e o concreto `model.*` (ex.: `Arena`, `ArenaTeam`, `ArenaGenerator`, `GamePlayer`).
- Comentários explicativos dos conflitos removidos a pedido do usuário (decisão de manter o código limpo).

### Documentação
- `AGENTS.md`: adicionada a regra de imports (import explícito no topo, proibido FQN inline, exceções para conflito de nome com comentário curto e reflection/string via `Class.forName`; proibido wildcard import; `import static` apenas idiomático).
- `ARCHITECTURE.md`: pacote `hook/` adicionado ao mapa de pacotes; novas seções "4. NPCs da Loja" (`NpcHook`, `FancyNpcsHook`, `CitizensHook`, `ShopNpcManager.resolveHook`, listeners por hook, `SkinTrait`) e "5. Tempo limite de partida" (`Game.handleTimeLimit`, critérios do `forceTimeLimitEnd`); princípio "Imports no topo" adicionado às regras de arquitetura.
- `config.yml`: corrigido typo `forçsa` → `força` em comentário da opção `npc-backend`.

## [0.0.1-171] - 2026-08-07

### Adicionado
- **Tempo limite de partida por arena**:
  - Nova chave `time-limit` no YAML da arena, em segundos (`0` = sem limite, padrão).
  - Campo `timeLimit` + getters/setters em `model/Arena` e na interface `api/model/Arena`; incluído no `copy()`.
  - Persistência em `manager/ArenaManager`: `config.set("time-limit", ...)` no save e `getInt("time-limit", 0)` no load.
  - `Game.handleTimeLimit()` executado no tick de `PLAYING`: avisos na barra de ação com `game.time_limit_warning` em `60`, `30`, `10`..`1` segundos restantes.
  - `Game.forceTimeLimitEnd()` encerra a partida e decide o vencedor por **4 critérios em ordem**: mais jogadores vivos → cama intacta → mais abates (soma de `GamePlayer.getKills()` por time) → empate (`game.time_limit_tie`).
  - Novas chaves de lang: `game.time_limit_warning`, `game.time_limit_winner`, `game.time_limit_tie`.
  - Helper `Game.broadcastMessage(Component)` para mensagens unificadas de partida.
  - `example.yml` documenta a opção: `time-limit: 1200`.

### Documentação
- `README.md`: regras do tempo limite explicadas no tutorial e seção de referência do YAML; fireball (bola de fogo como projétil) adicionado à lista de recursos.

## [0.0.1-169] - 2026-08-07

### Adicionado
- **Hook de NPCs da loja para Citizens** como alternativa ao FancyNpcs:
  - Interface `hook/NpcHook` (contrato): spawn, skin, displayName, nome do backend e `isManagedEntity(Entity)` (default `false`) para detecção de entidades gerenciadas.
  - `hook/FancyNpcsHook` e `hook/CitizensHook` implementadas **via reflexão** (Citizens não vira dependência de compilação; permanece `optional` no `plugin.yml`).
  - `CitizensNpcListener`: `PlayerInteractAtEntityEvent` em `MONITOR` abre a loja ao clicar no NPC.
  - Marker persistente `bw-shop-marker` (`data().setPersistent`) em NPCs do Citizens para reconhecimento dos próprios NPCs; skin via `SkinTrait.setSkinName()`; displayName via `npc.setName()` com conversão MiniMessage→legacy; `findMethod` percorre interfaces em cada nível da hierarquia.
  - Detecção de entidade gerenciada via `registry.getNPC(entity)` + marker.

### Alterado
- `ShopNpcManager`: backend resolvido por `config.yml` `npc-backend` (`auto`/`fancynpcs`/`citizens`; `auto` tenta FancyNpcs primeiro e faz fallback); `getBackendId()` delega ao hook; `isManagedEntity` unificada com fallback ao hook.
- `BedWarsPlugin`: registra o listener do hook ativo no startup (`startup.npcs_unavailable` quando nenhum backend disponível).
- `config.yml`: opção `npc-backend` documentada em comentário (linha sem valor ativo; default `auto` via `ConfigManager.getNpcBackend()`).
- `lang/pt_BR.yml`: chave `startup.npcs_no_backend` renomeada para `startup.npcs_unavailable`; mensagens sem a palavra "backend".

### Documentação
- `README.md`: Citizens (`v2.0+`) adicionado aos requisitos e ao passo de instalação; seção "NPC da loja (com FancyNPCs ou Citizens)" explicando o `npc-backend`.

## [0.0.1-167] - 2026-08-07

### Corrigido
- Joins por código (`--code`) agora validam o **modo** e o **jogador** antes de entrar na sala (evita entrar em partida de modo incompatível).
- Filas órfãs (sem sala/partida correspondente) descartadas no join.
- Espectadores unificados entre salas da mesma arena (sem duplicidade de espectadores).

## [0.0.1-165] - 2026-08-07

### Corrigido
- Fila de entradas pendentes chaveada por `(arena, modo)` para não misturar modos na construção da partida (join em fila de solo não contaminava fila de quarteto).

## [0.0.1-163] - 2026-08-07

### Corrigido
- `/bw join <arena>` sem `--mode` encontra qualquer **lobby aberto** da arena em vez de criar um lobby paralelo duplicado.

## [0.0.1-161] - 2026-08-07

### Corrigido
- `/bw start` com modo (ex.: quarteto) não encontrava a partida existente e criava **instância fantasma** (join agora busca a partida correta pelo modo).

## [0.0.1-159] - 2026-08-07

### Corrigido
- Respawn com prioridade de evento `MONITOR` para sobrepor outros plugins de respawn.
- Reafirmação do estado de espectador no tick seguinte para garantir que plugins concorrentes não sobrescrevam.

## [0.0.1-157] - 2026-08-07

### Corrigido
- Jogador eliminado **sem cama** vira espectador na própria partida (fica assistindo) em vez de voltar ao lobby.

## [0.0.1-155] - 2026-08-07

### Corrigido
- Nomes de arena/mapa aceitos em qualquer caixa (`case-insensitive`) em comandos e seleção.

## [0.0.1-154] - 2026-08-07

### Alterado
- Log de inventário (debug) exibe nome e UUID do jogador.

## [0.0.1-153] - 2026-08-07

### Corrigido
- Ender chest não vaza itens entre partidas: snapshot do conteúdo no início e `clear` no início de cada partida.

## [0.0.1-152] - 2026-08-07

### Corrigido
- Respawn com bloco no spawn do time não sufoca (checagem de bloco sólido antes de teleportar).
- Último respawn preservado quando a cama quebra durante o cooldown de respawn.

## [0.0.1-151] - 2026-08-07

### Adicionado
- **Bola de fogo dispara projétil**: `FIRE_CHARGE` clicado dispara um projétil real (em vez de agir como isqueiro/atalho); consome 1 item por disparo.

## [0.0.1-150] - 2026-08-07

### Corrigido
- Jogador eliminado não fica preso caindo no void: respawn para o lobby.

## [0.0.1-149] - 2026-08-07

### Corrigido
- Armadura de time travada em 100%: bloqueio de movimento da armadura em qualquer inventário e bloqueio de **arraste** para o slot de armadura.

## [0.0.1-148] - 2026-08-07

### Alterado
- Botão "voltar" das categorias da loja usa flecha (→) em vez de barreira (material `ARROW`).

## [0.0.1-147] - 2026-08-07

### Adicionado
- Conjuntos de armadura do `shop.yml` padrão com encantamento de proteção I/II/III (progressão por nível).

## [0.0.1-146] - 2026-08-07

### Removido
- Upgrade `dragon_buff` não implementado removido do `shop.yml` padrão.

## [0.0.1-145] - 2026-08-07

### Corrigido
- Bloqueio de recompra de armadura agora considera o **encantamento de proteção** na comparação de progressão (não comparava apenas o material).

## [0.0.1-144] - 2026-08-07

### Corrigido
- Reafirma o respawn no spawn do time 1 tick depois para evitar sobrescrita por outros plugins de respawn.

## [0.0.1-143] - 2026-08-07

### Corrigido
- Reload apagava `spawn_item` do YAML: flush dos dados do cache **antes** do load para não sobrescrever.

## [0.0.1-142] - 2026-08-07

### Documentação
- `README.md` e `ARCHITECTURE.md` atualizados: armaduras de time, loja posicionável, `spawn_item` e sistema de mundos ativo (Schematic).

## [0.0.1-141] - 2026-08-07

### Adicionado
- `spawn_item` por arena (item de saída customizado).
- Bloqueio de recompra de armadura (não deixa comprar conjunto que já está equipado).
- Javadoc e correções apontadas pelo IDE (warnings).

## [0.0.1-140] - 2026-08-07

### Adicionado
- Posicionamento da loja com `type: row/column` e centralização por linha (config de layout da loja por arena).

## [0.0.1-139] - 2026-08-07

### Adicionado
- Armadura da loja vira **couro tingido na cor do time**, unbreakable e travada no slot de armadura.

## [0.0.1-138] - 2026-08-07

### Corrigido
- TNT destrói apenas blocos colocados por jogadores; mapa original protegido (anti-grief).

## [0.0.1-137] - 2026-08-07

### Alterado
- `shop.yml` adaptado; DIAMOND volta a ser item (não conjunto de armadura).

## [0.0.1-136] - 2026-08-07

### Adicionado
- Kits recursivos com itens dentro de itens na loja (sub-itens aninhados).

## [0.0.1-135] - 2026-08-07

### Adicionado
- Loja vende conjuntos de armadura completos (CHAINMAIL/IRON/DIAMOND).

## [0.0.1-134] - 2026-08-07

### Corrigido
- Restauração de inventário e estados do jogador centralizada e localizada (função única `restoreInventory`).

## [0.0.1-133] - 2026-08-07

### Alterado
- `version_check` loga quando a versão local é mais nova que a publicada (não tratava como erro).

## [0.0.1-132] - 2026-08-07

### Documentação
- `example.yml` totalmente comentado (referência de todas as opções) + seção no README.

## [0.0.1-131] - 2026-08-07

### Corrigido
- `enable-cmd` aceita valor scalar no YAML (não só lista).
- `version_check` roda assíncrono (não trava a main thread).

### Refatoração
- `version_check` lê a versão remota direto do `pom.xml` (removido `.github/version.json`).

## [0.0.1-130] - 2026-08-07

### Adicionado
- Config `enable-cmd` no YAML da arena libera comandos específicos durante a partida.

## [0.0.1-129] - 2026-08-07

### Corrigido
- Lore do item de fornalha na loja não atualizava após upgrade: `purchaseItem` agora re-renderiza a loja.

## [0.0.1-128] - 2026-08-07

### Corrigido
- `/bw start` não iniciava partida com jogadores em um único time: `forceStart` ignora a exigência de 2 times; countdown automático mantém a regra.

## [0.0.1-127] - 2026-08-07

### Adicionado
- **Código de partida de 6 caracteres** (ABC123) gerado por sala.
- `/bw join <arena> --code <codigo>` entra na sala específica.

## [0.0.1-125] - 2026-08-07

### Corrigido
- `/bw start` acusava "configuração faltando" com YAML completo: `validateArena` rodava contra o cache sem world; agora usa `ensureWorldLoaded` antes de validar.

## [0.0.1-124] - 2026-08-07

### Corrigido
- Save sobrescrevia spawn/cama do disco com `null` quando o cache tinha locations não resolvidas (mundo carregado).

## [0.0.1-123] - 2026-08-07

### Adicionado
- Breadcrumb no título da loja (Loja > Compra > Categoria).

## [0.0.1-122] - 2026-08-07

### Adicionado
- Preço de upgrade da fornalha por nível: `level-default` + `upgrade.price/material` no YAML, com cobrança e exibição dinâmica na loja.

## [0.0.1-121] - 2026-08-07

### Corrigido
- Contagem regressiva não parava e partida iniciava com todos no mesmo time: `updateCountdownState` valida 2+ times.

## [0.0.1-120] - 2026-08-07

### Corrigido
- Tab-complete do `/bw` explodindo com `zip file closed` após reload do PlugMan: try/catch defensivo no `onTabComplete`.

## [0.0.1-119] - 2026-08-07

### Corrigido
- `lang/pt_BR.yml` com YAML inválido (indentação mista na seção `log`) fazia todas as mensagens virarem `[missing]`.

## [0.0.1-118] - 2026-08-07

### Corrigido
- `plugin.yml` com `authors` inválido (YAML quebrado gerado a partir de `project.developers`) causava erro no PlugMan.

## [0.0.1-117] - 2026-08-07

### Corrigido
- Jogadores perdiam o inventário ao sair da partida: snapshot único de inventário; leave em `ENDING` restaura; respawn sem cama via `leaveGame`.

## [0.0.1-116] - 2026-08-07

### Corrigido
- `ClipboardHolder` do FAWE não fechado (resource leak no paste de schematic).

## [0.0.1-115] - 2026-08-07

### Adicionado
- Permissão `bw.player` e mensagem customizada nos comandos `bw` e `spawn`.

## [0.0.1-114] - 2026-08-07

### Interno
- Limpeza de warnings do compilador (campos/variáveis mortos, NPE no schematic).

## [0.0.1-113] - 2026-08-07

### Adicionado
- Permissão `bw.admin` exigida no `/bw admin`.

## [0.0.1-112] - 2026-08-07

### Adicionado
- Tab do `/bw join` completo por flag (`--mode`, `--team`, `--code`); crafting bloqueado em partida.

## [0.0.1-111] - 2026-08-07

### Corrigido
- Lã comprada na loja usa a cor do time.

## [0.0.1-110] - 2026-08-07

### Corrigido
- Reload com partida em `ENDING` perdia inventário; `joinAsSpectator` inoperante.

## [0.0.1-109] - 2026-08-07

### Adicionado
- `/bw join` aceita flags opcionais `--mode` e `--team`.

## [0.0.1-108] - 2026-08-07

### Corrigido
- Autocomplete do `/bw join` sugere modos válidos da arena.

## [0.0.1-107] - 2026-08-07

### Corrigido
- `restoreInventory` não limpa inventário em chamada dupla (idempotente).

## [0.0.1-106] - 2026-08-07

### Removido
- Item Dragon Buff da loja (upgrade não implementado).

## [0.0.1-105] - 2026-08-07

### Corrigido
- Jogadores vivos podem dropar itens; respawn com prioridade alta.

## [0.0.1-104] - 2026-08-07

### Corrigido
- Só permite quebrar blocos colocados por jogadores ou camas.

## [0.0.1-103] - 2026-08-07

### Corrigido
- Reload atualiza itens da loja (invalida cache do `ShopManager`).

## [0.0.1-102] - 2026-08-07

### Corrigido
- Upgrade da fornalha não entregava item físico.

## [0.0.1-101] - 2026-08-07

### Corrigido
- Não abre menu de seleção de time durante a partida.

## [0.0.1-100] - 2026-08-07

### Corrigido
- Sair no lobby não declara vitória antes da partida começar.

## [0.0.1-099] - 2026-08-07

### Corrigido
- `isAvailable` captura `Error` do ASP para funcionar sem o plugin instalado.

## [0.0.1-098] - 2026-08-07

### Interno
- Artifact da action usa glob automático `sBedWars-*.jar`.

## [0.0.1-097] - 2026-08-07

### Alterado
- Persistência `Saveable`; log de idioma em `lang`; versão `0.0.1-097`.

## [0.0.1-096] - 2026-08-07

### Corrigido
- Falha no spawn de NPCs com skins inválidas (fallback).

### Alterado
- Persistência centralizada; logs migradas; remoção de `StatsManager`.
- Nome do JAR `sBedWars-v0.0.1-096.jar`; versão no `pom.xml`.

## [0.0.1-095] - 2026-08-07

### Corrigido
- Roupas dos times, seletor wool, bug de countdown e log de NPC.

## [0.0.1-094] - 2026-08-07

### Corrigido
- Jogadores do mesmo jogo ficavam ocultos no join em fila (visibilidade entre players).

## [0.0.1-093] - 2026-08-07

### Adicionado
- **Pré-build assíncrono da instância no join** (paste do schematic fora da main thread para não travar o servidor).

## [0.0.1-092] - 2026-08-07

### Corrigido
- Instância de partida montada do disco evita reset após unload/load do mundo.

## [0.0.1-091] - 2026-08-07

### Adicionado
- Título de morte centralizado com contagem de respawn.

## [0.0.1-090] - 2026-08-07

### Adicionado
- Comando `discard` para sair do modo edição sem salvar.

## [0.0.1-089] - 2026-08-07

### Corrigido
- Compatibilidade de NPCs da loja com FancyNPCs.

## [0.0.1-088] - 2026-08-07

### Documentação
- README com modos e times (balanceamento por time).

## [0.0.1-087] - 2026-08-07

### Adicionado
- **Modo de partida por sala**: solo/dupla/trio/quarteto (lobbies separados por modo).

## [0.0.1-086] - 2026-08-07

### Adicionado
- **Instâncias de partida por arena**: partidas simultâneas do mesmo mapa (mundo `bw_<nome>` copiado por instância).

## [0.0.1-085] - 2026-08-07

### Interno
- Workflow usa JDK 25 para ler FancyNpcs 2.11 (class version 69).

## [0.0.1-084] - 2026-08-07

### Adicionado
- Workflow GitHub Actions com lint, build e publicação do JAR em release.

## [0.0.1-083] - 2026-08-07

### Interno
- `minimizeJar` ativado no shade para reduzir o tamanho do JAR.

## [0.0.1-082] - 2026-08-07

### Corrigido
- Reutiliza mundo existente em vez de recriar na `arena/reset` (falha no `createWorld` sem `paper-world-defaults.yml`).

## [0.0.1-081] - 2026-08-07

### Corrigido
- Criação de mundo não capturada derrubava o comando `join` (`paper-world-defaults.yml` ausente) — agora com tratamento de falha.

## [0.0.1-080] - 2026-08-07

### Corrigido
- Arena perdia configuração após unload/load do plugin (refs de mundo desatualizadas); javadoc das mudanças da loja.

## [0.0.1-079] - 2026-08-07

### Adicionado
- Loja oculta a fileira de categorias ao entrar em uma categoria; itens padrão de BedWars.

## [0.0.1-078] - 2026-08-07

### Adicionado
- Tab-completion das ações do comando `shop-npc`.

## [0.0.1-077] - 2026-08-07

### Corrigido
- Troca de categoria pela linha superior não atualizava os itens da loja.

## [0.0.1-076] - 2026-08-07

### Adicionado
- Título da GUI da loja usa o `displayName` configurado.

## [0.0.1-075] - 2026-08-07

### Corrigido
- Loja não abria ao clicar no NPC do FancyNPCs (`NpcInteractEvent`).

## [0.0.1-074] - 2026-08-07

### Adicionado
- Skin e displayName por NPC da loja (FancyNPCs).

## [0.0.1-073] - 2026-08-07

### Corrigido
- Compatibilidade de NPCs da loja com FancyNPCs e Citizens.

## [0.0.1-072] - 2026-08-07

### Corrigido
- Compatibilidade de NPCs da loja com FancyNPCs e Citizens.

## [0.0.1-071] - 2026-08-07

### Corrigido
- Compatibilidade de NPCs da loja com FancyNPCs e Citizens.

## [0.0.1-070] - 2026-08-07

### Corrigido
- Erros de acentuação e gramática no `pt_BR.yml`.

## [0.0.1-068] - 2026-08-07

### Adicionado
- Save detecta automaticamente a área construída (sem precisar de seleção FAWE).

## [0.0.1-067] - 2026-08-07

### Corrigido
- Reset não apagava o mundo real; camas fora do schematic não eram restauradas.

## [0.0.1-066] - 2026-08-07

### Corrigido
- Mundo de partida reutilizado sujo (minérios/camas de partidas anteriores persistiam) — agora sempre recriado limpo.

## [0.0.1-065] - 2026-08-07

### Documentação
- `AGENTS.md` e `ARCHITECTURE.md` conforme análise do Codacy (headings, limites, exemplos).

## [0.0.1-064] - 2026-08-07

### Interno
- `.gitignore` atualizado pela extensão do Codacy (`.codacy`, `.github/instructions`).

## [0.0.1-063] - 2026-08-07

### Documentação
- `AGENTS.md` detalhado (estrutura, arquitetura e regras).

## [0.0.1-062] - 2026-08-07

### Documentação
- Regras do projeto e Codacy movidas para `AGENTS.md`.

## [0.0.1-061] - 2026-08-07

### Documentação
- Badge Codacy no README.

## [0.0.1-060] - 2026-08-07

### Documentação
- Instruções do Codacy para IA.

## [0.0.1-059] - 2026-08-07

### Corrigido
- Reset de arena não limpa o mundo: unload/delete verificados antes de recriar.

## [0.0.1-058] - 2026-08-07

### Corrigido
- Prefixo duplicado nos logs de debug do `GameManager`.

## [0.0.1-057] - 2026-08-07

### Corrigido
- NPCs da loja duplicados: leftover do `add` não era removido no reset.

## [0.0.1-056] - 2026-08-07

### Documentação
- Banner bStats no README.

## [0.0.1-055] - 2026-08-07

### Corrigido
- Preserva localizações de arenas quando o mundo não está carregado.

## [0.0.1-054] - 2026-08-07

### Alterado
- Debug logs via lang `pt_BR` (chaves `debug:*`).

## [0.0.1-053] - 2026-08-07

### Corrigido
- `ConfigManager` não adiciona forge/generators defaults ao `config.yml`.

## [0.0.1-052] - 2026-08-07

### Corrigido
- `spigot.respawn()` + spectator mode no respawn.

## [0.0.1-051] - 2026-08-07

### Adicionado
- Morte sem death screen: spectator direto + respawn automático.

## [0.0.1-050] - 2026-08-07

### Alterado
- Remove debug logs de forge; respawn title com countdown.

## [0.0.1-049] - 2026-08-07

### Adicionado
- Spectator vai ao lobby sempre; `respawn-delay` (3s) por arena; action bar com contagem.

## [0.0.1-048] - 2026-08-07

### Adicionado
- `spectator.teleport-to-lobby` (config) teleporta automaticamente ao lobby.

## [0.0.1-047] - 2026-08-07

### Corrigido
- `StartCommand` auto-join do executor; novo `/spawn` (lobby).

## [0.0.1-046] - 2026-08-07

### Corrigido
- `start()` sem `startGameTick()` fazia fornalhas nunca rodarem quando usava `/bw start`.

## [0.0.1-045] - 2026-08-07

### Alterado
- Logs de debug em `initForgeTicks`/`putForgeTicks` para diagnosticar fornalhas.

## [0.0.1-044] - 2026-08-07

### Adicionado
- Armadura de couro colorida do time no lobby (imóvel).

## [0.0.1-043] - 2026-08-07

### Corrigido
- Morte no lobby teleporta de volta ao spawn em vez de matar o player.

## [0.0.1-042] - 2026-08-07

### Corrigido
- Gerador sobrescreve em vez de erro de duplicata; restaura bloco original no marcador.

## [0.0.1-041] - 2026-08-07

### Corrigido
- README com comando correto `shop-npc`; cleanup de NPCs obsoletos no startup.

## [0.0.1-040] - 2026-08-07

### Corrigido
- Partida não encerra quando todos os players saem.

## [0.0.1-039] - 2026-08-07

### Adicionado
- **Checkstyle adicionado ao build**; 708 violações de estilo corrigidas (build falha em qualquer violação).

## [0.0.1-038] - 2026-08-07

### Corrigido
- UUID em geradores, `removegenerator`, exemplo de arena, revisão save/load, warnings de deprecação.

## [0.0.1-037] - 2026-08-07

### Refatoração
- `Game.java` com `gameTick` centralizado substitui N tasks por uma única.

## [0.0.1-036] - 2026-08-07

### Corrigido
- `getPlugin(BedWars)` → `JavaPlugin.getPlugin(BedWarsPlugin.class)` em 5 arquivos.

## [0.0.1-035] - 2026-08-07

### Corrigido
- Placeholder `{2}` → `{0}` e prefixo `v` nas versões do version-check.

## [0.0.1-034] - 2026-08-07

### Corrigido
- Parser do `version.json` ignora espaços após os dois pontos.

## [0.0.1-033] - 2026-08-07

### Refatoração
- `version.json` movido para `.github/version.json`.

## [0.0.1-032] - 2026-08-07

### Refatoração
- Version-check lê de `version.json` via `raw.githubusercontent`.

## [0.0.1-031] - 2026-08-07

### Adicionado
- `VersionChecker` consulta GitHub por atualizações.

## [0.0.1-030] - 2026-08-07

### Adicionado
- Todas as mensagens hardcoded movidas para `lang/pt_BR.yml` e traduzidas para português (MiniMessage).

## [0.0.1-029] - 2026-08-07

### Adicionado
- Seleção de times reformulada; `/bw join` entra automaticamente; correções de NPEs; lang convertida para MiniMessage.

## [0.0.1-028] - 2026-08-07

### Corrigido
- Tags `<red>` substituídas por `&c` (cores legadas) nas mensagens.

## [0.0.1-026] - 2026-08-07

### Corrigido
- `TeamSelectionGui` slot 49 fora do limite (45 slots); lang separada para item de porta de saída.

## [0.0.1-025] - 2026-08-07

### Corrigido
- `forceEnd` não agenda tasks; evita crash ao dar unload no plugin.

## [0.0.1-024] - 2026-08-07

### Corrigido
- GUI de confirmação de saída com 2 botões (Sim/Não); warning removido.

## [0.0.1-023] - 2026-08-07

### Corrigido
- `onDisable` restaura inventário e encerra partidas; auto-save removido; NPCs limpos ao sair.

## [0.0.1-022] - 2026-08-07

### Adicionado
- Comando `/bw start`; limpeza de NPCs; remoção do auto-save.

## [0.0.1-021] até [0.0.1-001] - 2026

### Adicionado
- Fundação do plugin: comandos base, arenas, partidas, spawns de recursos, loja, editor, espectadores, respawn, persistência, version-check e integrações (FAWE/ASP/FancyNpcs/bStats).

### Alterado
- Lançamentos iniciais `0.0.1-002` a `0.0.1-020` sem mensagens descritivas; incluem merge da `main`, `renovate.json` e commits iniciais de scaffolding.
