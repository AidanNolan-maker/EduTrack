import {
    createContext,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from 'react'
import {
    login as loginApi,
    type LoginRequest,
    type LoginResponse,
} from '../api/authApi'

interface AuthUser {
    id: string
    firstName: string
    lastName: string
    email: string
    role: LoginResponse['role']
}

interface AuthContextValue {
    user: AuthUser | null
    token: string | null
    isAuthenticated: boolean
    login: (request: LoginRequest) => Promise<void>
    logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const TOKEN_KEY = 'edutrack_token'
const USER_KEY = 'edutrack_user'

interface AuthProviderProps {
    children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
    const [token, setToken] = useState<string | null>(() =>
        localStorage.getItem(TOKEN_KEY),
    )

    const [user, setUser] = useState<AuthUser | null>(() => {
        const storedUser = localStorage.getItem(USER_KEY)

        if (!storedUser) {
            return null
        }

        try {
            return JSON.parse(storedUser)as AuthUser
        } catch {
            localStorage.removeItem(USER_KEY)
            return null
        }
    })

    useEffect(() => {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token)
        } else {
            localStorage.removeItem(TOKEN_KEY)
        }
    }, [token])

    useEffect(() => {
        if (user) {
            localStorage.setItem(USER_KEY, JSON.stringify(user))
        } else {
            localStorage.removeItem(USER_KEY)
        }
    }, [user])

    async function login(request: LoginRequest) {
        const response = await loginApi(request)

        const authenticatedUser: AuthUser = {
            id: response.userId,
            firstName: response.firstName,
            lastName: response.lastName,
            email: response.email,
            role: response.role,
        }

        setToken(response.token)
        setUser(authenticatedUser)
    }

    function logout() {
        setToken(null)
        setUser(null)
    }

    return (
        <AuthContext.Provider
            value={{
                user,
                token,
                isAuthenticated: token !== null && user !== null,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext)

    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider')
    }

    return context
}