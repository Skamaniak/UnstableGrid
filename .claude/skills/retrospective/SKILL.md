---
name: retrospective
description: Review the current session to extract lessons learned and improve the development workflow. Updates agents, skills, CLAUDE.md, and shared conventions based on what went well and what went wrong. Invoke manually after a feature is complete.
user-invocable: true
disable-model-invocation: true
---

You are running a retrospective on the current conversation. Your goal is to extract lessons learned and improve the development workflow artifacts (agents, skills, CLAUDE.md, shared conventions) so future sessions benefit from this session's experience.

## Step 1 — Session analysis

Review the full conversation and identify:

1. **What went well** — smooth steps, correct first-time outputs, good agent behavior.
2. **What went wrong** — bugs the agents introduced, code review blockers, misunderstandings, wasted rounds, incorrect assumptions.
3. **Patterns** — recurring issues that suggest a systemic fix (e.g., "the implementer keeps making the same font color mistake" suggests a shared convention is missing).

For each problem, classify it:
- **Agent gap** — the agent's prompt is missing guidance that would have prevented the issue.
- **Convention gap** — a shared convention is missing or incomplete in `shared-conventions.md`.
- **Skill gap** — the workflow skill (e.g., `develop-feature`) has a step that's unclear, missing, or wrong.
- **CLAUDE.md gap** — project-level knowledge is missing that would help all agents.
- **Linter gap** — the issue is a mechanical pattern (structural, not semantic) that PMD could catch automatically via an XPath rule in `quality/pmd/unstable-grid-ruleset.xml`.
- **One-off** — specific to this feature, not worth generalizing. Skip these.

Present your analysis to the user as a concise summary before making any changes.

## Step 2 — Propose changes

For each non-one-off issue, propose a specific change. Group by file:

- **`.claude/agents/shared-conventions.md`** — new rules or updates to existing rules.
- **`.claude/agents/<agent>.md`** — agent prompt improvements.
- **`.claude/skills/<skill>/SKILL.md`** — workflow step changes.
- **`CLAUDE.md`** — project-level knowledge updates.

For each proposed change, state:
- What to add/modify/remove
- Why (link to the session event that motivated it)
- Whether it's a new rule or an update to an existing one

For **Linter gap** issues specifically, propose the change as:
- **Rule name** (PascalCase, descriptive)
- **What it detects** (the pattern in plain English)
- **XPath sketch** (approximate PMD 7 XPath — node names, attributes, structure)
- **Priority** (1 = critical/crash, 2 = high/convention, 3 = medium/suggestion)
- **File:** `quality/pmd/unstable-grid-ruleset.xml`

Linter rules should only be proposed for **mechanical, structural patterns** — things that can be reliably detected from the AST alone (e.g., "method X calls Y", "class extending Z has a public setter"). Do NOT propose linter rules for semantic issues that require understanding intent (e.g., "variable name is misleading").

**Do NOT propose changes yet. Present the list and wait for user approval.**

## Step 3 — Apply approved changes

After the user approves (or adjusts), apply the changes. Follow the guardrails below strictly.

## Guardrails — what NOT to do

These guardrails prevent the retrospective from degrading the workflow over time. Every proposed change must pass ALL of these checks:

### Avoid over-fitting
- **Do NOT add rules for one-off bugs.** If a bug happened once and is unlikely to recur, skip it. Only add rules for patterns (happened 2+ times, or is a class of mistake likely to recur).
- **Do NOT add rules that Claude can derive from reading code.** If the codebase already makes the convention obvious (e.g., every existing test uses `TestAssetFactory`), don't add a rule saying "use TestAssetFactory."
- **Do NOT add rules that duplicate existing rules.** Search all files first.

### Keep files lean
- **CLAUDE.md must stay under 200 lines.** If it's over, trim low-value content before adding new content. Every line must pass: "Would removing this cause Claude to make mistakes?"
- **Agent prompts must stay under 200 lines.** Reference `shared-conventions.md` instead of inlining conventions.
- **`shared-conventions.md` should stay under 150 lines per section.** If a section grows too large, consider splitting into `.claude/rules/` with path scoping.
- **Skills must stay under 500 lines.**

### Single source of truth
- **Never duplicate a convention across files.** If a rule exists in `shared-conventions.md`, agents should reference it, not repeat it.
- **If you add to `shared-conventions.md`, check all agents for inline duplicates** and remove them, replacing with a reference.
- **If you update CLAUDE.md, check that it doesn't contradict `shared-conventions.md`** or agent prompts.

### Scope and specificity
- **Rules must be actionable.** "Be careful with colors" is not a rule. "Call `font.setColor(Color.WHITE)` after any temporary font color change in draw methods" is a rule.
- **Rules must be scoped.** Specify when the rule applies (e.g., "when implementing draw methods" or "when adding new UI elements"), not blanket advice.
- **Prefer updating existing rules over adding new ones.** If a rule covers 80% of the case, extend it rather than creating a second rule.

## Step 4 — Verify

After applying changes:
1. Check line counts of modified files against the limits above.
2. Grep for duplicated content across agents, shared-conventions, and CLAUDE.md.
3. If any PMD rules were added or modified, run `./gradlew core:pmdMain core:pmdTest` to verify:
   - The ruleset XML parses without errors.
   - New rules do not produce false positives on the existing codebase (unless the existing codebase genuinely violates the new rule — in which case, flag it to the user rather than removing the rule).
   - Existing rules still pass.
4. Report what was changed and the current line counts.
