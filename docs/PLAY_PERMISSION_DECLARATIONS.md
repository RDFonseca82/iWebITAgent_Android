# Rascunhos para declarações de permissões

Os textos devem corresponder exatamente ao comportamento mostrado nos vídeos e à ficha da loja.

## QUERY_ALL_PACKAGES

**Funcionalidade principal**

O iWebIT Agent é um agente de inventário, monitorização e gestão de dispositivos Android empresariais. Uma das suas funções centrais é criar e manter na plataforma iWebIT um inventário completo das aplicações instaladas, incluindo package name, nome, versão, datas de instalação/atualização, estado, origem, SDK alvo e permissões perigosas pedidas/concedidas.

**Por que não é suficiente uma alternativa menos abrangente**

O conjunto de aplicações varia entre dispositivos e organizações e não é conhecido antecipadamente. Uma lista fixa de packages ou queries específicas produziria um inventário incompleto e impediria a deteção de software não autorizado, desatualizado ou desativado. A informação não é usada para publicidade nem vendida.

**Instruções de revisão**

1. Associar a app com o `IDSync` de teste fornecido em App access.
2. Executar uma sincronização completa.
3. Abrir a plataforma de teste.
4. Consultar o inventário de aplicações do dispositivo associado.

## Localização em segundo plano

**Finalidade**

Permitir que uma organização autorizada localize e proteja um dispositivo Android empresarial associado à plataforma iWebIT, incluindo quando a aplicação está fechada ou não está a ser utilizada.

**Benefício para o utilizador**

Localização de equipamentos perdidos ou deslocados, confirmação do dispositivo gerido e apoio técnico/operacional.

**Consentimento**

Antes do pedido Android, a app apresenta uma explicação própria com a palavra “localização”, indica que a recolha pode ocorrer quando a app está fechada ou não está a ser usada, identifica a plataforma iWebIT, explica a finalidade e permite recusar. No Android 11+, primeiro é concedida localização normal e depois a app explica como selecionar “Permitir sempre” nas definições.

**Instruções de revisão**

1. Associar a app com o `IDSync` de teste.
2. No ecrã de explicação, escolher Continuar.
3. Conceder localização.
4. Escolher Permitir sempre nas definições apresentadas.
5. Solicitar/consultar a localização na plataforma de teste.

## Foreground service — location

O serviço de foreground obtém uma localização atual quando existe um pedido de localização para um dispositivo associado. Mantém uma notificação visível enquanto a operação decorre, envia o resultado por HTTPS para a plataforma e termina imediatamente depois do resultado ou erro.

## Foreground service — mediaProjection

O serviço é usado para criar uma captura de ecrã destinada a suporte técnico. Cada sessão apresenta o diálogo oficial de consentimento MediaProjection do Android. Sem aceitação do utilizador não é criada nem enviada qualquer captura. Durante a captura existe uma notificação de foreground e o serviço termina depois de guardar/enviar o resultado.

## REQUEST_DELETE_PACKAGES

A plataforma pode apresentar ao dispositivo uma lista de aplicações cuja remoção foi recomendada por um administrador autorizado. A app abre sempre o diálogo oficial `ACTION_DELETE` do Android; a remoção só acontece após confirmação explícita no dispositivo. Não existe desinstalação silenciosa.

