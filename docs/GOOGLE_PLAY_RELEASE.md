# Publicação do iWebIT Agent na Google Play

## Artefacto correto

- Package name da app Play: `app.iwebit.agent`
- Variante Gradle: `play`
- Artefacto obrigatório para uma app nova: Android App Bundle (`.aab`)
- Tarefa local: `./gradlew bundlePlayRelease`
- Saída: `app/build/outputs/bundle/playRelease/app-play-release.aab`

O APK/AAB `enterprise` mantém o package `com.rdfonseca.iwebit` e o autoupdate GitHub. Não deve ser enviado para a app `app.iwebit.agent` na Play Console.

A variante `play`:

- não declara `REQUEST_INSTALL_PACKAGES`;
- não agenda nem apresenta o autoupdate GitHub;
- não expõe `UpdateInstallActivity` nem o `FileProvider` do instalador;
- recebe atualizações exclusivamente através da Google Play.

## Gerar o AAB assinado no GitHub

1. Fazer commit e push.
2. Abrir **Actions > Gerar AAB para Google Play**.
3. Escolher **Run workflow**.
4. Quando terminar, descarregar o artefacto `iWebIT-Agent-Google-Play-...`.
5. Extrair o ZIP e enviar o ficheiro `.aab` para a Play Console.

O workflow usa estes Secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Nunca enviar um bundle assinado com `CN=Android Debug`. Para uma app nova, ativar Play App Signing. O AAB é assinado com a upload key; a Google assina os APKs entregues aos utilizadores com a app signing key.

## Ordem recomendada na Play Console

1. Confirmar que o package reservado é `app.iwebit.agent`.
2. Preencher **Main store listing** usando `PLAY_STORE_LISTING.pt-PT.md`.
3. Adicionar email de suporte, website e política de privacidade.
4. Em **App access**, indicar que partes da app estão restritas e fornecer um `IDSync` de teste válido, permanente e sem autenticação adicional.
5. Declarar **Ads: No**, se continuar sem SDKs de publicidade.
6. Preencher **Data safety** usando `PLAY_DATA_SAFETY.md` como base e confirmar os dados com o responsável de privacidade.
7. Preencher **Target audience and content**, classificação etária e **News apps**.
8. Preencher as declarações de permissões:
   - localização em segundo plano;
   - `QUERY_ALL_PACKAGES`;
   - foreground service `location`;
   - foreground service `mediaProjection`.
9. Anexar vídeos públicos ou não listados que mostrem os fluxos completos pedidos nos formulários.
10. Criar primeiro uma release em **Internal testing** e analisar o Pre-launch report.
11. Depois avançar para Closed testing ou Production, conforme o tipo e antiguidade da conta.

## Vídeos para revisão

### Localização em segundo plano

Mostrar sem cortes:

1. abertura da app;
2. explicação visível de localização;
3. escolha **Continuar**;
4. concessão de localização aproximada/precisa;
5. instruções para **Permitir sempre**;
6. mudança da opção nas definições Android;
7. localização recebida na plataforma iWebIT.

### Media projection

Mostrar:

1. pedido de captura;
2. ecrã de consentimento oficial do Android;
3. notificação do foreground service;
4. captura enviada;
5. resultado apresentado na plataforma.

### QUERY_ALL_PACKAGES

Mostrar o inventário de aplicações na plataforma e explicar que a gestão/inventário de todas as aplicações instaladas é uma função central do agente empresarial.

## Bloqueios que exigem ação externa

- A política de privacidade publicada precisa de mencionar expressamente inventário de aplicações, identificadores do dispositivo, informação técnica/diagnóstico, mensagens de suporte, localização em segundo plano e capturas de ecrã.
- A aprovação de `QUERY_ALL_PACKAGES` e localização em segundo plano depende da revisão da Google. As mesmas funcionalidades devem estar claramente descritas na ficha da loja.
- Se a conta for pessoal e tiver sido criada depois de 13 de novembro de 2023, pode ser necessário manter pelo menos 12 testers num closed test durante 14 dias consecutivos antes de pedir acesso a produção.

## Checklist técnico já verificado

- `compileSdk` e `targetSdk`: API 36
- Android 16: suportado
- Package Play: `app.iwebit.agent`
- `versionCode`: 403 local; o GitHub gera um código crescente
- `versionName`: 4.3 local; o GitHub acrescenta execução/tentativa
- AAB abaixo dos limites da Play
- Bibliotecas nativas alinhadas para páginas de memória de 16 KB
- Lint Play sem erros
- Testes unitários das duas variantes sem falhas

