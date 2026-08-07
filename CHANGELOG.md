# Changelog

Todas as mudanças notáveis do plugin **BedWars** (Paper 1.21.4) são documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Cada versão deriva dos commits em `main`; versões pares de "bump" (atualização do número no `pom.xml`) são omitidas.

## [0.0.1-174] - 2026-08-07

### Interno
- `pom.xml` atualizado para a versão `0.0.1-173` (bump de controle de versão).

## [0.0.1-173] - 2026-08-07

### Refatoração
- Imports movidos para o topo dos arquivos e fully-qualified names inline substituídos pelo nome curto com `import` único (regra de estilo de `AGENTS.md`).
- FQN inline mantido apenas nos casos de conflito real de nome entre a interface `api.model.*` e o concreto `model.*` (ex.: `Arena`, `ArenaTeam`, `ArenaGenerator`, `GamePlayer`).
- Comentários explicativos dos conflitos removidos a pedido do usuário.

### Documentação
- `AGENTS.md`: adicionada a regra de imports (import no topo, sem FQN inline, exceções para conflito de nome e reflection).
- `ARCHITECTURE.md`: pacote `hook/` adicionado ao mapa de pacotes; novas seções "NPCs da Loja" (`NpcHook`, `FancyNpcsHook`, `CitizensHook`, `ShopNpcManager.resolveHook`) e "Tempo limite de partida"; princípio "Imports no topo" adicionado às regras de arquitetura.
- `config.yml`: corrigido typo `forçsa` → `força` em comentário da opção `npc-backend`.

## [0.0.1-171] - 2026-08-07

### Adicionado
- Tempo limite de partida por arena: nova chave `time-limit` no YAML da arena (em segundos; `0` = sem limite, padrão).
- Avisos de tempo restante na barra de ação em `60`, `30`, `10`..`1` segundos (chave `game.time_limit_warning`).
- Encerramento automático com determinação de vencedor por critérios em ordem: mais jogadores vivos → cama intacta → mais abates → empate (chaves `game.time_limit_winner` e `game.time_limit_tie`).
- Novos campos `timeLimit`/`getTimeLimit()`/`setTimeLimit()` em `model.Arena` e na interface `api.model.Arena` (incluído na cópia).

### Alterado
- `manager/ArenaManager`: salva e carrega `time-limit` das arenas.
- `Game`: novo `handleTimeLimit()` no tick de `PLAYING`, `forceTimeLimitEnd()` e helper `broadcastMessage(Component)`.
- `lang/pt_BR.yml`: novas chaves `game.time_limit_warning`, `game.time_limit_winner`, `game.time_limit_tie`.
- `example.yml`: opção `time-limit: 1200` documentada.

### Documentação
- `README.md`: regras do tempo limite no tutorial, secção de referência do YAML e referência ao fireball (bola de fogo como projétil).

## [0.0.1-169] - 2026-08-07

### Adicionado
- Hook de NPCs da loja para **Citizens** como alternativa ao FancyNpcs (detecção automática via `npc-backend` no `config.yml`; `auto` tenta FancyNpcs primeiro).
- `hook/NpcHook` (interface): contrato comum de spawn, skin, displayName e detecção de entidades gerenciadas (`isManagedEntity` com default `false`).
- `hook/FancyNpcsHook` e `hook/CitizensHook` (via reflexão, sem Citizens como dependência de compilação).
- `CitizensNpcListener`: interação com NPCs do Citizens abre a loja.
- Marker persistente `bw-shop-marker` em NPCs do Citizens para reconhecimento dos próprios NPCs.

### Alterado
- `ShopNpcManager`: backend resolvido pelo config (`npc-backend`), `getBackendId()` por hook, detecção unificada de entidades gerenciadas.
- `BedWarsPlugin`: registra o listener do hook ativo no startup.
- `config.yml`: opção `npc-backend` (valores `auto`, `fancynpcs`, `citizens`) sem valor ativo por padrão (usa `auto`).
- `lang/pt_BR.yml`: chave `startup.npcs_no_backend` renomeada para `startup.npcs_unavailable`; mensagens sem a palavra "backend".

### Documentação
- `README.md`: Citizens adicionado aos requisitos e ao passo de instalação; seção "NPC da loja (com FancyNPCs ou Citizens)".

## [0.0.1-167] - 2026-08-07

### Corrigido
- Joins por código validam modo e jogador antes de entrar na sala.
- Filas órfãs descartadas; espectadores unificados entre salas.

## [0.0.1-165] - 2026-08-07

### Corrigido
- Fila de entradas pendentes chaveada por `(arena, modo)` para não misturar modos na construção da partida.

## [0.0.1-163] - 2026-08-07

### Corrigido
- Join sem modo encontra qualquer lobby aberto da arena em vez de criar lobby paralelo.

## [0.0.1-161] - 2026-08-07

### Corrigido
- Start de partida com modo (ex.: quarteto) encontrava a partida existente em vez de criar instância fantasma.

## [0.0.1-159] - 2026-08-07

### Corrigido
- Respawn com prioridade `MONITOR` para sobrepor outros plugins; reafirmação no tick seguinte para espectadores.

## [0.0.1-157] - 2026-08-07

### Corrigido
- Jogador eliminado sem cama vira espectador na partida em vez de voltar ao lobby.

## [0.0.1-155] - 2026-08-07

### Corrigido
- Nomes de arena/mapa aceitos em qualquer caixa (case-insensitive).

## [0.0.1-154] - 2026-08-07

### Alterado
- Log de inventário exibe nome e UUID do jogador.

## [0.0.1-153] - 2026-08-07

### Corrigido
- Ender chest não vaza itens entre partidas (snapshot + clear no início).

## [0.0.1-152] - 2026-08-07

### Corrigido
- Respawn com bloco no spawn do time não sufoca; último respawn preservado quando a cama quebra durante o cooldown.

## [0.0.1-151] - 2026-08-07

### Adicionado
- Bola de fogo dispara projétil em vez de agir como isqueiro (consumo de 1 por disparo).

## [0.0.1-150] - 2026-08-07

### Corrigido
- Jogador eliminado não fica preso caindo no void (respawn para o lobby).

## [0.0.1-149] - 2026-08-07

### Corrigido
- Armadura de time travada em 100% (bloqueio de movimento em qualquer inventário e arraste para slot de armadura).

## [0.0.1-148] - 2026-08-07

### Alterado
- Botão "voltar" das categorias da loja usa flecha em vez de barreira.

## [0.0.1-147] - 2026-08-07

### Adicionado
- Conjuntos de armadura do `shop.yml` padrão com encantamento de proteção I/II/III.

## [0.0.1-146] - 2026-08-07

### Removido
- Upgrade `dragon_buff` não implementado removido do `shop.yml` padrão.

## [0.0.1-145] - 2026-08-07

### Corrigido
- Bloqueio de recompra de armadura considera o encantamento de proteção na comparação de progressão.

## [0.0.1-144] - 2026-08-07

### Corrigido
- Reafirma o respawn no spawn do time 1 tick depois para evitar sobrescrita por outros plugins.

## [0.0.1-143] - 2026-08-07

### Corrigido
- Reload apagava `spawn_item` do YAML (flush antes do load).

## [0.0.1-142] - 2026-08-07

### Documentação
- `README.md` e `ARCHITECTURE.md` atualizados: armaduras de time, loja posicionável, `spawn_item` e sistema de mundos ativo.

## [0.0.1-141] - 2026-08-07

### Adicionado
- `spawn_item` por arena.
- Bloqueio de recompra de armadura.
- Javadoc e correções apontadas pelo IDE.

## [0.0.1-140] - 2026-08-07

### Adicionado
- Posicionamento da loja com type `row`/`column` e centralização por linha.

## [0.0.1-139] - 2026-08-07

### Adicionado
- Armadura da loja vira couro tingido na cor do time, unbreakable e travada no slot.

## [0.0.1-138] - 2026-08-07

### Corrigido
- TNT destrói apenas blocos colocados por jogadores; mapa original protegido.

## [0.0.1-137] - 2026-08-07

### Alterado
- `shop.yml` adaptado; DIAMOND volta a ser item (não conjunto).

## [0.0.1-136] - 2026-08-07

### Adicionado
- Kits recursivos com itens dentro de itens na loja.

## [0.0.1-135] - 2026-08-07

### Adicionado
- Loja vende conjuntos de armadura completos (CHAINMAIL/IRON/DIAMOND).

## [0.0.1-134] - 2026-08-07

### Corrigido
- Restauração de inventário e estados do jogador centralizada e localizada.

## [0.0.1-133] - 2026-08-07

### Alterado
- `version_check` loga quando a versão local é mais nova que a publicada.

## [0.0.1-132] - 2026-08-07

### Documentação
- `example.yml` totalmente comentado (referência de todas as opções) e seção correspondente no README.

## [0.0.1-131] - 2026-08-07

### Corrigido
- `enable-cmd` aceita valor scalar no YAML.
- `version_check` roda assíncrono (não trava a main thread).

### Refatoração
- `version_check` lê a versão remota direto do `pom.xml` (removido `.github/version.json`).

## [0.0.1-130] - 2026-08-07

### Adicionado
- Config `enable-cmd` no YAML da arena libera comandos específicos durante a partida.

## [0.0.1-129] - 2026-08-07

### Corrigido
- Lore do item de fornalha na loja não atualizava após upgrade (`purchaseItem` agora re-renderiza a loja).

## [0.0.1-128] - 2026-08-07

### Corrigido
- `/bw start` não iniciava partida com jogadores em um único time (`forceStart` ignora exigência de 2 times; countdown automático mantém a regra).

## [0.0.1-127] - 2026-08-07

### Adicionado
- Código de partida de 6 caracteres (ABC123) gerado por sala.
- `/bw join <arena> --code <codigo>` entra na sala específica.

## [0.0.1-125] - 2026-08-07

### Corrigido
- `/bw start` acusava configuração faltando com YAML completo (`validateArena` rodava contra o cache sem world; agora usa `ensureWorldLoaded` antes de validar).

## [0.0.1-124] - 2026-08-07

### Corrigido
- Save sobrescrevia spawn/cama do disco com `null` quando o cache tinha locations não resolvidas (mundo carregado).

## [0.0.1-123] - 2026-08-07

### Adicionado
- Breadcrumb no título da loja (Loja > Compra > Categoria).

## [0.0.1-122] - 2026-08-07

### Adicionado
- Preço de upgrade da fornalha por nível (`level-default` + `upgrade.price/material` no YAML, cobrança e exibição dinâmica na loja).

## [0.0.1-121] - 2026-08-07

### Corrigido
- Contagem regressiva não parava e partida iniciava com todos no mesmo time (`updateCountdownState` valida 2+ times).

## [0.0.1-120] - 2026-08-07

### Corrigido
- Tab-complete do `/bw` explodindo com `zip file closed` após reload do PlugMan (try/catch defensivo no `onTabComplete`).

## [0.0.1-119] - 2026-08-07

### Corrigido
- `lang/pt_BR.yml` com YAML inválido (indentação mista na seção `log`) fazia todas as mensagens virarem `[missing]`.

## [0.0.1-118] - 2026-08-07

### Corrigido
- `plugin.yml` com `authors` inválido (YAML quebrado gerado a partir de `project.developers`) causando erro no PlugMan.

## [0.0.1-117] - 2026-08-07

### Corrigido
- Jogadores perdiam o inventário ao sair da partida (snapshot único de inventário; leave em `ENDING` restaura; respawn sem cama via `leaveGame`).

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
- Tab do `/bw join` completo por flag; crafting bloqueado em partida.

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
- `restoreInventory` não limpa inventário em chamada dupla.

## [0.0.1-106] - 2026-08-07

### Removido
- Item Dragon Buff da loja (upgrade não implementado).

## [0.0.1-105] - 2026-08-07

### Corrigido
- Jogadores vivos podem dropar itens; respawn tem prioridade alta.

## [0.0.1-104] - 2026-08-07

### Corrigido
- Só permite quebrar blocos colocados por jogadores ou camas.

## [0.0.1-103] - 2026-08-07

### Corrigido
- Reload atualiza itens da loja (invalida cache do ShopManager).

## [0.0.1-102] - 2026-08-07

### Corrigido
- Upgrade da fornalha não entrega item físico.

## [0.0.1-101] - 2026-08-07

### Corrigido
- Não abre menu de seleção de time durante a partida.

## [0.0.1-100] - 2026-08-07

### Corrigido
- Sair no lobby não declara vitória antes da partida começar.

## [0.0.1-099] - 2026-08-07

### Corrigido
- `isAvailable` captura `Error` do ASP para funcionar sem o plugin.

## [0.0.1-098] - 2026-08-07

### Interno
- Artifact da action usa glob automático `sBedWars-*.jar`.

## [0.0.1-097] - 2026-08-07

### Alterado
- Persistência `Saveable`, log de idioma em `lang` e versão `0.0.1-097`.

## [0.0.1-096] - 2026-08-07

### Corrigido
- Falha no spawn de NPCs com skins inválidas (fallback).

### Alterado
- Persistência centralizada; logs migradas; remoção de `StatsManager`.
- Nome do JAR `sBedWars-v0.0.1-096.jar`; versão no `pom.xml`.

## [0.0.1-095] - 2026-08-07

### Corrigido
- Roupas dos times, seletor wool, countdown bug e log de NPC.

## [0.0.1-094] - 2026-08-07

### Corrigido
- Jogadores do mesmo jogo ficavam ocultos no join em fila.

## [0.0.1-093] - 2026-08-07

### Adicionado
- Pré-build assíncrono da instância no join (paste fora da main thread).

## [0.0.1-092] - 2026-08-07

### Corrigido
- Instância de partida montada do disco evita reset após unload/load.

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
- Modo de partida por sala (solo/dupla/trio/quarteto).

## [0.0.1-086] - 2026-08-07

### Adicionado
- Instâncias de partida por arena (partidas simultâneas do mesmo mapa).

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
- Reutilizar mundo existente em vez de recriar na `arena/reset` (falha no createWorld sem `paper-world-defaults.yml`).

## [0.0.1-081] - 2026-08-07

### Corrigido
- Criação de mundo não capturada derrubava o comando join (`paper-world-defaults.yml` ausente).

## [0.0.1-080] - 2026-08-07

### Corrigido
- Arena perdia configuração após unload/load do plugin (refs de mundo desatualizadas); javadoc das mudanças da loja.

## [0.0.1-079] - 2026-08-07

### Adicionado
- Loja oculta fileira de categorias ao entrar em uma categoria; itens padrão de BedWars.

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
- Save detecta automaticamente a área construída (sem seleção FAWE).

## [0.0.1-067] - 2026-08-07

### Corrigido
- Reset não apagava o mundo real; camas fora do schematic não eram restauradas.

## [0.0.1-066] - 2026-08-07

### Corrigido
- Mundo de partida reutilizado sujo (minérios/camas de partidas anteriores persistiam).

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
- Reset de arena não limpa o mundo (unload/delete verificados).

## [0.0.1-058] - 2026-08-07

### Corrigido
- Prefixo duplicado nos logs de debug do GameManager.

## [0.0.1-057] - 2026-08-07

### Corrigido
- NPCs da loja duplicados (leftover do `add` não era removido).

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
- Checkstyle adicionado ao build; 708 violações de estilo corrigidas.

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
- Todas as mensagens hardcoded movidas para `lang/pt_BR.yml` e traduzidas para português.

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
- Fundação do plugin: comandos base, arenas, partidas, spawns de recursos, loja, editor, espectadores, respawn, persistência, versão e integrações (FAWE/ASP/FancyNpcs/bStats).

### Alterado
- Lançamentos iniciais `0.0.1-002` a `0.0.1-020` sem mensagens descritivas; incluem merge da `main`, `renovate.json` e commits iniciais de scaffolding.
