import { useAuth } from '../context/AuthContext'

function DashboardPage() {
    const { user, logout } = useAuth()

    return (
        <main>
            <h1>EduTrack Dashboard</h1>

            <p>
                Welcome, {user?.firstName} {user?.lastName}!
            </p>

            <p>Email: {user?.email}</p>
            <p>Role: {user?.role}</p>

            <button onClick={logout}>
                Log Out
            </button>
        </main>
    )
}

export default DashboardPage