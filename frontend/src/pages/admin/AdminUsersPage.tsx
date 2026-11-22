import React, { useState, useEffect, useCallback } from 'react';
import {
  listUsers,
  provisionAssessor,
  provisionGovernmentAdmin,
  provisionSuperAdmin,
  type AdminUserResponse,
} from '@/api/adminApi';
import { useAuth } from '@/context/AuthContext';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import type { ApiError } from '@/api/types';

export function AdminUsersPage(): React.JSX.Element {
  const { hasRole } = useAuth();
  const isSuperAdmin = hasRole('ROLE_super_admin') || hasRole('super_admin');

  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [provisionTab, setProvisionTab] = useState<'assessor' | 'gov' | 'super'>('assessor');
  const [submitting, setSubmitting] = useState(false);

  // Assessor Form
  const [assessorForm, setAssessorForm] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    phone: '',
    stateId: 101,
    districtId: 1001,
  });

  // Gov Admin Form
  const [govForm, setGovForm] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    roleCode: 'district_officer' as 'government_admin' | 'government_officer' | 'district_officer' | 'analyst',
    stateId: 101,
    districtId: 1001,
  });

  // Super Admin Form
  const [superForm, setSuperForm] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
  });

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const data = await listUsers();
      setUsers(data);
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to load users.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleProvisionAssessor = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await provisionAssessor({
        username: assessorForm.username.trim(),
        email: assessorForm.email.trim(),
        password: assessorForm.password,
        fullName: assessorForm.fullName || undefined,
        phone: assessorForm.phone || undefined,
        stateId: Number(assessorForm.stateId),
        districtId: Number(assessorForm.districtId),
      });
      setSuccessMsg(`Assessor "${res.username}" provisioned successfully with assessor authorities.`);
      setShowModal(false);
      setAssessorForm({ username: '', email: '', password: '', fullName: '', phone: '', stateId: 101, districtId: 1001 });
      await fetchUsers();
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to provision assessor.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleProvisionGov = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await provisionGovernmentAdmin({
        username: govForm.username.trim(),
        email: govForm.email.trim(),
        password: govForm.password,
        fullName: govForm.fullName || undefined,
        roleCode: govForm.roleCode,
        stateId: Number(govForm.stateId),
        districtId: Number(govForm.districtId),
      });
      setSuccessMsg(`Government officer "${res.username}" provisioned with role "${govForm.roleCode}".`);
      setShowModal(false);
      setGovForm({ username: '', email: '', password: '', fullName: '', roleCode: 'district_officer', stateId: 101, districtId: 1001 });
      await fetchUsers();
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to provision government user.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleProvisionSuper = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await provisionSuperAdmin({
        username: superForm.username.trim(),
        email: superForm.email.trim(),
        password: superForm.password,
        fullName: superForm.fullName || undefined,
      });
      setSuccessMsg(`Super Administrator "${res.username}" provisioned successfully.`);
      setShowModal(false);
      setSuperForm({ username: '', email: '', password: '', fullName: '' });
      await fetchUsers();
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to provision super admin.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-navy">
            User Accounts & Role Provisioning
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Manage platform users and provision specialized assessor, officer, and administrative accounts.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchUsers}
            disabled={loading}
            className="btn-secondary text-xs px-3 py-2"
          >
            Refresh
          </button>
          <button
            type="button"
            onClick={() => { setShowModal(true); setErrorMsg(null); setSuccessMsg(null); }}
            className="btn-primary text-xs px-4 py-2 font-medium"
          >
            + Provision Privileged User
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMsg && (
        <div className="p-4 rounded-md bg-emerald-50 border border-emerald-200 text-sm text-emerald-800">
          {successMsg}
        </div>
      )}
      {errorMsg && (
        <div className="p-4 rounded-md bg-red-50 border border-red-200 text-sm text-red-800">
          {errorMsg}
        </div>
      )}

      {/* User Table */}
      {loading ? (
        <div className="py-12 flex flex-col items-center gap-3">
          <LoadingSpinner size="lg" />
          <span className="text-sm text-gray-500">Loading user directory...</span>
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg border border-dashed p-8">
          <p className="text-gray-500 text-sm">No user records found.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-gray-700 uppercase font-semibold text-xs tracking-wider">
                <tr>
                  <th scope="col" className="px-4 py-3 text-left">User ID</th>
                  <th scope="col" className="px-4 py-3 text-left">Username</th>
                  <th scope="col" className="px-4 py-3 text-left">Email Address</th>
                  <th scope="col" className="px-4 py-3 text-left">Status</th>
                  <th scope="col" className="px-4 py-3 text-left">Last Login</th>
                  <th scope="col" className="px-4 py-3 text-left">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">#{u.id}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">{u.username}</td>
                    <td className="px-4 py-3 text-gray-600">{u.email}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex px-2 py-0.5 rounded-full text-xs font-bold ${
                          u.userStatusId === 2
                            ? 'bg-emerald-100 text-emerald-800'
                            : u.userStatusId === 1
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {u.userStatusCode || (u.userStatusId === 2 ? 'ACTIVE' : u.userStatusId === 1 ? 'PENDING' : 'INACTIVE')}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500">
                      {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : 'Never'}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Provisioning Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
            <div className="flex items-center justify-between border-b pb-3 mb-4">
              <h3 className="text-lg font-bold text-gray-900">Provision Privileged User Account</h3>
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 font-bold"
              >
                ✕
              </button>
            </div>

            {/* Role Provision Tabs */}
            <div className="flex border-b mb-4 text-xs font-semibold">
              <button
                type="button"
                onClick={() => setProvisionTab('assessor')}
                className={`flex-1 py-2 text-center border-b-2 ${
                  provisionTab === 'assessor'
                    ? 'border-navy text-navy font-bold'
                    : 'border-transparent text-gray-500 hover:text-navy'
                }`}
              >
                Assessor
              </button>
              <button
                type="button"
                onClick={() => setProvisionTab('gov')}
                className={`flex-1 py-2 text-center border-b-2 ${
                  provisionTab === 'gov'
                    ? 'border-navy text-navy font-bold'
                    : 'border-transparent text-gray-500 hover:text-navy'
                }`}
              >
                Government Admin
              </button>
              {isSuperAdmin && (
                <button
                  type="button"
                  onClick={() => setProvisionTab('super')}
                  className={`flex-1 py-2 text-center border-b-2 ${
                    provisionTab === 'super'
                      ? 'border-navy text-navy font-bold'
                      : 'border-transparent text-gray-500 hover:text-navy'
                  }`}
                >
                  Super Admin
                </button>
              )}
            </div>

            {/* Assessor Form */}
            {provisionTab === 'assessor' && (
              <form onSubmit={handleProvisionAssessor} className="space-y-4 text-sm">
                <p className="text-xs text-gray-500">
                  Assigns role 10 (ROLE_assessor) with authorities to conduct evaluations and manage trainee assessments.
                </p>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
                  <input
                    type="text"
                    required
                    value={assessorForm.username}
                    onChange={(e) => setAssessorForm({ ...assessorForm, username: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Email *</label>
                  <input
                    type="email"
                    required
                    value={assessorForm.email}
                    onChange={(e) => setAssessorForm({ ...assessorForm, email: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Password *</label>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={assessorForm.password}
                    onChange={(e) => setAssessorForm({ ...assessorForm, password: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Full Name</label>
                  <input
                    type="text"
                    value={assessorForm.fullName}
                    onChange={(e) => setAssessorForm({ ...assessorForm, fullName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div className="flex justify-end gap-3 pt-3 border-t">
                  <button type="button" onClick={() => setShowModal(false)} className="btn-secondary text-xs">
                    Cancel
                  </button>
                  <button type="submit" disabled={submitting} className="btn-primary text-xs">
                    {submitting ? 'Provisioning...' : 'Provision Assessor'}
                  </button>
                </div>
              </form>
            )}

            {/* Gov Admin Form */}
            {provisionTab === 'gov' && (
              <form onSubmit={handleProvisionGov} className="space-y-4 text-sm">
                <p className="text-xs text-gray-500">
                  Provision government department officers and state analysts.
                </p>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Role Classification *</label>
                  <select
                    value={govForm.roleCode}
                    onChange={(e) => setGovForm({ ...govForm, roleCode: e.target.value as any })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none bg-white"
                  >
                    <option value="district_officer">District Officer (district_officer)</option>
                    <option value="government_officer">Government Officer (government_officer)</option>
                    <option value="government_admin">Government Administrator (government_admin)</option>
                    <option value="analyst">Policy Analyst (analyst)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
                  <input
                    type="text"
                    required
                    value={govForm.username}
                    onChange={(e) => setGovForm({ ...govForm, username: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Email *</label>
                  <input
                    type="email"
                    required
                    value={govForm.email}
                    onChange={(e) => setGovForm({ ...govForm, email: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Password *</label>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={govForm.password}
                    onChange={(e) => setGovForm({ ...govForm, password: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div className="flex justify-end gap-3 pt-3 border-t">
                  <button type="button" onClick={() => setShowModal(false)} className="btn-secondary text-xs">
                    Cancel
                  </button>
                  <button type="submit" disabled={submitting} className="btn-primary text-xs">
                    {submitting ? 'Provisioning...' : 'Provision Officer'}
                  </button>
                </div>
              </form>
            )}

            {/* Super Admin Form */}
            {provisionTab === 'super' && (
              <form onSubmit={handleProvisionSuper} className="space-y-4 text-sm">
                <p className="text-xs text-amber-700 bg-amber-50 p-2 rounded border border-amber-200">
                  Warning: Super Administrators have unrestricted platform permissions across all tenants and security settings.
                </p>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
                  <input
                    type="text"
                    required
                    value={superForm.username}
                    onChange={(e) => setSuperForm({ ...superForm, username: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Email *</label>
                  <input
                    type="email"
                    required
                    value={superForm.email}
                    onChange={(e) => setSuperForm({ ...superForm, email: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Password *</label>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={superForm.password}
                    onChange={(e) => setSuperForm({ ...superForm, password: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-xs focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div className="flex justify-end gap-3 pt-3 border-t">
                  <button type="button" onClick={() => setShowModal(false)} className="btn-secondary text-xs">
                    Cancel
                  </button>
                  <button type="submit" disabled={submitting} className="btn-primary text-xs bg-red-600 hover:bg-red-700 border-red-700">
                    {submitting ? 'Provisioning...' : 'Provision Super Admin'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
