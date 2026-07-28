require "json"
require "open3"
require "pathname"

class LocalizationScopeAudit
  class Violation < StandardError; end

  README_PATTERN = %r{(^|/)README(\.ko)?\.md\z}
  LLM_FACING_PATTERN = %r{(^|/)(AGENTS|CLAUDE|SKILL)\.md\z}
  MANUAL_PAIR_PATTERN = %r{\Adocs/manual/(en|ko)/.+\.md\z}
  MARKDOWN_PATTERN = /\.md\z/
  KOTLIN_PATTERN = /\.(kt|kts)\z/
  COMMENT_PATTERN = %r{^\s*(//|/\*|\*|@param|@property|@return|@throws|TODO|FIXME)}
  KDOC_TAG_PATTERN = /@(param|property|return|throws)/

  attr_reader :root

  def initialize(root:)
    @root = Pathname(root).expand_path
  end

  def inventory
    markdown_files = tracked_files.select { |path| path.match?(MARKDOWN_PATTERN) }
    kotlin_files = tracked_files.select { |path| path.match?(KOTLIN_PATTERN) }
    candidate_docs = markdown_files.reject { |path| excluded_markdown?(path) }

    {
      "candidateSingleLanguageMarkdown" => candidate_docs.length,
      "candidateMarkdownGroups" => group_counts(candidate_docs),
      "excludedReadme" => markdown_files.count { |path| path.match?(README_PATTERN) },
      "excludedLlmFacing" => markdown_files.count { |path| path.match?(LLM_FACING_PATTERN) },
      "manualPairFiles" => markdown_files.count { |path| path.match?(MANUAL_PAIR_PATTERN) },
      "manualParity" => manual_parity,
      "kotlinFiles" => kotlin_files.length,
      "kotlinFilesWithComments" => kotlin_comment_inventory.fetch("filesWithComments"),
      "commentLikeLines" => kotlin_comment_inventory.fetch("commentLikeLines"),
      "kdocTags" => kotlin_comment_inventory.fetch("kdocTags"),
      "commentGroups" => kotlin_comment_inventory.fetch("groups"),
    }
  end

  def validate!
    errors = []
    assert_no_primary_scope_leak(errors)
    assert_manual_parity(errors)
    raise Violation, errors.uniq.sort.join("\n") unless errors.empty?

    true
  end

  private

  def tracked_files
    @tracked_files ||= begin
      output, status = Open3.capture2("git", "ls-files", chdir: root.to_s)
      raise Violation, "git ls-files failed" unless status.success?

      output.lines.map(&:chomp).reject(&:empty?)
    end
  end

  def excluded_markdown?(path)
    path.match?(README_PATTERN) ||
      path.match?(LLM_FACING_PATTERN) ||
      path.match?(MANUAL_PAIR_PATTERN)
  end

  def group_counts(paths)
    paths.each_with_object(Hash.new(0)) do |path, counts|
      parts = path.split("/")
      key = parts.first == "docs" ? parts.first(2).join("/") : parts.first
      counts[key] += 1
    end.sort.to_h
  end

  def manual_parity
    relative_en = manual_paths("en")
    relative_ko = manual_paths("ko")
    {
      "en" => relative_en.length,
      "ko" => relative_ko.length,
      "missingKo" => relative_en - relative_ko,
      "missingEn" => relative_ko - relative_en,
    }
  end

  def manual_paths(locale)
    tracked_files
      .grep(%r{\Adocs/manual/#{locale}/.+\.md\z})
      .map { |path| path.delete_prefix("docs/manual/#{locale}/") }
      .sort
  end

  def kotlin_comment_inventory
    @kotlin_comment_inventory ||= begin
      groups = Hash.new(0)
      files_with_comments = 0
      comment_like_lines = 0
      kdoc_tags = 0

      tracked_files.grep(KOTLIN_PATTERN).each do |relative_path|
        lines = root.join(relative_path).read.lines
        hits = lines.count { |line| line.match?(COMMENT_PATTERN) }
        next if hits.zero?

        files_with_comments += 1
        comment_like_lines += hits
        kdoc_tags += lines.count { |line| line.match?(KDOC_TAG_PATTERN) }
        groups[relative_path.split("/").first] += hits
      end

      {
        "filesWithComments" => files_with_comments,
        "commentLikeLines" => comment_like_lines,
        "kdocTags" => kdoc_tags,
        "groups" => groups.sort.to_h,
      }
    end
  end

  def assert_no_primary_scope_leak(errors)
    primary = tracked_files.grep(MARKDOWN_PATTERN).reject { |path| excluded_markdown?(path) }
    leaks = primary.select do |path|
      path.match?(README_PATTERN) ||
        path.match?(LLM_FACING_PATTERN) ||
        path.match?(MANUAL_PAIR_PATTERN)
    end
    leaks.each { |path| errors << "excluded Markdown entered primary scope: #{path}" }
  end

  def assert_manual_parity(errors)
    parity = manual_parity
    parity.fetch("missingKo").each { |path| errors << "manual ko missing #{path}" }
    parity.fetch("missingEn").each { |path| errors << "manual en missing #{path}" }
  end
end
