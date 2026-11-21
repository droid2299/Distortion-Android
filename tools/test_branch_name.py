import contextlib
import io
import unittest

from check_branch_name import main, valid_branch


class BranchNameTest(unittest.TestCase):
    def test_each_supported_change_type(self):
        for kind in ("feature", "bug", "chore", "docs", "refactor", "test", "perf", "hotfix"):
            with self.subTest(kind=kind):
                self.assertTrue(valid_branch(f"{kind}/account_free_v2"))

    def test_real_dependency_branch(self):
        self.assertTrue(valid_branch("dependabot/gradle/base/androidx.core-core-ktx-1.19.1"))

    def test_rejects_other_prefixes_and_non_snake_case(self):
        for name in ("codex/account_free", "fix/player", "account_free", "feature/", "feature/BadName", "feature/account-free", "bug/a/b", "bug/_player", "bug/player_", "bug/player__queue", "feature/player\n", "dependabot/../bad", "dependabot//bad"):
            with self.subTest(name=name):
                self.assertFalse(valid_branch(name))

    def test_cli_returns_failure_for_missing_or_extra_arguments(self):
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(1, main([]))
            self.assertEqual(1, main(["feature/player", "extra"]))
            self.assertEqual(1, main(["other/player"]))

    def test_cli_accepts_valid_branch(self):
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, main(["bug/player_queue"]))


if __name__ == "__main__":
    unittest.main()
