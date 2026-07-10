#!/usr/bin/env python3
import html
import fnmatch
import os
import re
import shutil
import subprocess
import sys
from urllib.parse import parse_qs


WORKSPACE_ROOT = "/workspaces/codespaces-blank"


def print_header():
    sys.stdout.write("Content-Type: text/html; charset=utf-8\r\n\r\n")


def escape(s: str) -> str:
    return html.escape(s, quote=True)


def get_package_name(file_path: str) -> str:
    try:
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            for _ in range(80):
                line = f.readline()
                if not line:
                    break
                m = re.match(r"\s*package\s+([\w\.]+)\s*;", line)
                if m:
                    return m.group(1)
    except OSError:
        return "(unavailable)"
    return "(default/no package)"


def _run_search_with_rg(
    query: str,
    include: str,
    exclude: str,
    is_regex: bool,
    ignore_case: bool,
):
    cmd = ["rg", "--line-number", "--with-filename", "--no-heading", "--color", "never"]
    if is_regex:
        cmd.append("--regexp")
    else:
        cmd.append("--fixed-strings")
    if ignore_case:
        cmd.append("--ignore-case")

    if include:
        cmd.extend(["--glob", include])
    if exclude:
        cmd.extend(["--glob", "!" + exclude])

    cmd.extend([query, "."])

    proc = subprocess.run(
        cmd,
        cwd=WORKSPACE_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="ignore",
        check=False,
    )
    return proc.returncode, proc.stdout, proc.stderr


def _run_search_with_python(
    query: str,
    include: str,
    exclude: str,
    is_regex: bool,
    ignore_case: bool,
):
    include_glob = include or "**/*"
    exclude_glob = exclude or ""
    flags = re.IGNORECASE if ignore_case else 0
    results = []

    if is_regex:
        try:
            pattern = re.compile(query, flags)
        except re.error as ex:
            return 2, "", "Invalid regex: {}".format(ex)
    else:
        needle = query.lower() if ignore_case else query

    for root, _, files in os.walk(WORKSPACE_ROOT):
        for name in files:
            abs_path = os.path.join(root, name)
            rel_path = os.path.relpath(abs_path, WORKSPACE_ROOT).replace(os.sep, "/")

            if include_glob and not fnmatch.fnmatch(rel_path, include_glob):
                continue
            if exclude_glob and fnmatch.fnmatch(rel_path, exclude_glob):
                continue

            try:
                with open(abs_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line_no, line in enumerate(f, start=1):
                        hay = line if not ignore_case else line.lower()
                        matched = pattern.search(line) if is_regex else (needle in hay)
                        if matched:
                            results.append("{}:{}:{}".format(rel_path, line_no, line.rstrip("\n")))
            except OSError:
                continue

    if results:
        return 0, "\n".join(results) + "\n", ""
    return 1, "", ""


def run_search(query: str, include: str, exclude: str, is_regex: bool, ignore_case: bool):
    if shutil.which("rg"):
        return _run_search_with_rg(query, include, exclude, is_regex, ignore_case)
    return _run_search_with_python(query, include, exclude, is_regex, ignore_case)


def get_param(params, key: str, default: str = "") -> str:
    values = params.get(key)
    if not values:
        return default
    return values[0]


def render_page(title: str, body: str):
    print_header()
    sys.stdout.write(
        """<!doctype html>
<html lang=\"en\">
<head>
  <meta charset=\"utf-8\">
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
  <title>{title}</title>
  <style>
    body {{ font-family: 'Segoe UI', Tahoma, sans-serif; margin: 0; background: #f6f9fd; color: #1f2937; }}
    .wrap {{ max-width: 1100px; margin: 28px auto; padding: 0 16px; }}
    .card {{ background: #fff; border: 1px solid #dbe3ef; border-radius: 12px; padding: 22px; }}
    h1 {{ margin-top: 0; }}
    h2 {{ margin-top: 24px; }}
    .file {{ border: 1px solid #e4e9f1; border-radius: 10px; margin: 14px 0; padding: 12px; }}
    .meta {{ color: #4b5563; font-size: 0.95rem; margin-bottom: 8px; }}
    ul {{ margin: 0; padding-left: 22px; }}
    li {{ margin: 6px 0; }}
    code {{ background: #eef2f7; border: 1px solid #d7dee8; border-radius: 5px; padding: 2px 6px; }}
    .line {{ font-family: Consolas, monospace; white-space: pre-wrap; }}
    a {{ color: #0b63d8; text-decoration: none; }}
    a:hover {{ text-decoration: underline; }}
    .err {{ background: #fff1f2; border-left: 4px solid #e11d48; padding: 10px 12px; border-radius: 8px; }}
    .ok {{ background: #effaf3; border-left: 4px solid #16a34a; padding: 10px 12px; border-radius: 8px; }}
  </style>
</head>
<body>
  <main class=\"wrap\">
    <section class=\"card\">
      {body}
    </section>
  </main>
</body>
</html>
""".format(title=escape(title), body=body)
    )


def main():
    params = parse_qs(os.environ.get("QUERY_STRING", ""), keep_blank_values=True)
    query = get_param(params, "query", "").strip()
    include = get_param(params, "include", "src/**/*.java").strip()
    exclude = get_param(params, "exclude", "").strip()
    is_regex = get_param(params, "regex", "") == "1"
    ignore_case = get_param(params, "ignore_case", "") == "1"

    if not query:
        render_page(
            "Code Search Tracker",
            "<h1>Code Search Tracker</h1><p><a href='/docs/code-search.html'>Back to Search Form</a></p>"
            "<div class='err'>Missing query. Please provide keywords or a pattern.</div>",
        )
        return

    rc, out, err = run_search(query, include, exclude, is_regex, ignore_case)

    if rc not in (0, 1):
        render_page(
            "Code Search Tracker",
            "<h1>Code Search Tracker</h1><p><a href='/docs/code-search.html'>Back to Search Form</a></p>"
            + "<div class='err'><strong>Search error:</strong><br>"
            + escape(err or "Unknown error")
            + "</div>",
        )
        return

    lines = [ln for ln in out.splitlines() if ln.strip()]
    grouped = {}
    for ln in lines:
        m = re.match(r"^(.*?):(\d+):(.*)$", ln)
        if not m:
            continue
        file_path, line_no, content = m.group(1), int(m.group(2)), m.group(3)
        grouped.setdefault(file_path, []).append((line_no, content))

    body_parts = []
    body_parts.append("<h1>Code Search Results</h1>")
    body_parts.append("<p><a href='/docs/code-search.html'>Back to Search Form</a> | <a href='/docs/index.html'>Back to Home</a></p>")
    body_parts.append(
        "<div class='ok'><strong>Query:</strong> <code>{}</code> | <strong>Include:</strong> <code>{}</code>{}</div>".format(
            escape(query),
            escape(include),
            " | <strong>Exclude:</strong> <code>{}</code>".format(escape(exclude)) if exclude else "",
        )
    )

    if not grouped:
        body_parts.append("<h2>No matches found</h2>")
        render_page("Code Search Results", "".join(body_parts))
        return

    body_parts.append("<h2>Matched Files: {}</h2>".format(len(grouped)))
    for file_path in sorted(grouped.keys()):
        abs_path = os.path.join(WORKSPACE_ROOT, file_path.lstrip("./"))
        package_name = get_package_name(abs_path)
        body_parts.append("<div class='file'>")
        body_parts.append("<div><strong>File:</strong> <code>{}</code></div>".format(escape(file_path)))
        body_parts.append("<div class='meta'><strong>Package:</strong> <code>{}</code></div>".format(escape(package_name)))
        body_parts.append("<ul>")
        for line_no, content in sorted(grouped[file_path], key=lambda x: x[0]):
            body_parts.append(
                "<li><strong>Line {}</strong><div class='line'>{}</div></li>".format(
                    line_no, escape(content.rstrip())
                )
            )
        body_parts.append("</ul>")
        body_parts.append("</div>")

    render_page("Code Search Results", "".join(body_parts))


if __name__ == "__main__":
    main()
