---
name: develop-feature
description: Full feature development pipeline for Unstable Grid. Runs requirements gathering (interactive) → feature-planner → (manual gate) → implementer → test-generator → code-reviewer in sequence.
user-invocable: true
disable-model-invocation: true
---

You are orchestrating a full feature development pipeline for Unstable Grid. The user has invoked this workflow with a feature description. Follow the steps below in order.

## Step 1 — Requirements gathering (interactive)

Before any planning begins, your job is to ensure the feature requirements are clear, complete, and unambiguous enough that the planner can produce a spec with high confidence. Act as a requirements designer — interview the user, surface gaps, and refine until the requirements are solid.

### ProcessAdd

1. **Analyze** the user's initial feature description. Consider:
   - Is the scope clear? Could two reasonable people interpret it differently?
   - Are the user-facing behaviors specified (what the player sees, clicks, experiences)?
   - Are edge cases and failure modes addressed?
   - For UI features: are interactions, visual feedback, and mode transitions described?
   - For gameplay features: are numbers/balancing parameters mentioned or explicitly deferred?
   - Are there dependencies on unimplemented systems (see Known Issues in CLAUDE.md)?

2. **Present a requirements summary** back to the user structured as:
   - **Understanding:** 2-3 sentences of what you believe the feature is.
   - **Assumptions:** things you're filling in that the user didn't explicitly state.
   - **Open questions:** specific, numbered questions about gaps or ambiguities you identified. Keep questions focused and actionable — avoid vague "anything else?" questions. Ask about concrete behaviors, not implementation details (that's the planner's job).
   - **Readiness verdict:** one of:
     - **Ready** — requirements are clear enough to proceed to planning.
     - **Almost ready** — minor clarifications needed (list them), but you can proceed if the user prefers.
     - **Needs discussion** — significant ambiguities that will likely cause rework if not resolved now.

3. **Iterate** with the user. If they answer questions, update your understanding and reassess. If new questions emerge from their answers, ask those too. Keep rounds focused — aim for 1-2 rounds, not an interrogation.

4. **Confirm and hand off.** Once you judge the requirements are Ready (or the user says "just go"), compile the final requirements into a clean brief that will be passed to the planner. The brief should be a concise, structured summary — not a transcript of the conversation.

### Guidelines
- Be opinionated. If something smells like it will cause confusion during implementation, say so. Suggest concrete alternatives rather than just flagging problems.
- Don't ask about implementation details (class names, data structures, file locations) — that's the planner's domain.
- Do ask about player experience, game feel, interactions, edge cases, and scope boundaries.
- If the user's description is already thorough and unambiguous, say so and move on quickly — don't manufacture questions just to seem thorough.
- Respect the user's time. If they say "just build it" or clearly want to skip ahead, compile what you have and proceed.

## Step 2 — Plan

Invoke the `feature-planner` subagent with the compiled requirements brief from Step 1. Wait for it to finish and return the spec file path.

Once the planner finishes, present the user with:
- The path to the saved spec file
- A brief summary of the plan (from the planner's output)

## Step 3 — Manual gate

Print the full contents of the spec file so the user can review it inline without opening an editor.

Then ask the user:

> "Please review the spec above and let me know if you'd like to proceed with implementation, or if you have changes to make first."

Do not proceed until the user explicitly confirms they want to continue. If they request changes, update the spec file accordingly, print the updated spec, and ask again. Once they confirm, continue to Step 4.

Note: Steps 4–6 reference each other internally — the `test-generator` and `code-reviewer` loop described in Step 6 invokes the `implementer` (Step 4) and `test-generator` (Step 5) as needed.

## Step 4 — Implement

Invoke the `implementer` subagent, passing it the spec file path. Wait for it to finish. Report what was implemented.

## Step 5 — Generate tests

Invoke the `test-generator` subagent, passing it the spec file path and the list of files changed by the implementer. Wait for it to finish. Report which test classes were written and the test run result.

## Step 6 — Code review

Invoke the `code-reviewer` subagent, passing it the spec file path and the list of changed files. Wait for it to finish. Present the full review output to the user.

If the verdict is **Approved** or **Approved with warnings**, the workflow is complete. Summarise what was built and the spec file path. Then read the spec file, extract the **Manual test scenarios** section, and print it to the console so the user can walk through manual verification immediately.

If the verdict is **Needs changes**, extract the list of Blockers from the review and pass them to the `implementer` subagent as a fix list (along with the spec file path). Once the implementer finishes, invoke the `test-generator` again for any files it touched, then re-run the `code-reviewer`. Repeat this loop until the verdict is no longer **Needs changes**. After two failed review cycles, stop and present the outstanding blockers to the user for manual resolution rather than looping again.
