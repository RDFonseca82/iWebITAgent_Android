<?php
declare(strict_types=1);

/*
 * Exemplo para /scripts/script_android.php
 *
 * O agente envia:
 *   Content-Type: application/x-www-form-urlencoded
 *   json=<objeto JSON codificado>
 *
 * TypeSync:
 *   1 = sincronização completa
 *   2 = sincronização minimal
 *   ausente = atualização de localização (formato legado)
 */

header('Content-Type: application/json; charset=utf-8');

function respond(int $httpStatus, array $body): void
{
    http_response_code($httpStatus);
    echo json_encode($body, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit; // Impede que o template HTML seja acrescentado à resposta.
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respond(405, ['success' => false, 'error' => 'Método não permitido']);
}

$rawJson = $_POST['json'] ?? '';
if (!is_string($rawJson) || $rawJson === '') {
    respond(400, ['success' => false, 'error' => 'Campo POST json em falta']);
}

try {
    $data = json_decode($rawJson, true, 512, JSON_THROW_ON_ERROR);
} catch (JsonException $exception) {
    respond(400, [
        'success' => false,
        'error' => 'JSON inválido',
        'detail' => $exception->getMessage(),
    ]);
}

if (!is_array($data)) {
    respond(400, ['success' => false, 'error' => 'O JSON não contém um objeto']);
}

$uniqueId = (string)($data['UniqueId'] ?? '');
$idSync = (string)($data['idsync'] ?? '');
$typeSync = array_key_exists('TypeSync', $data)
    ? (int)$data['TypeSync']
    : null;

if ($uniqueId === '' || $idSync === '') {
    respond(422, [
        'success' => false,
        'error' => 'UniqueId ou idsync em falta',
    ]);
}

/*
 * IMPORTANTE:
 * Não fazer UPDATE de todas as colunas em todos os pedidos.
 * O payload minimal não contém inventário, diagnóstico de rede ou dados Android
 * completos. Atualizar campos ausentes para NULL apagaria os dados recebidos
 * anteriormente numa sincronização completa.
 */
switch ($typeSync) {
    case 1:
        // Campos escalares da sincronização completa.
        $version = (string)($data['version'] ?? '');
        $model = (string)($data['model'] ?? '');
        $manufacturer = (string)($data['manufacturer'] ?? '');
        $androidRelease = (string)($data['release'] ?? '');
        $securityPatch = (string)($data['securityPatch'] ?? '');
        $ipAddress = (string)($data['ipAddress'] ?? '');

        // Objetos/listas novos. Guardar como JSON ou em tabelas relacionadas.
        $health = $data['health'] ?? [];
        $networkDiagnostics = $data['networkDiagnostics'] ?? [];
        $advancedApplications = $data['advancedApplications'] ?? [];
        $applicationInventorySummary = $data['applicationInventorySummary'] ?? [];
        $incidents = $data['incidents'] ?? [];
        $supportBundle = $data['supportBundle'] ?? null;

        // Compatibilidade com o inventário antigo.
        $androidProduct = $data['AndroidProduct'] ?? [];

        /*
         * Executar aqui o UPDATE completo e os UPSERT das tabelas relacionadas.
         * Ao usar PDO, preparar os parâmetros; não concatenar valores em SQL.
         */
        break;

    case 2:
        // A sincronização minimal só atualiza estes campos e saúde/incidências.
        $currentTime = (string)($data['currentTime'] ?? '');
        $ipAddress = (string)($data['ipAddress'] ?? '');
        $freeBytes = (string)($data['freeBytes'] ?? '');
        $availMem = (string)($data['availMem'] ?? '');
        $batteryCapacity = (string)($data['batteryCapacity'] ?? '');
        $health = $data['health'] ?? [];
        $incidents = $data['incidents'] ?? [];

        // Não limpar os campos exclusivos da sincronização completa.
        break;

    case null:
        // Payload legado de localização.
        $latitude = (string)($data['latitude'] ?? '');
        $longitude = (string)($data['longitude'] ?? '');
        $currentTime = (string)($data['currentTime'] ?? '');
        break;

    default:
        respond(422, [
            'success' => false,
            'error' => 'TypeSync desconhecido',
            'TypeSync' => $typeSync,
        ]);
}

respond(200, [
    'success' => true,
    'UniqueId' => $uniqueId,
    'idsync' => $idSync,
    'TypeSync' => $typeSync,
    'receivedFields' => array_keys($data),
]);
