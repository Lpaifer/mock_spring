# Prática 1 sobre Mock Objects no projeto Gestão de Jogos

Respostas ao exercício `Ex_A3_EntendendoMocks_EstudoCaso_.docx`, fundamentadas no código do projeto `mock_spring`. O escopo deste material é somente a Prática 1.

As respostas abaixo retratam a versão anterior à Prática 2. A evolução do DAO, dos construtores e do fluxo de finalização está registrada na [solução da Prática 2](README-PRATICA-2.md).

**Aluno:** preencher. **RA:** preencher. **Data da execução:** preencher.

## 1 Executar a aplicação e apresentar os resultados

O projeto é uma aplicação Java com Spring Boot 3.3.3. O [pom.xml](pom.xml) configura Java 17, inclui as dependências Web e de testes e utiliza JaCoCo 0.8.12. A dependência `spring-boot-starter-test` disponibiliza JUnit e Mockito para os testes.

**Evidências de execução: pendentes.** Os comandos abaixo orientam a coleta dos prints; não representam uma execução realizada durante a elaboração destas respostas. Os arquivos antigos de `target/` não devem ser apresentados como resultados de uma nova execução.

### Java Application

Na IDE, executar o método `main` da classe [Mock_SpringApplication](src/main/java/br/com/valueprojects/mock_spring/Mock_SpringApplication.java) como aplicação Java. A execução inicializa a aplicação Spring Boot e o servidor Web; os testes JUnit são executados separadamente.

Como alternativa, na raiz do projeto, pelo PowerShell:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd spring-boot:run
```

O primeiro comando permite conferir a versão do Java efetivamente usada pelo Maven. Utilizar um JDK compatível com o projeto, como Java 17 ou 21, e conferir `JAVA_HOME`. Encerrar a aplicação com `Ctrl+C` antes das outras execuções, se necessário.

**Print 1:** inserir o console da execução como Java Application, mostrando a inicialização concluída. Anotar a versão do Java utilizada e o resultado observado.

### JUnit

Na IDE, executar os testes de `src/test/java` com JUnit. Pelo terminal, a execução correspondente é:

```powershell
.\mvnw.cmd test
```

As classes de teste existentes são `FinalizaJogoTest`, `JogoTest`, `JulgamentoTest` e `TesteUnidade2ApplicationTests`. Elas verificam regras do jogo, julgamento, finalização com Mockito e carregamento do contexto Spring.

Há um método `naoDeveJulgarSemResultado()` dentro de uma classe interna de `JulgamentoTest`, sem `@Nested`. Essa estrutura precisa ser considerada ao conferir quais testes foram descobertos: apenas existir um método com `@Test` no código não garante sua execução nessa classe interna.

**Print 2:** inserir o resultado do JUnit, mostrando os testes efetivamente executados, falhas, erros e testes ignorados. Preencher os totais a partir da execução atual.

### Build install

Executar na raiz do projeto:

```powershell
.\mvnw.cmd clean install
```

O comando limpa os artefatos anteriores, compila, executa os testes, empacota a aplicação e instala o artefato no repositório Maven local. Conferir o resultado final e os seguintes arquivos:

| Local | Finalidade |
| --- | --- |
| `target/surefire-reports/` | Resultados dos testes executados pelo Maven. |
| `target/teste-na-pratica_spring-0.0.1-SNAPSHOT.jar` | Aplicação empacotada. |
| `target/site/jacoco/index.html` | Relatório de cobertura gerado pelo JaCoCo configurado no POM. |

**Print 3:** inserir o console do build, incluindo o resumo dos testes e `BUILD SUCCESS`, caso a execução tenha sucesso. **Print 4:** inserir a pasta `target/` com os artefatos gerados. Se houver falha, registrar a mensagem e sua causa, sem declarar aprovação antes de uma execução bem-sucedida.

## 2 Quais classes de negócio foram projetadas para atender ao estudo de caso

As classes de negócio estão no pacote `br.com.valueprojects.mock_spring.model`:

| Classe | Responsabilidade |
| --- | --- |
| `Jogo` | Representa um jogo, sua descrição, data, resultados e estado de finalização. Controla a inclusão de resultados, impedindo registros consecutivos do mesmo participante e limitando a quantidade por participante. |
| `Participante` | Representa o participante por identificador e nome. Sua comparação de igualdade é utilizada nas regras de inclusão de resultados. |
| `Resultado` | Associa um participante à métrica ou pontuação obtida no jogo. |
| `Juiz` | Analisa os resultados e determina a maior e a menor pontuação. Lança uma exceção quando não existem resultados. |
| `FinalizaJogo` | Consulta jogos em andamento, verifica sua antiguidade, finaliza os elegíveis, contabiliza as finalizações e solicita a atualização ao DAO. |
| `Pagamento` | Representa um valor e uma data de pagamento. Ainda não está integrado ao processo de finalização. |

Outras classes dão suporte ao fluxo: `CriadorDeJogo` facilita a montagem de jogos, `JogoDao` executa operações JDBC no banco, `JogoDaoFalso` simula armazenamento em memória e `JogoController` expõe operações HTTP.

O código implementa partes do estudo de caso. Não existe identificação de temporada nem pagamento automático aos vencedores. `Juiz` retorna a pontuação máxima, sem retornar diretamente o participante vencedor. O critério de `FinalizaJogo` usa uma contagem de dias maior ou igual a sete, podendo incluir jogos muito antigos; ele não delimita a semana anterior no calendário.

## 3 Quais são os problemas de testar com dependências reais e DAO real

Testar a regra com um DAO real exige preparar e manter sua infraestrutura. No projeto, o construtor de [JogoDao](src/main/java/infra/JogoDao.java) abre uma conexão JDBC com o banco MySQL `mocks`, em `localhost`, utilizando o usuário `root` e senha vazia.

Esse teste depende de banco acessível, driver JDBC, credenciais, tabelas e dados adequados. Uma falha de conexão ou de SQL pode interromper o teste antes que a regra de finalização seja avaliada. A execução também tende a ser mais lenta e exige isolamento e limpeza dos dados para que um teste não interfira em outro.

No POM atual não há uma dependência declarada para o driver MySQL. A implementação real, portanto, também exige conferir como esse driver será disponibilizado. Os testes com mocks não exercitam essa conexão e não validam o SQL do DAO.

Para testar isoladamente `FinalizaJogo`, o Mockito substitui o DAO e fornece uma lista de jogos conhecida. Os testes com banco real continuam necessários em outro nível para verificar conexão, consultas e persistência; eles complementam os testes unitários.

## 4 Qual é a complexidade de projetar um DAO falso

Um DAO falso é uma implementação simplificada criada para substituir a persistência real durante os testes. Em [JogoDaoFalso](src/main/java/infra/JogoDaoFalso.java), os jogos são armazenados em uma lista, e os métodos `emAndamento()` e `finalizados()` filtram essa lista pelo estado de cada jogo.

A dificuldade está em manter essa implementação coerente com o contrato esperado: armazenamento, consultas, identificação, atualização e casos de erro precisam ser considerados. Conforme as operações crescem, o falso também precisa evoluir e pode introduzir seus próprios defeitos.

O falso deste projeto ilustra três cuidados:

- A lista é `static`, podendo manter dados compartilhados entre testes executados na mesma JVM.
- `atualiza()` não faz nada. Como a lista guarda referências aos objetos, alterar um jogo pode produzir uma mudança visível na consulta mesmo sem atualizar explicitamente o armazenamento. Isso difere de um banco real.
- `JogoDaoFalso` não implementa uma interface comum com `JogoDao` nem o estende. Como `FinalizaJogo` recebe um `JogoDao`, o falso não pode ser injetado diretamente nesse construtor.

Um contrato comum permitiria intercambiar as implementações. No teste atual, `mock(JogoDao.class)` já produz um objeto compatível com o tipo exigido. Apesar do nome da variável `daoFalso`, esse objeto é criado pelo Mockito e não é uma instância de `JogoDaoFalso`.

## 5 Qual é a importância da injeção de dependência em testes

Injeção de dependência significa fornecer a uma classe o colaborador de que ela precisa, em vez de obrigá-la a criar esse colaborador internamente. Isso permite controlar as condições do teste e executar a regra real com uma dependência substituta.

O construtor de `FinalizaJogo` já utiliza esse mecanismo:

```java
public FinalizaJogo(JogoDao dao) {
    this.dao = dao;
}
```

O teste fornece o objeto criado pelo Mockito:

```java
JogoDao daoFalso = mock(JogoDao.class);
FinalizaJogo finalizador = new FinalizaJogo(daoFalso);
```

Assim, a implementação real de `finaliza()` é executada sem precisar consultar o MySQL. O teste controla o retorno do DAO e verifica o efeito nos jogos ou as interações realizadas. Essa injeção ocorre por código Java e não depende da inicialização do Spring.

## 6 Quais são as formas de injeção de dependência

| Forma | Como funciona | Relação com os testes |
| --- | --- | --- |
| Construtor | A dependência é recebida na criação do objeto, como em `new FinalizaJogo(dao)`. | Explicita o colaborador obrigatório e facilita montar o objeto diretamente no teste. É a forma usada neste fluxo. |
| Método ou setter | Um método recebe e atribui a dependência, por exemplo `setDao(dao)`. | Permite configurar ou trocar o colaborador após a criação; exige cuidado para não utilizar o objeto antes dessa configuração. |
| Campo | Um mecanismo como o Spring preenche diretamente um atributo, por exemplo um campo com `@Autowired`. | Pode exigir contexto de teste ou ferramentas para preencher o campo; a dependência não fica explícita no construtor. |

Os exemplos de setter e campo explicam alternativas; essas formas não estão implementadas em `FinalizaJogo`. A injeção por construtor utilizada no projeto torna o teste simples e mantém o DAO em um atributo `final`.

## 7 Qual é a importância de trabalhar com Stub e Mock conforme o código

Substitutos de teste permitem controlar as dependências e avaliar o comportamento da classe em condições conhecidas. Stub fornece respostas programadas; a verificação de comportamento observa as interações com o colaborador. O mesmo objeto do Mockito pode cumprir os dois papéis.

### Stub e verificação de estado

No método `deveFinalizarJogosDaSemanaAnterior()`, de [FinalizaJogoTest](src/test/java/br/com/valueprojects/mock_spring/FinalizaJogoTest.java), dois jogos com data antiga são preparados e devolvidos pelo DAO:

```java
JogoDao daoFalso = mock(JogoDao.class);
when(daoFalso.emAndamento()).thenReturn(jogosAnteriores);

FinalizaJogo finalizador = new FinalizaJogo(daoFalso);
finalizador.finaliza();

assertTrue(jogo1.isFinalizado());
assertTrue(jogo2.isFinalizado());
assertEquals(2, finalizador.getTotalFinalizados());
```

`when(...).thenReturn(...)` configura o stub: quando a aplicação consultar jogos em andamento, receberá a lista preparada. Os `assert` verificam o estado resultante dos objetos reais e o total de finalizações. O stub não altera sozinho os jogos; a alteração é executada pelo finalizador real.

### Mock e verificação de comportamento

No método `deveVerificarSeMetodoAtualizaFoiInvocado()`, a interação com a persistência é verificada:

```java
verify(daoFalso, times(1)).atualiza(jogo1);
```

Essa verificação exige que o finalizador solicite uma atualização para `jogo1`. Um teste baseado apenas no estado poderia passar mesmo que `jogo.finaliza()` fosse chamado e a solicitação de persistência fosse esquecida. A verificação da interação cobre esse aspecto do comportamento.

O teste atual verifica explicitamente a atualização de `jogo1`; embora prepare dois jogos, não verifica a chamada de atualização de `jogo2`. A conclusão sobre as interações deve respeitar esse alcance.

## 8 Qual é a finalidade do método verify do Mockito

`verify()` confirma se um método de um mock foi chamado, com quais argumentos e, quando especificado, quantas vezes. Ele verifica uma interação já ocorrida; não executa o método real do DAO para persistir dados.

No exemplo existente:

```java
verify(daoFalso, times(1)).atualiza(jogo1);
```

O teste passa se `atualiza(jogo1)` tiver sido chamado exatamente uma vez. Se a chamada estiver ausente ou ocorrer mais de uma vez com esse argumento, a verificação falha. A forma abaixo também exige uma chamada, pois uma vez é o padrão de `verify()`:

```java
verify(daoFalso).atualiza(jogo1);
```

A verificação não comprova que o jogo foi gravado em um banco real nem estabelece ordem entre chamadas. Ela comprova que o código solicitou a operação ao colaborador substituto.

Há um comentário `Mockito.verifyNoInteractions(daoFalso)` no teste atual. Descomentá-lo depois de `finalizador.finaliza()` faria o teste falhar, porque o DAO já recebeu chamadas a `emAndamento()` e `atualiza()`. Esse método exige ausência de qualquer interação, o que não é a expectativa desse cenário.

## Referências para consulta

- Enunciado da disciplina: `Ex_A3_EntendendoMocks_EstudoCaso_.docx`, Prática 1.
- Código principal: [FinalizaJogo](src/main/java/br/com/valueprojects/mock_spring/model/FinalizaJogo.java), [JogoDao](src/main/java/infra/JogoDao.java), [JogoDaoFalso](src/main/java/infra/JogoDaoFalso.java) e [testes de finalização](src/test/java/br/com/valueprojects/mock_spring/FinalizaJogoTest.java).
- [Documentação do Mockito sobre stubbing e verificação de interações](https://javadoc.io/static/org.mockito/mockito-core/5.11.0/org/mockito/Mockito.html).
- [Documentação do JUnit sobre testes em classes internas](https://docs.junit.org/5.10.3/user-guide/#writing-tests-nested).
