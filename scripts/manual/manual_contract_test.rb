require "fileutils"
require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "manual_contract"

class ManualContractTest < Minitest::Test
  RELEASE_COMMIT = "aead213d2d25307d7d3684226943a5f95c7411f2"

  def test_accepts_matching_locales_documents_links_and_release_sources
    with_fixture do |root, manifest_path|
      write_document(root, "en/index.md", "[Start](getting-started.md)\n\n[Source](#{source_url})\n")
      write_document(root, "en/getting-started.md", "# Start\n")
      write_document(root, "ko/index.md", "[시작](getting-started.md)\n\n[소스](#{source_url})\n")
      write_document(root, "ko/getting-started.md", "# 시작\n")
      write_manifest(manifest_path, documents: {
        "en" => %w[en/index.md en/getting-started.md],
        "ko" => %w[ko/index.md ko/getting-started.md],
      })

      assert ManualContract.new(root: root, manifest: manifest_path).validate!
    end
  end

  def test_rejects_locale_document_path_mismatch
    with_fixture do |root, manifest_path|
      write_document(root, "en/index.md", "# Index\n")
      write_document(root, "ko/getting-started.md", "# 시작\n")
      write_manifest(manifest_path, documents: {
        "en" => ["en/index.md"],
        "ko" => ["ko/getting-started.md"],
      })

      error = assert_raises(ManualContract::Violation) do
        ManualContract.new(root: root, manifest: manifest_path).validate!
      end
      assert_includes error.message, "locale document paths differ"
    end
  end

  def test_rejects_missing_declared_document
    with_fixture do |root, manifest_path|
      write_manifest(manifest_path, documents: {
        "en" => ["en/index.md"],
        "ko" => ["ko/index.md"],
      })

      error = assert_raises(ManualContract::Violation) do
        ManualContract.new(root: root, manifest: manifest_path).validate!
      end
      assert_includes error.message, "missing document en/index.md"
      assert_includes error.message, "missing document ko/index.md"
    end
  end

  def test_rejects_missing_relative_link
    with_fixture do |root, manifest_path|
      %w[en ko].each { |locale| write_document(root, "#{locale}/index.md", "[Missing](missing.md)\n") }
      write_manifest(manifest_path, documents: {
        "en" => ["en/index.md"],
        "ko" => ["ko/index.md"],
      })

      error = assert_raises(ManualContract::Violation) do
        ManualContract.new(root: root, manifest: manifest_path).validate!
      end
      assert_includes error.message, "missing or unsafe document reference missing.md"
    end
  end

  def test_rejects_mutable_github_source_ref
    with_fixture do |root, manifest_path|
      mutable_url = "https://github.com/bluetape4k/bluetape4k-text/blob/develop/README.md"
      %w[en ko].each { |locale| write_document(root, "#{locale}/index.md", "[Source](#{mutable_url})\n") }
      write_manifest(manifest_path, documents: {
        "en" => ["en/index.md"],
        "ko" => ["ko/index.md"],
      })

      error = assert_raises(ManualContract::Violation) do
        ManualContract.new(root: root, manifest: manifest_path).validate!
      end
      assert_includes error.message, "source link must use releaseRef 0.3.0"
    end
  end

  def test_accepts_release_commit_source_ref
    with_fixture do |root, manifest_path|
      commit_url = "https://github.com/bluetape4k/bluetape4k-text/blob/#{RELEASE_COMMIT}/README.md"
      %w[en ko].each { |locale| write_document(root, "#{locale}/index.md", "[Source](#{commit_url})\n") }
      write_manifest(manifest_path, documents: {
        "en" => ["en/index.md"],
        "ko" => ["ko/index.md"],
      })

      assert ManualContract.new(root: root, manifest: manifest_path).validate!
    end
  end

  def test_rejects_document_symlink_outside_manual_root
    with_fixture do |root, manifest_path|
      outside = File.join(root, "outside.md")
      File.write(outside, "outside\n")
      %w[en ko].each do |locale|
        path = File.join(root, "docs/manual/#{locale}/index.md")
        FileUtils.mkdir_p(File.dirname(path))
        File.symlink(outside, path)
      end
      write_manifest(manifest_path, documents: {
        "en" => ["en/index.md"],
        "ko" => ["ko/index.md"],
      })

      error = assert_raises(ManualContract::Violation) do
        ManualContract.new(root: root, manifest: manifest_path).validate!
      end
      assert_includes error.message, "unsafe document realpath en/index.md"
    end
  end

  def test_requires_exactly_24_documents_per_locale_when_complete
    with_fixture do |root, manifest_path|
      write_document(root, "en/index.md", "# Index\n")
      write_document(root, "ko/index.md", "# 색인\n")
      write_manifest(manifest_path, documents: {
        "en" => ["en/index.md"],
        "ko" => ["ko/index.md"],
      }, content_status: "complete")

      error = assert_raises(ManualContract::Violation) do
        ManualContract.new(root: root, manifest: manifest_path).validate!
      end
      assert_includes error.message, "English document count must be 24"
      assert_includes error.message, "Korean document count must be 24"
    end
  end

  def test_release_diagram_links_do_not_increase_the_published_asset_count
    with_fixture do |root, manifest_path|
      inventory = {
        "schemaVersion" => 1,
        "sourcePolicy" => "release-readme",
        "diagrams" => [{ "id" => "one" }, { "id" => "two" }],
      }
      File.write(File.join(root, "docs/manual/release-diagrams.yaml"), YAML.dump(inventory))

      contract = ManualContract.new(root: root, manifest: manifest_path)

      assert_equal 12, contract.send(:expected_asset_count)
    end
  end

  private

  def with_fixture
    Dir.mktmpdir do |root|
      manifest_path = File.join(root, "docs/manual/manifest.yaml")
      FileUtils.mkdir_p(File.dirname(manifest_path))
      yield root, manifest_path
    end
  end

  def write_document(root, relative_path, content)
    path = File.join(root, "docs/manual", relative_path)
    FileUtils.mkdir_p(File.dirname(path))
    File.write(path, content)
  end

  def write_manifest(path, documents:, content_status: "in-progress")
    manifest = {
      "schemaVersion" => 2,
      "repository" => "bluetape4k-text",
      "stableVersion" => "0.3.0",
      "stableMinor" => "0.3",
      "releaseTag" => "0.3.0",
      "releaseRef" => "0.3.0",
      "releaseCommit" => RELEASE_COMMIT,
      "publication" => {
        "manualVersion" => "0.3",
        "sourceRoot" => "docs/manual",
        "locales" => %w[en ko],
        "contentStatus" => content_status,
      },
      "overview" => { "documents" => documents, "assets" => [] },
      "evidence" => [],
      "modules" => [],
    }
    File.write(path, YAML.dump(manifest))
  end

  def source_url
    "https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/README.md"
  end
end
