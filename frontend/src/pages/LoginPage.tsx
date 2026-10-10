import { type FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function LoginPage() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState<string | null>(null)
    const [isSubmitting, setIsSubmitting] = useState(false)

    const from = location.state?.from?.pathName ?? '/'

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()

        setError(null)
        setIsSubmitting(true)

        try {
            await login({
                email,
                password,
            })

            navigate(from, { replace: true })
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Unable to log in',
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main>
            <h1>Log In</h1>
            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        required
                    />
                </div>

                <div>
                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        required
                    />
                </div>

                {error && <p>{error}</p>}

                <button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? 'Logging in...' : 'Log In'}
                </button>
            </form>

            <p>
                Don't have an account? <Link to="/register">Register</Link>
            </p>
        </main>
    )
}

export default LoginPage