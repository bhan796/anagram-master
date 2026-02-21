import Foundation

struct InMemoryDictionaryProvider: DictionaryProviding {
    private let words: Set<String>

    init(words: Set<String>) {
        self.words = words
    }

    func contains(_ normalizedWord: String) -> Bool {
        words.contains(normalizedWord)
    }
}

final class BundledDictionaryProvider: DictionaryProviding {
    private let words: Set<String>

    init(resourceName: String = "dictionary_sample", subdirectory: String = "Data", bundle: Bundle = .main) {
        self.words = Self.loadWords(resourceName: resourceName, subdirectory: subdirectory, bundle: bundle)
    }

    func contains(_ normalizedWord: String) -> Bool {
        words.contains(normalizedWord)
    }

    private static func loadWords(resourceName: String, subdirectory: String, bundle: Bundle) -> Set<String> {
        guard let url = bundle.url(forResource: resourceName, withExtension: "txt", subdirectory: subdirectory),
              let raw = try? String(contentsOf: url, encoding: .utf8) else {
            return []
        }

        let entries = raw
            .split(whereSeparator: \.isNewline)
            .map(String.init)
            .map(WordNormalizer.normalize)
            .filter { WordNormalizer.isAlphabetical($0) }

        return Set(entries)
    }
}
