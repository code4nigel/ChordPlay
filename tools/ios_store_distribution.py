#!/usr/bin/env python3
"""Generate AltStore Classic / SideStore source repository feed from an IPA release."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path


REPO = "code4nigel/ChordPlay"
PUBLIC_IPA = "ChordPlay.ipa"


def make_source(ipa_path: Path, metadata: dict, repo: str) -> dict:
    release_url = f"https://github.com/{repo}/releases/tag/ios"
    download_url = f"https://github.com/{repo}/releases/download/ios"
    source_url = f"{download_url}/altstore-source.json"
    icon_url = f"https://raw.githubusercontent.com/{repo}/main/Logo.png"

    version = metadata.get("version", "1.0.0")
    build = str(metadata.get("build", "1"))
    sha256 = metadata.get("sha256", hashlib.sha256(ipa_path.read_bytes()).hexdigest())

    return {
        "name": "ChordPlay",
        "identifier": "com.chordplay.source",
        "sourceURL": source_url,
        "subtitle": "Immersive music player with synced lyrics and dynamic themes",
        "website": release_url,
        "iconURL": icon_url,
        "tintColor": "#7C4DFF",
        "apps": [
            {
                "name": "ChordPlay",
                "bundleIdentifier": metadata.get("bundle_id", "com.chordplay.music"),
                "developerName": "ChordPlay Team",
                "subtitle": "Your Music. Everywhere.",
                "localizedDescription": "ChordPlay is a beautiful local and streaming music player featuring synced lyrics, dynamic themes, audio enhancements, and offline support.",
                "iconURL": icon_url,
                "tintColor": "#7C4DFF",
                "category": "music",
                "versions": [
                    {
                        "version": version,
                        "buildVersion": build,
                        "date": datetime.now(timezone.utc).isoformat(),
                        "localizedDescription": f"ChordPlay {version} for iOS.",
                        "downloadURL": f"{download_url}/{PUBLIC_IPA}?build={build}",
                        "size": ipa_path.stat().st_size,
                        "minOSVersion": metadata.get("minimum_ios", "16.0"),
                        "sha256": sha256,
                    }
                ],
            }
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("ipa", type=Path, help="Path to IPA file")
    parser.add_argument("output", type=Path, help="Path to output altstore-source.json")
    parser.add_argument("--repo", default=REPO, help="GitHub repository (owner/name)")
    args = parser.parse_args()

    ipa_path, output_path = args.ipa.resolve(strict=True), args.output.resolve()
    meta_path = ipa_path.with_suffix(".ipa.json")
    metadata = json.loads(meta_path.read_text()) if meta_path.exists() else {}

    source = make_source(ipa_path, metadata, args.repo)
    output_path.write_text(json.dumps(source, indent=2) + "\n")
    print(f"Generated AltStore source feed: {output_path}")


if __name__ == "__main__":
    main()

