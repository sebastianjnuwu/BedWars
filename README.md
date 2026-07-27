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

## Tutorial — Como utilizar o plugin?

### 1. Definir o lobby###

Primeiro, vá até o local onde será o lobby principal e execute o comando abaixo.

> Esse lobby é obrigatório, pois sempre que uma partida terminar ou um administrador finalizar a edição de uma arena, todos os jogadores serão teleportados automaticamente para esse local.

```bash
/bw admin setlobby
```

### 2. Criar a arena


Crie uma nova arena utilizando um **nome único e sem espaços**. Esse nome será usado como identificador interno da arena.

> Ao executar o comando, o plugin criará automaticamente um **mundo vazio (Void)** para a edição da arena. A partir daí, você pode construir a arena manualmente ou utilizar o **WorldEdit/FAWE** para colar um mapa já existente e continuar a configuração normalmente.

```bash
/bw admin create <nome_da_arena>
```


### 3. Carregar a arena

Após criar a arena, carregue-a para iniciar a edição.

> Esse comando carregará o mundo da arena e teleportará você para ele. Caso a arena ainda não esteja carregada, ela será carregada automaticamente.

```bash
/bw admin load <nome_da_arena>
```

Segue a continuação no mesmo padrão:

### 4. Entrar no modo de edição

Com a arena carregada, entre no modo de edição para começar a configurá-la.

> **Após concluir a edição, não se esqueça de salvar a arena utilizando o comando:**  `/bw admin save <nome_da_arena>`


```bash
/bw admin edit <nome_da_arena>
```

### 5. Definir o spawn de espera

Vá até o local onde os jogadores deverão aguardar o início da partida e execute o comando abaixo.

> Todos os jogadores serão teleportados para esse ponto antes do jogo começar.

```bash
/bw admin arena <nome_da_arena> spawn
```

### 6. Adicionar os times

Adicione todos os times que farão parte da partida. Cada time criado precisará ser configurado posteriormente.

**Cores disponíveis:**

* Azul
* Vermelho
* Verde
* Amarelo
* Roxo
* Rosa
* Laranja
* Ciano

**Exemplo:**

```bash
/bw admin arena <nome_da_arena> addteam azul
/bw admin arena <nome_da_arena> addteam vermelho
/bw admin arena <nome_da_arena> addteam verde
/bw admin arena <nome_da_arena> addteam amarelo
```

### 7. Definir o spawn dos times

Vá até o local onde os jogadores de cada time deverão nascer no início da partida e execute o comando correspondente.

**Exemplo:**

```bash
/bw admin arena <nome_da_arena> setspawn azul
/bw admin arena <nome_da_arena> setspawn vermelho
```

> Repita o processo para todos os times adicionados.

### 8. Definir a cama dos times

Posicione-se sobre a cama correspondente ao time e execute o comando abaixo.

**Exemplo:**

```bash
/bw admin arena <nome_da_arena> setbed azul
/bw admin arena <nome_da_arena> setbed vermelho
```

> Repita o processo para todos os times.

### 9. Adicionar geradores

#### Geradores globais

Vá até o local onde cada gerador deverá aparecer durante a partida e execute um dos comandos abaixo.

```bash
/bw admin arena <nome_da_arena> addgenerator ferro
/bw admin arena <nome_da_arena> addgenerator ouro
/bw admin arena <nome_da_arena> addgenerator diamante
/bw admin arena <nome_da_arena> addgenerator esmeralda
```

#### Geradores das bases (Forja)

Cada base deve possuir uma forja associada ao seu respectivo time.

**Exemplo:**

```bash
/bw admin arena <nome_da_arena> addgenerator forge azul
/bw admin arena <nome_da_arena> addgenerator forge vermelho
```

> Repita o processo para todos os times.

### 10. Configurações opcionais

Defina a quantidade mínima de jogadores necessária para iniciar a partida.

```bash
/bw admin arena <nome_da_arena> setminplayers 4
```

Defina o tempo da contagem regressiva antes do início da partida.

```bash
/bw admin arena <nome_da_arena> setcountdown 30
```

Verifique o status da arena para conferir quais configurações ainda estão pendentes.

```bash
/bw admin arena <nome_da_arena> status
```

### 11. Salvar a arena

Após concluir toda a configuração, salve a arena.

```bash
/bw admin save <nome_da_arena>
```

### 12. Jogar na arena

Entrar na arena com um time escolhido automaticamente.

```bash
/bw join <nome_da_arena>
```

Entrar em um time específico.

```bash
/bw join <nome_da_arena> azul
```

Iniciar a partida manualmente, ignorando a quantidade mínima de jogadores.

```bash
/bw start <nome_da_arena>
```

Sair da partida.

```bash
/bw leave
```

## 🚀 Compilação

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

Pull Requests são muito bem-vindos!

> Atenção: este projeto ainda está em desenvolvimento e não possui uma versão estável. As versões atuais são instáveis (unstable) e podem conter bugs, mudanças incompatíveis e funcionalidades incompletas. Não é recomendado utilizá-las em ambiente de produção.
