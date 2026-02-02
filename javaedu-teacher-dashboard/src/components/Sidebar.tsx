import { NavLink } from 'react-router-dom';
import { logout } from '../api/client';

const navItems = [
  { path: '/', label: 'Dashboard', icon: '📊' },
  { path: '/courses', label: 'Courses', icon: '📚' },
  { path: '/exercises', label: 'Exercises', icon: '📝' },
  { path: '/students', label: 'Students', icon: '👥' },
  { path: '/analytics', label: 'Analytics', icon: '📈' },
];

export default function Sidebar() {
  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  return (
    <div className="w-64 bg-gray-900 text-white flex flex-col">
      <div className="p-6 border-b border-gray-800">
        <h1 className="text-xl font-bold">JavaEdu</h1>
        <p className="text-gray-400 text-sm">Teacher Dashboard</p>
      </div>

      <nav className="flex-1 p-4">
        <ul className="space-y-2">
          {navItems.map((item) => (
            <li key={item.path}>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-300 hover:bg-gray-800'
                  }`
                }
              >
                <span>{item.icon}</span>
                <span>{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="p-4 border-t border-gray-800">
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-gray-800 transition-colors"
        >
          <span>🚪</span>
          <span>Logout</span>
        </button>
      </div>
    </div>
  );
}
