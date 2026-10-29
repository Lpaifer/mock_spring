# Prática 2 com finalização de jogos e envio de SMS

A Prática 2 do exercício `Ex_A3_EntendendoMocks_EstudoCaso_.docx` amplia a Gestão de Jogos: os jogos da semana anterior devem ser salvos como finalizados, identificando os vencedores, antes de qualquer envio de SMS.

A suíte mantém **quatro testes principais da Prática 2**, os sete testes originais e 26 testes complementares para cobrir os demais caminhos da aplicação. A implementação de negócio e a persistência transacional permanecem iguais.

**Validação em 28/10/2026:** `clean install` aprovado, 37 testes executados, sem falhas, erros ou testes ignorados. O JaCoCo confirmou **100% das instruções, branches e linhas**, sem exclusões de classes. Essa exigência é obrigatória no build padrão.

## Regra de negócio

1. Selecionar jogos em andamento da semana anterior, de segunda a domingo, no fuso do relógio da aplicação.
2. Avaliar todos os jogos selecionados. O vencedor é o participante com a maior pontuação; em empate, permanece o primeiro que atingiu a maior pontuação. Um jogo sem resultados impede o lote.
3. Persistir todos os jogos e vencedores em uma transação JDBC. Uma falha no segundo jogo desfaz também a atualização do primeiro.
4. Somente após o commit, atualizar os objetos em memória e solicitar os SMS.

O contador de finalizações representa a última execução e só é atualizado após a confirmação do lote. Os testes usam um relógio fixo em 28/10/2026 no fuso `America/Sao_Paulo`; os jogos elegíveis estão entre 19/10 e 25/10, inclusive. Em execução normal, o relógio utiliza o fuso do sistema operacional.

## Os quatro testes principais da Prática 2

| Classe | Teste | O que demonstra |
| --- | --- | --- |
| [FinalizaJogoTest](src/test/java/br/com/valueprojects/mock_spring/FinalizaJogoTest.java) | `deveSalvarTodosOsJogosAntesDeEnviarSms` | Stub fornece dois jogos; `InOrder` exige salvar o lote com os vencedores antes dos dois SMS. |
| [FinalizaJogoTest](src/test/java/br/com/valueprojects/mock_spring/FinalizaJogoTest.java) | `naoDeveEnviarSmsQuandoPersistenciaFalhar` | Persistência lança exceção; `verifyNoInteractions` confirma ausência de SMS e o jogo permanece sem finalização. |
| [JogoDaoTest](src/test/java/br/com/valueprojects/mock_spring/test/JogoDaoTest.java) | `deveConfirmarTodosOsVencedoresNoBancoAntesDoSms` | Outra conexão H2 encontra os dois jogos e vencedores confirmados durante a chamada do SMS mockado. |
| [JogoDaoTest](src/test/java/br/com/valueprojects/mock_spring/test/JogoDaoTest.java) | `deveDesfazerLoteENaoEnviarSmsSeSegundoJogoFalhar` | Um erro SQL real no segundo update provoca rollback do lote e nenhum SMS. |

Os dois primeiros são testes unitários com mocks do repositório e do serviço de SMS. Os outros dois combinam persistência H2 real e mock de SMS. Eles continuam concentrados em duas classes para facilitar a apresentação da regra principal.

Os testes anteriores em `JogoTest`, `JuizTest`, `JulgamentoTest` e `TesteUnidade2ApplicationTests` permanecem. O helper [CenariosPratica2](src/test/java/br/com/valueprojects/mock_spring/CenariosPratica2.java) concentra o relógio, os participantes e a criação de bancos isolados.

## Testes complementares para cobertura integral

Os demais cenários ficam separados dos quatro testes principais, sem duplicar esses mesmos testes:

| Classe | Quantidade | Comportamentos verificados |
| --- | --- | --- |
| [FinalizaJogoComplementarTest](src/test/java/br/com/valueprojects/mock_spring/FinalizaJogoComplementarTest.java) | 8 | Falha de consulta, lista vazia, datas fora da semana, limites da semana, ausência de resultados, falha de SMS, contador por execução e relógio padrão. |
| [JogoDaoComplementarTest](src/test/java/br/com/valueprojects/mock_spring/test/JogoDaoComplementarTest.java) | 7 | Dados recarregados, falha de commit, vencedor inválido, finalização duplicada, jogo sem resultados, erro ao cadastrar resultados, conexão indisponível e ausência de chave gerada. |
| [JuizComplementarTest](src/test/java/br/com/valueprojects/mock_spring/test/JuizComplementarTest.java) | 4 | Reutilização do juiz, empate, pontuações negativas e jogo sem resultados. |
| [DominioPratica2Test](src/test/java/br/com/valueprojects/mock_spring/test/DominioPratica2Test.java) | 3 | Limite de resultados, dados de jogo/participante/pagamento, builder e igualdade de participantes. |
| [JogoDaoFalsoTest](src/test/java/br/com/valueprojects/mock_spring/test/JogoDaoFalsoTest.java) | 2 | Isolamento entre instâncias e validação do lote antes de modificar os jogos. |
| [JogoControllerTest](src/test/java/br/com/valueprojects/mock_spring/test/JogoControllerTest.java) | 1 | Cadastro, listagem e julgamento do controller existente. |
| [AplicacaoPratica2Test](src/test/java/br/com/valueprojects/mock_spring/AplicacaoPratica2Test.java) | 1 | Inicialização pelo `main`, configuração, banco real H2 e demonstração de SMS simulado. |

Essa separação mantém a leitura dos quatro cenários centrais simples e permite cumprir o requisito de cobertura de toda a aplicação.

## Como os mocks são utilizados

O stub define os jogos devolvidos pelo repositório:

```java
when(dao.emAndamento()).thenReturn(List.of(primeiro, segundo));
```

O teste verifica a ordem e o conteúdo do lote, sem precisar de `ArgumentCaptor`:

```java
InOrder ordem = inOrder(dao, sms);
ordem.verify(dao).emAndamento();
ordem.verify(dao).finalizaTodos(List.of(
        new FinalizacaoJogo(primeiro, BRUNO),
        new FinalizacaoJogo(segundo, BRUNO)));
ordem.verify(sms).enviar(BRUNO, primeiro);
ordem.verify(sms).enviar(BRUNO, segundo);
ordem.verifyNoMoreInteractions();
```

No teste de falha, o stub lança uma exceção e a verificação exige ausência de envio:

```java
doThrow(new IllegalStateException("Falha ao salvar"))
        .when(dao).finalizaTodos(anyList());

assertThrows(IllegalStateException.class, finalizador::finaliza);
verifyNoInteractions(sms);
```

O teste de rollback cria uma restrição apenas no banco isolado do teste para impedir a atualização do segundo jogo. Após a falha, consulta a base e comprova que nenhum dos jogos foi finalizado e nenhum vencedor foi gravado.

## Implementação e persistência

- [FinalizaJogo](src/main/java/br/com/valueprojects/mock_spring/model/FinalizaJogo.java) coordena a seleção, o julgamento, a persistência e os envios.
- [Juiz](src/main/java/br/com/valueprojects/mock_spring/model/Juiz.java) identifica o vencedor e reinicia os dados entre julgamentos.
- [Jogo](src/main/java/br/com/valueprojects/mock_spring/model/Jogo.java) registra o participante vencedor junto da finalização.
- [RepositorioJogos](src/main/java/infra/RepositorioJogos.java) define o contrato comum para JDBC, fake e mocks.
- [JogoDao](src/main/java/infra/JogoDao.java) recebe `DataSource`, usa SQL parametrizado e confirma o lote inteiro antes de retornar; em caso de falha, executa rollback.
- [JogoDaoFalso](src/main/java/infra/JogoDaoFalso.java) implementa o contrato em memória, com dados isolados por instância.
- [EnviadorSms](src/main/java/br/com/valueprojects/mock_spring/model/EnviadorSms.java) define a notificação; [SmsSimulado](src/main/java/infra/SmsSimulado.java) é o adaptador didático para console.
- [Pratica2Config](src/main/java/br/com/valueprojects/mock_spring/Pratica2Config.java) configura as dependências e a demonstração opcional.
- [schema.sql](src/main/resources/schema.sql) cria participantes, jogos e resultados, com referência ao vencedor.

A aplicação utiliza H2 em memória, inicializado automaticamente. Os dados duram enquanto a aplicação estiver em execução; não houve migração de dados de uma instalação MySQL existente.

## Executar e conferir os resultados

Com JDK 21 configurado em `JAVA_HOME`, na raiz de `mock_spring`:

```powershell
.\mvnw.cmd clean install
```

Para executar somente os quatro testes principais da Prática 2:

```powershell
.\mvnw.cmd "-Dtest=FinalizaJogoTest,JogoDaoTest" test
```

Resultado da suíte completa em 28/10/2026, com Java 21.0.12:

```text
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

| Métrica JaCoCo | Coberto | Perdido | Cobertura |
| --- | --- | --- | --- |
| Instruções | 1.140 | 0 | 100% |
| Branches | 70 | 0 | 100% |
| Linhas | 277 | 0 | 100% |

O relatório considera as 14 classes analisadas pelo JaCoCo, sem exclusões configuradas. O [POM](pom.xml) executa `report` e `check` na fase `verify`, com limite de zero instruções, branches e linhas perdidas. A verificação é obrigatória, sem ativação de perfil:

```powershell
.\mvnw.cmd clean verify
```

`clean verify` e `clean install` falham se os testes falharem ou a cobertura ficar abaixo de 100% nessas três métricas. Executar apenas `test` não executa a verificação de cobertura da fase `verify`.

Artefatos locais:

- [Relatório HTML JaCoCo](target/site/jacoco/index.html).
- [Relatórios JUnit](target/surefire-reports/).
- [Log do build](target/evidencias/pratica-2/build-install.log).
- [Resumo da validação](target/evidencias/pratica-2/validacao.json).

A pasta `target/` é ignorada pelo Git e removida por `clean`. Testes e cobertura são regenerados pelo build; os logs selecionados devem ser preservados antes de uma nova limpeza.

## Demonstração da aplicação

Após o build:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/teste-na-pratica_spring-0.0.1-SNAPSHOT.jar --spring.main.web-application-type=none --pratica2.demo=true
```

A demonstração cadastra dois jogos da semana anterior, persiste o lote e mostra os SMS simulados, os vencedores recarregados do banco e `Total finalizado: 2`. Bruno vence Corrida e Ana vence Cata moedas. Sem `--pratica2.demo=true`, esses dados não são criados automaticamente.

O adaptador não realiza envio por uma operadora. Se um envio falhar após o commit, a falha é propagada e os envios seguintes são interrompidos; os jogos permanecem salvos. Não há fila persistente ou retentativas nesta prática.

## Registro da dupla

O enunciado solicita programação em pares. Preencher conforme o trabalho realizado:

- Integrante 1 e RA: preencher.
- Integrante 2 e RA: preencher.
- Participação e revisão da dupla: preencher.

Para a entrega, apresentar os quatro cenários principais, identificar os testes complementares e anexar o resultado atual dos 37 testes e do JaCoCo com 100% de cobertura.
