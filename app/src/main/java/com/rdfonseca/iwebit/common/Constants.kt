package com.rdfonseca.iwebit.common

object Constants {
    const val USER_PREFERENCES_NAME = "user_preferences"
    const val SITE_URL = "https://www.iwebit.app/"
    const val PLATAFORM_URL = "https://intranet.iwebit.app/login.php"
    const val BASE_URL = "https://agent.iwebit.app/"
    const val DOMAIN_URL = "https://agent.iwebit.app/"
    const val PRIVACY_POLICY_URL = "https://www.iwebit.app/privacypolicy.php"
    const val PLAYSTORE_APP_URL = "https://play.google.com/store/apps/details?id=rdfonseca.iwebit"
    const val LOG_FILE_NAME = "log.txt"

    const val SYSTRAY_NOTIFICATION_ID = 1
    const val SCREENSHOT_NOTIFICATION_ID = 2
    const val SCREENSHOT_REQUESTED_NOTIFICATION_ID = 21
    const val SCREENSHOT_REQUESTED_DONE_NOTIFICATION_ID = 22
    const val REMOTE_MESSAGE_NOTIFICATION_ID = 3
    const val FORCE_FULL_SYNC_DONE_NOTIFICATION_ID = 4
    const val UNINSTALL_APPS_NOTIFICATION_ID = 5
    const val WARNING_NOTIFICATION_ID = 6
    const val APP_UPDATE_NOTIFICATION_ID = 7
    const val CHANNEL_ID = "channel1"
    const val CHANNEL_NAME = "IWebITTrayChannel"

    const val HTTP_EXCEPTION_MESSAGE = "Oops, algo deu errado."
    const val SOCKET_TIMEOUT_EXCEPTION_MESSAGE = "Não foi possível estabelecer ligação."
    const val IO_EXCEPTION_MESSAGE = "Rede não disponível."
    const val NULL_POINTER_EXCEPTION_MESSAGE = "Este ID não foi encontrado."
    const val LOCATION_EXCEPTION_MESSAGE = "Erro ao realizar sincronização, captura de localização não permitida ou desligada."

    const val ACTION_ASK_AND_CAPTURE_SCREEN = "ACTION_ASK_AND_CAPTURE_SCREEN"
    const val ACTION_REQUESTED_ASK_AND_CAPTURE_SCREEN = "ACTION_REQUESTED_ASK_AND_CAPTURE_SCREEN"
    const val ACTION_CAPTURED_SCREEN = "ACTION_CAPTURED_SCREEN"
    const val ACTION_CAPTURED_REQUESTED_SCREEN = "ACTION_CAPTURED_REQUESTED_SCREEN"
    const val ACTION_FORCE_FULL_SYNC = "ACTION_FORCE_FULL_SYNC"
    const val ACTION_TRY_AGAIN_LOCATION = "ACTION_TRY_AGAIN_LOCATION"
    const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
    const val ACTION_UPDATE_APPS_TO_UNINSTALL = "ACTION_UPDATE_APPS_TO_UNINSTALL"

    const val LOCATION_DATA_EXTRA = "LOCATION_DATA_EXTRA"
    const val APPLICATIONS_DATA_EXTRA = "APPLICATIONS_DATA_EXTRA"
    const val GERENCIADOR_REQUEST_EXTRA = "GERENCIADOR_REQUEST_EXTRA"
    const val FORCE_FULL_EXTRA = "FORCE_FULL_EXTRA"
    const val SUPPORT_BUNDLE_EXTRA = "SUPPORT_BUNDLE_EXTRA"

    const val WORKER_FULL_SYNC_TAG = "FullSyncW»"
    const val WORKER_FORCE_SYNC_TAG = "ForceSyncW»"
    const val WORKER_MIN_SYNC_TAG = "MinSyncW»"
    const val WORKER_LOCATION_SYNC_TAG = "LocationSyncW»"
    const val WORKER_SUPPORT_BUNDLE_TAG = "SupportBundleSyncW"
    const val WORKER_APP_UPDATE_TAG = "AppUpdateW"

}