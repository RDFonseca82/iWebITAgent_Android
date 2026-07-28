# Identidade permanente do agente Android

Desde a versão 4.3, o `UniqueID` determinístico já não contém informação de IP, Wi-Fi ou outra rede. A versão 4.5 corrige a migração para nunca substituir um `UniqueID` válido que já esteja associado à plataforma.

## Ordem de escolha na versão 4.5

1. `UniqueID` existente no DataStore;
2. `UniqueID` existente no backup crítico/Android Backup;
3. identificador determinístico calculado localmente, apenas quando os anteriores não existem.

O mesmo valor é usado no login, no inventário inicial, nas sincronizações completas, mínimas, de localização e nos pedidos de suporte.

## Como é calculado para uma instalação nova

1. O agente lê o `ANDROID_ID` do dispositivo.
2. Combina-o com o certificado usado para assinar a aplicação.
3. Aplica SHA-256 com um namespace próprio do agente.
4. O resultado volta a ser derivado por SHA-256 para produzir o `UniqueID` enviado à plataforma.

Com o mesmo dispositivo, utilizador/perfil Android, package name e certificado de assinatura, o valor mantém-se após reinícios, alterações de IP/rede, atualizações e reinstalações.

O Android pode fornecer outra identidade depois de uma reposição de fábrica, mudança de utilizador/perfil ou mudança da chave de assinatura. Uma aplicação Android normal não dispõe de um identificador que sobreviva tecnicamente a todas essas situações; nesses casos, a recuperação absoluta exige que a plataforma mantenha um vínculo próprio ao equipamento.

## Proteção e recuperação

A cópia redundante passa a guardar:

- `UniqueID`;
- `IDSync`;
- empresa;
- tempos de sincronização.

O DataStore e o backup crítico estão incluídos nas regras de Android Backup e transferência de dispositivo. Se o DataStore estiver vazio ou corrompido, o agente repõe os valores a partir da cópia redundante.

Se ambos desaparecerem, o agente calcula o identificador determinístico e consulta `https://agent.iwebit.app/` para recuperar uma associação que a plataforma já conheça.

O logout limpa a associação à empresa, mas mantém o `UniqueID`. A recuperação automática fica desativada até existir um novo login válido.

## Migração de instalações antigas

A versão 4.5 mantém qualquer `UniqueID` SHA-256 já guardado, em vez de o substituir silenciosamente. Isto preserva a chave usada pela plataforma em atualizações diretas de versões antigas.

Se a versão 4.3 ou 4.4 já tiver substituído o identificador antigo e não existir backup desse valor, o identificador anterior não pode ser reconstruído. Nesse equipamento, o hash atual fica estável definitivamente a partir da 4.5 e poderá ser necessário associá-lo uma vez na plataforma.