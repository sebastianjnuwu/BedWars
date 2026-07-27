# 🛏️ BedWars

Um plugin moderno de **BedWars** para **Paper 1.21.4**, desenvolvido com foco em desempenho, organização do código e alta personalização.

> **Status:** 🚧 Em desenvolvimento

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

#### Geradores globais

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

### 11. Salvar a arena

Após concluir toda a configuração:

```bash
/bw admin save <nome_da_arena>
```

### 12. Jogar na arena

```bash
/bw join <nome_da_arena>         # time automático
/bw join <nome_da_arena> azul    # time específico
/bw start <nome_da_arena>        # iniciar manualmente
/bw leave                         # sair da partida
```

### Comandos administrativos adicionais

| Comando | Descrição |
|---------|-----------|
| `/bw admin list` | Lista todas as arenas registradas |
| `/bw admin delete <nome>` | Deleta uma arena |
| `/bw admin load <nome>` | Carrega o schematic de uma arena em um mundo void |
| `/bw admin reload` | Recarrega arquivos de configuração |
| `/bw admin arena <nome> removeteam <cor>` | Remove um time |

## 🚀 Compilação

```bash
mvn clean package
```

O JAR será gerado em `target/BedWars-1.0.0.jar`.

# 🤝 Contribuindo

Pull Requests são muito bem-vindos!
