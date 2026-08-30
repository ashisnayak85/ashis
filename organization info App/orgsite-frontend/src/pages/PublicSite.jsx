import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { getPublicOrg } from "../api/organization";
import { resolveImageUrl } from "../api/config";

const CATEGORY_LABELS = {
  RESTAURANT: "Restaurant",
  CAFE_TEA_SHOP: "Cafe / Tea Shop",
  SCHOOL: "School",
  RETAIL_SHOP: "Shop",
  SALON_SPA: "Salon & Spa",
  GYM_FITNESS: "Gym & Fitness",
  CLINIC: "Clinic",
  OTHER: "Business",
};

export default function PublicSite() {
  const { slug } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError(false);
    getPublicOrg(slug)
      .then(setData)
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) return <div className="public-loading">Loading...</div>;

  if (error || !data) {
    return (
      <div className="public-not-found">
        <h1>Page not found</h1>
        <p>We couldn't find a business page at "/{slug}".</p>
        <Link to="/">Go home</Link>
      </div>
    );
  }

  const { organization: org, gallery, items, testimonials, announcements } = data;
  const theme = org.themeColor || "#2563eb";

  return (
    <div className="public-page" style={{ "--theme-color": theme }}>
      <header className="public-hero" style={org.coverImageUrl ? { backgroundImage: `url(${resolveImageUrl(org.coverImageUrl)})` } : undefined}>
        <div className="public-hero-overlay">
          {org.logoUrl && <img className="public-logo" src={resolveImageUrl(org.logoUrl)} alt={`${org.name} logo`} />}
          <span className="public-category-tag">{CATEGORY_LABELS[org.category] || org.category}</span>
          <h1>{org.name}</h1>
          {org.tagline && <p className="public-tagline">{org.tagline}</p>}
          <div className="public-hero-cta">
            {org.phone && <a className="btn-primary" href={`tel:${org.phone}`}>Call Now</a>}
            {org.whatsapp && (
              <a className="btn-secondary-light" href={`https://wa.me/${org.whatsapp.replace(/[^0-9]/g, "")}`} target="_blank" rel="noreferrer">
                WhatsApp
              </a>
            )}
          </div>
        </div>
      </header>

      {announcements?.length > 0 && (
        <div className="public-announcement-bar">
          {announcements.map((a) => (
            <span key={a.id}>📢 <strong>{a.title}</strong> — {a.description}</span>
          ))}
        </div>
      )}

      <main className="public-main">
        {org.description && (
          <section className="public-section">
            <h2>About</h2>
            <p>{org.description}</p>
          </section>
        )}

        {items?.length > 0 && (
          <section className="public-section">
            <h2>Menu &amp; Services</h2>
            <div className="public-items-grid">
              {items.map((item) => (
                <div className="public-item-card" key={item.id}>
                  {item.imageUrl && <img src={resolveImageUrl(item.imageUrl)} alt={item.title} />}
                  <div className="public-item-body">
                    <div className="public-item-top">
                      <strong>{item.title}</strong>
                      {item.price && <span className="price-tag">{item.price}</span>}
                    </div>
                    {item.description && <p>{item.description}</p>}
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {gallery?.length > 0 && (
          <section className="public-section">
            <h2>Gallery</h2>
            <div className="public-gallery-grid">
              {gallery.map((g) => (
                <img key={g.id} src={resolveImageUrl(g.imageUrl)} alt={g.title || "Gallery photo"} />
              ))}
            </div>
          </section>
        )}

        {testimonials?.length > 0 && (
          <section className="public-section">
            <h2>What people say</h2>
            <div className="public-testimonials-grid">
              {testimonials.map((t) => (
                <div className="public-testimonial-card" key={t.id}>
                  <p>&ldquo;{t.description}&rdquo;</p>
                  <strong>— {t.title}</strong>
                </div>
              ))}
            </div>
          </section>
        )}

        <section className="public-section public-contact">
          <h2>Visit / Contact</h2>
          <div className="public-contact-grid">
            <div>
              {org.address && <p>📍 {org.address}</p>}
              {org.hoursText && <p>🕒 {org.hoursText}</p>}
              {org.phone && <p>📞 <a href={`tel:${org.phone}`}>{org.phone}</a></p>}
              {org.email && <p>✉️ <a href={`mailto:${org.email}`}>{org.email}</a></p>}
              <div className="public-social-links">
                {org.facebookUrl && <a href={org.facebookUrl} target="_blank" rel="noreferrer">Facebook</a>}
                {org.instagramUrl && <a href={org.instagramUrl} target="_blank" rel="noreferrer">Instagram</a>}
                {org.websiteUrl && <a href={org.websiteUrl} target="_blank" rel="noreferrer">Website</a>}
              </div>
            </div>
            {org.mapEmbedUrl && (
              <iframe
                title="Location map"
                src={org.mapEmbedUrl}
                loading="lazy"
                className="public-map"
              />
            )}
          </div>
        </section>
      </main>

      <footer className="public-footer">
        <p>{org.name} · Powered by OrgSite</p>
      </footer>
    </div>
  );
}
