#!/usr/bin/env ruby

require "open3"
require "set"

class ReleaseInventory
  class Violation < StandardError; end

  DEFAULT_REF = "0.3.0"
  DEFAULT_SHA = "aead213d2d25307d7d3684226943a5f95c7411f2"
  MODULES = %w[bom tokenizer-core tokenizer-korean tokenizer-japanese lingua text-search].freeze
  EXAMPLES = %w[
    examples/text-search-examples
    examples/lingua-examples
    examples/tokenizer-safety-examples
  ].freeze
  EVIDENCE = %w[
    docs/superpowers/research/2026-05-27-issue-86-quality-report.md
    docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json
  ].freeze

  def initialize(root:, ref:, expected_sha:, git_runner: nil)
    @root = File.expand_path(root)
    @ref = ref
    @expected_sha = expected_sha
    @git_runner = git_runner || method(:run_git)
  end

  def validate!
    sha, found = @git_runner.call(["rev-parse", "--verify", "#{@ref}^{commit}"])
    raise Violation, "release ref not found: #{@ref}" unless found
    unless sha.strip.casecmp?(@expected_sha)
      raise Violation, "release ref #{@ref} resolves to #{sha.strip}, expected #{@expected_sha}"
    end

    output, found = @git_runner.call(["ls-tree", "-r", "--name-only", sha.strip])
    raise Violation, "release tree could not be read: #{sha.strip}" unless found
    tree = output.lines(chomp: true).to_set

    errors = []
    MODULES.each { |path| errors << "missing release module #{path}" unless contains_directory?(tree, path) }
    EXAMPLES.each { |path| errors << "missing release example #{path}" unless contains_directory?(tree, path) }
    EVIDENCE.each { |path| errors << "missing release evidence #{path}" unless tree.include?(path) }
    raise Violation, errors.join("\n") unless errors.empty?

    { "modules" => MODULES, "examples" => EXAMPLES, "evidence" => EVIDENCE }
  end

  private

  def contains_directory?(tree, directory)
    tree.any? { |path| path.start_with?("#{directory}/") }
  end

  def run_git(arguments)
    stdout, _stderr, status = Open3.capture3("git", "-C", @root, *arguments)
    [stdout, status.success?]
  end
end

if $PROGRAM_NAME == __FILE__
  arguments = ARGV.dup
  ref_index = arguments.index("--ref")
  ref = ref_index ? arguments.fetch(ref_index + 1) : ReleaseInventory::DEFAULT_REF
  inventory = ReleaseInventory.new(
    root: Dir.pwd,
    ref: ref,
    expected_sha: ReleaseInventory::DEFAULT_SHA,
  ).validate!
  puts "Release inventory valid: #{inventory.fetch('modules').length} modules, " \
       "#{inventory.fetch('examples').length} examples, #{inventory.fetch('evidence').length} evidence files."
end
