import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout";
import { listContentBlocks, createContentBlock, updateContentBlock, deleteContentBlock } from "../../api/contentBlocks";
import { uploadImage } from "../../api/upload";
import { resolveImageUrl } from "../../api/config";

const TYPES = [
  { value: "GALLERY", label: "Gallery Photo" },
  { value: "ITEM", label: "Menu / Product / Service" },
  { value: "TESTIMONIAL", label: "Testimonial" },
  { value: "ANNOUNCEMENT", label: "Announcement" },
];

const EMPTY_FORM = {
  type: "ITEM",
  title: "",
  subtitle: "",
  description: "",
  imageUrl: "",
  price: "",
  sortOrder: 0,
  visible: true,
};

export default function AdminContent() {
  const [blocks, setBlocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("ALL");

  function load() {
    setLoading(true);
    listContentBlocks()
      .then(setBlocks)
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function startEdit(block) {
    setEditingId(block.id);
    setForm({ ...block });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetForm() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function handleImageUpload(file) {
    if (!file) return;
    setUploading(true);
    try {
      const url = await uploadImage(file);
      update("imageUrl", url);
    } catch (err) {
      setError(err.response?.data?.message || "Upload failed.");
    } finally {
      setUploading(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    try {
      if (editingId) {
        await updateContentBlock(editingId, form);
      } else {
        await createContentBlock(form);
      }
      resetForm();
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save.");
    }
  }

  async function handleDelete(id) {
    if (!confirm("Delete this item?")) return;
    await deleteContentBlock(id);
    if (editingId === id) resetForm();
    load();
  }

  const visibleBlocks = filter === "ALL" ? blocks : blocks.filter((b) => b.type === filter);

  return (
    <AdminLayout>
      <h1>Content</h1>
      <p className="admin-sub">Menu items, gallery photos, testimonials, and announcements shown on your public page.</p>

      <form className="content-form" onSubmit={handleSubmit}>
        <h2>{editingId ? "Edit item" : "Add new item"}</h2>
        {error && <div className="alert-error">{error}</div>}

        <div className="form-grid">
          <label>
            Type
            <select value={form.type} onChange={(e) => update("type", e.target.value)}>
              {TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </label>

          <label>
            Sort order
            <input type="number" value={form.sortOrder} onChange={(e) => update("sortOrder", Number(e.target.value))} />
          </label>

          <label className="span-2">
            {form.type === "TESTIMONIAL" ? "Customer name" : "Title"}
            <input value={form.title || ""} onChange={(e) => update("title", e.target.value)} required />
          </label>

          {form.type === "ITEM" && (
            <label>
              Price
              <input placeholder="e.g. ₹150" value={form.price || ""} onChange={(e) => update("price", e.target.value)} />
            </label>
          )}

          <label className="span-2">
            {form.type === "TESTIMONIAL" ? "Quote" : "Description"}
            <textarea rows={3} value={form.description || ""} onChange={(e) => update("description", e.target.value)} />
          </label>

          <label>
            Image
            <input type="file" accept="image/*" onChange={(e) => handleImageUpload(e.target.files[0])} />
            {uploading && <span className="upload-status">Uploading...</span>}
            {form.imageUrl && (
              <div className="preview-row">
                <img className="preview-thumb" src={resolveImageUrl(form.imageUrl)} alt="Preview" />
                <button type="button" className="remove-image-btn" onClick={() => update("imageUrl", "")}>
                  Remove image
                </button>
              </div>
            )}
          </label>

          <label className="checkbox-label">
            <input type="checkbox" checked={form.visible} onChange={(e) => update("visible", e.target.checked)} />
            Visible on public page
          </label>
        </div>

        <div className="form-actions">
          <button className="btn-primary" type="submit">{editingId ? "Update" : "Add item"}</button>
          {editingId && <button className="btn-secondary" type="button" onClick={resetForm}>Cancel</button>}
        </div>
      </form>

      <div className="content-list-header">
        <h2>Your items ({blocks.length})</h2>
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">All types</option>
          {TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </select>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : visibleBlocks.length === 0 ? (
        <p className="empty-state">Nothing here yet. Add your first item above.</p>
      ) : (
        <div className="content-list">
          {visibleBlocks.map((b) => (
            <div className="content-list-item" key={b.id}>
              {b.imageUrl && <img src={resolveImageUrl(b.imageUrl)} alt={b.title} />}
              <div className="content-list-item-body">
                <div className="content-list-item-top">
                  <span className="type-badge">{b.type}</span>
                  {!b.visible && <span className="type-badge hidden">Hidden</span>}
                </div>
                <strong>{b.title}</strong> {b.price && <span className="price-tag">{b.price}</span>}
                <p>{b.description}</p>
              </div>
              <div className="content-list-item-actions">
                <button className="btn-secondary" onClick={() => startEdit(b)}>Edit</button>
                <button className="btn-danger" onClick={() => handleDelete(b.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </AdminLayout>
  );
}
