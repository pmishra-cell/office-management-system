#!/usr/bin/env python3
import json
import os
import sys

WORKSPACE_ROOT = "/workspaces/codespaces-blank"
DATA_FILE = os.path.join(WORKSPACE_ROOT, "docs", "txt", "general-commands.txt")


def send_json(status_code: int, payload: dict):
    sys.stdout.write(f"Status: {status_code}\r\n")
    sys.stdout.write("Content-Type: application/json; charset=utf-8\r\n\r\n")
    sys.stdout.write(json.dumps(payload))


def ensure_parent_dir():
    os.makedirs(os.path.dirname(DATA_FILE), exist_ok=True)


def read_content() -> str:
    if not os.path.exists(DATA_FILE):
        return ""
    with open(DATA_FILE, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()


def write_content(content: str):
    ensure_parent_dir()
    with open(DATA_FILE, "w", encoding="utf-8") as f:
        f.write(content)


def parse_json_body() -> dict:
    length_raw = os.environ.get("CONTENT_LENGTH", "0").strip()
    try:
        length = int(length_raw) if length_raw else 0
    except ValueError:
        length = 0

    raw = sys.stdin.read(length) if length > 0 else ""
    if not raw:
        return {}

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return {}

    return data if isinstance(data, dict) else {}


def main():
    method = os.environ.get("REQUEST_METHOD", "GET").upper()

    if method == "GET":
        send_json(200, {
            "ok": True,
            "path": "docs/txt/general-commands.txt",
            "content": read_content(),
        })
        return

    if method == "POST":
        body = parse_json_body()
        content = body.get("content", "")
        if not isinstance(content, str):
            send_json(400, {"ok": False, "error": "Field 'content' must be a string"})
            return

        write_content(content)
        send_json(200, {
            "ok": True,
            "path": "docs/txt/general-commands.txt",
            "savedLength": len(content),
        })
        return

    send_json(405, {"ok": False, "error": "Method not allowed"})


if __name__ == "__main__":
    main()
