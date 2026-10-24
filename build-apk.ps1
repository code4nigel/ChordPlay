<#
.SYNOPSIS
    Convenience wrapper to run .\scripts\build-apk.ps1
#>
[CmdletBinding()]
param(
    [ValidateSet("devDebug", "prodRelease", "devRelease", "prodDebug")]
    [string]$Variant = "devDebug",

    [ValidateSet("patch", "minor", "major", "none")]
    [string]$Bump = "patch",

    [int]$ExplicitVersionCode = 0,

    [string]$ExplicitVersionName = "",

    [switch]$NoBump,

    [switch]$Clean,

    [string]$DestinationDir = "version/Android"
)

& "$PSScriptRoot\scripts\build-apk.ps1" @PSBoundParameters
exit $LASTEXITCODE
