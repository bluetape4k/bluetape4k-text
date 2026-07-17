require "pathname"
require "yaml"

class ManualContract
  class Violation < StandardError; end

  EXPECTED_SCHEMA = 2
  EXPECTED_REPOSITORY = "bluetape4k-text"
  EXPECTED_LOCALES = %w[en ko].freeze
  EXPECTED_DOCUMENT_COUNT = 24
  EXPECTED_ASSET_COUNT = 12

  def initialize(root:, manifest:)
    @root = Pathname(root).expand_path
    @manifest_path = Pathname(manifest).expand_path
    @manual_root = @manifest_path.dirname
  end

  def validate!
    errors = []
    manifest = load_manifest(errors)
    raise Violation, errors.join("\n") unless manifest

    validate_header(manifest, errors)
    validate_documents(manifest, errors)
    validate_assets(manifest, errors)
    validate_evidence(manifest, errors)
    validate_module_routes(manifest, errors)

    raise Violation, errors.uniq.sort.join("\n") unless errors.empty?

    true
  end

  private

  def load_manifest(errors)
    unless @manifest_path.file?
      errors << "manual manifest not found: #{@manifest_path}"
      return nil
    end

    manifest = YAML.safe_load(@manifest_path.read)
    unless manifest.is_a?(Hash)
      errors << "manual manifest must be a mapping"
      return nil
    end
    manifest
  rescue Psych::SyntaxError => error
    errors << "manual manifest YAML is invalid: #{error.problem}"
    nil
  end

  def validate_header(manifest, errors)
    errors << "schemaVersion must be #{EXPECTED_SCHEMA}" unless manifest["schemaVersion"] == EXPECTED_SCHEMA
    errors << "repository must be #{EXPECTED_REPOSITORY}" unless manifest["repository"] == EXPECTED_REPOSITORY
    errors << "stableVersion must be 0.2.1" unless manifest["stableVersion"] == "0.2.1"
    errors << "stableMinor must be 0.2" unless manifest["stableMinor"] == "0.2"
    errors << "releaseTag must be 0.2.1" unless manifest["releaseTag"] == "0.2.1"
    errors << "releaseRef must be 0.2.1" unless manifest["releaseRef"] == "0.2.1"

    release_commit = manifest["releaseCommit"]
    errors << "releaseCommit must be a 40-character Git SHA" unless release_commit.is_a?(String) && release_commit.match?(/\A[0-9a-f]{40}\z/)

    publication = manifest["publication"]
    unless publication.is_a?(Hash)
      errors << "publication must be a mapping"
      return
    end
    errors << "manualVersion must be 0.2" unless publication["manualVersion"] == "0.2"
    errors << "sourceRoot must be docs/manual" unless publication["sourceRoot"] == "docs/manual"
    errors << "publication locales must be en and ko" unless publication["locales"] == EXPECTED_LOCALES
    unless %w[in-progress complete].include?(publication["contentStatus"])
      errors << "contentStatus must be in-progress or complete"
    end
  end

  def validate_documents(manifest, errors)
    documents = manifest.dig("overview", "documents")
    unless documents.is_a?(Hash)
      errors << "overview documents must be a mapping"
      return
    end

    locale_paths = {}
    EXPECTED_LOCALES.each do |locale|
      paths = documents[locale]
      unless paths.is_a?(Array) && paths.all? { |path| path.is_a?(String) }
        errors << "#{language(locale)} documents must be an array of paths"
        next
      end
      errors << "#{language(locale)} documents contain duplicates" unless paths.uniq.length == paths.length
      locale_paths[locale] = paths
      if complete?(manifest) && paths.length != EXPECTED_DOCUMENT_COUNT
        errors << "#{language(locale)} document count must be #{EXPECTED_DOCUMENT_COUNT}"
      end
      paths.each { |path| validate_document(locale, path, manifest, errors) }
    end

    return unless locale_paths.keys.sort == EXPECTED_LOCALES.sort

    english = locale_paths.fetch("en").map { |path| path.delete_prefix("en/") }
    korean = locale_paths.fetch("ko").map { |path| path.delete_prefix("ko/") }
    errors << "locale document paths differ" unless english == korean

    if complete?(manifest)
      EXPECTED_LOCALES.each do |locale|
        actual = Dir.glob(@manual_root.join(locale, "**/*.md")).map do |path|
          Pathname(path).relative_path_from(@manual_root).to_s
        end.sort
        declared = locale_paths.fetch(locale, []).sort
        (actual - declared).each { |path| errors << "unregistered document #{path}" }
        (declared - actual).each { |path| errors << "declared document not found #{path}" }
      end
    end
  end

  def validate_document(locale, path, manifest, errors)
    unless safe_relative?(path) && path.start_with?("#{locale}/")
      errors << "unsafe #{language(locale)} document path #{path}"
      return
    end

    absolute = @manual_root.join(path).cleanpath
    unless absolute.file?
      errors << "missing document #{path}"
      return
    end
    unless within_realpath?(absolute, @manual_root)
      errors << "unsafe document realpath #{path}"
      return
    end

    validate_document_links(absolute, manifest, errors)
  end

  def validate_document_links(document, manifest, errors)
    content = document.read
    markdown_links = content.scan(/!?\[[^\]]*\]\(([^)]+)\)/).flatten
    reference_links = content.scan(/^[ \t]{0,3}\[[^\]]+\]:[ \t]*(?:<([^>\r\n]+)>|([^ \t\r\n]+))/).map { |match| match.compact.first }
    html_links = content.scan(/<(?:a|img)\b[^>]*(?:href|src)=["']([^"']+)["']/i).flatten

    (markdown_links + reference_links + html_links).each do |raw|
      target = raw.to_s.strip.split(/\s+["']/, 2).first.to_s.delete_prefix("<").delete_suffix(">")
      validate_source_ref(target, manifest, errors)

      path = target.split(/[?#]/, 2).first
      next if path.empty? || path.start_with?("#", "/") || path.match?(/\A[a-z][a-z0-9+.-]*:/i)

      absolute = document.dirname.join(path).cleanpath
      unless within?(absolute, @root) && absolute.exist?
        errors << "missing or unsafe document reference #{raw}"
        next
      end
      unless within_realpath?(absolute, @root)
        errors << "missing or unsafe document reference #{raw}"
      end
    end
  end

  def validate_source_ref(target, manifest, errors)
    match = target.match(%r{\Ahttps://github\.com/bluetape4k/bluetape4k-text/blob/([^/]+)/})
    return unless match

    allowed = [manifest["releaseRef"], manifest["releaseCommit"]]
    unless allowed.include?(match[1])
      errors << "source link must use releaseRef #{manifest['releaseRef']} or releaseCommit"
    end
  end

  def validate_assets(manifest, errors)
    assets = manifest.dig("overview", "assets")
    unless assets.is_a?(Array) && assets.all? { |path| path.is_a?(String) }
      errors << "overview assets must be an array of paths"
      return
    end
    errors << "overview assets contain duplicates" unless assets.uniq.length == assets.length
    expected_count = expected_asset_count
    errors << "asset count must be #{expected_count}" if complete?(manifest) && assets.length != expected_count
    assets.each do |path|
      unless safe_relative?(path) && path.start_with?("assets/")
        errors << "unsafe asset path #{path}"
        next
      end
      absolute = @manual_root.join(path).cleanpath
      errors << "missing asset #{path}" unless absolute.file? && within_realpath?(absolute, @manual_root)
    end
  end

  def expected_asset_count
    EXPECTED_ASSET_COUNT
  end

  def validate_evidence(manifest, errors)
    evidence = manifest["evidence"]
    unless evidence.is_a?(Array)
      errors << "evidence must be an array"
      return
    end
    evidence.each do |entry|
      unless entry.is_a?(Hash) && entry["id"].is_a?(String) && entry["kind"].is_a?(String) && entry["path"].is_a?(String)
        errors << "evidence entries must contain id, kind, and path"
        next
      end
      validate_repository_path(entry["path"], "evidence #{entry['id']}", errors)
    end
  end

  def validate_module_routes(manifest, errors)
    modules = manifest["modules"]
    unless modules.is_a?(Array)
      errors << "modules must be an array"
      return
    end
    modules.each do |entry|
      unless entry.is_a?(Hash)
        errors << "module entries must be mappings"
        next
      end
      Array(entry["sourcePaths"]).each do |path|
        validate_repository_path(path, "module #{entry['id']} source", errors)
      end
      EXPECTED_LOCALES.each do |locale|
        route = entry[locale]
        next if route.nil? && !complete?(manifest)
        errors << "module #{entry['id']} missing #{language(locale)} route" unless route.is_a?(String)
      end
    end
  end

  def validate_repository_path(path, label, errors)
    unless path.is_a?(String) && safe_relative?(path)
      errors << "unsafe #{label} path #{path.inspect}"
      return
    end
    absolute = @root.join(path).cleanpath
    unless absolute.exist? && within?(absolute, @root) && within_realpath?(absolute, @root)
      errors << "missing or unsafe #{label} path #{path}"
    end
  end

  def complete?(manifest)
    manifest.dig("publication", "contentStatus") == "complete"
  end

  def safe_relative?(path)
    return false unless path.is_a?(String) && !path.empty?

    pathname = Pathname(path)
    !pathname.absolute? && !pathname.each_filename.include?("..")
  end

  def within?(path, parent)
    expanded_path = Pathname(path).expand_path.to_s
    expanded_parent = Pathname(parent).expand_path.to_s
    expanded_path == expanded_parent || expanded_path.start_with?("#{expanded_parent}#{File::SEPARATOR}")
  end

  def within_realpath?(path, parent)
    within?(Pathname(path).realpath, Pathname(parent).realpath)
  rescue Errno::ENOENT
    false
  end

  def language(locale)
    locale == "en" ? "English" : "Korean"
  end
end
