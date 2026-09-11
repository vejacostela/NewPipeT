import unittest

from compatibility_document import document


class CompatibilityDocumentTest(unittest.TestCase):
    def test_revision_and_expiry(self):
        result = document(2000, 2, True, 48, 1015, 1100)
        self.assertEqual(2000000, result["revision"])
        self.assertEqual(2000 + 48 * 3600, result["expires_at"])
        self.assertEqual(1100, result["max_version_code"])

    def test_parameter_limits(self):
        for retries, hours, minimum, maximum in [(4, 24, 1, 1000), (2, 169, 1, 1000), (2, 24, 100, 1)]:
            with self.assertRaises(ValueError):
                document(2000, retries, True, hours, minimum, maximum)


if __name__ == "__main__":
    unittest.main()
