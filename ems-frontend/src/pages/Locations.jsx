import { useEffect, useState, useCallback, useMemo } from 'react';
import { Country, State } from 'country-state-city';
import { getLocations, createLocation, updateLocation, deleteLocation } from '../api/locations';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const emptyForm = {
  name: '', code: '', address: '',
  country: '', countryIso: '', state: '',
  pincode: '', officeContact: '', active: true,
};

// A phone number is "valid enough" here if it's mostly digits, optionally
// starting with +, 7-20 characters total - matches the backend's @Pattern
// in LocationDTO so the user sees the same rule before it ever reaches the server.
const PHONE_PATTERN = /^\+?[0-9][0-9\-\s]{6,18}[0-9]$/;

// All ~195 countries, sorted alphabetically - this is the single source of
// truth for both the Country dropdown and for validating "does this country
// actually exist" before the form can even be submitted.
const ALL_COUNTRIES = Country.getAllCountries().sort((a, b) => a.name.localeCompare(b.name));

export default function Locations() {
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');       // page-level errors (e.g. list failed to load)
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null); // null closed, {} new, {...} edit
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');    // modal-level: generic message
  const [formErrors, setFormErrors] = useState([]);  // modal-level: per-field messages
  const { hasRole } = useAuth();
  const canWrite = hasRole('ADMIN') || hasRole('MANAGER');

  // States for the currently selected country - recalculated only when the
  // chosen country changes, not on every keystroke elsewhere in the form.
  const availableStates = useMemo(
    () => (form.countryIso ? State.getStatesOfCountry(form.countryIso) : []),
    [form.countryIso]
  );

  const load = useCallback(() => {
    setLoading(true);
    getLocations()
      .then((res) => setLocations(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openNew() {
    setForm(emptyForm);
    setFormError('');
    setFormErrors([]);
    setEditing({});
  }

  function openEdit(loc) {
    // The backend only stores the country NAME (e.g. "India"), but the state
    // dropdown needs the country's ISO code to know which states to offer -
    // so look it up once here, by matching the stored name back to the list.
    const matchedCountry = ALL_COUNTRIES.find((c) => c.name === loc.country);
    setForm({
      name: loc.name, code: loc.code, address: loc.address || '',
      country: loc.country || '', countryIso: matchedCountry?.isoCode || '',
      state: loc.state || '', pincode: loc.pincode || '',
      officeContact: loc.officeContact || '', active: loc.active,
    });
    setFormError('');
    setFormErrors([]);
    setEditing(loc);
  }

  function handleCountryChange(isoCode) {
    const country = ALL_COUNTRIES.find((c) => c.isoCode === isoCode);
    // Changing the country invalidates whatever state was previously picked -
    // a state from the old country is never valid for the new one, so reset it.
    setForm({ ...form, country: country?.name || '', countryIso: isoCode, state: '' });
  }

  function validate() {
    if (!form.country) return 'Please select a country';
    if (availableStates.length > 0 && !form.state) return 'Please select a state';
    if (!PHONE_PATTERN.test(form.officeContact)) {
      return 'Enter a valid office contact number (digits only, optional leading +, 7-20 characters)';
    }
    return '';
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setFormError('');
    setFormErrors([]);
    const validationError = validate();
    if (validationError) {
      setFormError(validationError);
      return;
    }
    // countryIso is a frontend-only helper for looking up states - the
    // backend only knows about the plain name, so it's stripped before sending.
    const { countryIso, ...payload } = form;
    try {
      if (editing.id) {
        await updateLocation(editing.id, payload);
        setSuccess('Location updated');
      } else {
        await createLocation(payload);
        setSuccess('Location created');
      }
      setEditing(null);
      load();
    } catch (err) {
      // err.errors holds the backend's per-field messages (e.g. from @Valid),
      // so the user sees exactly which field(s) failed and why, without
      // closing the modal. Falls back to err.message when there's no list
      // (e.g. a duplicate-code error, which is a single message, not a list).
      setFormError(err.message);
      setFormErrors(err.errors || []);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this location?')) return;
    try {
      await deleteLocation(id);
      setSuccess('Location deleted');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Locations</h1>
        {canWrite && <button className="btn btn-primary" onClick={openNew}>+ New Location</button>}
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>City</th>
              <th>State</th>
              <th>Country</th>
              <th>Contact</th>
              <th>Employees</th>
              <th>Active</th>
              {canWrite && <th></th>}
            </tr>
          </thead>
          <tbody>
            {locations.length ? locations.map((l) => (
              <tr key={l.id}>
                <td>{l.code}</td>
                <td>{l.name}</td>
                <td>{l.city}</td>
                <td>{l.state}</td>
                <td>{l.country}</td>
                <td>{l.officeContact}</td>
                <td>{l.employeeCount ?? 0}</td>
                <td>{l.active ? 'Yes' : 'No'}</td>
                {canWrite && (
                  <td className="row-actions">
                    <button className="btn btn-link" onClick={() => openEdit(l)}>Edit</button>
                    {hasRole('ADMIN') && (
                      <button className="btn btn-link btn-danger" onClick={() => handleDelete(l.id)}>Delete</button>
                    )}
                  </td>
                )}
              </tr>
            )) : (
              <tr><td colSpan={9} className="empty-row">No locations found</td></tr>
            )}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Location' : 'New Location'}</h2>
            <ErrorBanner message={formError} errors={formErrors} />
            <div className="form-grid">
              <label>
                Name
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label>
                Code
                <input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} required />
              </label>
              <label>
                Address
                <input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </label>

              <label>
                Country
                <select value={form.countryIso} onChange={(e) => handleCountryChange(e.target.value)} required>
                  <option value="" disabled>Select country</option>
                  {ALL_COUNTRIES.map((c) => (
                    <option key={c.isoCode} value={c.isoCode}>{c.name}</option>
                  ))}
                </select>
              </label>

              <label>
                State
                <select
                  value={form.state}
                  onChange={(e) => setForm({ ...form, state: e.target.value })}
                  disabled={!form.countryIso || availableStates.length === 0}
                  required={availableStates.length > 0}
                >
                  <option value="" disabled>
                    {form.countryIso ? 'Select state' : 'Select a country first'}
                  </option>
                  {availableStates.map((s) => (
                    <option key={s.isoCode} value={s.name}>{s.name}</option>
                  ))}
                </select>
              </label>

              <label>
                Pincode
                <input value={form.pincode} onChange={(e) => setForm({ ...form, pincode: e.target.value })} />
              </label>
              <label>
                Office Contact Number
                <input
                  type="tel"
                  value={form.officeContact}
                  onChange={(e) => setForm({ ...form, officeContact: e.target.value })}
                  placeholder="+91 80-1234-5678"
                  required
                />
              </label>
              <label className="checkbox-label">
                <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
                Active
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-link" onClick={() => setEditing(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
