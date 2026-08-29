import { NavLink, useNavigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { logout } from '../store/authSlice'

export default function Navbar() {
  const dispatch = useDispatch()
  const navigate = useNavigate()

  const handleLogout = () => {
    dispatch(logout())
    navigate('/login')
  }

  return (
    <nav className="nav">
      <div className="nav-logo">Intensify<span>.</span></div>
      <div className="nav-links">
        <NavLink to="/dashboard" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>Dashboard</NavLink>
        <NavLink to="/questions" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>Practice</NavLink>
        <NavLink to="/history" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>History</NavLink>
        <NavLink to="/progress" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>Progress</NavLink>
      </div>
      <div className="nav-actions">
        <button className="btn btn-ghost btn-sm" onClick={handleLogout}>Sign out</button>
      </div>
    </nav>
  )
}
