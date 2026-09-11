# NewPipeT

Cliente de streaming livre, baseado no NewPipe, mantido em **vejacostela/NewPipeT**.

O NewPipeExtractor está em [`extractor/`](extractor) e é compilado junto com o aplicativo. A versão do extractor não é baixada do repositório de outra conta. A biblioteca inicial e seus autores continuam creditados em [`extractor/UPSTREAM.md`](extractor/UPSTREAM.md); as licenças originais são preservadas.

## Melhorias deste fork

- Identificador Android próprio: `io.github.vejacostela.newpipet`.
- Atualizações estáveis e beta pelo próprio GitHub, com metadados assinados pela chave do APK instalado.
- Ajustes remotos de compatibilidade opcionais, assinados, limitados e com validade.
- Recuperação limitada para falhas de rede e endereços de reprodução comprovadamente expirados.
- Compartilhamento de extrações simultâneas e validade de cache limitada pela expiração do stream.
- Diagnóstico local sem histórico ou URLs dos vídeos no relatório resumido.
- Testes de busca, canal, playlist, vídeo decodificado e avanço do áudio.

## Compilar

Use JDK 21 e o Android SDK indicado em `buildSrc/src/main/kotlin/ProjectConfig.kt`.

```sh
./gradlew assembleContinuous testDebugUnitTest -DskipFormatKtlint
python3 -m unittest discover -s scripts -p 'test_*.py'
```

A integração contínua gera um APK de teste. Versões de teste possuem um identificador separado e não recebem atualizações de produção.

## Distribuição e manutenção

Veja [operação do NewPipeT](doc/NEWPIPET.md) e [privacidade](doc/PRIVACY.md). A publicação exige a chave de assinatura do mantenedor, configurada nos secrets do próprio repositório. O workflow de release cria um rascunho para conferência.

O aplicativo original e este fork possuem identificadores diferentes. Para transferir dados, exporte um backup no aplicativo anterior e importe pelo menu de backup do NewPipeT.

## Support

Relate problemas nas [issues deste projeto](https://github.com/vejacostela/NewPipeT/issues). Não há envio automático de relatórios a mantenedores do projeto original.

## Créditos e licença

Derivado de NewPipe e NewPipeExtractor, de TeamNewPipe e seus contribuidores. Este fork não é uma distribuição oficial da equipe original. Código sob [GPL-3.0-or-later](LICENSE); componentes de terceiros mantêm suas próprias licenças.
