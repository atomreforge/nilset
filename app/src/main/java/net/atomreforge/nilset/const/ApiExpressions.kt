package net.atomreforge.nilset.const

object ApiExpressions {
    object Endpoint {
        const val REGISTER = "api/v1/register"
        const val LOGIN = "api/v1/login"
        const val REFRESH_ACCESS_TOKEN = "api/v1/refresh-access-token"
        const val USER_ME = "api/v1/user/{username}/me"
        const val CALENDAR = "api/v1/user/{username}/calendar"
        const val SIGN_OUT = "api/v1/user/signout"
    }

    object Json {
        const val REGISTER_WAY = "registerway"
        const val LOGIN_WAY = "loginway"
        const val REGISTER_CODE = "registercode"
        const val MESSAGE = "message"
        const val SESSION = "session"
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val USER_UID = "uid"
        const val USER_REGISTER_TIME = "register_time"
        const val USER_ROLE = "role"
        const val USER_GITHUB_ID = "github_id"
        const val USER_GITHUB_LOGIN = "github_login"
        const val CALENDAR_ID = "calendar_id"
        const val RECORDS = "records"
        const val WEEKDAY = "weekday"
        const val START_MIN = "start_min"
        const val END_MIN = "end_min"
        const val TITLE = "title"
    }

    object Header {
        const val AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer"
    }
}
