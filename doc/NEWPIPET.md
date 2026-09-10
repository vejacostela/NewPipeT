# Operação do NewPipeT

## Controle do projeto

Aplicativo e extractor são mantidos em `vejacostela/NewPipeT`. O módulo `extractor/` contém o código-fonte editável e é compilado pela dependência Gradle `project(":extractor")`. A automação anterior que seguia commits da conta TeamNewPipe foi substituída por testes do nosso próprio código.

O import inicial tem procedência e licença registradas em `extractor/UPSTREAM.md`. Créditos de autores, namespaces internos e licenças não são contas de acesso e foram preservados. Outras bibliotecas livres continuam tendo seus próprios mantenedores. Não foram removidos colaboradores nem alteradas permissões da conta GitHub.

O identificador Android é `io.github.vejacostela.newpipet`. Ele permite instalar esta versão ao lado do NewPipe original. A mudança de identificador não migra os dados automaticamente: use exportação e importação de backup.

## Desenvolvimento

Use JDK 21 e o Android SDK configurado em `buildSrc/src/main/kotlin/ProjectConfig.kt`. O extractor compila bytecode Java 17.

```sh
./gradlew assembleContinuous lintContinuous testDebugUnitTest -DskipFormatKtlint
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Altere parsers do YouTube em `extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/`. Acrescente testes para a mudança e atualize `extractor/VERSION`. Alterações na API do extractor precisam compilar junto com o aplicativo.

A camada de extração do aplicativo fica em `ExtractorHelper`; `SingleFlight` compartilha requisições simultâneas e impede que uma resposta antiga sobrescreva uma atualização forçada. O cache de streams dura no máximo cinco minutos, reduzidos conforme a expiração conhecida das URLs. Metadados de canais e playlists mantêm sua validade anterior.

## Assinatura e versões

A assinatura pertence ao mantenedor deste repositório. Não é possível usar a chave de outra equipe para atualizar este aplicativo. Guarde uma cópia privada da chave: a chave pública do APK instalado também verifica os metadados de atualização e os ajustes de compatibilidade.

Configure estes quatro secrets no GitHub (Settings > Secrets and variables > Actions), preferencialmente nos ambientes `newpipet-beta` e `newpipet-stable`:

| Secret | Conteúdo |
| --- | --- |
| `NEWPIPET_KEYSTORE_BASE64` | Keystore JKS ou PKCS12 em base64, com chave RSA |
| `NEWPIPET_STORE_PASSWORD` | Senha do keystore |
| `NEWPIPET_KEY_ALIAS` | Alias da chave |
| `NEWPIPET_KEY_PASSWORD` | Senha da chave privada |

Use a mesma chave nos canais beta e estável. Nunca inclua a chave privada, senhas ou o keystore em commits. Os workflows não geram uma chave substituta quando os secrets estão ausentes.

1. Incorpore a mudança revisada em `dev`.
2. Execute **Build signed NewPipeT release** selecionando `dev`, canal, uma tag nova e um `version_code` maior que todos os builds já distribuídos.
3. O workflow exige compilação, testes unitários e a verificação de reprodução. Uma falha impede o job de assinatura.
4. O job seguinte gera APK e metadados assinados; o job com permissão de escrita recebe somente esses artefatos.
5. Confira o rascunho da release e publique. O canal beta usa releases marcadas como pré-lançamento; o estável as ignora.

Cada release contém `NewPipeT.apk`, `newpipet-update.json` e `SHA256SUMS`. As tags e seus artefatos devem ser imutáveis. O aplicativo busca apenas releases do próprio repositório, verifica a assinatura dos metadados com a chave do APK instalado e oferece somente versões mais novas. A instalação é iniciada pelo usuário e o Android verifica a assinatura do APK; o aplicativo não instala APKs silenciosamente. O hash do arquivo está disponível para conferência adicional.

O canal padrão é estável. Usuários podem escolher beta em Configurações > Atualizações. Versões debug e continuous não recebem atualizações de produção.

## Ajustes de compatibilidade

O recurso é opcional e vem desativado. Quando habilitado, aproveita as verificações de atualização; não consulta servidores a cada vídeo. Não contém JavaScript, bibliotecas ou código executável.

O documento assinado fica no asset `compatibility.json` da release `compatibility` do próprio repositório. Seu conteúdo permite apenas controlar o número de novas tentativas de rede (0 a 3) e habilitar a renovação de URLs comprovadamente expiradas.

O workflow **Sign compatibility settings** gera um artefato assinado com validade de 48 horas. Execute-o em `dev`, revise o resultado e publique o asset na release indicada abaixo.

O formato de payload está em `config/compatibility.example.json`. Use uma revisão crescente, timestamps UTC em segundos, validade máxima de sete dias e intervalo de `version_code` compatível. `scripts/SignDocument.java` assina o payload usando a mesma chave de release. Configure `NEWPIPET_KEYSTORE` com o caminho local do keystore e os três secrets de senha/alias no ambiente local, sem incluí-los na linha de comando:

```sh
java scripts/SignDocument.java config/compatibility.json compatibility.json
```

Publique `compatibility.json` como asset da release `compatibility`, marcada como pré-lançamento para não aparecer como versão estável. A release de configuração não deve conter um asset `newpipet-update.json`. Assinaturas inválidas, revisões repetidas/anteriores e parâmetros fora dos limites são recusados. Após o vencimento, o aplicativo utiliza seus padrões. Desativar o recurso também restaura os padrões imediatamente.

Para reverter um ajuste, publique os valores anteriores em uma revisão nova; não diminua a revisão. Para reverter código, faça uma nova release com correção e `version_code` maior, preservando a compatibilidade do banco de dados.

## Monitoramento e testes reais

**YouTube compatibility** executa a cada doze horas, manualmente e nas PRs que afetam os componentes monitorados. Usa um emulador Android 35 e o mesmo resolvedor de mídia da aplicação. Verifica:

- busca por playlists, leitura da playlist e leitura de um canal;
- extração dos streams de um vídeo público de controle;
- renderização do primeiro quadro e avanço da saída de áudio, com volume zerado.

Os testes de rede são opt-in e ignorados no CI offline. Um teste ignorado não demonstra compatibilidade. O relatório do workflow diferencia exceções de extração, falhas HTTP, timeout e erros do decodificador. Um vídeo de controle removido ou limitação da rede do runner pode fazer a verificação falhar; é necessário verificar a causa antes de atribuir a falha ao extractor.

Se o relatório mostrar `SignInConfirmNotBotException` e `LOGIN_REQUIRED`, o YouTube exigiu confirmação de acesso naquela execução. Isso mantém o teste de reprodução reprovado e impede a assinatura de uma release. Não marque esse caso como sucesso nem remova o teste. Repita a verificação em um dispositivo ou runner de teste em que o acesso ao vídeo público esteja disponível.

Opcionalmente, configure a variável de repositório `NEWPIPET_PLAYBACK_RUNNER` com o rótulo de um runner Linux Android sob controle do mantenedor, com SDK e KVM disponíveis. Ela é usada pela validação de release e pelos testes manuais/agendados em `dev`. Pull requests continuam usando runners do GitHub. Sem essa variável, todos usam `ubuntu-latest`. Nenhum runner adicional é criado automaticamente.

Na validação de 10/09/2026, a busca, a playlist e o canal passaram no GitHub, mas a reprodução recebeu essa exigência de confirmação. Esse resultado não comprova reprodução funcional nem uma quebra do parser. A compilação e os testes JVM/Android são verificações separadas. A análise Android Lint conserva a configuração não bloqueante herdada; consulte também seu relatório antes de publicar.

No emulador ou dispositivo de teste:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.schabi.newpipe.compatibility.YouTubePlaybackTest \
  -Pandroid.testInstrumentationRunnerArguments.youtubeLiveTests=true -DskipFormatKtlint
```

Antes de promover uma beta, confira também troca de qualidade, retomada após perda de rede, pausa durante recuperação, reprodução em segundo plano, legendas, tamanho de fonte ampliado e TalkBack. Os testes novos não são um benchmark completo de bateria ou acessibilidade.

## Troubleshooting

Erros de rede têm até duas novas tentativas por vídeo, com pausas de um e dois segundos. Uma URL Googlevideo com expiração comprovada pode ser renovada uma vez; nem todo HTTP 403 é uma URL expirada. Erros permanentes não recebem esse tratamento. Pausa, troca de item e encerramento cancelam uma recuperação pendente.

A tela de diagnóstico guarda contadores somente na memória da sessão. Ela permite copiar os contadores ou zerá-los; não envia telemetria automaticamente. O relatório detalhado de falha exportado omite a requisição e as mensagens das exceções, preservando os tipos e as linhas de código. Revise o comentário que você digita antes de compartilhar.

Se os testes falharem após uma mudança externa, reproduza a falha, corrija o parser no extractor local, acrescente um teste e siga a sequência beta > validação > estável. O projeto não prevê alterações futuras arbitrárias do YouTube.
