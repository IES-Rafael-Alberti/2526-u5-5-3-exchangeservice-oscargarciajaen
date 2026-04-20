package org.iesra.revilofe

/**
 * Representa una cantidad de dinero en una moneda.
 * @constructor Crea una instancia de Money.
 * @property amount La cantidad de dinero, en la unidad más pequeña de la moneda (p.ej. centavos).
 * @property currency El código de la moneda, en formato ISO 4217 (tres letras).
 *
 */
data class Money(val amount: Long, val currency: String)
