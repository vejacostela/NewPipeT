# Privacidade do NewPipeT

NewPipeT é um fork mantido em `vejacostela/NewPipeT`. O aplicativo acessa os serviços de vídeo selecionados, servidores de mídia e imagens para apresentar e reproduzir conteúdo. Esses serviços recebem as requisições de rede necessárias ao funcionamento.

Inscrições, histórico, configurações e downloads são armazenados localmente conforme as opções escolhidas pelo usuário. A mudança para o identificador próprio do NewPipeT exige importação de backup para trazer dados de outro aplicativo.

Se a busca de atualizações for habilitada, o aplicativo consulta a API do GitHub e os assets de releases em `vejacostela/NewPipeT`. Ajustes remotos de compatibilidade são opcionais, vêm desativados e usam exclusivamente assets assinados desse repositório. O GitHub recebe essas conexões de rede. Cookies de navegação dos serviços não são anexados pelo cliente de atualização.

Os novos contadores de reprodução ficam apenas na memória da sessão. Não há envio automático de telemetria, histórico ou relatórios de falha aos mantenedores. O usuário pode copiar um resumo sem URLs de vídeos. Nos relatórios detalhados exportados, a requisição e as mensagens de exceção são omitidas; o comentário digitado pelo usuário permanece e deve ser revisado antes do envio.

Os botões de suporte direcionam para as issues deste fork. Abrir ou publicar uma issue é uma ação do usuário e fica sujeito à visibilidade do GitHub. A opção de envio ao endereço de e-mail do projeto original foi retirada.

Créditos e links de licença identificam a origem de componentes; não significam transferência de propriedade ou envio de dados.
