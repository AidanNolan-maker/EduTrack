export interface RegisterRequest {
    firstName: string
    lastName: string
    email: string
    password: string
}

export interface UserResponse {
    id: string
    firstName: string
    lastName: string
    email: string
    role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'
}

export interface LoginRequest {
    email: string
    password: string
}

export interface LoginResponse {
    token: string
    userId: string
    firstName: string
    lastName: string
    email: string
    role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'
}

export async function register(
    request: RegisterRequest
): Promise<UserResponse> {
    const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
    })

    if (!response.ok) {
        const error = await response.json().catch(() => null)

        throw new Error(
            error?.error ?? `Registration failed: ${response.status}`,
        )
    }

    return response.json()
}

export async function login(
    request: LoginRequest,
): Promise<LoginResponse> {
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
    })

    if (!response.ok) {
        const error = await response.json().catch(() => null)

        throw new Error(
            error?.error ?? `Login failed: ${response.status}`,
        )
    }

    return response.json()
}
