import hashlib
import tempfile
import unittest
from pathlib import Path

from release_manifest import APP_ID, manifest


class ReleaseManifestTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.apk = Path(self.directory.name) / "app-release.apk"
        self.apk.write_bytes(b"test apk content")
        self.metadata = {"applicationId": APP_ID, "elements": [
            {"outputFile": self.apk.name, "versionCode": 1100, "versionName": "0.30.0"}
        ]}

    def test_hash_and_version_come_from_built_apk(self):
        result = manifest(self.metadata, self.apk, "v0.30.0", "stable")
        self.assertEqual(1100, result["version_code"])
        self.assertEqual(hashlib.sha256(self.apk.read_bytes()).hexdigest(), result["apk_sha256"])
        self.assertEqual("https://github.com/vejacostela/NewPipeT/releases/download/v0.30.0/NewPipeT.apk", result["apk_url"])

    def test_other_application_is_rejected(self):
        self.metadata["applicationId"] = "org.schabi.newpipe"
        with self.assertRaises(ValueError):
            manifest(self.metadata, self.apk, "v0.30.0", "stable")

    def test_wrong_artifact_is_rejected(self):
        self.metadata["elements"][0]["outputFile"] = "other.apk"
        with self.assertRaises(ValueError):
            manifest(self.metadata, self.apk, "v0.30.0", "stable")

    def test_bad_tags_and_channels_are_rejected(self):
        for tag, channel in [("../../bad", "stable"), ("v1?url=bad", "stable"), ("v1", "unknown")]:
            with self.assertRaises(ValueError):
                manifest(self.metadata, self.apk, tag, channel)

    def test_android_version_limit(self):
        for code in (0, -1, 2100000001):
            self.metadata["elements"][0]["versionCode"] = code
            with self.assertRaises(ValueError):
                manifest(self.metadata, self.apk, "v1", "beta")


if __name__ == "__main__":
    unittest.main()
