# Background remoto

O agente consulta o endpoint já existente durante a sincronização mínima:

`GET https://agent.iwebit.app/scripts/script_api.php?UniqueID=<UniqueID>`

O JSON devolvido pode incluir estes dois campos opcionais:

```json
{
  "SetBackground": 1,
  "BackgroundImage": "https://agent.iwebit.app/backgrounds/empresa.jpg"
}
```

- `SetBackground = 1`: descarrega e aplica a imagem no ecrã principal do Android.
- `SetBackground = 0`: não altera o background e limpa o estado interno do último comando.
- `BackgroundImage`: aceita um URL HTTPS completo ou um caminho relativo, por exemplo `/backgrounds/empresa.jpg`.

A imagem é aplicada em fullscreen com recorte central, sem deformação. O agente limita o download a 10 MB, rejeita HTTP e imagens inválidas, e regista sucesso ou erro nos logs de diagnóstico. O ecrã de bloqueio não é alterado.

O agente evita voltar a descarregar a mesma imagem. Se o utilizador alterar manualmente o background enquanto `SetBackground` continua em `1`, o agente repõe o background remoto na sincronização mínima seguinte. Para publicar conteúdo novo no mesmo ficheiro, altere o URL, por exemplo:

`https://agent.iwebit.app/backgrounds/empresa.jpg?v=2`

A consulta é feita pelo ciclo de sincronização mínima (normalmente a cada 15 minutos; o Android pode atrasá-la em modo Doze). Uma resposta antiga sem estes campos continua compatível: assume `SetBackground = 0`.

Exemplo PHP:

```php
echo json_encode([
    // ...campos já existentes...
    'SetBackground' => 1,
    'BackgroundImage' => 'https://agent.iwebit.app/backgrounds/empresa.jpg'
]);
```
