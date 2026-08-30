export function ErrorBanner({ message, errors }) {
  const hasList = Array.isArray(errors) && errors.length > 0;
  if (!message && !hasList) return null;
  return (
    <div className="banner banner-error">
      {hasList ? (
        <ul className="banner-error-list">
          {errors.map((err, i) => (
            <li key={i}>{err}</li>
          ))}
        </ul>
      ) : (
        message
      )}
    </div>
  );
}

export function SuccessBanner({ message }) {
  if (!message) return null;
  return <div className="banner banner-success">{message}</div>;
}

export function Loading() {
  return <div className="page-loading">Loading…</div>;
}
