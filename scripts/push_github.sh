#!/usr/bin/env bash
set -euo pipefail

# Non-interactive GitHub push for environments without TTY.
# Usage:
#   GITHUB_TOKEN=xxxx ./scripts/push_github.sh
# Optional:
#   GITHUB_USERNAME=xiulongahb
#   GITHUB_REPO=AutoTxt

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

USERNAME="${GITHUB_USERNAME:-xiulongahb}"
REPO="${GITHUB_REPO:-AutoTxt}"
TOKEN="${GITHUB_TOKEN:-}"

if [[ -z "${TOKEN}" ]]; then
  echo "Missing GITHUB_TOKEN."
  echo ""
  echo "Create a GitHub Personal Access Token (PAT) with repo write permission, then run:"
  echo "  GITHUB_TOKEN=YOUR_TOKEN ./scripts/push_github.sh"
  exit 2
fi

ASKPASS_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "${ASKPASS_DIR}"
}
trap cleanup EXIT

cat > "${ASKPASS_DIR}/askpass.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
case "${1:-}" in
  *Username*|*username*)
    printf '%s' "${GITHUB_USERNAME:-xiulongahb}"
    ;;
  *Password*|*password*|*token*|*Token*)
    printf '%s' "${GITHUB_TOKEN}"
    ;;
  *)
    printf '%s' "${GITHUB_TOKEN}"
    ;;
esac
EOF
chmod +x "${ASKPASS_DIR}/askpass.sh"

export GITHUB_USERNAME="${USERNAME}"
export GIT_ASKPASS="${ASKPASS_DIR}/askpass.sh"
export GIT_TERMINAL_PROMPT=0

# Ensure push URL is GitHub HTTPS (do not embed token in remote)
git remote set-url --push origin "https://github.com/${USERNAME}/${REPO}.git" >/dev/null 2>&1 || true

echo "Pushing to GitHub as ${USERNAME}/${REPO}..."
git -c http.version=HTTP/1.1 push -4 origin HEAD:main
echo "Done."

