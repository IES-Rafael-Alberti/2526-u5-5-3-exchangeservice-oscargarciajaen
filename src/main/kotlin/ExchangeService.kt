package org.iesra.revilofe

/**
 * Servicio que realiza conversiones de moneda usando un [ExchangeRateProvider].
 * Soporta monedas con códigos de 3 letras (ISO 4217).
 * Soporta una conversión cruzada de un solo paso, es decir, A -> C -> B, pero no A -> C -> D -> B.
 * Requiere que las monedas de origen y destino sean válidas y que la cantidad sea positiva.
 * Si la moneda de origen y destino son iguales, devuelve la cantidad original sin consultar tasas.
 * Si la tasa directa existe, la usa; si no, intenta una conversión cruzada con monedas intermedias.
 *
 * @property rateProvider Proveedor de tasas de cambio que implementa ExchangeRateProvider.
 * @param supportedCurrencies Conjunto de monedas soportadas para conversiones cruzadas.
 * @constructor Crea un servicio de intercambio de divisas.
 * @see ExchangeRateProvider
 * @see Money
 *
 */
class ExchangeService(
    private val rateProvider: ExchangeRateProvider,
    private val supportedCurrencies: Set<String> = setOf("USD", "EUR", "GBP", "JPY")
) {
    /**
     * Convierte la cantidad `money` a la `targetCurrency`.
     * Si no existe la tasa directa, intenta una conversión cruzada.
     * Si la moneda de origen y destino son iguales, devuelve la cantidad original.
     * @param money La cantidad de dinero a convertir, con su moneda.
     * @param targetCurrency La moneda a la que se desea convertir, en formato ISO 4217 (tres letras).
     * @return La cantidad convertida a la moneda de destino, en la unidad más pequeña de esa moneda (p.ej. centavos).
     * @throws IllegalArgumentException Si la moneda de destino o la moneda del dinero no son válidas, o si la cantidad es negativa o cero.
     * @throws IllegalArgumentException Si no se encuentra una tasa para el par directo ni para ninguna conversión cruzada.
     *
     */
    fun exchange(money: Money, targetCurrency: String): Long {
        // Verifica que la moneda de destino y la moneda del dinero sean válidas
        require(targetCurrency.length == 3) { "Invalid currency code: $targetCurrency" }
        require(money.currency.length == 3) { "Invalid currency code: ${money.currency}" }
        require(money.amount > 0) { "Amount must be positive: ${money.amount}" }

        // Verifica si la moneda de origen y destino son iguales
        if (money.currency.equals(targetCurrency, ignoreCase = true)) {
            return money.amount
        }

        // Verifica si la moneda de origen y destino son soportadas
        val directPair = "${money.currency.uppercase()}${targetCurrency.uppercase()}"
        val directRate = runCatching { rateProvider.rate(directPair) }.getOrNull()
        if (directRate != null && directRate > 0) {
            return (money.amount * directRate).toLong()
        }

        // Si no hay tasa directa, intenta una conversión cruzada. Asume que hay monedas intermedias y que como máximo hará un solo cruce. Es decir, no soporta conversiones cruzadas de más de dos pasos. Siendo A origen y B destino, solo soporta A -> C -> B, no A -> C -> D -> B.
        val intermediates = supportedCurrencies.filter {
            !it.equals(money.currency, ignoreCase = true) &&
                    !it.equals(targetCurrency, ignoreCase = true)
        }

        var crossConversionResult: Long? = null
        var index = 0

        while (index < intermediates.size && crossConversionResult == null) {
            val intermediate = intermediates[index]
            val pair1 = "${money.currency.uppercase()}${intermediate.uppercase()}"
            val pair2 = "${intermediate.uppercase()}${targetCurrency.uppercase()}"

            val rate1 = runCatching { rateProvider.rate(pair1) }.getOrNull()
            val rate2 = runCatching { rateProvider.rate(pair2) }.getOrNull()

            if (rate1 != null && rate1 > 0 && rate2 != null && rate2 > 0) {
                crossConversionResult = (money.amount * rate1 * rate2).toLong()
            }

            index++
        }

        // Si no se encontró una conversión directa ni cruzada, lanza una excepción
        return crossConversionResult ?: throw IllegalArgumentException(
            "No rate found for pair $directPair nor any cross conversion."
        )
    }
}