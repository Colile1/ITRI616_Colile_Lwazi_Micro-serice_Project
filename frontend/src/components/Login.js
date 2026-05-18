// Login.js : login form component with input validation and XSS prevention
import React, { useState } from 'react';
import { authApi } from '../api/api';
import { useAuth } from '../context/AuthContext';

// Pure function: validates login input fields client-side
const validateLoginForm = (username, password) => {
  if (!username || username.trim().length < 3) return 'Username must be at least 3 characters';
  if (!password || password.length < 6) return 'Password must be at least 6 characters';
  if (/<script|javascript:|on\w+=/i.test(username)) return 'Invalid characters in username';
  return null;
};

export default function Login() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const validationError = validateLoginForm(username, password);
    if (validationError) { setError(validationError); return; }

    setLoading(true);
    try {
      const res = await authApi.login(username, password);
      login(res.data.token, res.data.username, res.data.role);
    } catch (err) {
      if (err.response?.status === 429) {
        setError('Too many login attempts. Please wait one minute before trying again.');
      } else if (err.response?.status === 401) {
        setError('Invalid username or password.');
      } else {
        setError('An error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>Secure Leave Management System</h1>
        <h2 style={styles.subtitle}>Sign In</h2>
        <p style={styles.orgName}>NWU – ITRI615 Project | Colile Sibanda</p>

        {error && <div style={styles.error} role="alert">{error}</div>}

        <form onSubmit={handleSubmit} style={styles.form}>
          <label style={styles.label}>
            Username
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              style={styles.input}
              maxLength={50}
              autoComplete="username"
              required
              aria-label="Username"
            />
          </label>

          <label style={styles.label}>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={styles.input}
              maxLength={100}
              autoComplete="current-password"
              required
              aria-label="Password"
            />
          </label>

          <button type="submit" style={styles.button} disabled={loading}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div style={styles.hint}>
          <strong>Demo accounts:</strong><br />
          admin / admin123 (Admin)<br />
          lwazi.manager / password (Manager)<br />
          colile.employee / password (Employee)
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: { display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' },
  card: { background: '#fff', padding: '2rem', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.15)', width: '360px' },
  title: { fontSize: '1.2rem', marginBottom: '0.25rem', color: '#003580' },
  subtitle: { fontSize: '1.5rem', marginBottom: '0.25rem', color: '#333' },
  orgName: { fontSize: '0.8rem', color: '#888', marginBottom: '1.5rem' },
  form: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  label: { display: 'flex', flexDirection: 'column', gap: '0.25rem', fontSize: '0.9rem', fontWeight: '600' },
  input: { padding: '0.6rem', border: '1px solid #ccc', borderRadius: '4px', fontSize: '1rem' },
  button: { padding: '0.75rem', background: '#003580', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '1rem', cursor: 'pointer' },
  error: { background: '#ffe0e0', color: '#c00', padding: '0.75rem', borderRadius: '4px', marginBottom: '1rem', fontSize: '0.9rem' },
  hint: { marginTop: '1.5rem', fontSize: '0.8rem', color: '#555', background: '#f9f9f9', padding: '0.75rem', borderRadius: '4px' },
};
