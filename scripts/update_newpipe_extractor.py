#!/usr/bin/env python3
"""Update the pinned NewPipeExtractor revision in the Gradle version catalog."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


KEY = "teamnewpipe-newpipe-extractor"
SHA_PATTERN = re.compile(
    rf'^(?P<prefix>{re.escape(KEY)}\s*=\s*")(?P<sha>[0-9a-f]{{40}})(?P<suffix>"\s*)$',
    re.MULTILINE,
)


def update_catalog(catalog: Path, revision: str) -> tuple[str, bool]:
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("extractor revision must be a full, lowercase 40-character SHA")
    contents = catalog.read_text(encoding="utf-8")
    matches = list(SHA_PATTERN.finditer(contents))
    if len(matches) != 1:
        raise RuntimeError(f"expected exactly one {KEY} entry, found {len(matches)}")
    current = matches[0].group("sha")
    if current == revision:
        return current, False
    catalog.write_text(
        SHA_PATTERN.sub(rf"\g<prefix>{revision}\g<suffix>", contents, count=1),
        encoding="utf-8",
    )
    return current, True


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("revision")
    parser.add_argument("--catalog", type=Path, default=Path("gradle/libs.versions.toml"))
    args = parser.parse_args()
    previous, changed = update_catalog(args.catalog, args.revision)
    print(f"previous={previous}")
    print(f"current={args.revision}")
    print(f"changed={'true' if changed else 'false'}")


if __name__ == "__main__":
    main()
