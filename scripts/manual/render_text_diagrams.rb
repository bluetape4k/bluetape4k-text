#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "open3"

ROOT = File.expand_path("../..", __dir__)
WIDTH = 1600
HEIGHT = 1040

PALETTE = {
  bg: "#07111f", panel: "#0d1b2d", card: "#11263b",
  border: "#31506c", text: "#eef6ff", muted: "#a9bfd2",
  cyan: "#55c7e8", teal: "#5ed6bd", purple: "#b69af5",
  amber: "#e7b85c", rose: "#ef8297", olive: "#9fbd72"
}.freeze

ASSETS = {
  "repository-learning-map" => "overview",
  "capability-map" => "architecture",
  "text-processing-pipeline" => "architecture",
  "language-detection-selection" => "guides",
  "search-flow" => "text-search",
  "request-safety-boundary" => "operations"
}.freeze

def esc(value)
  value.to_s.gsub("&", "&amp;").gsub("<", "&lt;").gsub(">", "&gt;")
end

def text(x, y, value, size: 22, fill: PALETTE[:text], anchor: "start", weight: 400, family: "Comic Mono", klass: nil)
  class_attr = klass ? %( class="#{klass}") : ""
  %(<text#{class_attr} x="#{x}" y="#{y}" text-anchor="#{anchor}" font-family="#{family}, monospace" font-size="#{size}" font-weight="#{weight}" fill="#{fill}">#{esc(value)}</text>)
end

def multiline(x, y, lines, size: 18, fill: PALETTE[:muted], gap: 26, anchor: "start", weight: 400)
  lines.each_with_index.map do |line, index|
    text(x, y + index * gap, line, size: size, fill: fill, anchor: anchor, weight: weight)
  end.join("\n")
end

def card(x, y, width, height, title, lines, accent:, title_size: 23, center: false)
  anchor = center ? "middle" : "start"
  text_x = center ? x + width / 2.0 : x + 25
  <<~SVG
    <g class="card-group" data-card="#{esc(title)}">
      <rect class="card" x="#{x}" y="#{y}" width="#{width}" height="#{height}" rx="20" fill="#{PALETTE[:card]}" stroke="#{accent}" stroke-width="2.5"/>
      #{text(text_x, y + 39, title, size: title_size, anchor: anchor, weight: 700, family: "Architects Daughter")}
      #{multiline(text_x, y + 70, lines, size: 16, anchor: anchor, gap: 23)}
    </g>
  SVG
end

def pill(x, y, label, color, width: nil)
  width ||= [label.length * 10 + 36, 110].max
  <<~SVG
    <g class="label-pill">
      <rect x="#{x}" y="#{y}" width="#{width}" height="34" rx="17" fill="#{PALETTE[:bg]}" stroke="#{color}" stroke-width="2"/>
      #{text(x + width / 2.0, y + 23, label, size: 14, fill: color, anchor: "middle", weight: 700)}
    </g>
  SVG
end

def marker_id(color)
  "arrow-#{PALETTE.key(color) || color.delete('#')}-14"
end

def marker(color)
  <<~SVG
    <marker id="#{marker_id(color)}" markerWidth="14" markerHeight="14" refX="9" refY="5" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 10 10">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#{color}" stroke="#{color}" stroke-width="0.6"/>
    </marker>
  SVG
end

def connector(d, color:, source:, target:, width: 5, dash: nil)
  dash_attr = dash ? %( stroke-dasharray="#{dash}") : ""
  %(<path class="connector" data-source="#{source}" data-target="#{target}" d="#{d}" fill="none" stroke="#{color}" stroke-width="#{width}" stroke-linecap="round" stroke-linejoin="round"#{dash_attr} marker-end="url(##{marker_id(color)})"/>)
end

def defs
  colors = %i[cyan teal purple amber rose olive].map { |name| PALETTE.fetch(name) }
  <<~SVG
    <defs>
      <filter id="shadow" x="-20%" y="-20%" width="140%" height="150%">
        <feDropShadow dx="0" dy="8" stdDeviation="10" flood-color="#000814" flood-opacity="0.42"/>
      </filter>
      #{colors.map { |color| marker(color) }.join("\n")}
    </defs>
  SVG
end

def shell(title, subtitle, body, legend: nil)
  <<~SVG
    <?xml version="1.0" encoding="UTF-8"?>
    <svg xmlns="http://www.w3.org/2000/svg" width="#{WIDTH}" height="#{HEIGHT}" viewBox="0 0 #{WIDTH} #{HEIGHT}" role="img" aria-labelledby="title desc">
      <title id="title">#{esc(title)}</title>
      <desc id="desc">#{esc(subtitle)}</desc>
      #{defs}
      <rect width="#{WIDTH}" height="#{HEIGHT}" fill="#{PALETTE[:bg]}"/>
      <rect class="frame" x="36" y="32" width="1528" height="976" rx="32" fill="none" stroke="#{PALETTE[:border]}" stroke-width="2"/>
      #{text(76, 92, title, size: 42, weight: 700, family: "Architects Daughter", klass: "title")}
      #{text(78, 128, subtitle, size: 18, fill: PALETTE[:muted], klass: "subtitle")}
      #{body}
      #{legend}
      #{text(1520, 980, "bluetape4k-text 0.3.0", size: 15, fill: "#7892aa", anchor: "end")}
    </svg>
  SVG
end

def repository_learning_map
  rows = [
    ["Tokenize Korean", "KoreanProcessor", "Korean + safety examples", :cyan],
    ["Tokenize Japanese", "JapaneseProcessor", "Japanese processor guide", :teal],
    ["Detect language", "UnicodeDetector / Lingua", "Selection + Lingua examples", :purple],
    ["Filter dictionaries", "Tokenizer core contracts", "Dictionary + blockword guide", :amber],
    ["Search many patterns", "Aho-Corasick automaton", "Search examples + benchmark", :rose],
    ["Protect the boundary", "Request models", "Input safety + operations", :olive]
  ]
  body = []
  rows.each_with_index do |(outcome, library, learn, color_name), index|
    y = 190 + index * 116
    color = PALETTE.fetch(color_name)
    body << connector("M 490 #{y + 49} H 600", color: color, source: "outcome-#{index}", target: "library-#{index}")
    body << connector("M 1000 #{y + 49} H 1110", color: color, source: "library-#{index}", target: "learn-#{index}")
  end
  body << pill(80, 146, "Outcome", PALETTE[:muted], width: 126)
  body << pill(600, 146, "Library", PALETTE[:muted], width: 126)
  body << pill(1110, 146, "Learn + run", PALETTE[:muted], width: 146)
  rows.each_with_index do |(outcome, library, learn, color_name), index|
    y = 190 + index * 116
    color = PALETTE.fetch(color_name)
    body << card(80, y, 410, 98, outcome, ["Start with the reader's outcome"], accent: color)
    body << card(600, y, 400, 98, library, ["Published runtime capability"], accent: color)
    body << card(1110, y, 410, 98, learn, ["Manual detail and runnable evidence"], accent: color, title_size: 21)
  end
  shell("Repository learning map", "Choose a problem, understand the library boundary, then learn from detailed guides and examples.", body.join("\n"))
end

def capability_map
  body = []
  body << connector("M 510 264 H 414 Q 400 264 400 278 V 536 Q 400 550 386 550 H 370", color: PALETTE[:cyan], source: "korean", target: "core")
  body << connector("M 510 494 H 450 Q 436 494 436 508 V 606 Q 436 620 422 620 H 370", color: PALETTE[:teal], source: "japanese", target: "core")
  body << connector("M 1220 264 H 910", color: PALETTE[:cyan], source: "safety-example", target: "korean")
  body << connector("M 1370 568 V 596 Q 1370 610 1356 610 H 944 Q 930 610 930 624 V 710 Q 930 724 916 724 H 910", color: PALETTE[:purple], source: "lingua-example", target: "lingua")
  body << connector("M 1220 724 H 1070 Q 1056 724 1056 710 V 568", color: PALETTE[:rose], source: "search-example", target: "text-search")
  body << connector("M 1370 338 V 366 Q 1370 380 1356 380 H 814 Q 800 380 800 394 V 420", color: PALETTE[:teal], source: "safety-example", target: "japanese")
  body << card(90, 190, 280, 148, "Text BOM", ["aligns published versions", "consumer-facing platform"], accent: PALETTE[:purple])
  body << card(90, 510, 280, 152, "Tokenizer core", ["request + message models", "dictionary contracts"], accent: PALETTE[:amber])
  body << card(510, 190, 400, 148, "Korean tokenizer", ["KoreanProcessor facade", "sentences, nouns, blockwords"], accent: PALETTE[:cyan])
  body << card(510, 420, 400, 148, "Japanese tokenizer", ["JapaneseProcessor facade", "Kuromoji-backed tokens"], accent: PALETTE[:teal])
  body << card(510, 650, 400, 148, "Language detection", ["UnicodeDetector + Lingua", "cheap signal or model confidence"], accent: PALETTE[:purple])
  body << card(950, 420, 240, 148, "Text search", ["immutable", "Aho-Corasick"], accent: PALETTE[:rose])
  body << card(1220, 190, 300, 148, "Tokenizer safety", ["boundary failures", "redacted responses"], accent: PALETTE[:cyan])
  body << card(1220, 420, 300, 148, "Lingua examples", ["confidence + routing", "ambiguous input"], accent: PALETTE[:purple])
  body << card(1220, 650, 300, 148, "Search examples", ["DSL + replacement", "Flow early stop"], accent: PALETTE[:rose])
  body << pill(90, 844, "6 published artifacts", PALETTE[:teal], width: 218)
  body << pill(324, 844, "3 runnable examples", PALETTE[:olive], width: 206)
  shell("Capability and module map", "Published libraries stay reusable; runnable examples prove integration and safety contracts.", body.join("\n"))
end

def text_processing_pipeline
  body = []
  body << connector("M 390 318 H 450", color: PALETTE[:cyan], source: "input", target: "guard")
  body << connector("M 750 318 H 810", color: PALETTE[:amber], source: "guard", target: "detect")
  body << connector("M 1110 318 H 1170", color: PALETTE[:purple], source: "detect", target: "route")
  body << connector("M 1320 406 V 574", color: PALETTE[:teal], source: "route", target: "tokenize")
  body << connector("M 1170 664 H 1020", color: PALETTE[:olive], source: "tokenize", target: "enrich")
  body << connector("M 720 664 H 570", color: PALETTE[:rose], source: "enrich", target: "result")
  body << card(90, 230, 300, 176, "1 · Accept input", ["untrusted text", "request metadata"], accent: PALETTE[:cyan])
  body << card(450, 230, 300, 176, "2 · Guard", ["non-blank", "length <= 100,000"], accent: PALETTE[:amber])
  body << card(810, 230, 300, 176, "3 · Detect if needed", ["Unicode script first", "Lingua for confidence"], accent: PALETTE[:purple])
  body << card(1170, 230, 300, 176, "4 · Route", ["one explicit policy", "do not call everything"], accent: PALETTE[:teal])
  body << card(1170, 574, 300, 180, "5 · Tokenize", ["Korean or Japanese", "processor facade"], accent: PALETTE[:teal])
  body << card(720, 574, 300, 180, "6 · Enrich", ["dictionary filtering", "multi-pattern search"], accent: PALETTE[:olive])
  body << card(270, 574, 300, 180, "7 · Return result", ["stable response shape", "no raw-text leakage"], accent: PALETTE[:rose])
  body << pill(90, 872, "Optional stages stay optional", PALETTE[:purple], width: 276)
  shell("Text processing pipeline", "Compose only the stages required by the request contract.", body.join("\n"))
end

def language_detection_selection
  body = []
  body << connector("M 700 310 V 350 Q 700 364 686 364 H 369 Q 355 364 355 378 V 420", color: PALETTE[:cyan], source: "contract", target: "known")
  body << connector("M 900 310 V 350 Q 900 364 914 364 H 1231 Q 1245 364 1245 378 V 420", color: PALETTE[:purple], source: "contract", target: "unknown")
  body << connector("M 355 550 V 700", color: PALETTE[:teal], source: "known", target: "direct")
  body << connector("M 1245 550 V 650", color: PALETTE[:purple], source: "unknown", target: "detected")
  body << connector("M 1070 715 H 1000", color: PALETTE[:amber], source: "detected", target: "policy")
  body << connector("M 825 780 V 850", color: PALETTE[:olive], source: "policy", target: "selected")
  body << connector("M 530 765 H 600 Q 614 765 614 779 V 886 Q 614 900 628 900 H 650", color: PALETTE[:teal], source: "direct", target: "selected")
  body << card(600, 180, 400, 130, "Start with the service contract", ["Is the endpoint already language-specific?"], accent: PALETTE[:cyan], center: true)
  body << card(180, 420, 350, 130, "Language is known", ["skip detection", "validate and call directly"], accent: PALETTE[:cyan])
  body << card(1070, 420, 350, 130, "Language is unknown", ["inspect Unicode scripts", "use Lingua when confidence matters"], accent: PALETTE[:purple], title_size: 22)
  body << card(180, 700, 350, 130, "Direct processor", ["KoreanProcessor", "JapaneseProcessor"], accent: PALETTE[:teal])
  body << card(1070, 650, 350, 130, "Detected languages", ["evidence, not an instruction", "ambiguous sets are possible"], accent: PALETTE[:amber])
  body << card(650, 650, 350, 130, "Routing policy", ["supported languages", "fallback and ambiguity rules"], accent: PALETTE[:olive])
  body << card(650, 850, 350, 100, "Selected processor", ["one intentional execution path"], accent: PALETTE[:teal])
  shell("Language detection selection", "Detection informs routing; the application still owns the policy.", body.join("\n"))
end

def search_flow
  body = []
  body << connector("M 360 308 H 420", color: PALETTE[:cyan], source: "keywords", target: "builder")
  body << connector("M 700 308 H 760", color: PALETTE[:teal], source: "builder", target: "trie")
  body << connector("M 1040 308 H 1100", color: PALETTE[:purple], source: "trie", target: "automaton")
  body << connector("M 1240 396 V 530 Q 1240 544 1226 544 H 1120 Q 1106 544 1106 558 V 620", color: PALETTE[:purple], source: "automaton", target: "scan")
  body << connector("M 1200 708 H 1120", color: PALETTE[:cyan], source: "text", target: "scan")
  body << connector("M 840 708 H 760", color: PALETTE[:amber], source: "scan", target: "matches")
  body << connector("M 480 708 H 400", color: PALETTE[:olive], source: "matches", target: "apis")
  body << card(80, 220, 280, 176, "Keywords + values", ["patterns", "application payloads"], accent: PALETTE[:cyan])
  body << card(420, 220, 280, 176, "Builder / DSL", ["case + overlap", "boundary + normalization"], accent: PALETTE[:teal])
  body << card(760, 220, 280, 176, "Trie + failure links", ["prepare once", "reuse transitions"], accent: PALETTE[:purple])
  body << card(1100, 220, 280, 176, "Immutable automaton", ["safe concurrent reads", "options are fixed"], accent: PALETTE[:purple])
  body << card(1200, 620, 280, 176, "Input text", ["single bounded scan", "Unicode normalization option"], accent: PALETTE[:cyan])
  body << card(840, 620, 280, 176, "Walk automaton", ["follow transitions", "emit every configured hit"], accent: PALETTE[:amber])
  body << card(480, 620, 280, 176, "Matches", ["range + keyword", "associated value"], accent: PALETTE[:olive])
  body << card(120, 620, 280, 176, "Consumer APIs", ["parse / first / contains", "tokenize / replace / Flow"], accent: PALETTE[:rose], title_size: 22)
  body << pill(80, 884, "Build once", PALETTE[:purple], width: 132)
  body << pill(226, 884, "Scan many times", PALETTE[:olive], width: 166)
  shell("Aho-Corasick search flow", "Build an immutable automaton once, then scan input in one pass.", body.join("\n"))
end

def request_safety_boundary
  body = []
  body << connector("M 360 318 H 430", color: PALETTE[:cyan], source: "input", target: "request")
  body << connector("M 710 318 H 780", color: PALETTE[:amber], source: "request", target: "blank")
  body << connector("M 1060 318 H 1130", color: PALETTE[:amber], source: "blank", target: "length")
  body << connector("M 1270 406 V 600", color: PALETTE[:teal], source: "length", target: "processor")
  body << connector("M 1130 688 H 1000", color: PALETTE[:olive], source: "processor", target: "result")
  body << connector("M 920 406 V 470 Q 920 484 906 484 H 530 Q 516 484 516 498 V 790", color: PALETTE[:rose], source: "blank", target: "reject")
  body << connector("M 1270 406 V 510 Q 1270 524 1256 524 H 572 Q 558 524 558 538 V 790", color: PALETTE[:amber], source: "length", target: "reject")
  body << card(80, 230, 280, 176, "Untrusted input", ["raw request text", "never log the body"], accent: PALETTE[:cyan])
  body << card(430, 230, 280, 176, "Request model", ["construct at boundary", "fail before heavy work"], accent: PALETTE[:amber])
  body << card(780, 230, 280, 176, "Non-blank check", ["reject empty meaning", "no tokenizer call"], accent: PALETTE[:amber])
  body << card(1130, 230, 280, 176, "Length check", ["maximum 100,000 chars", "tokenize + blockword"], accent: PALETTE[:amber])
  body << card(1130, 600, 280, 176, "Processor", ["tokenizer / dictionary", "model work starts here"], accent: PALETTE[:teal])
  body << card(720, 600, 280, 176, "Safe result", ["stable response shape", "bounded metadata"], accent: PALETTE[:olive])
  body << card(250, 790, 320, 150, "Reject at the edge", ["400 blank / 413 oversized", "redacted message + metrics"], accent: PALETTE[:rose])
  body << pill(650, 884, "Rose = invalid input", PALETTE[:rose], width: 210)
  body << pill(874, 884, "Amber = size violation", PALETTE[:amber], width: 232)
  shell("Request safety boundary", "Validate before dictionaries, tokenizers, or language models see the text.", body.join("\n"))
end

def svg_for(name)
  case name
  when "repository-learning-map" then repository_learning_map
  when "capability-map" then capability_map
  when "text-processing-pipeline" then text_processing_pipeline
  when "language-detection-selection" then language_detection_selection
  when "search-flow" then search_flow
  when "request-safety-boundary" then request_safety_boundary
  else raise ArgumentError, "unknown asset: #{name}"
  end
end

requested = ARGV.empty? ? ASSETS.keys : ARGV
unknown = requested - ASSETS.keys
abort "unknown assets: #{unknown.join(', ')}" unless unknown.empty?

requested.each do |name|
  directory = File.join(ROOT, "docs/manual/assets", ASSETS.fetch(name))
  FileUtils.mkdir_p(directory)
  svg = File.join(directory, "#{name}.svg")
  png = File.join(directory, "#{name}.png")
  File.write(svg, svg_for(name))
  system("xmllint", "--noout", svg, exception: true)
  system("cairosvg", svg, "-o", png, "-s", "2", exception: true)
  puts "rendered #{name}: #{svg} -> #{png}"
end
