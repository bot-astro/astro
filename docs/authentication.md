# Authentication

Astro authenticates users through Discord OAuth 2.

The API keeps Discord tokens and sessions server-side, the browser receives only an HTTP-only session cookie.

## Sign in

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant API as Central API
    participant Discord

    User->>Browser: Sign in with Discord
    Browser->>API: Start authentication
    API-->>Browser: Redirect to Discord
    Browser->>Discord: Authorize Astro
    Discord-->>API: OAuth callback
    API->>Discord: Exchange code and fetch identity
    API->>API: Create authenticated session
    API-->>Browser: Set session cookie and redirect to Astro
```

The same flow can optionally include installing the Astro bot in a Discord
server.

## Authenticated requests

```mermaid
sequenceDiagram
    participant Browser
    participant API as Central API
    participant Endpoint as Protected endpoint

    Browser->>API: Request with session cookie
    API->>API: Validate session and credentials
    opt Discord token has expired
        API->>API: Refresh Discord credentials
    end
    API->>Endpoint: Continue as authenticated user
    Endpoint-->>Browser: Response
```

The current-user endpoint returns the corresponding Astro user. If that user
does not exist yet, Astro creates it on first access.
