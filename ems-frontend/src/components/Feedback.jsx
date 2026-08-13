export function ErrorBanner({ message }) {
  if (!message) return null;
  return <div className="banner banner-error">{message}</div>;
}

export function SuccessBanner({ message }) {
  if (!message) return null;
  return <div className="banner banner-success">{message}</div>;
}

export function Loading() {
  return <div className="page-loading">Loading…</div>;
}
