import tempfile
import unittest
from pathlib import Path

from update_newpipe_extractor import update_catalog


OLD = "1" * 40
NEW = "a" * 40


class UpdateCatalogTest(unittest.TestCase):
    def test_updates_only_exact_catalog_key(self):
        with tempfile.TemporaryDirectory() as directory:
            catalog = Path(directory) / "libs.versions.toml"
            catalog.write_text(
                f'other = "{OLD}"\nteamnewpipe-newpipe-extractor = "{OLD}"\n',
                encoding="utf-8",
            )
            previous, changed = update_catalog(catalog, NEW)
            contents = catalog.read_text(encoding="utf-8")
            self.assertEqual(OLD, previous)
            self.assertTrue(changed)
            self.assertIn(f'other = "{OLD}"', contents)
            self.assertIn(f'teamnewpipe-newpipe-extractor = "{NEW}"', contents)

    def test_is_idempotent(self):
        with tempfile.TemporaryDirectory() as directory:
            catalog = Path(directory) / "libs.versions.toml"
            catalog.write_text(f'teamnewpipe-newpipe-extractor = "{NEW}"\n', encoding="utf-8")
            previous, changed = update_catalog(catalog, NEW)
            self.assertEqual(NEW, previous)
            self.assertFalse(changed)

    def test_rejects_non_full_sha(self):
        with tempfile.TemporaryDirectory() as directory:
            catalog = Path(directory) / "libs.versions.toml"
            catalog.write_text(f'teamnewpipe-newpipe-extractor = "{OLD}"\n', encoding="utf-8")
            with self.assertRaises(ValueError):
                update_catalog(catalog, "dev")


if __name__ == "__main__":
    unittest.main()
