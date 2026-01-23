package data.config

/**
 * Centralized application configuration.
 * Single source of truth for base URL and other settings.
 */
object AppConfig {
    /**
     * Backend API base URL.
     * All API services should use this instead of hardcoding URLs.
     */
    const val BASE_URL: String = "http://localhost:8080"
    
    /**
     * API request timeout in milliseconds
     */
    const val REQUEST_TIMEOUT_MS: Long = 15000
    
    /**
     * API connect timeout in milliseconds
     */
    const val CONNECT_TIMEOUT_MS: Long = 15000
}
