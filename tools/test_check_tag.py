import subprocess
import tempfile
import unittest
from pathlib import Path

from check_tag import check, release_tags


class ReleaseHistoryTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="distortion-tag-tests-")
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name).resolve()
        self.git("init", "-q")
        self.git("config", "user.name", "Release Test")
        self.git("config", "user.email", "release-test@example.invalid")
        self.git("config", "commit.gpgsign", "false")
        self.git("commit", "--allow-empty", "-qm", "Legacy history")
        self.legacy = self.git("rev-parse", "HEAD")
        self.git("tag", "v2.4.1")
        self.git("commit", "--allow-empty", "-qm", "Distortion release baseline")
        self.git("tag", "v1.0.0")
        self.git("commit", "--allow-empty", "-qm", "Current distribution")

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.repo, capture_output=True,
                              text=True, check=True).stdout.strip()

    def test_preserves_but_excludes_pre_baseline_tags(self):
        self.assertEqual(["v1.0.0"], release_tags(self.repo))
        self.assertIn("v2.4.1", self.git("tag", "-l"))
        self.assertIsNone(check("v1.1.0", release_tags(self.repo)))

    def test_annotated_current_releases_prevent_downgrades(self):
        self.git("tag", "-a", "v1.1.0", "-m", "Stable release")
        self.assertIn("v1.1.0", release_tags(self.repo))
        self.assertIsNotNone(check("v1.0.4", release_tags(self.repo)))
        self.assertIsNone(check("v1.1.1", release_tags(self.repo)))

    def test_future_major_versions_are_not_filtered_out(self):
        self.git("tag", "v3.0.0")
        self.assertIsNotNone(check("v1.1.0", release_tags(self.repo)))

    def test_unrelated_upstream_lineage_is_excluded(self):
        self.git("tag", "v9.0.0", self.legacy)
        self.assertNotIn("v9.0.0", release_tags(self.repo))

    def test_missing_baseline_fails_closed(self):
        self.git("tag", "-d", "v1.0.0")
        with self.assertRaises(subprocess.CalledProcessError):
            release_tags(self.repo)

    def test_checkout_before_baseline_cannot_be_released(self):
        self.git("checkout", "--detach", self.legacy)
        with self.assertRaises(ValueError):
            release_tags(self.repo)

    def test_semver_and_beta_guards_remain_strict(self):
        for invalid in ("1.1.0", "v01.1.0", "v1.1.0-beta.0"):
            self.assertIsNotNone(check(invalid, ["v1.0.3"]))
        self.assertIsNone(check("v1.1.0-beta.1", ["v1.0.3"]))
        self.assertIsNotNone(check("v1.1.0-beta.1", ["v1.1.0-beta.2"]))
        self.assertIsNone(check("v1.0.4", ["v1.0.3", "v1.1.0-beta.2"]))


if __name__ == "__main__":
    unittest.main()
