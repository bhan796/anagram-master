import Foundation

protocol ConundrumProviding {
    func randomConundrum() -> Conundrum?
    func allConundrums() -> [Conundrum]
}

final class BundledConundrumProvider: ConundrumProviding {
    private let conundrums: [Conundrum]

    init(resourceName: String = "conundrums", subdirectory: String = "Data", bundle: Bundle = .main) {
        self.conundrums = Self.loadConundrums(resourceName: resourceName, subdirectory: subdirectory, bundle: bundle)
    }

    func randomConundrum() -> Conundrum? {
        guard let selected = conundrums.randomElement() else { return nil }
        return Conundrum(
            id: selected.id,
            scrambled: Self.scrambleWord(selected.answer),
            answer: selected.answer
        )
    }

    func allConundrums() -> [Conundrum] {
        conundrums
    }

    private static func loadConundrums(resourceName: String, subdirectory: String, bundle: Bundle) -> [Conundrum] {
        guard let url = bundle.url(forResource: resourceName, withExtension: "json", subdirectory: subdirectory),
              let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode([Conundrum].self, from: data) else {
            return []
        }

        return decoded
    }

    private static func scrambleWord(_ answer: String) -> String {
        let normalized = answer.uppercased()
        guard normalized.count > 1 else { return normalized }

        let chars = Array(normalized)
        for _ in 0..<12 {
            let shuffled = chars.shuffled()
            let candidate = String(shuffled)
            if candidate != normalized {
                return candidate
            }
        }

        return String(normalized.dropFirst()) + String(normalized.prefix(1))
    }
}
