#!/usr/bin/env ruby
# frozen_string_literal: true

require "rexml/document"

ROOT = File.expand_path("../..", __dir__)
ASSETS = {
  "overview/repository-learning-map" => { cards: 13, connectors: 7 },
  "architecture/capability-map" => { cards: 9, connectors: 6 },
  "architecture/text-processing-pipeline" => { cards: 7, connectors: 6 },
  "guides/language-detection-selection" => { cards: 7, connectors: 6 },
  "text-search/search-flow" => { cards: 8, connectors: 7 },
  "operations/request-safety-boundary" => { cards: 7, connectors: 6 }
}.freeze

failures = []

ASSETS.each do |rel, expected|
  svg = File.join(ROOT, "docs/manual/assets", "#{rel}.svg")
  png = File.join(ROOT, "docs/manual/assets", "#{rel}.png")
  unless File.file?(svg) && File.file?(png)
    failures << "#{rel}: missing SVG or PNG"
    next
  end

  doc = REXML::Document.new(File.read(svg))
  root = doc.root
  unless root.attributes["width"] == "1600" && root.attributes["height"] == "1040" && root.attributes["viewBox"] == "0 0 1600 1040"
    failures << "#{rel}: canvas must be 1600x1040"
  end

  cards = REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' card ')]")
  connectors = REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' connector ')]")
  markers = REXML::XPath.match(doc, "//marker[@markerUnits='userSpaceOnUse']")
  decorated_cards = REXML::XPath.match(
    doc,
    "//*[contains(concat(' ', normalize-space(@class), ' '), ' card-group ')]/rect[not(contains(concat(' ', normalize-space(@class), ' '), ' card '))]"
  )

  failures << "#{rel}: cards=#{cards.size}, expected >= #{expected[:cards]}" if cards.size < expected[:cards]
  failures << "#{rel}: connectors=#{connectors.size}, expected >= #{expected[:connectors]}" if connectors.size < expected[:connectors]
  failures << "#{rel}: markers=#{markers.size}, expected >= 6" if markers.size < 6
  failures << "#{rel}: card_decorations=#{decorated_cards.size}, expected 0" if decorated_cards.any?

  markers.each do |marker|
    size = marker.attributes["markerWidth"].to_i
    failures << "#{rel}: marker #{marker.attributes['id']} must be 14x14" unless size == 14 && marker.attributes["markerHeight"].to_i == 14
  end

  connectors.each do |connector|
    failures << "#{rel}: connector width must be >= 4" if connector.attributes["stroke-width"].to_f < 4
    unless connector.attributes["data-source"] && connector.attributes["data-target"]
      failures << "#{rel}: connector missing data-source/target"
    end
    failures << "#{rel}: diagonal connector command found" if connector.attributes["d"].to_s.match?(/\bL\s*\d+(?:\.\d+)?[ ,]+\d/)
    failures << "#{rel}: connector missing a 14x14 marker" unless connector.attributes["marker-end"].to_s.end_with?("-14)")
  end

  text_nodes = REXML::XPath.match(doc, "//text")
  failures << "#{rel}: missing Architects Daughter title" unless text_nodes.any? { |node| node.attributes["font-family"].to_s.include?("Architects Daughter") }
  failures << "#{rel}: missing Comic Mono text" unless text_nodes.any? { |node| node.attributes["font-family"].to_s.include?("Comic Mono") }

  bytes = File.binread(png, 24)
  unless bytes.start_with?("\x89PNG".b)
    failures << "#{rel}: invalid PNG signature"
  else
    width, height = bytes[16, 8].unpack("NN")
    failures << "#{rel}: PNG is #{width}x#{height}, expected 3200x2080" unless [width, height] == [3200, 2080]
  end

  puts "#{rel}: cards=#{cards.size} connectors=#{connectors.size} markers=#{markers.size} card_decorations=#{decorated_cards.size}"
end

if failures.empty?
  puts "diagram-validation: failures=0 assets=#{ASSETS.size}"
else
  warn failures.join("\n")
  abort "diagram-validation: failures=#{failures.size}"
end
