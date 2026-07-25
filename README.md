# 🛏️ BedWars

Um plugin moderno de **BedWars** para **Paper 1.21.4**, desenvolvido com foco em desempenho, organização do código e alta personalização.

> **Status:** 🚧 Em desenvolvimento

---

## ✨ Recursos

- 🗺️ Gerenciamento completo de arenas
- 🌍 Sistema de carregamento e salvamento de mundos
- 🛏️ Sistema de camas e eliminação
- 👥 Gerenciamento de equipes
- ⚙️ Configuração totalmente personalizável
- ⛏️ Geradores de recursos configuráveis
- 📦 Arquitetura modular para facilitar manutenção e expansão

---

# 📋 Roadmap

## ✅ Funcionando

### 🗺️ Arenas

- [x] Seleção de regiões utilizando WorldEdit (`//pos1` e `//pos2`)
- [x] Criar arenas (`/bw admin create <nome_da_arena>`)
- [x] Carregamento da arena (`/bw admin load <nome_da_arena>`)
- [x] Edição de arenas (`/bw admin edit <nome_da_arena>`)
- [x] Salvamento das arenas após edição (`/bw admin save <nome_da_arena>`)
- [x] Excluir arenas (`/bw admin delete <nome_da_arena>`)

## 🚧 Em Desenvolvimento

### ⚙️ Configuração

- [ ] Lobby global
- [ ] Spawn da arena
- [ ] Sistema de equipes
- [ ] Spawn das equipes
- [ ] Cama das equipes
- [ ] Número mínimo de jogadores
- [ ] Contagem regressiva configurável

### ⛏️ Geradores

- [ ] Ferro
- [ ] Ouro
- [ ] Diamante
- [ ] Esmeralda
- [ ] Sistema de níveis
- [ ] Configuração pelo `config.yml`
- [ ] Posicionamento via comandos

### 🎮 Gameplay

- [ ] Entrar em partidas
- [ ] Sair de partidas
- [ ] Início automático
- [ ] Sistema de quebra de camas
- [ ] Respawn temporizado
- [ ] Eliminação definitiva
- [ ] Encerramento automático da partida

### 🛒 Lojas

#### Loja de Itens

- [ ] GUI de compras
- [ ] Configuração de itens
- [ ] Configuração de preços
- [ ] Quick Buy

#### Loja de Melhorias

- [ ] Proteção
- [ ] Afiação
- [ ] Pressa
- [ ] Forja
- [ ] Armadilhas

---

### 📊 Interface

#### Scoreboard

- [ ] Estado das equipes
- [ ] Tempo da partida
- [ ] Estatísticas
- [ ] Nome do mapa

#### Hologramas

- [ ] Contador dos geradores

#### Feedback

- [ ] Titles
- [ ] Subtitles
- [ ] Sons
- [ ] Mensagens personalizadas

---

### 💣 Itens Especiais

- [ ] Fireball
- [ ] Bridge Egg
- [ ] Pop-up Tower
- [ ] Dream Defender
- [ ] Silverfish

---

### 💾 Persistência

#### Banco de Dados

- [ ] SQLite
- [ ] MySQL

#### Estatísticas

- [ ] Vitórias
- [ ] Derrotas
- [ ] Abates
- [ ] Final Kills
- [ ] Camas destruídas
- [ ] XP
- [ ] Prestígio

---

### 🛡️ Otimização

- [ ] Anti-Grief
- [ ] Reset automático das arenas
- [ ] Reconexão em partidas
- [ ] Tratamento de desconexões

---

# 📦 Requisitos

- Java **21+**
- Maven **3.9+**
- Paper **1.21.4**

---

# 🚀 Compilação

### Compilar

```bash
mvn clean compile
```

### Executar testes

```bash
mvn test
```

### Gerar o JAR

```bash
mvn clean package
```

### Instalar localmente

```bash
mvn clean install
```

### Executar

```bash
mvn spring-boot:run
```

### Limpar arquivos temporários

```bash
mvn clean
```

### Resolver dependências

```bash
mvn dependency:resolve
```

### Visualizar dependências

```bash
mvn dependency:tree
```

# 🤝 Contribuindo

Pull Requests são bem-vindos; Caso encontre algum problema ou tenha alguma sugestão, abra uma Issue.
