import re
import sys


MANUAL = re.compile(r"(?:feature|bug|chore|docs|refactor|test|perf|hotfix)/[a-z0-9]+(?:_[a-z0-9]+)*")
DEPENDABOT = re.compile(r"dependabot/[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9][A-Za-z0-9._-]*)*")


def valid_branch(name):
    return bool(MANUAL.fullmatch(name) or DEPENDABOT.fullmatch(name))


def main(args):
    if len(args) != 1 or not valid_branch(args[0]):
        print("Use <type>/<snake_case_description>: feature/, bug/, chore/, docs/, refactor/, test/, perf/ or hotfix/. Automated dependabot/ branches are allowed.", file=sys.stderr)
        return 1
    print(f"Valid branch: {args[0]}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
