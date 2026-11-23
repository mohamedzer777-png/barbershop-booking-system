# Run from this folder after creating an EMPTY repository on GitHub.
# Usage: .\PUBLISH_TO_GITHUB.ps1 -RepositoryUrl 'https://github.com/YOUR-USER/barbershop-booking-system.git'
param([Parameter(Mandatory=$true)][string]$RepositoryUrl)
$ErrorActionPreference = 'Stop'
if ($RepositoryUrl -notmatch '^https://github\.com/[a-zA-Z0-9-]+/[a-zA-Z0-9_.-]+\.git$') {
    throw 'Please supply an HTTPS GitHub repository URL ending in .git'
}
if (-not (Test-Path '.git')) { git init -b main }
git add .
git status --short
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -m 'Prepare secure portfolio MVP and CI'
    if ($LASTEXITCODE -ne 0) { throw 'Git commit failed. Configure git user.name/user.email and retry.' }
}
$remotes = @(git remote)
if ($remotes -contains 'origin') {
    $currentUrl = git remote get-url origin
    if ($currentUrl -ne $RepositoryUrl) { throw "Existing origin points to $currentUrl (expected $RepositoryUrl)" }
} else {
    git remote add origin $RepositoryUrl
}
git push -u origin main
