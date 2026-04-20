package org.iesra.revilofe

/**
 * Implementación en memoria de ExchangeRateProvider.
 * Recibe un mapa de pares de moneda (por ejemplo "USDEUR") a tasa.
 */
class InMemoryExchangeRateProvider(
    private val rates: Map<String, Double>
) : ExchangeRateProvider {
    init {
        require(rates.all { (k, _) -> k.length == 6 }) { "Invalid currency pair format" }
    }

    override fun rate(pair: String): Double {
        val normalizedPair = pair.uppercase()
        val rate = rates[normalizedPair] ?: throw IllegalArgumentException("Rate not found for pair $normalizedPair")
        require(rate > 0) { "Rate must be positive for pair $normalizedPair" }
        return rate
    }
}
