# Agendamento das sincronizações

## Sincronização manual

O botão **Sincronizar agora**, no ecrã de diagnóstico, agenda sempre uma sincronização completa forçada (`FullSyncWorker`). Se já existir uma sincronização manual em curso, o pedido anterior é substituído pelo mais recente.

## Arranque, reinício e atualização

O agente agenda uma cadeia única de trabalhos:

1. sincronização minimal (`MinSyncWorker`);
2. sincronização completa (`FullSyncWorker`), iniciada apenas depois de a minimal terminar com sucesso.

A cadeia é agendada:

- no primeiro arranque da aplicação após a instalação;
- depois de o utilizador concluir a associação/login, caso a instalação ainda não tivesse `IDSync`;
- após `BOOT_COMPLETED`;
- após `MY_PACKAGE_REPLACED`, quando a aplicação é atualizada.

Os dois trabalhos exigem ligação à Internet e usam repetição com espera exponencial. O nome único `InitialSyncW` e a política `KEEP` impedem que o arranque da aplicação e o broadcast do sistema criem cadeias simultâneas.

> O Android não permite que uma aplicação normal se execute automaticamente no instante exato da primeira instalação. Por isso, numa instalação nova, a cadeia começa no primeiro arranque da aplicação (e volta a ser pedida depois da associação/login).

## Sincronizações periódicas

Além da cadeia inicial, continuam ativas:

- sincronização minimal a cada 15 minutos;
- sincronização completa a cada 1 hora.
