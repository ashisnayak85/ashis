/**
 * Renders 5 stars. Two modes:
 *  - readOnly (default): shows `value` (can be a decimal like 4.6 - stars are
 *    proportionally filled via a CSS clip, same idea as Flipkart/Amazon list badges)
 *  - interactive (pass onChange): click to pick 1-5, used on the "rate your visit" form
 */
export default function StarRating({ value = 0, onChange, size = 20, readOnly = true }) {
  const stars = [1, 2, 3, 4, 5];

  if (readOnly) {
    return (
      <span className="star-rating" style={{ fontSize: size }} aria-label={`${value} out of 5 stars`}>
        {stars.map((n) => {
          const fillPct = Math.max(0, Math.min(1, value - (n - 1))) * 100;
          return (
            <span className="star-wrap" key={n}>
              <span className="star star-empty">★</span>
              <span className="star star-filled" style={{ width: `${fillPct}%` }}>★</span>
            </span>
          );
        })}
      </span>
    );
  }

  return (
    <span className="star-rating star-rating-input" style={{ fontSize: size }}>
      {stars.map((n) => (
        <button
          type="button"
          key={n}
          className={`star-btn ${n <= value ? "star-btn-active" : ""}`}
          onClick={() => onChange?.(n)}
          aria-label={`Rate ${n} star${n > 1 ? "s" : ""}`}
        >
          ★
        </button>
      ))}
    </span>
  );
}
