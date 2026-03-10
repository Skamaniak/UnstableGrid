---
name: develop-feature
description: Full feature development pipeline for Unstable Grid. Runs feature-planner → (manual gate) → implementer → test-generator → code-reviewer in sequence.
user-invocable: true
disable-model-invocation: true
---

You are orchestrating a full feature development pipeline for Unstable Grid. The user has invoked this workflow with a feature description. Follow the steps below in order.

## Step 1 — Plan

Invoke the `feature-planner` subagent with the feature description provided by the user. Wait for it to finish and return the spec file path.

Once the planner finishes, present the user with:
- The path to the saved spec file
- A brief summary of the plan (from the planner's output)

## Step 2 — Manual gate

Print the full contents of the spec file so the user can review it inline without opening an editor.

Then ask the user:

> "Please review the spec above and let me know if you'd like to proceed with implementation, or if you have changes to make first."

Do not proceed until the user explicitly confirms they want to continue. If they request changes, update the spec file accordingly, print the updated spec, and ask again. Once they confirm, continue to Step 3.

## Step 3 — Implement

Invoke the `implementer` subagent, passing it the spec file path. Wait for it to finish. Report what was implemented.

## Step 4 — Generate tests

Invoke the `test-generator` subagent, passing it the spec file path and the list of files changed by the implementer. Wait for it to finish. Report which test classes were written and the test run result.

## Step 5 — Code review

Invoke the `code-reviewer` subagent, passing it the spec file path and the list of changed files. Wait for it to finish. Present the full review output to the user.

If the verdict is **Approved** or **Approved with warnings**, the workflow is complete. Summarise what was built and the spec file path.

If the verdict is **Needs changes**, extract the list of Blockers from the review and pass them to the `implementer` subagent as a fix list (along with the spec file path). Once the implementer finishes, invoke the `test-generator` again for any files it touched, then re-run the `code-reviewer`. Repeat this loop until the verdict is no longer **Needs changes**. After two failed review cycles, stop and present the outstanding blockers to the user for manual resolution rather than looping again.
