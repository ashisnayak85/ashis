import { useEffect, useState } from 'react';
import { getActiveDepartments } from '../api/departments';
import { getActiveLocations } from '../api/locations';
import { getFiles, fileDownloadUrl, QUALIFICATION_CERT_ENTITY_TYPE } from '../api/files';
import { ErrorBanner } from './Feedback';

const emptyForm = {
  employeeCode: '',
  firstName: '',
  lastName: '',
  email: '',
  mobile: '',
  dateOfBirth: '',
  dateOfJoining: '',
  salary: '',
  designation: '',
  departmentId: '',
  locationId: '',
  active: true,

  // Qualification & experience
  qualification: '',
  yearOfPassing: '',
  totalExperience: '',
  maritalStatus: '',
  aadharNumber: '',
  salaryCalculationBasis: '',

  // Present address
  presentAddressLine: '',
  presentCityDistrict: '',
  presentState: '',
  presentPincode: '',

  // Permanent address
  permanentAddressLine: '',
  permanentCityDistrict: '',
  permanentState: '',
  permanentPincode: '',

  // Bank details
  bankName: '',
  bankAccountNumber: '',
  bankIfscCode: '',

  // Statutory information
  pfApplicable: false,
  pfNumber: '',
  uanNumber: '',
  restrictPf: false,
  zeroPension: false,
  zeroPt: false,
  esiApplicable: false,
  esiNumber: '',
  esiDispensation: false,
};

// UI-only fields that mirror server fields but should never be sent to the
// backend as-is - stripped out in handleSubmit before the payload is built.
const UI_ONLY_FIELDS = ['sameAsPresent'];

export default function EmployeeFormModal({ employee, onClose, onSubmit }) {
  const [form, setForm] = useState(
    employee ? { ...emptyForm, ...employee, sameAsPresent: false } : { ...emptyForm, sameAsPresent: false }
  );
  const [departments, setDepartments] = useState([]);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [certificateFile, setCertificateFile] = useState(null);
  const [existingCertificate, setExistingCertificate] = useState(null);

  useEffect(() => {
    getActiveDepartments().then((res) => setDepartments(res.data || []));
  }, []);

  useEffect(() => {
    getActiveLocations().then((res) => setLocations(res.data || []));
  }, []);

  // If editing an employee who already has a certificate on file, show it
  // instead of leaving the upload box looking empty.
  useEffect(() => {
    if (!employee?.id) return;
    getFiles(QUALIFICATION_CERT_ENTITY_TYPE, employee.id)
      .then((res) => {
        const files = res.data || [];
        if (files.length) setExistingCertificate(files[files.length - 1]);
      })
      .catch(() => {}); // non-fatal - form still works without this
  }, [employee?.id]);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function toggleSameAsPresent(checked) {
    setForm((f) => ({
      ...f,
      sameAsPresent: checked,
      ...(checked
        ? {
            permanentAddressLine: f.presentAddressLine,
            permanentCityDistrict: f.presentCityDistrict,
            permanentState: f.presentState,
            permanentPincode: f.presentPincode,
          }
        : {}),
    }));
  }

  function updatePresentField(field, value) {
    setForm((f) => {
      const next = { ...f, [field]: value };
      // Keep permanent address mirrored live while "same as present" is checked.
      if (f.sameAsPresent) {
        const permanentField = 'permanent' + field.slice('present'.length);
        next[permanentField] = value;
      }
      return next;
    });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const cleaned = { ...form };
      UI_ONLY_FIELDS.forEach((f) => delete cleaned[f]);

      await onSubmit(
        {
          ...cleaned,
          departmentId: form.departmentId ? Number(form.departmentId) : null,
          locationId: form.locationId ? Number(form.locationId) : null,
          salary: form.salary === '' ? null : Number(form.salary),
          yearOfPassing: form.yearOfPassing === '' ? null : Number(form.yearOfPassing),
          totalExperience: form.totalExperience === '' ? null : Number(form.totalExperience),
        },
        certificateFile
      );
    } catch (err) {
      setError(err.message || 'Failed to save employee');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>{employee ? 'Edit Employee' : 'New Employee'}</h2>
        <ErrorBanner message={error} />

        <fieldset>
          <legend>Basic Information</legend>
          <div className="form-grid">
            <label>
              Employee Code
              <input value={form.employeeCode} onChange={(e) => update('employeeCode', e.target.value.toUpperCase())} required disabled={!!employee} />
            </label>
            <label>
              First Name
              <input value={form.firstName} onChange={(e) => update('firstName', e.target.value)} required />
            </label>
            <label>
              Last Name
              <input value={form.lastName} onChange={(e) => update('lastName', e.target.value)} required />
            </label>
            <label>
              Email
              <input type="email" value={form.email} onChange={(e) => update('email', e.target.value)} required />
            </label>
            <label>
              Mobile
              <input value={form.mobile || ''} onChange={(e) => update('mobile', e.target.value)} placeholder="10-digit Indian mobile" />
            </label>
            <label>
              Date of Birth
              <input type="date" value={form.dateOfBirth || ''} onChange={(e) => update('dateOfBirth', e.target.value)} />
            </label>
            <label>
              Date of Joining
              <input type="date" value={form.dateOfJoining || ''} onChange={(e) => update('dateOfJoining', e.target.value)} required />
            </label>
            <label>
              Designation
              <input value={form.designation || ''} onChange={(e) => update('designation', e.target.value)} />
            </label>
            <label>
              Department
              <select value={form.departmentId || ''} onChange={(e) => update('departmentId', e.target.value)} required>
                <option value="" disabled>Select department</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </label>
            <label>
              Location
              <select value={form.locationId || ''} onChange={(e) => update('locationId', e.target.value)}>
                <option value="">Select location</option>
                {locations.map((l) => (
                  <option key={l.id} value={l.id}>{l.name}{l.city ? ` (${l.city})` : ''}</option>
                ))}
              </select>
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={!!form.active} onChange={(e) => update('active', e.target.checked)} />
              Active
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Qualification &amp; Experience</legend>
          <div className="form-grid">
            <label>
              Qualification
              <input value={form.qualification || ''} onChange={(e) => update('qualification', e.target.value)} placeholder="e.g. B.Tech Computer Science" />
            </label>
            <label>
              Year of Passing
              <input type="number" min="1950" max="2100" value={form.yearOfPassing ?? ''} onChange={(e) => update('yearOfPassing', e.target.value)} />
            </label>
            <label>
              Total Experience (years)
              <input type="number" min="0" step="0.1" value={form.totalExperience ?? ''} onChange={(e) => update('totalExperience', e.target.value)} placeholder="e.g. 3.5" />
            </label>
            <label>
              Marital Status
              <select value={form.maritalStatus || ''} onChange={(e) => update('maritalStatus', e.target.value)}>
                <option value="">Select</option>
                <option value="SINGLE">Single</option>
                <option value="MARRIED">Married</option>
                <option value="DIVORCED">Divorced</option>
                <option value="WIDOWED">Widowed</option>
              </select>
            </label>
            <label>
              Aadhar Number
              <input
                value={form.aadharNumber || ''}
                onChange={(e) => update('aadharNumber', e.target.value.replace(/\D/g, '').slice(0, 12))}
                placeholder="12-digit Aadhar number"
                inputMode="numeric"
              />
            </label>
            <label>
              Salary
              <input type="number" min="0" value={form.salary ?? ''} onChange={(e) => update('salary', e.target.value)} />
            </label>
            <label>
              Salary Calculated On
              <select value={form.salaryCalculationBasis || ''} onChange={(e) => update('salaryCalculationBasis', e.target.value)}>
                <option value="">Select</option>
                <option value="MONTHLY">Monthly</option>
                <option value="DAILY">Daily</option>
                <option value="HOURLY">Hourly</option>
                <option value="ANNUAL">Annual</option>
              </select>
            </label>
          </div>

          <label className="file-upload-label">
            Qualification Certificate (PDF)
            <input type="file" accept="application/pdf" onChange={(e) => setCertificateFile(e.target.files[0] || null)} />
            {certificateFile && <span className="file-chip">Selected: {certificateFile.name}</span>}
            {!certificateFile && existingCertificate && (
              <span className="file-chip">
                Current file:{' '}
                <a href={fileDownloadUrl(existingCertificate.id)} target="_blank" rel="noreferrer">
                  {existingCertificate.originalFilename}
                </a>
              </span>
            )}
          </label>
        </fieldset>

        <fieldset>
          <legend>Present Address</legend>
          <div className="form-grid">
            <label className="span-2">
              Address
              <input value={form.presentAddressLine || ''} onChange={(e) => updatePresentField('presentAddressLine', e.target.value)} />
            </label>
            <label>
              City / District
              <input value={form.presentCityDistrict || ''} onChange={(e) => updatePresentField('presentCityDistrict', e.target.value)} />
            </label>
            <label>
              State
              <input value={form.presentState || ''} onChange={(e) => updatePresentField('presentState', e.target.value)} />
            </label>
            <label>
              Pincode
              <input value={form.presentPincode || ''} onChange={(e) => updatePresentField('presentPincode', e.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>
            Permanent Address
            <label className="checkbox-label inline-checkbox">
              <input type="checkbox" checked={!!form.sameAsPresent} onChange={(e) => toggleSameAsPresent(e.target.checked)} />
              Same as present address
            </label>
          </legend>
          <div className="form-grid">
            <label className="span-2">
              Address
              <input value={form.permanentAddressLine || ''} onChange={(e) => update('permanentAddressLine', e.target.value)} disabled={form.sameAsPresent} />
            </label>
            <label>
              City / District
              <input value={form.permanentCityDistrict || ''} onChange={(e) => update('permanentCityDistrict', e.target.value)} disabled={form.sameAsPresent} />
            </label>
            <label>
              State
              <input value={form.permanentState || ''} onChange={(e) => update('permanentState', e.target.value)} disabled={form.sameAsPresent} />
            </label>
            <label>
              Pincode
              <input
                value={form.permanentPincode || ''}
                onChange={(e) => update('permanentPincode', e.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                disabled={form.sameAsPresent}
              />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Bank Details</legend>
          <div className="form-grid">
            <label>
              Bank Name
              <input value={form.bankName || ''} onChange={(e) => update('bankName', e.target.value)} />
            </label>
            <label>
              Account Number
              <input value={form.bankAccountNumber || ''} onChange={(e) => update('bankAccountNumber', e.target.value.replace(/\D/g, ''))} inputMode="numeric" />
            </label>
            <label>
              IFSC Code
              <input value={form.bankIfscCode || ''} onChange={(e) => update('bankIfscCode', e.target.value.toUpperCase())} placeholder="e.g. HDFC0001234" maxLength={11} />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Statutory Information</legend>
          <div className="form-grid">
            <label className="checkbox-label">
              <input type="checkbox" checked={!!form.pfApplicable} onChange={(e) => update('pfApplicable', e.target.checked)} />
              PF Applicable
            </label>
            <label>
              PF Number
              <input value={form.pfNumber || ''} onChange={(e) => update('pfNumber', e.target.value)} disabled={!form.pfApplicable} />
            </label>
            <label>
              UAN Number
              <input value={form.uanNumber || ''} onChange={(e) => update('uanNumber', e.target.value)} disabled={!form.pfApplicable} />
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={!!form.restrictPf} onChange={(e) => update('restrictPf', e.target.checked)} disabled={!form.pfApplicable} />
              Restrict PF
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={!!form.zeroPension} onChange={(e) => update('zeroPension', e.target.checked)} disabled={!form.pfApplicable} />
              Zero Pension
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={!!form.zeroPt} onChange={(e) => update('zeroPt', e.target.checked)} />
              Zero PT
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={!!form.esiApplicable} onChange={(e) => update('esiApplicable', e.target.checked)} />
              ESI Applicable
            </label>
            <label>
              ESI Number
              <input value={form.esiNumber || ''} onChange={(e) => update('esiNumber', e.target.value)} disabled={!form.esiApplicable} />
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={!!form.esiDispensation} onChange={(e) => update('esiDispensation', e.target.checked)} disabled={!form.esiApplicable} />
              ESI Dispensation
            </label>
          </div>
        </fieldset>

        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  );
}
