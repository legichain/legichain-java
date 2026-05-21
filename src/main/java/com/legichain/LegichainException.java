package com.legichain;

import com.legichain.model.ProblemDetails;

/** Thrown for every non-2xx API response. Carries the full RFC 7807
 *  Problem Details body so callers can branch on {@link #code()} or
 *  {@link #status()} and never need to scrape free-text. */
public class LegichainException extends RuntimeException {

    private final ProblemDetails problem;

    public LegichainException(ProblemDetails problem) {
        super(formatMessage(problem));
        this.problem = problem;
    }

    public LegichainException(String message) {
        super(message);
        this.problem = null;
    }

    public LegichainException(String message, Throwable cause) {
        super(message, cause);
        this.problem = null;
    }

    private static String formatMessage(ProblemDetails p) {
        if (p == null) return "Legichain API error";
        String d = p.detail() == null ? p.code() : p.detail();
        return "Legichain " + p.status() + " " + p.title() + " (" + p.code() + "): " + d;
    }

    public ProblemDetails problem() { return problem; }

    /** HTTP status of the failing response, or 0 if unknown. */
    public int status() { return problem == null ? 0 : problem.status(); }
    /** Stable error code (e.g. {@code AUTH_002_INVALID_TOKEN}). */
    public String code() { return problem == null ? null : problem.code(); }
    /** Short title (human-readable). */
    public String title() { return problem == null ? null : problem.title(); }
    /** Long explanation. */
    public String detail() { return problem == null ? null : problem.detail(); }
    /** Request ID — quote when contacting support. */
    public String instance() { return problem == null ? null : problem.instance(); }
}
