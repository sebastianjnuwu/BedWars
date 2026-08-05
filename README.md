# 🛏️ BedWars

Um plugin moderno de **BedWars** para **Paper 1.21.4**, desenvolvido com foco em desempenho, organização do código e alta personalização.

> **Status:** 🚧 Em desenvolvimento

## 📊 Estatísticas

O plugin utiliza o [bStats](https://bstats.org/) para coletar estatísticas anônimas de uso.

![bStats](https://bstats.org/signatures/bukkit/sBedWars.svg)

### Codacy

Qualidade e segurança do código analisadas pelo [Codacy](https://www.codacy.com/).

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ece841e4ff31406999194219a9035770)](https://app.codacy.com/gh/sebastianjnuwu/BedWars/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

## ✨ Recursos

- 🗺️ Gerenciamento completo de arenas
- 🌍 Sistema de carregamento e salvamento de mundos
- 🛏️ Sistema de camas e eliminação
- 👥 Gerenciamento de equipes
- ⚙️ Configuração totalmente personalizável
- ⛏️ Geradores de recursos configuráveis
- 📦 Arquitetura modular para facilitar manutenção e expansão

## 📦 Requisitos

- Java **v21+**
- Maven **v3.9+**
- FastAsyncWorldEdit **v2.15+**
- AdvancedSlimePaper **v4.0+**
- FancyNPCs **v2.9+** 

## Tutorial — Como utilizar o plugin?

### 1. Instalar o servidor e os plugins necessários

Antes de usar o BedWars, você precisa preparar um servidor Paper 1.21.4 com os plugins de suporte abaixo:

- Baixe o servidor Paper **1.21.4** e coloque o arquivo JAR na pasta do servidor.
- Instale os plugins obrigatórios:
  - FastAsyncWorldEdit **v2.15+**
  - AdvancedSlimePaper **v4.0+**
  - FancyNPCs **v2.9+**

Depois, coloque o arquivo do BedWars gerado em `target/BedWars-1.0.0.jar` na pasta `plugins/` do servidor.

### 2. Definir o lobby

Primeiro, vá até o local onde será o lobby principal e execute o comando abaixo.

> Esse lobby é obrigatório — sempre que uma partida terminar ou um administrador finalizar a edição de uma arena, todos os jogadores serão teleportados automaticamente para esse local.

```bash
/bw admin setlobby
```

### 3. Criar a arena

Crie uma nova arena utilizando um **nome único e sem espaços**. Esse nome será usado como identificador interno da arena.

> O plugin criará automaticamente um **mundo vazio (Void)** e te teleportará para lá em modo criativo com voo ativo. Construa a arena manualmente ou utilize **WorldEdit/FAWE** para colar um mapa existente.

```bash
/bw admin create <nome_da_arena>
```

### 4. Construir e salvar o mapa

Após construir a arena com FAWE, selecione toda a construção com `//pos1` e `//pos2` e execute:

```bash
/bw admin save <nome_da_arena>
```

> O comando lê a seleção do FAWE automaticamente, gera o arquivo `.schem` na pasta `maps/` e o template SlimeWorld em `templates/`.

### 5. Editar a arena (configurar spawn, times, etc.)

Com a arena salva, entre no modo de edição para configurá-la.

> O comando carrega o schematic salvo, teleporta você para o spawn da arena (se definido) e ativa os marcadores visuais.

```bash
/bw admin edit <nome_da_arena>
```

### 6. Definir o spawn de espera

Vá até o local onde os jogadores deverão aguardar o início da partida e execute:

```bash
/bw admin arena <nome_da_arena> spawn
```

### 7. Adicionar os times

Adicione todos os times que farão parte da partida.

**Cores disponíveis:** `azul`, `vermelho`, `verde`, `amarelo`, `roxo`, `rosa`, `laranja`, `ciano`

```bash
/bw admin arena <nome_da_arena> addteam azul
/bw admin arena <nome_da_arena> addteam vermelho
/bw admin arena <nome_da_arena> addteam verde
/bw admin arena <nome_da_arena> addteam amarelo
```

### 8. Definir o spawn dos times

Vá até o local onde os jogadores de cada time deverão nascer e execute:

```bash
/bw admin arena <nome_da_arena> setspawn azul
/bw admin arena <nome_da_arena> setspawn vermelho
```

> Repita para todos os times.

### 9. Definir a cama dos times

Posicione-se sobre a cama correspondente ao time e execute:

```bash
/bw admin arena <nome_da_arena> setbed azul
/bw admin arena <nome_da_arena> setbed vermelho
```

> Repita para todos os times.

### 10. Adicionar geradores

#### Geradores base (configuração automática)

Por padrão, cada arena já possui configurações para os tipos **ferro**, **ouro**, **diamante** e **esmeralda**. Para adicionar um gerador no mundo:

```bash
/bw admin arena <nome_da_arena> addgenerator ferro
/bw admin arena <nome_da_arena> addgenerator ouro
/bw admin arena <nome_da_arena> addgenerator diamante
/bw admin arena <nome_da_arena> addgenerator esmeralda
```

#### Geradores das bases (Forja)

```bash
/bw admin arena <nome_da_arena> addgenerator forja azul
/bw admin arena <nome_da_arena> addgenerator forja vermelho
```

> Repita para todos os times.

### 11. Configurações opcionais

```bash
/bw admin arena <nome_da_arena> setminplayers 4
/bw admin arena <nome_da_arena> setcountdown 30
/bw admin arena <nome_da_arena> status
/bw admin arena <nome_da_arena> teams
```

> O comando `status` mostra quantos times a arena tem e **quais modos ela suporta**. Um modo é válido quando o número de times é divisível pelo tamanho do time (ex.: arena com 2 times aceita `solo` e `dupla`, mas não `trio`).

### 12. Adicionar NPC da loja

Instale o **FancyNPCs** no servidor. Durante a edição da arena, posicione-se onde o NPC deverá ficar e execute:

```bash
/bw admin arena <nome_da_arena> shop-npc add [skin] [displayName]
/bw admin arena <nome_da_arena> shop-npc displayName <id> <nome>
```

> O NPC será spawnado automaticamente quando a partida iniciar e removido ao final. Use `list` para ver os NPCs adicionados e `remove <id>` para remover. Cada NPC guarda seu próprio skin e displayName no arquivo da arena.

```bash
/bw admin arena <nome_da_arena> shop-npc list
/bw admin arena <nome_da_arena> shop-npc remove 0
```

### 13. Salvar a arena

Após concluir toda a configuração:

```bash
/bw admin save <nome_da_arena>
```

### 13.1 Descartar a edição (sair sem salvar)

Se quiser sair do modo de edição **sem salvar** as alterações de bloco feitas no mapa, use:

```bash
/bw admin discard <nome_da_arena>
```

> O comando encerra a sessão de edição, remove os NPCs da loja e restaura o mundo a partir do último schematic salvo, descartando as mudanças feitas durante a sessão.

### 13.2 Referência completa do YAML da arena

Todos os comandos acima salvam as configurações no arquivo `arenas/<nome>.yml`. Para saber **o que cada opção significa**, consulte o arquivo `arenas/example.yml` (dentro do JAR ou em `src/main/resources/arenas/`) — ele é totalmente comentado e serve como referência.

Destaques que você pode editar **direto no YAML** (sem comando):

- `enable-cmd:` — lista de comandos liberados durante a partida (além de `/bw` e `/bedwars`). Ex.:
  ```yaml
  enable-cmd:
    - "g"        # libera /g
    - "msg"      # libera /msg e /msg <jogador> ...
  ```
- `forge:` — níveis da fornalha, preço e moeda de upgrade de cada nível.
- `generator_config:` — intervalo (em ticks) de cada gerador.
- `difficulty` / `time` / `cycle_day` / `cycle_weather` / `spawn_mobs` / `spawn_animals` — ambiente do mundo.

### 14. Jogar na arena

```bash
/bw join <nome_da_arena>              # partida livre (time automático)
/bw join <nome_da_arena> azul         # partida livre, time específico
/bw join <nome_da_arena> dupla        # partida no modo dupla (2 por time)
/bw join <nome_da_arena> solo         # partida no modo solo (1 por time)
/bw start <nome_da_arena>             # iniciar manualmente (partida livre)
/bw leave                             # sair da partida
```

> O modo é escolhido **na hora de entrar**, não fica fixo na arena. `solo`, `dupla`, `trio` e `quarteto` definem quantos jogadores cabem por time. Quem entra sem modo joga numa partida **livre** (capacidade derivada do `min_players` da arena).
>
> Enquanto um administrador estiver editando a arena, jogadores **não podem** entrar na partida.

### 14.1 Partidas simultâneas no mesmo mapa

Uma única arena pode hospedar **várias partidas ao mesmo tempo**, inclusive em modos diferentes. Ao entrar em uma arena que já está com sala cheia (ou em andamento), o plugin **cria automaticamente uma nova instância** com um mundo próprio:

```
/bw join <nome_da_arena> [modo]
```

- Cada partida roda em um mundo dedicado `bw_<arena>_<id>` (ex.: `bw_lush_0`, `bw_lush_1`, `bw_lush_2`...), clonado do schematic da arena.
- Jogadores que chegam enquanto a sala atual não está cheia entram nela; quando a sala atinge a capacidade, a próxima partida é criada em outra instância.
- Jogadores que especificam um modo só entram (ou criam) partidas **daquele modo**; quem não especifica entra em partidas livres.
- Um modo é rejeitado se o número de times da arena não for divisível pelo tamanho do time (ex.: 2 times → `solo` e `dupla` ok; `trio` e `quarteto` bloqueados).
- Ao fim da partida o mundo da instância é descartado (unload + delete) e um novo mundo é recriado do schematic para a próxima partida.
- Nada é persistido em disco durante a partida: o arquivo da arena (`arenas/<nome>.yml`) continua sendo apenas a configuração, e o schematic continua uma única vez em `maps/`.

Isso permite, por exemplo, rodar **partidas solo, dupla e quarteto** do mesmo mapa ao mesmo tempo apenas com `/bw join <arena> <modo>` — cada sala lotada gera uma nova instância automaticamente.

### 14.2 Modos e times

O modo define **quantos jogadores cabem por time**; o número de times/camas é fixo do mapa:

| Modo | Jogadores por time | Mapa com 4 camas |
|------|--------------------|------------------|
| `solo` | 1 | até 4 jogadores (1v1 x4) |
| `dupla` | 2 | até 8 (2v2 x4) |
| `trio` | 3 | **bloqueado** (4 não é divisível por 3) |
| `quarteto` | 4 | até 16 (4v4 x4) |

Regras importantes:

- **O modo é o máximo, não o mínimo.** Uma partida `quarteto` num mapa de 4 camas funciona com menos gente: 4 jogadores viram 1v1v1v1, 8 viram 2v2v2v2, e assim por diante.
- **Distribuição automática balanceada.** Quem entra sem escolher time vai para o time com menos jogadores (`findSmallestTeam`). Por isso nunca fica um time vazio por acaso — com 12 jogadores numa partida quarteto de 4 camas, o resultado é 3v3v3v3, não 3 times cheios + 1 sobrando.
- **Mapa com 3 camas não aceita quarteto** (nem trio num mapa de 2 camas): o modo é rejeitado se o número de times não for divisível pelo tamanho do time. Consulte os modos válidos com `/bw admin arena <arena> status`.
- Só fica um time vazio se **jogadores forçarem na mão**, escolhendo times específicos (`/bw join <arena> azul` etc.) — a partida começa com os times preenchidos mesmo assim.

### 15. Configurar a loja da arena

Cada arena pode usar uma loja diferente. As lojas ficam em `plugins/BedWars/shop/<nome>.yml`.

#### Criar arquivo da loja
Copie a `default.yml` com outro nome ou edite diretamente:
```yaml
# plugins/BedWars/shop/minhaloja.yml
categories:
  blocks:
    display-name: "<red>Blocks</red>"
    icon: SANDSTONE
    lore:
      - "<gray>Buy blocks</gray>"
    items:
      - stack: WOOL
        price: 4 iron
      - stack: OBSIDIAN
        price: 20 gold
      - stack:
          type: DIAMOND_SWORD
          display-name: "<yellow>Super Sword</yellow>"
          enchants:
            sharpness: 1
        price: 4 gold
```

**Formatos suportados:**
- **Short stack:** `"material;amount;display-name;lore for <preco> <moeda>"`
- **Long stack:** `stack: { type, amount, display-name, lore, enchants, tag }` + `price: "<preco> <moeda>"`
- **Moedas:** `iron`, `gold`, `diamond`, `emerald`
- **Upgrade de forja:** `upgrade: forge`
- **Posicionamento:** `skip`, `column`, `row`, `linebreak`, `absolute`
#### Vincular loja à arena

```bash
/bw admin arena <nome_da_arena> shop minhaloja
```

Se não definir `shop`, a arena usa `default.yml`.

#### NPC da loja (com FancyNPCs)

Instale o **FancyNPCs** no servidor. Os NPCs são spawnados automaticamente quando a partida inicia e removidos ao final. Para configurar as posições durante a edição da arena:

```bash
/bw admin arena <nome_da_arena> shop-npc add [skin] [displayName]
/bw admin arena <nome_da_arena> shop-npc displayName <id> <nome>
```

Para gerenciar:

```bash
/bw admin arena <nome_da_arena> shop-npc list
/bw admin arena <nome_da_arena> shop-npc remove <id>
```

> O nome do NPC será `bw-shop-<arena>-<id>` — o plugin reconhece automaticamente NPCs com nome `shop` ou prefixo `bw-shop-` e abre a loja ao interagir.
### Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/bw admin create <nome>` | Cria uma nova arena | `bw.admin` |
| `/bw admin delete <nome>` | Deleta uma arena | `bw.admin` |
| `/bw admin list` | Lista todas as arenas registradas | `bw.admin` |
| `/bw admin save <nome>` | Salva o schematic da arena | `bw.admin` |
| `/bw admin load <nome>` | Carrega o schematic em um mundo void | `bw.admin` |
| `/bw admin edit <nome>` | Entra no modo de edição da arena | `bw.admin` |
| `/bw admin discard <nome>` | Sai do modo de edição sem salvar as alterações | `bw.admin` |
| `/bw admin setlobby` | Define o lobby principal | `bw.admin` |
| `/bw admin reload` | Recarrega arquivos de configuração | `bw.admin` |
| `/bw admin arena <arena> spawn` | Define o spawn de espera | `bw.admin` |
| `/bw admin arena <arena> status` | Exibe o status da arena | `bw.admin` |
| `/bw admin arena <arena> setminplayers <num>` | Define mínimo de jogadores | `bw.admin` |
| `/bw admin arena <arena> setcountdown <seg>` | Define contagem regressiva | `bw.admin` |
| `/bw admin arena <arena> setmap <mapa\|default>` | Aponta a arena para um schematic compartilhado | `bw.admin` |
| `/bw admin arena <arena> addteam <cor>` | Adiciona um time | `bw.admin` |
| `/bw admin arena <arena> removeteam <cor>` | Remove um time | `bw.admin` |
| `/bw admin arena <arena> setspawn <cor>` | Define o spawn do time | `bw.admin` |
| `/bw admin arena <arena> setbed <cor>` | Define a cama do time | `bw.admin` |
| `/bw admin arena <arena> teams` | Lista os times | `bw.admin` |
| `/bw admin arena <arena> addgenerator <tipo>` | Adiciona um gerador | `bw.admin` |
| `/bw admin arena <arena> shop-npc add [skin] [displayName]` | Adiciona NPC da loja | `bw.admin` |
| `/bw admin arena <arena> shop-npc displayName <id> <nome>` | Define o nome de exibição de um NPC | `bw.admin` |
| `/bw admin arena <arena> shop-npc list` | Lista NPCs da loja | `bw.admin` |
| `/bw admin arena <arena> shop-npc remove <id>` | Remove NPC da loja | `bw.admin` |
| `/bw join <arena> [time]` | Entra em uma partida | `bw.player` |
| `/bw leave` | Sai da partida atual | `bw.player` |
| `/bw start <arena>` | Inicia a partida manualmente | `bw.player` |

## 🚀 Compilação

```bash
mvn clean package
```

O JAR será gerado em `target/BedWars-1.0.0.jar`.

# 🤝 Contribuindo

Pull Requests são muito bem-vindos!
