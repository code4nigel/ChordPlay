#!/usr/bin/env python3
"""Inspect a release iOS device app and package an unsigned IPA for sideloading/AltStore."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import plistlib
import re
import shutil
import subprocess
import tempfile


def command(*args: str) -> str:
    return subprocess.check_output(args, text=True).strip()


def inspect_bundle(app: Path) -> dict:
    info = plistlib.loads((app / "Info.plist").read_bytes())
    executable = app / info["CFBundleExecutable"]
    if not os.access(executable, os.X_OK):
        raise ValueError("The main app binary is missing or not executable")
    if "audio" not in info.get("UIBackgroundModes", []):
        raise ValueError("Background audio playback configuration is absent in UIBackgroundModes")

    return {
        "bundle_id": info.get("CFBundleIdentifier", "com.chordplay.music"),
        "version": info.get("CFBundleShortVersionString", "1.0.0"),
        "build": info.get("CFBundleVersion", "1"),
        "minimum_ios": info.get("MinimumOSVersion", "16.0"),
        "signing": "Unsigned engineering build; sign locally or install via AltStore / SideStore / Sideloadly",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("app", type=Path, help="Path to .app bundle")
    parser.add_argument("output", type=Path, help="Path to output .ipa")
    args = parser.parse_args()

    app, output = args.app.resolve(strict=True), args.output.resolve()
    metadata = inspect_bundle(app)

    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        output.unlink()

    with tempfile.TemporaryDirectory(prefix="chordplay-ipa-") as directory:
        payload = Path(directory) / "Payload"
        payload.mkdir()
        shutil.copytree(app, payload / app.name, symlinks=True)
        if platform.system() == "Darwin":
            command("ditto", "-c", "-k", "--keepParent", str(payload), str(output))
        else:
            shutil.make_archive(str(output.with_suffix("")), "zip", str(payload.parent))
            Path(str(output.with_suffix("")) + ".zip").rename(output)

    with output.open("rb") as binary:
        metadata["sha256"] = hashlib.file_digest(binary, "sha256").hexdigest()

    output.with_suffix(".ipa.sha256").write_text(f"{metadata['sha256']}  {output.name}\n")
    output.with_suffix(".ipa.json").write_text(json.dumps(metadata, indent=2) + "\n")

    print(f"Successfully packaged: {output} (SHA256: {metadata['sha256']})")


if __name__ == "__main__":
    main()
