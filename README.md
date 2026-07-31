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

- Java **21+**
- Maven **3.9+**
- Paper **1.21.4**
- FastAsyncWorldEdit **2.15+**
- AdvancedSlimePaper **4.0+**
- FancyNPCs **2.9+** (opcional — necessário apenas para NPCs da loja)

## Tutorial — Como utilizar o plugin?

### 1. Definir o lobby

Primeiro, vá até o local onde será o lobby principal e execute o comando abaixo.

> Esse lobby é obrigatório — sempre que uma partida terminar ou um administrador finalizar a edição de uma arena, todos os jogadores serão teleportados automaticamente para esse local.

```bash
/bw admin setlobby
```

### 2. Criar a arena

Crie uma nova arena utilizando um **nome único e sem espaços**. Esse nome será usado como identificador interno da arena.

> O plugin criará automaticamente um **mundo vazio (Void)** e te teleportará para lá em modo criativo com voo ativo. Construa a arena manualmente ou utilize **WorldEdit/FAWE** para colar um mapa existente.

```bash
/bw admin create <nome_da_arena>
```

### 3. Construir e salvar o mapa

Após construir a arena com FAWE, selecione toda a construção com `//pos1` e `//pos2` e execute:

```bash
/bw admin save <nome_da_arena>
```

> O comando lê a seleção do FAWE automaticamente, gera o arquivo `.schem` na pasta `maps/` e o template SlimeWorld em `templates/`.

### 4. Editar a arena (configurar spawn, times, etc.)

Com a arena salva, entre no modo de edição para configurá-la.

> O comando carrega o schematic salvo, teleporta você para o spawn da arena (se definido) e ativa os marcadores visuais.

```bash
/bw admin edit <nome_da_arena>
```

### 5. Definir o spawn de espera

Vá até o local onde os jogadores deverão aguardar o início da partida e execute:

```bash
/bw admin arena <nome_da_arena> spawn
```

### 6. Adicionar os times

Adicione todos os times que farão parte da partida.

**Cores disponíveis:** `azul`, `vermelho`, `verde`, `amarelo`, `roxo`, `rosa`, `laranja`, `ciano`

```bash
/bw admin arena <nome_da_arena> addteam azul
/bw admin arena <nome_da_arena> addteam vermelho
/bw admin arena <nome_da_arena> addteam verde
/bw admin arena <nome_da_arena> addteam amarelo
```

### 7. Definir o spawn dos times

Vá até o local onde os jogadores de cada time deverão nascer e execute:

```bash
/bw admin arena <nome_da_arena> setspawn azul
/bw admin arena <nome_da_arena> setspawn vermelho
```

> Repita para todos os times.

### 8. Definir a cama dos times

Posicione-se sobre a cama correspondente ao time e execute:

```bash
/bw admin arena <nome_da_arena> setbed azul
/bw admin arena <nome_da_arena> setbed vermelho
```

> Repita para todos os times.

### 9. Adicionar geradores

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

### 10. Configurações opcionais

```bash
/bw admin arena <nome_da_arena> setminplayers 4
/bw admin arena <nome_da_arena> setcountdown 30
/bw admin arena <nome_da_arena> status
/bw admin arena <nome_da_arena> teams
```

### 11. Adicionar NPC da loja

Instale o **FancyNPCs** no servidor. Durante a edição da arena, posicione-se onde o NPC deverá ficar e execute:

```bash
/bw admin arena <nome_da_arena> shop-npc add [skin]
```

> O NPC será spawnado automaticamente quando a partida iniciar e removido ao final. Use `list` para ver os NPCs adicionados e `remove <id>` para remover.

```bash
/bw admin arena <nome_da_arena> shop-npc list
/bw admin arena <nome_da_arena> shop-npc remove 0
```

### 12. Salvar a arena

Após concluir toda a configuração:

```bash
/bw admin save <nome_da_arena>
```

### 13. Jogar na arena

```bash
/bw join <nome_da_arena>         # time automático
/bw join <nome_da_arena> azul    # time específico
/bw start <nome_da_arena>        # iniciar manualmente
/bw leave                        # sair da partida
```

> Enquanto um administrador estiver editando a arena, jogadores **não podem** entrar na partida.

### 14. Configurar a loja da arena

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
/bw admin arena <nome_da_arena> shop-npc add [skin]
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
| `/bw admin setlobby` | Define o lobby principal | `bw.admin` |
| `/bw admin reload` | Recarrega arquivos de configuração | `bw.admin` |
| `/bw admin arena <arena> spawn` | Define o spawn de espera | `bw.admin` |
| `/bw admin arena <arena> status` | Exibe o status da arena | `bw.admin` |
| `/bw admin arena <arena> setminplayers <num>` | Define mínimo de jogadores | `bw.admin` |
| `/bw admin arena <arena> setcountdown <seg>` | Define contagem regressiva | `bw.admin` |
| `/bw admin arena <arena> addteam <cor>` | Adiciona um time | `bw.admin` |
| `/bw admin arena <arena> removeteam <cor>` | Remove um time | `bw.admin` |
| `/bw admin arena <arena> setspawn <cor>` | Define o spawn do time | `bw.admin` |
| `/bw admin arena <arena> setbed <cor>` | Define a cama do time | `bw.admin` |
| `/bw admin arena <arena> teams` | Lista os times | `bw.admin` |
| `/bw admin arena <arena> addgenerator <tipo>` | Adiciona um gerador | `bw.admin` |
| `/bw admin arena <arena> shop-npc add [skin]` | Adiciona NPC da loja | `bw.admin` |
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
