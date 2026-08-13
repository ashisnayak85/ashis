import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user } = useAuth();

  return (
    <div>
      <h1>Profile</h1>
      <div className="card-panel">
        <p><strong>Username:</strong> {user?.username}</p>
        <p><strong>Roles:</strong> {user?.roles?.join(', ')}</p>
      </div>
    </div>
  );
}
