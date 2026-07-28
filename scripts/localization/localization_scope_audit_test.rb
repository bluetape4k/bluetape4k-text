require "fileutils"
require "minitest/autorun"
require "tmpdir"

require_relative "localization_scope_audit"

class LocalizationScopeAuditTest < Minitest::Test
  def test_reports_scope_inventory_and_manual_parity
    with_repo do |root|
      write(root, "README.md", "# README\n")
      write(root, "README.ko.md", "# README\n")
      write(root, "AGENTS.md", "# Agent guidance\n")
      write(root, "docs/manual/en/index.md", "# Manual\n")
      write(root, "docs/manual/ko/index.md", "# 매뉴얼\n")
      write(root, "docs/lessons/one.md", "# Lesson\n")
      write(root, "src/main/kotlin/Sample.kt", <<~KOTLIN)
        /**
         * Sample.
         * @param value value.
         */
        fun sample(value: String) = value
      KOTLIN
      git(root, "add", ".")
      git(root, "commit", "-m", "fixture")

      inventory = LocalizationScopeAudit.new(root: root).inventory

      assert_equal 1, inventory.fetch("candidateSingleLanguageMarkdown")
      assert_equal({ "docs/lessons" => 1 }, inventory.fetch("candidateMarkdownGroups"))
      assert_equal 2, inventory.fetch("excludedReadme")
      assert_equal 1, inventory.fetch("excludedLlmFacing")
      assert_equal({ "en" => 1, "ko" => 1, "missingKo" => [], "missingEn" => [] }, inventory.fetch("manualParity"))
      assert_equal 1, inventory.fetch("kotlinFilesWithComments")
      assert_equal 1, inventory.fetch("kdocTags")
    end
  end

  def test_rejects_manual_path_mismatch
    with_repo do |root|
      write(root, "docs/manual/en/index.md", "# Manual\n")
      write(root, "docs/manual/ko/getting-started.md", "# 시작\n")
      git(root, "add", ".")
      git(root, "commit", "-m", "fixture")

      error = assert_raises(LocalizationScopeAudit::Violation) do
        LocalizationScopeAudit.new(root: root).validate!
      end
      assert_includes error.message, "manual ko missing index.md"
      assert_includes error.message, "manual en missing getting-started.md"
    end
  end

  private

  def with_repo
    Dir.mktmpdir do |root|
      git(root, "init")
      git(root, "config", "user.email", "test@example.com")
      git(root, "config", "user.name", "Test")
      yield root
    end
  end

  def write(root, relative_path, content)
    path = File.join(root, relative_path)
    FileUtils.mkdir_p(File.dirname(path))
    File.write(path, content)
  end

  def git(root, *args)
    system("git", *args, chdir: root, out: File::NULL, err: File::NULL) || flunk("git #{args.join(' ')} failed")
  end
end
