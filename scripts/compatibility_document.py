#!/usr/bin/env python3
"""Generate bounded, expiring compatibility data for signing by the release key."""
import argparse
import json
import time
from pathlib import Path

from release_manifest import APP_ID


def document(now: int, retries: int, refresh: bool, hours: int, minimum: int, maximum: int) -> dict:
    if not 0 <= retries <= 3 or not 1 <= hours <= 168:
        raise ValueError("Retry count or validity outside supported bounds")
    if not 0 < minimum <= maximum <= 2100000000:
        raise ValueError("Invalid Android version range")
    return {
        "schema": 1, "kind": "compatibility", "application_id": APP_ID,
        "revision": now * 1000, "issued_at": now, "expires_at": now + hours * 3600,
        "min_version_code": minimum, "max_version_code": maximum,
        "playback": {"network_retries": retries, "refresh_expired_streams": refresh},
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--network-retries", type=int, default=2)
    parser.add_argument("--refresh-expired", choices=("true", "false"), default="true")
    parser.add_argument("--hours", type=int, default=48)
    parser.add_argument("--min-version", type=int, default=1015)
    parser.add_argument("--max-version", type=int, default=2100000000)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    payload = document(int(time.time()), args.network_retries, args.refresh_expired == "true",
                       args.hours, args.min_version, args.max_version)
    args.output.write_text(json.dumps(payload, indent=2) + "\n")


if __name__ == "__main__":
    main()
