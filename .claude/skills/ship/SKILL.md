---
name: ship
description: Commit all current changes with descriptive messages and push to remote. Splits changes into meaningful commits and separates workflow changes from logic changes. Runs tests before committing. Use after a feature is complete and reviewed.
user-invocable: true
disable-model-invocation: true
---

You are shipping the current changes — committing and pushing to the remote repository. Follow these steps in order.

## Step 1 — Pre-flight checks

1. Run `./gradlew core:test` and confirm all tests pass. If any fail, stop and report — do not commit broken code.
2. Run `./gradlew build` to confirm compilation. If it fails, stop and report.

## Step 2 — Review and plan commits

1. Run `git status` (never use `-uall`) to see all modified and untracked files.
2. Run `git diff` to see unstaged changes.
3. Run `git log --oneline -5` to see recent commit message style.
4. **Categorize the changes** into separate commits. Each commit should represent a small, meaningful, end-to-end unit of value. Guidelines:
   - **Never bundle workflow changes with logic changes.** Changes to `.claude/agents/`, `.claude/skills/`, `CLAUDE.md`, and `shared-conventions.md` go in their own commit(s), separate from game code, tests, and asset changes.
   - **Group by coherent feature slice**, not by file type. A commit like "add AOE targeting for towers" that includes the model change, entity logic, JSON assets, and tests for that slice is good. A commit that is just "add all new files" is bad.
   - **Tests go with the code they test.** Don't separate test files into their own commit unless they are standalone regression tests unrelated to a code change in the same session.
   - **Spec files** go with the feature code they describe, not with workflow changes.
5. Present the proposed commit plan to the user:
   - For each commit: which files it includes and the draft commit message
   - Any files that look like they shouldn't be committed (`.env`, credentials, large binaries, etc.)
6. Wait for user approval or adjustments before proceeding.

## Step 3 — Commit

For each commit in the approved plan:

1. Stage the specific files for that commit using `git add <specific files>`. Never use `git add -A`.
2. Create the commit with a message that:
   - Follows the style of recent commits in the repo
   - Summarizes the "why" not just the "what"
   - Is concise (1-2 sentences)
   - Ends with:
     ```
     Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
     ```
3. Verify the commit succeeded before moving to the next one.

## Step 4 — Push

1. Check if the current branch tracks a remote branch: `git rev-parse --abbrev-ref --symbolic-full-name @{u}`
2. If it does, push with `git push`.
3. If it doesn't, push with `git push -u origin <current-branch>`.
4. Confirm the push succeeded and report the final status.

## Guardrails

- Never force-push (`--force`, `--force-with-lease`) unless the user explicitly requests it.
- Never push to `main`/`master` with `--force`. Warn the user if they request this.
- Never skip hooks (`--no-verify`) unless the user explicitly requests it.
- If a pre-commit hook fails, fix the issue and create a NEW commit — do not amend.
- If the user hasn't confirmed the commit plan, do not commit.
