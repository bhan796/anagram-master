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
        conundrums.randomElement()
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
}
