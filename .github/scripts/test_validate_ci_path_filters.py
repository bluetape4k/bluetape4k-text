#!/usr/bin/env python3
"""모듈 의존성을 반영한 CI path-filter 회귀 테스트."""

from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("validate-ci-path-filters.py")
FIXTURE = Path(__file__).with_name("testdata") / "core-only-change.json"
SPEC = importlib.util.spec_from_file_location("validate_ci_path_filters", SCRIPT)
assert SPEC and SPEC.loader
VALIDATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VALIDATOR)


def without_filter_path(workflow: str, filter_name: str, path: str) -> str:
    marker = f"            {filter_name}:\n"
    body_start = workflow.index(marker) + len(marker)
    body = VALIDATOR.filter_block(workflow, filter_name)
    line = f"              - '{path}'\n"
    if line not in body:
        raise AssertionError(f"{filter_name} does not contain {path}")
    return (
        workflow[:body_start]
        + body.replace(line, "", 1)
        + workflow[body_start + len(body) :]
    )


class CiPathFilterTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = VALIDATOR.WORKFLOW.read_text(encoding="utf-8")
        cls.nightly_workflow = VALIDATOR.NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        cls.fixture = json.loads(FIXTURE.read_text(encoding="utf-8"))

    def test_core_only_change_selects_consumers_but_not_unrelated_modules(self) -> None:
        selected = VALIDATOR.selected_filters(self.workflow, self.fixture["changed_paths"])

        self.assertEqual(tuple(self.fixture["expected_filters"]), selected)
        self.assertTrue(
            set(self.fixture["excluded_filters"]).isdisjoint(selected),
            f"unrelated filters selected: {selected}",
        )

    def test_dependency_path_is_required_for_each_downstream_filter(self) -> None:
        broken_workflow = without_filter_path(
            without_filter_path(self.workflow, "tokenizer-japanese", "tokenizer-core/**"),
            "tokenizer-korean",
            "tokenizer-core/**",
        )

        errors = VALIDATOR.validate_workflow(broken_workflow, self.nightly_workflow)

        self.assertIn(
            "tokenizer-japanese is missing dependency path: tokenizer-core/**",
            errors,
        )
        self.assertIn(
            "tokenizer-korean is missing dependency path: tokenizer-core/**",
            errors,
        )

    def test_existing_expected_job_guard_and_nightly_jobs_remain_covered(self) -> None:
        self.assertEqual(
            [], VALIDATOR.validate_workflow(self.workflow, self.nightly_workflow)
        )
        self.assertEqual(
            {f"test-{name}" for name in VALIDATOR.FILTERS},
            VALIDATOR.nightly_test_jobs(self.nightly_workflow),
        )


if __name__ == "__main__":
    unittest.main()
