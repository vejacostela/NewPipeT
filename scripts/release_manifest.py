#!/usr/bin/env python3
"""Prepare a release manifest from actual Android build metadata."""
import argparse
import hashlib
import json
import re
from pathlib import Path

APP_ID = "io.github.vejacostela.newpipet"
REPOSITORY = "vejacostela/NewPipeT"


def manifest(metadata: dict, apk: Path, tag: str, channel: str) -> dict:
    if not re.fullmatch(r"v[0-9][A-Za-z0-9._-]*", tag):
        raise ValueError("Use a version tag such as v0.30.0-beta1")
    if channel not in ("stable", "beta") or metadata["applicationId"] != APP_ID:
        raise ValueError("Unexpected channel or application ID")
    elements = metadata["elements"]
    if len(elements) != 1 or elements[0]["outputFile"] != apk.name:
        raise ValueError("Expected exactly one APK matching build metadata")
    built = elements[0]
    if not 0 < built["versionCode"] <= 2100000000:
        raise ValueError("Invalid version code")
    return {
        "schema": 1, "kind": "release", "application_id": APP_ID,
        "channel": channel, "version_code": built["versionCode"],
        "version_name": built["versionName"],
        "apk_url": f"https://github.com/{REPOSITORY}/releases/download/{tag}/NewPipeT.apk",
        "apk_sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--metadata", type=Path, required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--channel", choices=("stable", "beta"), required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = manifest(json.loads(args.metadata.read_text()), args.apk, args.tag, args.channel)
    args.output.write_text(json.dumps(result, indent=2) + "\n")


if __name__ == "__main__":
    main()
