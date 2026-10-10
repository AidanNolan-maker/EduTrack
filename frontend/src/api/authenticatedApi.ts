export interface ProtectedResponse {
    message: string
    userId: string
    email: string
    role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'
}

export async function getProtectedData(
    token: string,
): Promise<ProtectedResponse> {
    const response = await fetch('/api/test/protected', {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    })

    if (!response.ok) {
        throw new Error(`Protected request failed: ${response.status}`)
    }

    return response.json()
}