"""Create the public FoodCall source ZIP from an explicit file allowlist."""
from pathlib import Path
import re
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parent.parent
archive = root / "public" / "foodcall-website.zip"
tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=root).decode().split("\0")
files = set(filter(None, tracked)) | {"app/api/download/route.ts", "scripts/create-download.py"}
source_roots = ("app/", "lib/", "db/", "drizzle/")
public_files = {"public/manifest.webmanifest", "public/service-worker.js", "public/favicon.svg"}
project_files = {
    "README.md", "package.json", "pnpm-lock.yaml", "pnpm-workspace.yaml",
    "postcss.config.mjs", "next.config.ts", "vite.config.ts",
    "cloudflare-env.d.ts", "drizzle.config.ts", "tsconfig.json",
    "eslint.config.mjs", "build/sites-vite-plugin.ts",
    "build/sites-vite-plugin.LICENSE", "scripts/create-download.py",
    "scripts/run-framework.mjs", "scripts/execution-profile.mjs",
    "scripts/sites-env.mjs", "scripts/install-ci.mjs", "scripts/install-pnpm.sh",
    "scripts/pnpm-install.mjs", "scripts/npm-install.mjs",
}
def allowed(name: str) -> bool:
    if name in public_files | project_files:
        return True
    if name.startswith("public/assets/"):
        return Path(name).suffix.lower() in {".png", ".jpg", ".jpeg", ".webp", ".svg"}
    return name.startswith(source_roots) and Path(name).suffix.lower() in {".ts", ".tsx", ".css", ".sql", ".json"}

# Refuse to publish the archive if a credential accidentally lands in source.
secret_markers = (
    re.compile(rb"-----BEGIN [A-Z ]*PRIVATE KEY-----\s+[A-Za-z0-9+/]{64}"),
    re.compile(rb"AIza[0-9A-Za-z_-]{20,}"),
    re.compile(rb"gh[pousr]_[0-9A-Za-z]{20,}"),
    re.compile(rb"sk-[A-Za-z0-9_-]{20,}"),
    re.compile(rb"(?:password|secret|api[_-]?key)\s*[:=]\s*['\"][^'\"]{8,}['\"]", re.I),
)
approved = [name for name in sorted(files) if allowed(name) and (root / name).is_file()]
for name in approved:
    data = (root / name).read_bytes()
    if Path(name).suffix.lower() in {".ts", ".tsx", ".js", ".mjs", ".json", ".sql"}:
        if any(marker.search(data) for marker in secret_markers):
            raise SystemExit(f"Possible secret in {name}; archive not created")
readme = (
    "FoodCall website source\n\n"
    "The pages are React/Vinext components in app/. HTML is generated when the site runs, "
    "so there are no standalone caller.html or client.html files.\n"
    "Styles: app/globals.css; images: public/assets/; API: app/api/.\n"
    "Install dependencies with pnpm install and build with pnpm build.\n"
    "This archive contains no database contents or deployment secrets.\n"
)
with ZipFile(archive, "w", ZIP_DEFLATED, compresslevel=6) as out:
    out.writestr("FoodCall/DOWNLOAD-README.txt", readme)
    for relative in approved:
        out.write(root / relative, "FoodCall/" + relative)
print(f"Created {archive} ({archive.stat().st_size} bytes)")
