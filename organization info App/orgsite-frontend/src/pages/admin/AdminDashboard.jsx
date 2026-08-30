import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout";
import { getMyOrganization, updateMyOrganization, setPublished } from "../../api/organization";
import { uploadImage } from "../../api/upload";
import { resolveImageUrl } from "../../api/config";

const CATEGORIES = [
  "RESTAURANT", "CAFE_TEA_SHOP", "SCHOOL", "RETAIL_SHOP", "SALON_SPA", "GYM_FITNESS", "CLINIC", "OTHER",
];

export default function AdminDashboard() {
  const [org, setOrg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [uploadingLogo, setUploadingLogo] = useState(false);
  const [uploadingCover, setUploadingCover] = useState(false);

  useEffect(() => {
    getMyOrganization()
      .then(setOrg)
      .finally(() => setLoading(false));
  }, []);

  function update(field, value) {
    setOrg((o) => ({ ...o, [field]: value }));
  }

  async function handleSave(e) {
    e.preventDefault();
    setSaving(true);
    setMessage("");
    try {
      const updated = await updateMyOrganization(org);
      setOrg(updated);
      setMessage("Saved.");
    } catch (err) {
      setMessage(err.response?.data?.message || "Failed to save.");
    } finally {
      setSaving(false);
      setTimeout(() => setMessage(""), 3000);
    }
  }

  async function handleTogglePublish() {
    const updated = await setPublished(!org.published);
    setOrg(updated);
  }

  async function handleImageUpload(field, file) {
    if (!file) return;
    const setUploading = field === "logoUrl" ? setUploadingLogo : setUploadingCover;
    setUploading(true);
    try {
      const url = await uploadImage(file);
      update(field, url);
    } catch (err) {
      setMessage(err.response?.data?.message || "Upload failed.");
    } finally {
      setUploading(false);
    }
  }

  if (loading) return <AdminLayout><p>Loading...</p></AdminLayout>;
  if (!org) return <AdminLayout><p>Could not load your business profile.</p></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-header">
        <h1>Business Profile</h1>
        <div className="publish-toggle">
          <span className={`status-pill ${org.published ? "live" : "draft"}`}>
            {org.published ? "Live" : "Draft"}
          </span>
          <button className="btn-secondary" onClick={handleTogglePublish} type="button">
            {org.published ? "Unpublish" : "Publish page"}
          </button>
        </div>
      </div>
      <p className="admin-sub">Your public page: <code>/{org.slug}</code></p>

      <form className="profile-form" onSubmit={handleSave}>
        <div className="form-grid">
          <label>
            Business name
            <input value={org.name || ""} onChange={(e) => update("name", e.target.value)} required />
          </label>

          <label>
            Category
            <select value={org.category || "OTHER"} onChange={(e) => update("category", e.target.value)}>
              {CATEGORIES.map((c) => <option key={c} value={c}>{c.replaceAll("_", " ")}</option>)}
            </select>
          </label>

          <label className="span-2">
            Tagline
            <input
              placeholder="One short line under your name"
              value={org.tagline || ""}
              onChange={(e) => update("tagline", e.target.value)}
            />
          </label>

          <label className="span-2">
            Description / About
            <textarea rows={4} value={org.description || ""} onChange={(e) => update("description", e.target.value)} />
          </label>

          <label>
            Logo
            <input type="file" accept="image/*" onChange={(e) => handleImageUpload("logoUrl", e.target.files[0])} />
            {uploadingLogo && <span className="upload-status">Uploading...</span>}
            {org.logoUrl && (
              <div className="preview-row">
                <img className="preview-thumb" src={resolveImageUrl(org.logoUrl)} alt="Logo preview" />
                <button type="button" className="remove-image-btn" onClick={() => update("logoUrl", "")}>
                  Remove image
                </button>
              </div>
            )}
          </label>

          <label>
            Cover image
            <input type="file" accept="image/*" onChange={(e) => handleImageUpload("coverImageUrl", e.target.files[0])} />
            {uploadingCover && <span className="upload-status">Uploading...</span>}
            {org.coverImageUrl && (
              <div className="preview-row">
                <img className="preview-thumb wide" src={resolveImageUrl(org.coverImageUrl)} alt="Cover preview" />
                <button type="button" className="remove-image-btn" onClick={() => update("coverImageUrl", "")}>
                  Remove image
                </button>
              </div>
            )}
          </label>

          <label>
            Theme color
            <input type="color" value={org.themeColor || "#2563eb"} onChange={(e) => update("themeColor", e.target.value)} />
          </label>

          <label>
            Phone
            <input value={org.phone || ""} onChange={(e) => update("phone", e.target.value)} />
          </label>

          <label>
            WhatsApp number
            <input value={org.whatsapp || ""} onChange={(e) => update("whatsapp", e.target.value)} />
          </label>

          <label>
            Contact email
            <input type="email" value={org.email || ""} onChange={(e) => update("email", e.target.value)} />
          </label>

          <label className="span-2">
            Address
            <input value={org.address || ""} onChange={(e) => update("address", e.target.value)} />
          </label>

          <label className="span-2">
            Hours
            <input
              placeholder="e.g. Mon-Sat: 9am - 9pm, Sun: Closed"
              value={org.hoursText || ""}
              onChange={(e) => update("hoursText", e.target.value)}
            />
          </label>

          <label className="span-2">
            Google Maps embed URL (optional)
            <input value={org.mapEmbedUrl || ""} onChange={(e) => update("mapEmbedUrl", e.target.value)} />
          </label>

          <label>
            Facebook URL
            <input value={org.facebookUrl || ""} onChange={(e) => update("facebookUrl", e.target.value)} />
          </label>

          <label>
            Instagram URL
            <input value={org.instagramUrl || ""} onChange={(e) => update("instagramUrl", e.target.value)} />
          </label>

          <label>
            Website URL
            <input value={org.websiteUrl || ""} onChange={(e) => update("websiteUrl", e.target.value)} />
          </label>
        </div>

        <div className="form-actions">
          <button className="btn-primary" type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save changes"}
          </button>
          {message && <span className="save-message">{message}</span>}
        </div>
      </form>
    </AdminLayout>
  );
}
