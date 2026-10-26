param(
    [string]$DatabaseUrl = 'jdbc:postgresql://127.0.0.1:55439/kltn_issues_test',
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21.0.11'
)
$ErrorActionPreference = 'Stop'
if ($DatabaseUrl -notmatch '^jdbc:postgresql://(127\.0\.0\.1|localhost):[0-9]+/kltn_issues_test$') {
    throw 'Only a loopback kltn_issues_test database is allowed. Never supply a production URL.'
}
$issuesRoot = Split-Path -Parent $PSScriptRoot
$previousDb = $env:KLTN_TEST_DB_URL
$previousJava = $env:JAVA_HOME
try {
    $env:KLTN_TEST_DB_URL = $DatabaseUrl
    $env:JAVA_HOME = $JavaHome
    Push-Location (Join-Path $issuesRoot 'backend/restaurant-management')
    $issuesCache = Join-Path $issuesRoot '.cache/maven'
    $issuesMaven = Get-ChildItem -LiteralPath (Join-Path $issuesCache 'wrapper/dists') -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($issuesMaven) {
        & $issuesMaven.FullName "-Dmaven.repo.local=$issuesCache/repository" clean verify
    } else {
        & .\mvnw.cmd clean verify
    }
    if ($LASTEXITCODE -ne 0) { throw "Backend verification failed ($LASTEXITCODE)" }
} finally {
    Pop-Location
    $env:KLTN_TEST_DB_URL = $previousDb
    $env:JAVA_HOME = $previousJava
}
