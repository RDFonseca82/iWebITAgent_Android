# Auto-update pelo GitHub

O agente consulta, no arranque e depois a cada 6 horas, o manifesto publicado em:

`https://github.com/RDFonseca82/iWebITAgent_Android/releases/download/auto-update/update.json`

Quando encontra um `versionCode` superior ao instalado, descarrega o APK, confirma o tamanho e o SHA-256, valida o package name e exige que o certificado seja igual ao da aplicação instalada. Depois entrega o APK ao `PackageInstaller`: no Android 12 ou superior pede instalação sem ação do utilizador; se o sistema exigir confirmação, apresenta uma notificação de fallback.

## Requisitos

1. O repositório tem de ser público. Releases privadas exigem autenticação e nunca se deve incorporar um token GitHub no APK.
2. Todas as compilações têm de usar permanentemente a mesma keystore e o mesmo alias.
3. No primeiro uso, o Android pedirá autorização para esta aplicação instalar APKs de origem externa.
4. No Android 12 ou superior, o agente declara `UPDATE_PACKAGES_WITHOUT_USER_ACTION` e tenta atualizar-se silenciosamente. O fabricante, a política de segurança ou o Play Protect podem ainda exigir confirmação; o agente trata `STATUS_PENDING_USER_ACTION` e apresenta uma notificação. No Android 8 a 11, a confirmação continua obrigatória salvo em dispositivos Device Owner/MDM.

## GitHub Actions Secrets

Em `Settings > Secrets and variables > Actions`, criar:

- `ANDROID_KEYSTORE_BASE64`: conteúdo Base64 completo da keystore.
- `ANDROID_KEYSTORE_PASSWORD`: password da keystore.
- `ANDROID_KEY_ALIAS`: alias da chave.
- `ANDROID_KEY_PASSWORD`: password da chave.

No PowerShell, gerar o valor Base64 sem alterar a keystore:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks")) | Set-Clipboard
```

A keystore e as passwords não devem ser publicadas no repositório. Se esta keystore já esteve num repositório público, deve ser considerada comprometida e substituída antes da distribuição em produção.

## Publicação

Cada push para `master`, ou execução manual do workflow, faz testes, lint e `assembleRelease`. O workflow atribui um `versionCode` sempre crescente, assina o APK e atualiza a release fixa `auto-update` com:

- `iwebit-agent.apk`
- `update.json`

O APK é carregado antes do manifesto para que os agentes nunca recebam um manifesto novo apontado temporariamente para um APK antigo.

## Primeira migração

Uma aplicação só pode atualizar outra com a mesma assinatura. Se os APKs atualmente instalados foram assinados por uma chave debug diferente, é necessário desinstalá-los e instalar uma vez o primeiro APK desta release de produção. A partir daí, o auto-update mantém a mesma assinatura.