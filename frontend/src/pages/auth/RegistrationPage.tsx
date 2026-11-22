import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  registerTrainee,
  registerTrainingProvider,
  registerEmployer,
  checkApplicationStatus,
  getRegistrationReferenceData,
  type RegistrationReferenceData,
  type OnboardingApplicationResponse,
  type ApplicationStatusResponse,
} from '@/api/registrationApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import type { ApiError } from '@/api/types';

type TabType = 'trainee' | 'provider' | 'employer' | 'status';

export function RegistrationPage(): React.JSX.Element {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [activeTab, setActiveTab] = useState<TabType>('trainee');
  const [refData, setRefData] = useState<RegistrationReferenceData | null>(null);

  // Trainee Form State
  const [traineeForm, setTraineeForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    middleName: '',
    lastName: '',
    dateOfBirth: '',
    genderId: 1,
    phone: '',
    stateId: 101,
    districtId: 1001,
    addressLine1: '',
    pincode: '',
    currentEmploymentStatusId: 1,
  });

  // Training Provider Form State
  const [providerForm, setProviderForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    providerName: '',
    registrationNumber: '',
    organizationTypeId: 5,
    contactPersonName: '',
    contactEmail: '',
    contactPhone: '',
    stateId: 101,
    districtId: 1001,
    addressLine1: '',
    pincode: '',
  });

  // Employer Form State
  const [employerForm, setEmployerForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    employerName: '',
    registrationNumber: '',
    gstin: '',
    organizationTypeId: 1,
    contactPersonName: '',
    contactEmail: '',
    contactPhone: '',
    stateId: 101,
    districtId: 1001,
    addressLine1: '',
    pincode: '',
  });

  // Status Search State
  const [searchAppNumber, setSearchAppNumber] = useState('');
  const [statusResult, setStatusResult] = useState<ApplicationStatusResponse | null>(null);

  // Common UI State
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [applicationSuccess, setApplicationSuccess] = useState<OnboardingApplicationResponse | null>(null);

  useEffect(() => {
    getRegistrationReferenceData()
      .then((data) => {
        setRefData(data);
        if (data.states?.length > 0) {
          const defaultState = data.states[0].id;
          const defaultDist = data.districts?.length > 0 ? data.districts[0].id : 1001;
          setTraineeForm((prev) => ({ ...prev, stateId: defaultState, districtId: defaultDist }));
          providerForm.stateId = defaultState;
          employerForm.stateId = defaultState;
        }
      })
      .catch((err) => {
        console.error('Failed to load reference data:', err);
      });
  }, []);

  const handleTraineeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (traineeForm.password !== traineeForm.confirmPassword) {
      setErrorMsg('Passwords do not match');
      return;
    }

    setSubmitting(true);
    try {
      await registerTrainee({
        username: traineeForm.username,
        email: traineeForm.email,
        password: traineeForm.password,
        firstName: traineeForm.firstName,
        middleName: traineeForm.middleName || undefined,
        lastName: traineeForm.lastName || undefined,
        dateOfBirth: traineeForm.dateOfBirth || undefined,
        genderId: Number(traineeForm.genderId),
        phone: traineeForm.phone || undefined,
        stateId: Number(traineeForm.stateId),
        districtId: Number(traineeForm.districtId),
        addressLine1: traineeForm.addressLine1 || undefined,
        pincode: traineeForm.pincode || undefined,
        currentEmploymentStatusId: Number(traineeForm.currentEmploymentStatusId),
      });

      // Automatically authenticate and navigate
      await login({
        usernameOrEmail: traineeForm.username,
        password: traineeForm.password,
      });

      navigate('/trainee/dashboard', { replace: true });
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Registration failed. Please check your inputs.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleProviderSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (providerForm.password !== providerForm.confirmPassword) {
      setErrorMsg('Passwords do not match');
      return;
    }

    setSubmitting(true);
    try {
      const res = await registerTrainingProvider({
        username: providerForm.username,
        email: providerForm.email,
        password: providerForm.password,
        providerName: providerForm.providerName,
        registrationNumber: providerForm.registrationNumber,
        organizationTypeId: Number(providerForm.organizationTypeId),
        contactPersonName: providerForm.contactPersonName,
        contactEmail: providerForm.contactEmail,
        contactPhone: providerForm.contactPhone || undefined,
        stateId: Number(providerForm.stateId),
        districtId: Number(providerForm.districtId),
        addressLine1: providerForm.addressLine1 || undefined,
        pincode: providerForm.pincode || undefined,
      });

      setApplicationSuccess(res);
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to submit application.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEmployerSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (employerForm.password !== employerForm.confirmPassword) {
      setErrorMsg('Passwords do not match');
      return;
    }

    setSubmitting(true);
    try {
      const res = await registerEmployer({
        username: employerForm.username,
        email: employerForm.email,
        password: employerForm.password,
        employerName: employerForm.employerName,
        registrationNumber: employerForm.registrationNumber || undefined,
        gstin: employerForm.gstin || undefined,
        organizationTypeId: Number(employerForm.organizationTypeId),
        contactPersonName: employerForm.contactPersonName,
        contactEmail: employerForm.contactEmail,
        contactPhone: employerForm.contactPhone || undefined,
        stateId: Number(employerForm.stateId),
        districtId: Number(employerForm.districtId),
        addressLine1: employerForm.addressLine1 || undefined,
        pincode: employerForm.pincode || undefined,
      });

      setApplicationSuccess(res);
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to submit application.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setStatusResult(null);

    if (!searchAppNumber.trim()) {
      setErrorMsg('Please enter an application number');
      return;
    }

    setSubmitting(true);
    try {
      const res = await checkApplicationStatus(searchAppNumber.trim());
      setStatusResult(res);
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Application not found with the given number.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-between">
      {/* Tricolor Header Accent */}
      <div className="fixed top-0 left-0 right-0 h-1 bg-gradient-to-r from-saffron via-white to-india-green z-50" />

      {/* Main Container */}
      <div className="max-w-4xl w-full mx-auto px-4 py-8 sm:px-6">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 mb-2">
            <span className="text-2xl font-bold tracking-tight text-navy">
              Skill India Unified Portal
            </span>
          </div>
          <p className="text-sm text-gray-600">
            Account Registration, Organization Onboarding & Application Status Verification
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-gray-200 mb-6 bg-white rounded-t-lg shadow-sm">
          <button
            type="button"
            onClick={() => { setActiveTab('trainee'); setErrorMsg(null); setApplicationSuccess(null); }}
            className={`flex-1 py-3.5 px-4 text-center text-sm font-semibold transition-colors border-b-2 ${
              activeTab === 'trainee'
                ? 'border-navy text-navy bg-navy/5'
                : 'border-transparent text-gray-600 hover:text-navy hover:bg-gray-50'
            }`}
          >
            Candidate Registration
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('provider'); setErrorMsg(null); setApplicationSuccess(null); }}
            className={`flex-1 py-3.5 px-4 text-center text-sm font-semibold transition-colors border-b-2 ${
              activeTab === 'provider'
                ? 'border-navy text-navy bg-navy/5'
                : 'border-transparent text-gray-600 hover:text-navy hover:bg-gray-50'
            }`}
          >
            Training Provider
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('employer'); setErrorMsg(null); setApplicationSuccess(null); }}
            className={`flex-1 py-3.5 px-4 text-center text-sm font-semibold transition-colors border-b-2 ${
              activeTab === 'employer'
                ? 'border-navy text-navy bg-navy/5'
                : 'border-transparent text-gray-600 hover:text-navy hover:bg-gray-50'
            }`}
          >
            Employer / Industry
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('status'); setErrorMsg(null); setApplicationSuccess(null); }}
            className={`flex-1 py-3.5 px-4 text-center text-sm font-semibold transition-colors border-b-2 ${
              activeTab === 'status'
                ? 'border-navy text-navy bg-navy/5'
                : 'border-transparent text-gray-600 hover:text-navy hover:bg-gray-50'
            }`}
          >
            Track Status
          </button>
        </div>

        {/* Error Alert */}
        {errorMsg && (
          <div className="mb-6 p-4 rounded-md bg-red-50 border border-red-200 text-sm text-red-800" role="alert">
            <div className="font-semibold mb-1">Registration Notice</div>
            <div>{errorMsg}</div>
          </div>
        )}

        {/* Application Success Card (for Provider/Employer) */}
        {applicationSuccess && (
          <div className="bg-white rounded-lg shadow p-6 mb-6 border-l-4 border-emerald-500">
            <h3 className="text-lg font-bold text-gray-900 mb-2">Application Submitted Successfully</h3>
            <p className="text-sm text-gray-700 mb-4">{applicationSuccess.message}</p>
            <div className="bg-gray-50 p-4 rounded-md border text-sm space-y-2 mb-4">
              <div><span className="font-medium text-gray-700">Application Number:</span> <span className="font-mono font-bold text-navy">{applicationSuccess.applicationNumber}</span></div>
              <div><span className="font-medium text-gray-700">Organisation:</span> {applicationSuccess.organizationName}</div>
              <div><span className="font-medium text-gray-700">Registered Email:</span> {applicationSuccess.contactEmail}</div>
              <div><span className="font-medium text-gray-700">Status:</span> <span className="inline-flex px-2 py-0.5 rounded text-xs font-semibold bg-amber-100 text-amber-800">PENDING APPROVAL</span></div>
            </div>
            <p className="text-xs text-gray-500 mb-4">
              Save your Application Number. State administration verifies organizational credentials before authorizing portal login.
            </p>
            <button
              type="button"
              onClick={() => { setApplicationSuccess(null); }}
              className="btn-secondary text-sm mr-3"
            >
              Submit Another Application
            </button>
            <Link to="/login" className="btn-primary text-sm inline-block">
              Proceed to Sign In
            </Link>
          </div>
        )}

        {/* Tab 1: Trainee Self-Registration */}
        {!applicationSuccess && activeTab === 'trainee' && (
          <div className="bg-white rounded-b-lg shadow p-6 sm:p-8">
            <h2 className="text-lg font-bold text-navy mb-4">Candidate / Trainee Self-Registration</h2>
            <p className="text-xs text-gray-500 mb-6">
              Create your candidate profile to access training programs, digital attendance, Skill India credentials, and placement opportunities.
            </p>

            <form onSubmit={handleTraineeSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="trainee-username" className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
                  <input
                    id="trainee-username"
                    type="text"
                    required
                    value={traineeForm.username}
                    onChange={(e) => setTraineeForm({ ...traineeForm, username: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    placeholder="e.g. ramesh_kumar"
                  />
                </div>

                <div>
                  <label htmlFor="trainee-email" className="block text-xs font-medium text-gray-700 mb-1">Email Address *</label>
                  <input
                    id="trainee-email"
                    type="email"
                    required
                    value={traineeForm.email}
                    onChange={(e) => setTraineeForm({ ...traineeForm, email: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    placeholder="candidate@example.com"
                  />
                </div>

                <div>
                  <label htmlFor="trainee-password" className="block text-xs font-medium text-gray-700 mb-1">Password *</label>
                  <input
                    id="trainee-password"
                    type="password"
                    required
                    minLength={8}
                    value={traineeForm.password}
                    onChange={(e) => setTraineeForm({ ...traineeForm, password: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    placeholder="Min 8 characters"
                  />
                </div>

                <div>
                  <label htmlFor="trainee-confirmPassword" className="block text-xs font-medium text-gray-700 mb-1">Confirm Password *</label>
                  <input
                    id="trainee-confirmPassword"
                    type="password"
                    required
                    minLength={8}
                    value={traineeForm.confirmPassword}
                    onChange={(e) => setTraineeForm({ ...traineeForm, confirmPassword: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    placeholder="Re-enter password"
                  />
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Personal & Demographic Details</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label htmlFor="trainee-firstName" className="block text-xs font-medium text-gray-700 mb-1">First Name *</label>
                    <input
                      id="trainee-firstName"
                      type="text"
                      required
                      value={traineeForm.firstName}
                      onChange={(e) => setTraineeForm({ ...traineeForm, firstName: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="trainee-middleName" className="block text-xs font-medium text-gray-700 mb-1">Middle Name</label>
                    <input
                      id="trainee-middleName"
                      type="text"
                      value={traineeForm.middleName}
                      onChange={(e) => setTraineeForm({ ...traineeForm, middleName: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="trainee-lastName" className="block text-xs font-medium text-gray-700 mb-1">Last Name</label>
                    <input
                      id="trainee-lastName"
                      type="text"
                      value={traineeForm.lastName}
                      onChange={(e) => setTraineeForm({ ...traineeForm, lastName: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-3">
                  <div>
                    <label htmlFor="trainee-dob" className="block text-xs font-medium text-gray-700 mb-1">Date of Birth</label>
                    <input
                      id="trainee-dob"
                      type="date"
                      value={traineeForm.dateOfBirth}
                      onChange={(e) => setTraineeForm({ ...traineeForm, dateOfBirth: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="trainee-gender" className="block text-xs font-medium text-gray-700 mb-1">Gender *</label>
                    <select
                      id="trainee-gender"
                      value={traineeForm.genderId}
                      onChange={(e) => setTraineeForm({ ...traineeForm, genderId: Number(e.target.value) })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    >
                      {refData?.genders?.map((g) => (
                        <option key={g.id} value={g.id}>{g.name}</option>
                      )) || <option value={1}>Female</option>}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="trainee-phone" className="block text-xs font-medium text-gray-700 mb-1">Mobile Number</label>
                    <input
                      id="trainee-phone"
                      type="tel"
                      value={traineeForm.phone}
                      onChange={(e) => setTraineeForm({ ...traineeForm, phone: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                      placeholder="10-digit mobile"
                    />
                  </div>
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Address & Employment</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="trainee-state" className="block text-xs font-medium text-gray-700 mb-1">State *</label>
                    <select
                      id="trainee-state"
                      value={traineeForm.stateId}
                      onChange={(e) => setTraineeForm({ ...traineeForm, stateId: Number(e.target.value) })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    >
                      {refData?.states?.map((s) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      )) || <option value={101}>Delhi</option>}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="trainee-district" className="block text-xs font-medium text-gray-700 mb-1">District *</label>
                    <select
                      id="trainee-district"
                      value={traineeForm.districtId}
                      onChange={(e) => setTraineeForm({ ...traineeForm, districtId: Number(e.target.value) })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    >
                      {refData?.districts?.map((d) => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                      )) || <option value={1001}>New Delhi</option>}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-3">
                  <div className="md:col-span-2">
                    <label htmlFor="trainee-address" className="block text-xs font-medium text-gray-700 mb-1">Address</label>
                    <input
                      id="trainee-address"
                      type="text"
                      value={traineeForm.addressLine1}
                      onChange={(e) => setTraineeForm({ ...traineeForm, addressLine1: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                      placeholder="Locality / Street"
                    />
                  </div>
                  <div>
                    <label htmlFor="trainee-pincode" className="block text-xs font-medium text-gray-700 mb-1">Pincode</label>
                    <input
                      id="trainee-pincode"
                      type="text"
                      maxLength={6}
                      value={traineeForm.pincode}
                      onChange={(e) => setTraineeForm({ ...traineeForm, pincode: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full btn-primary justify-center py-2.5 text-sm font-medium"
                >
                  {submitting ? (
                    <span className="flex items-center gap-2">
                      <LoadingSpinner size="sm" />
                      <span>Creating Account & Authenticating...</span>
                    </span>
                  ) : (
                    'Complete Candidate Registration'
                  )}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Tab 2: Training Provider Application */}
        {!applicationSuccess && activeTab === 'provider' && (
          <div className="bg-white rounded-b-lg shadow p-6 sm:p-8">
            <h2 className="text-lg font-bold text-navy mb-4">Training Provider Onboarding Application</h2>
            <p className="text-xs text-gray-500 mb-6">
              Empanelled training partners, institutions, and vocational centres apply here. Upon administrator verification, your portal credentials are activated.
            </p>

            <form onSubmit={handleProviderSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="provider-name" className="block text-xs font-medium text-gray-700 mb-1">Institution / Provider Name *</label>
                  <input
                    id="provider-name"
                    type="text"
                    required
                    value={providerForm.providerName}
                    onChange={(e) => setProviderForm({ ...providerForm, providerName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="provider-reg-number" className="block text-xs font-medium text-gray-700 mb-1">Registration / NSDC Number *</label>
                  <input
                    id="provider-reg-number"
                    type="text"
                    required
                    value={providerForm.registrationNumber}
                    onChange={(e) => setProviderForm({ ...providerForm, registrationNumber: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="provider-contact-person" className="block text-xs font-medium text-gray-700 mb-1">Authorized Contact Person *</label>
                  <input
                    id="provider-contact-person"
                    type="text"
                    required
                    value={providerForm.contactPersonName}
                    onChange={(e) => setProviderForm({ ...providerForm, contactPersonName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="provider-contact-email" className="block text-xs font-medium text-gray-700 mb-1">Official Contact Email *</label>
                  <input
                    id="provider-contact-email"
                    type="email"
                    required
                    value={providerForm.contactEmail}
                    onChange={(e) => setProviderForm({ ...providerForm, contactEmail: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="provider-contact-phone" className="block text-xs font-medium text-gray-700 mb-1">Official Contact Phone</label>
                  <input
                    id="provider-contact-phone"
                    type="tel"
                    value={providerForm.contactPhone}
                    onChange={(e) => setProviderForm({ ...providerForm, contactPhone: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Provider Location</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label htmlFor="provider-state" className="block text-xs font-medium text-gray-700 mb-1">State *</label>
                    <select
                      id="provider-state"
                      value={providerForm.stateId}
                      onChange={(e) => setProviderForm({ ...providerForm, stateId: Number(e.target.value) })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    >
                      {refData?.states?.map((s) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      )) || <option value={101}>Delhi</option>}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="provider-district" className="block text-xs font-medium text-gray-700 mb-1">District *</label>
                    <select
                      id="provider-district"
                      value={providerForm.districtId}
                      onChange={(e) => setProviderForm({ ...providerForm, districtId: Number(e.target.value) })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    >
                      {refData?.districts?.map((d) => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                      )) || <option value={1001}>New Delhi</option>}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="provider-pincode" className="block text-xs font-medium text-gray-700 mb-1">Pincode</label>
                    <input
                      id="provider-pincode"
                      type="text"
                      maxLength={6}
                      value={providerForm.pincode}
                      onChange={(e) => setProviderForm({ ...providerForm, pincode: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Admin User Credentials</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="provider-username" className="block text-xs font-medium text-gray-700 mb-1">Portal Login Username *</label>
                    <input
                      id="provider-username"
                      type="text"
                      required
                      value={providerForm.username}
                      onChange={(e) => setProviderForm({ ...providerForm, username: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="provider-email" className="block text-xs font-medium text-gray-700 mb-1">Account Recovery Email *</label>
                    <input
                      id="provider-email"
                      type="email"
                      required
                      value={providerForm.email}
                      onChange={(e) => setProviderForm({ ...providerForm, email: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="provider-password" className="block text-xs font-medium text-gray-700 mb-1">Password *</label>
                    <input
                      id="provider-password"
                      type="password"
                      required
                      minLength={8}
                      value={providerForm.password}
                      onChange={(e) => setProviderForm({ ...providerForm, password: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="provider-confirmPassword" className="block text-xs font-medium text-gray-700 mb-1">Confirm Password *</label>
                    <input
                      id="provider-confirmPassword"
                      type="password"
                      required
                      minLength={8}
                      value={providerForm.confirmPassword}
                      onChange={(e) => setProviderForm({ ...providerForm, confirmPassword: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full btn-primary justify-center py-2.5 text-sm font-medium"
                >
                  {submitting ? (
                    <span className="flex items-center gap-2">
                      <LoadingSpinner size="sm" />
                      <span>Submitting Application...</span>
                    </span>
                  ) : (
                    'Submit Training Provider Application'
                  )}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Tab 3: Employer Application */}
        {!applicationSuccess && activeTab === 'employer' && (
          <div className="bg-white rounded-b-lg shadow p-6 sm:p-8">
            <h2 className="text-lg font-bold text-navy mb-4">Employer / Industry Partner Registration</h2>
            <p className="text-xs text-gray-500 mb-6">
              Enterprises and recruiting companies apply here to post vacancies, recruit certified trainees, and conduct employment verification.
            </p>

            <form onSubmit={handleEmployerSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="emp-company-name" className="block text-xs font-medium text-gray-700 mb-1">Company / Enterprise Name *</label>
                  <input
                    id="emp-company-name"
                    type="text"
                    required
                    value={employerForm.employerName}
                    onChange={(e) => setEmployerForm({ ...employerForm, employerName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="emp-reg-number" className="block text-xs font-medium text-gray-700 mb-1">CIN / Registration No.</label>
                  <input
                    id="emp-reg-number"
                    type="text"
                    value={employerForm.registrationNumber}
                    onChange={(e) => setEmployerForm({ ...employerForm, registrationNumber: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="emp-gstin" className="block text-xs font-medium text-gray-700 mb-1">GSTIN</label>
                  <input
                    id="emp-gstin"
                    type="text"
                    maxLength={15}
                    value={employerForm.gstin}
                    onChange={(e) => setEmployerForm({ ...employerForm, gstin: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="emp-contact-person" className="block text-xs font-medium text-gray-700 mb-1">Contact Person *</label>
                  <input
                    id="emp-contact-person"
                    type="text"
                    required
                    value={employerForm.contactPersonName}
                    onChange={(e) => setEmployerForm({ ...employerForm, contactPersonName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="emp-contact-email" className="block text-xs font-medium text-gray-700 mb-1">Contact Email *</label>
                  <input
                    id="emp-contact-email"
                    type="email"
                    required
                    value={employerForm.contactEmail}
                    onChange={(e) => setEmployerForm({ ...employerForm, contactEmail: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="emp-contact-phone" className="block text-xs font-medium text-gray-700 mb-1">Phone</label>
                  <input
                    id="emp-contact-phone"
                    type="tel"
                    value={employerForm.contactPhone}
                    onChange={(e) => setEmployerForm({ ...employerForm, contactPhone: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Enterprise Location</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="emp-state" className="block text-xs font-medium text-gray-700 mb-1">State *</label>
                    <select
                      id="emp-state"
                      value={employerForm.stateId}
                      onChange={(e) => setEmployerForm({ ...employerForm, stateId: Number(e.target.value) })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    >
                      {refData?.states?.map((s) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      )) || <option value={101}>Delhi</option>}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="emp-district" className="block text-xs font-medium text-gray-700 mb-1">District *</label>
                    <select
                      id="emp-district"
                      value={employerForm.districtId}
                      onChange={(e) => setEmployerForm({ ...employerForm, districtId: Number(e.target.value) })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    >
                      {refData?.districts?.map((d) => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                      )) || <option value={1001}>New Delhi</option>}
                    </select>
                  </div>
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Administrator Account Details</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="emp-username" className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
                    <input
                      id="emp-username"
                      type="text"
                      required
                      value={employerForm.username}
                      onChange={(e) => setEmployerForm({ ...employerForm, username: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="emp-email" className="block text-xs font-medium text-gray-700 mb-1">Recovery Email *</label>
                    <input
                      id="emp-email"
                      type="email"
                      required
                      value={employerForm.email}
                      onChange={(e) => setEmployerForm({ ...employerForm, email: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="emp-password" className="block text-xs font-medium text-gray-700 mb-1">Password *</label>
                    <input
                      id="emp-password"
                      type="password"
                      required
                      minLength={8}
                      value={employerForm.password}
                      onChange={(e) => setEmployerForm({ ...employerForm, password: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="emp-confirmPassword" className="block text-xs font-medium text-gray-700 mb-1">Confirm Password *</label>
                    <input
                      id="emp-confirmPassword"
                      type="password"
                      required
                      minLength={8}
                      value={employerForm.confirmPassword}
                      onChange={(e) => setEmployerForm({ ...employerForm, confirmPassword: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full btn-primary justify-center py-2.5 text-sm font-medium"
                >
                  {submitting ? (
                    <span className="flex items-center gap-2">
                      <LoadingSpinner size="sm" />
                      <span>Submitting Application...</span>
                    </span>
                  ) : (
                    'Submit Employer Application'
                  )}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Tab 4: Track Status */}
        {activeTab === 'status' && (
          <div className="bg-white rounded-b-lg shadow p-6 sm:p-8">
            <h2 className="text-lg font-bold text-navy mb-4">Track Onboarding Application Status</h2>
            <p className="text-xs text-gray-500 mb-6">
              Enter your tracking reference number (e.g., APP-TP-1732... or APP-EMP-1732...) to review administrative decision status.
            </p>

            <form onSubmit={handleStatusSearch} className="flex gap-3 mb-6">
              <input
                type="text"
                required
                value={searchAppNumber}
                onChange={(e) => setSearchAppNumber(e.target.value)}
                placeholder="Enter Application Number (e.g. APP-TP-...)"
                className="flex-1 px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
              />
              <button
                type="submit"
                disabled={submitting}
                className="btn-primary py-2 px-6 text-sm font-medium"
              >
                {submitting ? 'Checking...' : 'Check Status'}
              </button>
            </form>

            {statusResult && (
              <div className="border rounded-lg p-5 bg-gray-50">
                <div className="flex items-center justify-between border-b pb-3 mb-3">
                  <div>
                    <span className="text-xs text-gray-500">Application Number</span>
                    <div className="font-mono font-bold text-base text-navy">{statusResult.applicationNumber}</div>
                  </div>
                  <div>
                    <span
                      className={`inline-flex px-3 py-1 rounded-full text-xs font-bold ${
                        statusResult.status === 'APPROVED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : statusResult.status === 'REJECTED'
                          ? 'bg-red-100 text-red-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {statusResult.status}
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-gray-700">
                  <div><span className="font-medium text-gray-500">Organisation:</span> {statusResult.organizationName}</div>
                  <div><span className="font-medium text-gray-500">Applicant Type:</span> {statusResult.applicantType}</div>
                  <div><span className="font-medium text-gray-500">Contact Email:</span> {statusResult.contactEmail}</div>
                  <div><span className="font-medium text-gray-500">Submitted On:</span> {new Date(statusResult.submittedAt).toLocaleDateString()}</div>
                </div>

                {statusResult.status === 'APPROVED' && (
                  <div className="mt-4 p-3 bg-emerald-50 border border-emerald-200 rounded text-xs text-emerald-800">
                    Application has been approved! You may now sign in using your registered credentials.
                  </div>
                )}

                {statusResult.status === 'REJECTED' && (
                  <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded text-xs text-red-800">
                    <span className="font-bold">Rejection Reason:</span> {statusResult.rejectionReason}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Footer Link to Login */}
        <div className="mt-6 text-center text-sm text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="font-semibold text-navy hover:underline">
            Sign In here
          </Link>
        </div>
      </div>
    </div>
  );
}
