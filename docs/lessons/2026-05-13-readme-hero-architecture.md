# README Hero And Architecture Refresh

## Context

The text repository had architecture documentation but lacked a visual
entrypoint, explicit purpose/features, and a current WIP snapshot.

## Decision

Store the generated text-processing workbench in `docs/assets/text-workbench.png`,
normalize README language-switch placement, and create `WIP.md` showing that no
assigned open issue exists at this snapshot.

## Outcome

Both README locales now introduce the tokenizer, language detection, dictionary,
and Aho-Corasick scope before the module table.

## Verification

- Confirmed the generated asset exists as a PNG under `docs/assets`.
- Verified both README locales reference the shared image path.

## Future Guidance

Keep WIP snapshots issue-driven; if no assigned issue exists, record that
explicitly instead of inventing a backlog.
