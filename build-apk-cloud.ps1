<#
.SYNOPSIS
    Convenience wrapper to run .\scripts\build-apk-cloud.ps1
#>
[CmdletBinding()]
param(
    [ValidateSet("prodRelease", "prodBenchmark", "devRelease", "devBenchmark", "devDebug")]
    [string]$Variant = "prodRelease",

    [ValidateSet("patch", "minor", "major", "none")]
    [string]$Bump = "patch",

    [int]$ExplicitVersionCode = 0,

    [string]$ExplicitVersionName = "",

    [string]$Abi = "",

    [string]$Branch = "",

    [ValidateSet("3", "7", "14", "30")]
    [string]$RetentionDays = "7",

    [switch]$NoBump,

    [switch]$NoWait,

    [string]$DestinationDir = "version/Android"
)

& "$PSScriptRoot\scripts\build-apk-cloud.ps1" @PSBoundParameters
exit $LASTEXITCODE
