require "json"
require "minitest/autorun"
require "tmpdir"

require_relative "export_manifest"

class ManifestExporterTest < Minitest::Test
  def test_preserves_minor_version_document_order_and_one_final_newline
    Dir.mktmpdir do |root|
      source = File.join(root, "manifest.yaml")
      output = File.join(root, "manifest.json")
      File.write(source, <<~YAML)
        stableMinor: '0.3'
        overview:
          documents:
            en:
            - en/index.md
            - en/getting-started.md
        modules:
        - id: tokenizer-core
        - id: bluetape4k-text-bom
      YAML

      ManifestExporter.new(source: source, output: output).write
      rendered = File.binread(output)
      parsed = JSON.parse(rendered)

      assert_equal "0.3", parsed.fetch("stableMinor")
      assert_equal %w[en/index.md en/getting-started.md], parsed.dig("overview", "documents", "en")
      assert_equal %w[bluetape4k-text-bom tokenizer-core], parsed.fetch("modules").map { |entry| entry.fetch("id") }
      assert rendered.end_with?("\n")
      refute rendered.end_with?("\n\n")
      assert ManifestExporter.new(source: source, output: output).current?
    end
  end
end
