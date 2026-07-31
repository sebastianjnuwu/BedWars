# AGENTS.md

## Projeto

- Plugin **BedWars** para **Paper 1.21.4** (Java 21, Maven 3.9+).
- Dependências: FastAsyncWorldEdit, AdvancedSlimePaper, FancyNPCs (opcional), bStats.
- Consulte `README.md` (tutorial/uso) e `ARCHITECTURE.md` (estrutura) antes de alterar código.

## Comandos

- Validar build: `mvn -o clean compile -DskipTests` (executa o Checkstyle — exige 0 violações).
- Empacotar: `mvn clean package` → JAR em `target/BedWars-1.0.0.jar`.
- `src/test/` é ignorado; não adicionar testes.

## Convenção de commits

- Mensagem: `v0.0.1-0XX - <tipo>: <descrição>` (tipo: `fix`, `feat`, `docs`, `refactor`, `chore`...).
- Incrementar o `0XX` a cada commit.
- Nunca commitar sem pedido explícito do usuário.

## Estilo de código

- Java 21; seguir os padrões existentes (parâmetros `final`, switch expressions, nomes em inglês).
- Não adicionar comentários salvo se solicitado.
- Logs e mensagens em português; identificadores em inglês.

## Qualidade — Codacy

- Após **QUALQUER** edição de arquivo, quando o CLI/MCP do Codacy estiver disponível, rode a análise dos arquivos alterados e corrija os problemas reportados antes de encerrar.
- **NÃO** instalar o Codacy CLI manualmente (brew/npm/npx) — apenas avisar o usuário se não estiver instalado.
- Após **QUALQUER** mudança de dependência (`pom.xml`), rode a análise de segurança (trivy) e resolva vulnerabilidades antes de continuar.
- Não rodar análise buscando duplicação de código, métrica de complexidade ou cobertura.
