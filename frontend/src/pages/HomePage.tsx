import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getProtectedData, type ProtectedResponse } from '../api/authenticatedApi'

function HomePage() {
    const { user, token, login, logout, isAuthenticated } = useAuth()
    const [protectedData, setProtectedData] =
        useState<ProtectedResponse | null>(null)
    const [error, setError] = useState<string | null>(null)

    async function handleLogin() {
        try {
            setError(null)

            await login({
                email: 'http.login@example.com',
                password: 'password123',
            })
        } catch (error) {
            setError(
                error instanceof Error ? error.message : 'Login failed',
            )
        }
    }

    async function handleProtectedRequest() {
        if (!token) {
            return
        }

        try {
            setError(null)

            const data = await getProtectedData(token)
            setProtectedData(data)
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Protected request failed',
            )
        }
    }

    return (
        <main>
            <h1>EduTrack Authentication Test</h1>

            {!isAuthenticated ? (
                <button onClick={handleLogin}>
                    Log In
                </button>
            ) : (
                <>
                    <p>
                        Logged in as {user?.firstName} {user?.lastName}
                    </p>

                    <p>Email: {user?.email}</p>
                    <p>Role: {user?.role}</p>

                    <button onClick={handleProtectedRequest}>
                        Test Protected Endpoint
                    </button>

                    <button onClick={logout}>
                        Log Out
                    </button>
                </>
            )}

            {protectedData && (
                <section>
                    <h2>Protected Response</h2>
                    <p>{protectedData.message}</p>
                    <p>Email: {protectedData.email}</p>
                    <p>Role: {protectedData.role}</p>
                    <p>User ID: {protectedData.userId}</p>
                </section>
            )}

            {error && (
                <p>
                    Error: {error}
                </p>
            )}
        </main>
    )
}

export default HomePage