import React, { useState, useEffect, useCallback } from 'react';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
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

const optional = (value: string) => value.trim() || undefined;

function submissionError(error: unknown, fallback: string) {
  const apiError = error as ApiError;
  return apiError?.fieldErrors?.[0]?.message || apiError?.message || fallback;
}

export function RegistrationPage(): React.JSX.Element {
  const navigate = useNavigate();
  const { establishSession } = useAuth();

  const [activeTab, setActiveTab] = useState<TabType>('trainee');
  const [refData, setRefData] = useState<RegistrationReferenceData | null>(null);
  const [refLoading, setRefLoading] = useState(true);
  const [refError, setRefError] = useState<string | null>(null);

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
    genderId: 0,
    phone: '',
    stateId: 0,
    districtId: 0,
    addressLine1: '',
    pincode: '',
    currentEmploymentStatusId: 0,
    dataProcessingConsent: false,
  });

  // Training Provider Form State
  const [providerForm, setProviderForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    providerName: '',
    registrationNumber: '',
    contactPersonName: '',
    contactEmail: '',
    contactPhone: '',
    stateId: 0,
    districtId: 0,
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
    contactPersonName: '',
    contactEmail: '',
    contactPhone: '',
    stateId: 0,
    districtId: 0,
  });

  // Status Search State
  const [searchAppNumber, setSearchAppNumber] = useState('');
  const [statusResult, setStatusResult] = useState<ApplicationStatusResponse | null>(null);

  // Common UI State
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [applicationSuccess, setApplicationSuccess] = useState<OnboardingApplicationResponse | null>(null);

  const fetchReferenceData = useCallback(async () => {
    setRefLoading(true);
    setRefError(null);
    try {
      const data = await getRegistrationReferenceData();
      setRefData(data);
      if (!data.states.length || !data.districts.length) {
        setRefError('The location directory is unavailable. Please try again.');
      }
    } catch (err) {
      const apiErr = err as ApiError;
      setRefError(apiErr.message || 'Failed to load official reference data.');
    } finally {
      setRefLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReferenceData();
  }, [fetchReferenceData]);

  const handleTraineeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (traineeForm.password !== traineeForm.confirmPassword) {
      setErrorMsg('Passwords do not match');
      return;
    }

    if (!traineeForm.stateId || !traineeForm.districtId) {
      setErrorMsg('Please select a valid State and District from the official catalog.');
      return;
    }
    if (!traineeForm.genderId || !traineeForm.dataProcessingConsent) {
      setErrorMsg('Choose a gender option and confirm core data processing consent.');
      return;
    }

    setSubmitting(true);
    try {
      const authResponse = await registerTrainee({
        username: traineeForm.username.trim(),
        email: traineeForm.email.trim(),
        password: traineeForm.password,
        firstName: traineeForm.firstName.trim(),
        middleName: optional(traineeForm.middleName),
        lastName: optional(traineeForm.lastName),
        dateOfBirth: traineeForm.dateOfBirth || undefined,
        genderId: Number(traineeForm.genderId),
        phone: optional(traineeForm.phone),
        stateId: Number(traineeForm.stateId),
        districtId: Number(traineeForm.districtId),
        addressLine1: optional(traineeForm.addressLine1),
        pincode: traineeForm.pincode,
        currentEmploymentStatusId: traineeForm.currentEmploymentStatusId || undefined,
        dataProcessingConsent: traineeForm.dataProcessingConsent,
      });

      // Seamless single-phase session transition without double login request
      if (authResponse && authResponse.accessToken) {
        establishSession(authResponse);
        navigate('/trainee/dashboard', { replace: true });
      } else {
        navigate('/login', { replace: true });
      }
    } catch (err) {
      setErrorMsg(submissionError(err, 'Registration failed. Please check your inputs.'));
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

    if (!providerForm.stateId || !providerForm.districtId) {
      setErrorMsg('Please select a valid State and District from the official catalog.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await registerTrainingProvider({
        username: providerForm.username.trim(),
        email: providerForm.email.trim(),
        password: providerForm.password,
        providerName: providerForm.providerName.trim(),
        registrationNumber: providerForm.registrationNumber.trim(),
        contactPersonName: providerForm.contactPersonName.trim(),
        contactEmail: optional(providerForm.contactEmail),
        contactPhone: optional(providerForm.contactPhone),
        stateId: Number(providerForm.stateId),
        districtId: Number(providerForm.districtId),
        pincode: providerForm.pincode,
      });

      setApplicationSuccess(res);
    } catch (err) {
      setErrorMsg(submissionError(err, 'Failed to submit application.'));
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

    if (!employerForm.stateId || !employerForm.districtId) {
      setErrorMsg('Please select a valid State and District from the official catalog.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await registerEmployer({
        username: employerForm.username.trim(),
        email: employerForm.email.trim(),
        password: employerForm.password,
        employerName: employerForm.employerName.trim(),
        registrationNumber: optional(employerForm.registrationNumber),
        gstin: optional(employerForm.gstin),
        contactPersonName: employerForm.contactPersonName.trim(),
        contactEmail: optional(employerForm.contactEmail),
        contactPhone: optional(employerForm.contactPhone),
        stateId: Number(employerForm.stateId),
        districtId: Number(employerForm.districtId),
      });

      setApplicationSuccess(res);
    } catch (err) {
      setErrorMsg(submissionError(err, 'Failed to submit application.'));
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

        {/* Reference Data Loading or Failure Banner */}
        {refError && (
          <div className="mb-6 p-4 rounded-md bg-amber-50 border border-amber-300 text-sm text-amber-900 flex items-center justify-between" role="alert">
            <div>
              <span className="font-semibold">Official Geographic Catalog Error: </span>
              {refError}
            </div>
            <button
              type="button"
              onClick={fetchReferenceData}
              className="px-3 py-1 bg-amber-200 hover:bg-amber-300 text-amber-900 rounded font-medium text-xs transition-colors"
            >
              Retry
            </button>
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
              <div><span className="font-medium text-gray-700">Contact Email:</span> {applicationSuccess.contactEmail}</div>
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
            <h2 data-evaluator-registration className="text-lg font-bold text-navy mb-4">Candidate / Trainee Self-Registration</h2>
            <p className="text-xs text-gray-500 mb-6">
              Create your candidate profile to access training programs, digital attendance, Skill India credentials, and placement opportunities.
            </p>
            <p className="mb-4 text-xs text-slate-600">Fields marked * are required.</p>

            <form onSubmit={handleTraineeSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="trainee-username" className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
                  <input
                    id="trainee-username"
                    minLength={3}
                    maxLength={80}
                    autoComplete="username"
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
                    maxLength={255}
                    autoComplete="email"
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
                    maxLength={100}
                    autoComplete="new-password"
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
                    maxLength={100}
                    autoComplete="new-password"
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
                      maxLength={80}
                      type="text"
                      required
                      value={traineeForm.firstName}
                      onChange={(e) => setTraineeForm({ ...traineeForm, firstName: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="trainee-middleName" className="block text-xs font-medium text-gray-700 mb-1">Middle Name (optional)</label>
                    <input
                      id="trainee-middleName"
                      maxLength={80}
                      type="text"
                      value={traineeForm.middleName}
                      onChange={(e) => setTraineeForm({ ...traineeForm, middleName: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="trainee-lastName" className="block text-xs font-medium text-gray-700 mb-1">Last Name (optional)</label>
                    <input
                      id="trainee-lastName"
                      maxLength={80}
                      type="text"
                      value={traineeForm.lastName}
                      onChange={(e) => setTraineeForm({ ...traineeForm, lastName: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-3">
                  <div>
                    <label htmlFor="trainee-dob" className="block text-xs font-medium text-gray-700 mb-1">Date of Birth (optional)</label>
                    <input
                      id="trainee-dob"
                      max={new Date().toLocaleDateString('sv-SE')}
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
                      required
                      value={traineeForm.genderId || ''}
                      onChange={(e) => setTraineeForm({ ...traineeForm, genderId: Number(e.target.value) })}
                      disabled={refLoading || !!refError}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none disabled:bg-gray-100"
                    >
                      <option value="">Select a gender option</option>
                      {refData?.genders?.map((g) => (
                        <option key={g.id} value={g.id}>{g.name}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="trainee-phone" className="block text-xs font-medium text-gray-700 mb-1">Mobile Number (optional)</label>
                    <input
                      id="trainee-phone"
                      maxLength={15}
                      type="tel"
                      value={traineeForm.phone}
                      onChange={(e) => setTraineeForm({ ...traineeForm, phone: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                      placeholder="Include country code if needed"
                    />
                  </div>
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Location and employment</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <StateDistrictFields prefix="trainee" reference={refData} required disabled={refLoading || !!refError}
                    stateId={traineeForm.stateId} districtId={traineeForm.districtId}
                    onChange={(stateId, districtId) => setTraineeForm((form) => ({ ...form, stateId, districtId }))} />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-3">
                  <div className="md:col-span-2">
                    <label htmlFor="trainee-address" className="block text-xs font-medium text-gray-700 mb-1">Address (optional)</label>
                    <input
                      id="trainee-address"
                      maxLength={200}
                      type="text"
                      value={traineeForm.addressLine1}
                      onChange={(e) => setTraineeForm({ ...traineeForm, addressLine1: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                      placeholder="Locality / Street"
                    />
                  </div>
                  <div>
                    <label htmlFor="trainee-pincode" className="block text-xs font-medium text-gray-700 mb-1">PIN Code *</label>
                    <input
                      id="trainee-pincode"
                      required
                      inputMode="numeric"
                      pattern="[1-9][0-9]{5}"
                      title="Enter a six-digit PIN code"
                      type="text"
                      maxLength={6}
                      value={traineeForm.pincode}
                      onChange={(e) => setTraineeForm({ ...traineeForm, pincode: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                </div>
                <div className="mt-3 max-w-sm">
                  <label htmlFor="trainee-employment" className="form-label">Current employment status (optional)</label>
                  <select id="trainee-employment" className="form-input" value={traineeForm.currentEmploymentStatusId || ''}
                    disabled={refLoading || !!refError} onChange={(event) => setTraineeForm((form) => ({
                      ...form, currentEmploymentStatusId: Number(event.target.value),
                    }))}>
                    <option value="">Not provided</option>
                    {refData?.employmentStatuses.filter((status) => status.code !== 'UNKNOWN').map((status) => (
                      <option key={status.id} value={status.id}>{status.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <fieldset className="space-y-2 rounded-md border border-slate-300 p-4">
                <legend className="px-1 text-sm font-semibold text-navy">Core data processing consent *</legend>
                <label htmlFor="trainee-processing-consent" className="flex items-start gap-3 text-sm text-slate-800">
                  <input id="trainee-processing-consent" type="checkbox" className="mt-1 size-4 shrink-0 accent-navy"
                    required checked={traineeForm.dataProcessingConsent} aria-describedby="trainee-consent-purpose"
                    onChange={(event) => setTraineeForm((form) => ({ ...form, dataProcessingConsent: event.target.checked }))} />
                  I agree to the use of my information to create my trainee account and manage my registration and training records.
                </label>
                <p id="trainee-consent-purpose" className="text-xs text-slate-600">
                  You can manage employment follow-up, employer sharing, and communication permissions separately in the trainee portal.
                </p>
              </fieldset>

              <div>
                <button
                  type="submit"
                  disabled={submitting || refLoading || !!refError || !refData}
                  className="w-full btn-primary justify-center py-2.5 text-sm font-medium disabled:opacity-50"
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
            <p className="mb-4 text-xs text-slate-600">Fields marked * are required.</p>

            <form onSubmit={handleProviderSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="provider-name" className="block text-xs font-medium text-gray-700 mb-1">Institution / Provider Name *</label>
                  <input
                    id="provider-name"
                    maxLength={200}
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
                    maxLength={64}
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
                    maxLength={150}
                    type="text"
                    required
                    value={providerForm.contactPersonName}
                    onChange={(e) => setProviderForm({ ...providerForm, contactPersonName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="provider-contact-email" className="block text-xs font-medium text-gray-700 mb-1">Alternate Contact Email (optional)</label>
                  <input
                    id="provider-contact-email"
                    maxLength={255}
                    aria-describedby="provider-contact-email-note"
                    type="email"
                    value={providerForm.contactEmail}
                    onChange={(e) => setProviderForm({ ...providerForm, contactEmail: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                  <p id="provider-contact-email-note" className="mt-1 text-xs text-slate-600">Uses your account email when left blank.</p>
                </div>
                <div>
                  <label htmlFor="provider-contact-phone" className="block text-xs font-medium text-gray-700 mb-1">Contact Phone (optional)</label>
                  <input
                    id="provider-contact-phone"
                    maxLength={15}
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
                  <StateDistrictFields prefix="provider" reference={refData} required disabled={refLoading || !!refError}
                    stateId={providerForm.stateId} districtId={providerForm.districtId}
                    onChange={(stateId, districtId) => setProviderForm((form) => ({ ...form, stateId, districtId }))} />
                  <div>
                    <label htmlFor="provider-pincode" className="block text-xs font-medium text-gray-700 mb-1">PIN Code *</label>
                    <input
                      id="provider-pincode"
                      required
                      inputMode="numeric"
                      pattern="[1-9][0-9]{5}"
                      title="Enter a six-digit PIN code"
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
                      minLength={3}
                      maxLength={80}
                      autoComplete="username"
                      type="text"
                      required
                      value={providerForm.username}
                      onChange={(e) => setProviderForm({ ...providerForm, username: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="provider-email" className="block text-xs font-medium text-gray-700 mb-1">Account Email *</label>
                    <input
                      id="provider-email"
                      maxLength={255}
                      autoComplete="email"
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
                      maxLength={100}
                      autoComplete="new-password"
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
                      maxLength={100}
                      autoComplete="new-password"
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
                  disabled={submitting || refLoading || !!refError || !refData}
                  className="w-full btn-primary justify-center py-2.5 text-sm font-medium disabled:opacity-50"
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
            <p className="mb-4 text-xs text-slate-600">Fields marked * are required.</p>

            <form onSubmit={handleEmployerSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="emp-company-name" className="block text-xs font-medium text-gray-700 mb-1">Company / Enterprise Name *</label>
                  <input
                    id="emp-company-name"
                    maxLength={200}
                    type="text"
                    required
                    value={employerForm.employerName}
                    onChange={(e) => setEmployerForm({ ...employerForm, employerName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="emp-reg-number" className="block text-xs font-medium text-gray-700 mb-1">CIN / Registration No. (optional)</label>
                  <input
                    id="emp-reg-number"
                    maxLength={64}
                    type="text"
                    value={employerForm.registrationNumber}
                    onChange={(e) => setEmployerForm({ ...employerForm, registrationNumber: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="emp-gstin" className="block text-xs font-medium text-gray-700 mb-1">GSTIN (optional)</label>
                  <input
                    id="emp-gstin"
                    pattern="[A-Za-z0-9]{10,15}"
                    title="Enter 10 to 15 letters or digits"
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
                    maxLength={150}
                    type="text"
                    required
                    value={employerForm.contactPersonName}
                    onChange={(e) => setEmployerForm({ ...employerForm, contactPersonName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                </div>
                <div>
                  <label htmlFor="emp-contact-email" className="block text-xs font-medium text-gray-700 mb-1">Alternate Contact Email (optional)</label>
                  <input
                    id="emp-contact-email"
                    maxLength={255}
                    aria-describedby="emp-contact-email-note"
                    type="email"
                    value={employerForm.contactEmail}
                    onChange={(e) => setEmployerForm({ ...employerForm, contactEmail: e.target.value })}
                    className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                  />
                  <p id="emp-contact-email-note" className="mt-1 text-xs text-slate-600">Uses your account email when left blank.</p>
                </div>
                <div>
                  <label htmlFor="emp-contact-phone" className="block text-xs font-medium text-gray-700 mb-1">Phone (optional)</label>
                  <input
                    id="emp-contact-phone"
                    maxLength={15}
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
                  <StateDistrictFields prefix="emp" reference={refData} required disabled={refLoading || !!refError}
                    stateId={employerForm.stateId} districtId={employerForm.districtId}
                    onChange={(stateId, districtId) => setEmployerForm((form) => ({ ...form, stateId, districtId }))} />
                </div>
              </div>

              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-800 mb-3">Administrator Account Details</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="emp-username" className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
                    <input
                      id="emp-username"
                      minLength={3}
                      maxLength={80}
                      autoComplete="username"
                      type="text"
                      required
                      value={employerForm.username}
                      onChange={(e) => setEmployerForm({ ...employerForm, username: e.target.value })}
                      className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="emp-email" className="block text-xs font-medium text-gray-700 mb-1">Account Email *</label>
                    <input
                      id="emp-email"
                      maxLength={255}
                      autoComplete="email"
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
                      maxLength={100}
                      autoComplete="new-password"
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
                      maxLength={100}
                      autoComplete="new-password"
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
                  disabled={submitting || refLoading || !!refError || !refData}
                  className="w-full btn-primary justify-center py-2.5 text-sm font-medium disabled:opacity-50"
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
                  <div><span className="font-medium text-gray-500">Contact email hint:</span> {statusResult.contactEmail}</div>
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
