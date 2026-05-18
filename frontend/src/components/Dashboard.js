// Dashboard.js : main dashboard for logged-in users
import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { leaveApi } from '../api/api';
import LeaveForm from './LeaveForm';
import LeaveList from './LeaveList';
import ManagerDashboard from './ManagerDashboard';

export default function Dashboard() {
  const { user, logout, isManager } = useAuth();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchRequests = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const res = await leaveApi.getLeaveRequests();
      setRequests(res.data);
    } catch (err) {
      if (err.response?.status === 401) {
        logout();
      } else {
        setError('Failed to load leave requests.');
      }
    } finally {
      setLoading(false);
    }
  }, [logout]);

  useEffect(() => { fetchRequests(); }, [fetchRequests]);

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div>
          <h1 style={styles.headerTitle}>Leave Management System</h1>
          <span style={styles.headerSub}>Secure · NWU · ITRI615</span>
        </div>
        <div style={styles.userInfo}>
          <span style={styles.username}>
            {user.username} <span style={styles.roleBadge}>{user.role}</span>
          </span>
          <button onClick={logout} style={styles.logoutBtn}>Sign Out</button>
        </div>
      </header>

      <main style={styles.main}>
        {error && <div style={styles.error}>{error}</div>}

        {isManager ? (
          <>
            <ManagerDashboard requests={requests} onRefresh={fetchRequests} />
          </>
        ) : (
          <>
            <LeaveForm onSuccess={fetchRequests} />
            {loading ? (
              <div style={styles.loading}>Loading leave requests...</div>
            ) : (
              <LeaveList requests={requests} onRefresh={fetchRequests} />
            )}
          </>
        )}
      </main>
    </div>
  );
}

const styles = {
  page: { minHeight: '100vh', background: '#f0f2f5', fontFamily: 'Arial, sans-serif' },
  header: { background: '#003580', color: '#fff', padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  headerTitle: { margin: 0, fontSize: '1.3rem' },
  headerSub: { fontSize: '0.75rem', opacity: 0.8 },
  userInfo: { display: 'flex', alignItems: 'center', gap: '1rem' },
  username: { fontSize: '0.9rem' },
  roleBadge: { background: 'rgba(255,255,255,0.2)', padding: '0.1rem 0.4rem', borderRadius: '4px', fontSize: '0.75rem' },
  logoutBtn: { background: 'transparent', color: '#fff', border: '1px solid rgba(255,255,255,0.5)', padding: '0.4rem 1rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.85rem' },
  main: { maxWidth: '1100px', margin: '0 auto', padding: '2rem 1rem' },
  error: { background: '#ffe0e0', color: '#c00', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' },
  loading: { textAlign: 'center', color: '#888', padding: '2rem' },
};
