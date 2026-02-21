import Foundation

struct AppDependencies {
    let dictionary: DictionaryProviding
    let conundrums: ConundrumProviding

    static let live = AppDependencies(
        dictionary: BundledDictionaryProvider(),
        conundrums: BundledConundrumProvider()
    )
}