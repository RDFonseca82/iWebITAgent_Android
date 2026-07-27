# Identidade permanente do agente Android

Desde a versão 4.3, o `UniqueID` já não é criado a partir de um UUID aleatório nem contém informação de IP, Wi-Fi ou outra rede.

## Como é calculado

1. O agente lê o `ANDROID_ID` do dispositivo.
2. Combina-o com o certificado usado para assinar a aplicação.
3. Aplica SHA-256 com um namespace próprio do agente.
4. O resultado volta a ser derivado por SHA-256 para produzir o `UniqueID` enviado à plataforma.

Com o mesmo dispositivo, utilizador/perfil Android, package name e certificado de assinatura, o valor mantém-se após reinícios, alterações de IP/rede, atualizações e reinstalações.

O Android pode fornecer outra identidade depois de uma reposição de fábrica, mudança de utilizador/perfil ou mudança da chave de assinatura. Uma aplicação Android normal não dispõe de um identificador que sobreviva legal e tecnicamente a todas essas situações.

## Proteção do IDSync

Os dados de associação essenciais são guardados no DataStore e num segundo ficheiro de preferências:

- `IDSync`
- empresa
- tempos de sincronização

Se o DataStore estiver vazio ou corrompido, o agente repõe automaticamente estes valores a partir da cópia redundante. Estes dois ficheiros também estão incluídos nas regras de Android Backup e transferência de dispositivo.

Se ambos os ficheiros tiverem desaparecido, por exemplo depois de uma reinstalação sem restauro de backup, o agente consulta `https://agent.iwebit.app/` com o `UniqueID` permanente e recupera a associação já conhecida pela plataforma.

O comando explícito para terminar sessão desativa esta recuperação automática até existir um novo login válido.

## Migração da versão anterior

Na primeira execução da versão 4.3, instalações já associadas:

1. preservam o `IDSync`;
2. substituem o antigo `UniqueID` aleatório pelo identificador permanente;
3. enviam o novo valor na sincronização seguinte.

Convém deixar o agente efetuar pelo menos uma sincronização depois da atualização para que a plataforma conheça a nova identidade antes de testar uma desinstalação/reinstalação.
