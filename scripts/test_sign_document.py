"""Verify the release signer with an independent OpenSSL implementation and temporary test key."""
import base64
import json
import os
import secrets
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


@unittest.skipUnless(all(shutil.which(tool) for tool in ("java", "keytool", "openssl")),
                     "JDK and OpenSSL are required")
class SignDocumentTest(unittest.TestCase):
    def test_release_signature_and_tamper_rejection(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env = dict(os.environ, NEWPIPET_KEYSTORE=str(root / "test.p12"),
                       NEWPIPET_STORE_PASSWORD=secrets.token_hex(16), NEWPIPET_KEY_ALIAS="test")
            env["NEWPIPET_KEY_PASSWORD"] = env["NEWPIPET_STORE_PASSWORD"]

            def run(*command, **kwargs):
                return subprocess.run(command, env=env, capture_output=True, check=True,
                                      timeout=30, **kwargs)

            run("keytool", "-genkeypair", "-keystore", env["NEWPIPET_KEYSTORE"],
                "-storetype", "PKCS12", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048",
                "-storepass:env", "NEWPIPET_STORE_PASSWORD", "-keypass:env", "NEWPIPET_KEY_PASSWORD",
                "-dname", "CN=Temporary test only", "-validity", "1")
            cert = run("keytool", "-exportcert", "-rfc", "-alias", "test",
                       "-keystore", env["NEWPIPET_KEYSTORE"],
                       "-storepass:env", "NEWPIPET_STORE_PASSWORD").stdout
            public = run("openssl", "x509", "-pubkey", "-noout", input=cert).stdout
            (root / "public.pem").write_bytes(public)
            payload = b'{"kind":"release","version_code":1100}\n'
            (root / "payload.json").write_bytes(payload)
            signer = Path(__file__).with_name("SignDocument.java")
            run("java", str(signer), str(root / "payload.json"), str(root / "signed.json"))
            signed = json.loads((root / "signed.json").read_text())
            self.assertEqual(payload, base64.b64decode(signed["payload"]))
            (root / "signature.bin").write_bytes(base64.b64decode(signed["signature"]))
            command = ("openssl", "dgst", "-sha256", "-verify", str(root / "public.pem"),
                       "-signature", str(root / "signature.bin"), str(root / "payload.json"))
            run(*command)
            (root / "payload.json").write_bytes(payload.replace(b"1100", b"9999"))
            with self.assertRaises(subprocess.CalledProcessError):
                run(*command)


if __name__ == "__main__":
    unittest.main()
