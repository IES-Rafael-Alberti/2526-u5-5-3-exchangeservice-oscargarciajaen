package com.example.exchange

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.iesra.revilofe.ExchangeRateProvider
import org.iesra.revilofe.ExchangeService
import org.iesra.revilofe.InMemoryExchangeRateProvider
import org.iesra.revilofe.Money
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import org.junit.jupiter.api.Assertions.assertEquals

class ExchangeServiceDesignedBatteryTest : DescribeSpec({

    afterTest {
        clearAllMocks()
    }

    describe("battery designed from equivalence classes for ExchangeService") {

        describe("input validation") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            it("throws an exception when the amount is zero") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(0, "USD"), "EUR")
                }
            }

            it("throws an exception when the amount is negative") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(-5, "USD"), "EUR")
                }
            }

            it("throws an exception when the source currency code is invalid") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(0, "JDKF"), "EUR")
                }
            }

            it("throws an exception when the target currency code is invalid") {
                shouldThrow<IllegalArgumentException>{
                    service.exchange(Money(0, "USD"), "JDKF")
                }
            }

            it("Debe devolver la misma cantidad si origen y destino son iguales") {
                service.exchange(Money(5, "USD"), "USD") shouldBe 5
            }

            it ("Debe convertir correctamente usando una tasa directa con stub"){
                val rateProvider = mockk<ExchangeRateProvider>()
                every { rateProvider.rate("USDEUR") } returns 0.9
            }
        }

        it("Debe usar spy sobre InMemoryExchangeRateProvider para verificar una llamada real correcta.") {
            val provider = InMemoryExchangeRateProvider(
                mapOf("USDEUR" to 0.9)
            )
            val spyProvider = spyk(provider)
            val service = ExchangeService(spyProvider)

            service.exchange(Money(10, "USD"), "EUR") shouldBe 9L

            verify { spyProvider.rate("USDEUR") }
        }

        it ("Debe resolver una conversión cruzada cuando la tasa directa no exista usando mock"){
            val provider = mockk<ExchangeRateProvider>()
            every { provider.rate("USDJPY") } returns 150.0
            every { provider.rate("JPYEUR") } returns 0.006
            val service = ExchangeService(provider)
            service.exchange(Money(10, "USD"), "EUR") shouldBe 9L
        }

        it ("Debe intentar una segunda ruta intermedia si la primera falla usando mock."){
            val provider = mockk<ExchangeRateProvider>()
            every { provider.rate("USDJPY") } returns 150.0
            every { provider.rate("JPYEUR") } throws IllegalArgumentException()
            every { provider.rate("USDGBP") } returns 0.8
            every { provider.rate("GBPEUR") } returns 1.2
            val service = ExchangeService(provider)
            service.exchange(Money(10, "USD"), "EUR") shouldBe 9L
        }

        it ("Debe lanzar excepción si no existe ninguna ruta válida."){
            val provider = mockk<ExchangeRateProvider>()
            every { provider.rate("USDJPY") } returns 150.0
            every { provider.rate("JPYEUR") } throws IllegalArgumentException()
            every { provider.rate("USDGBP") } returns 0.8
            every { provider.rate("GBPEUR") } throws IllegalArgumentException()
            val service = ExchangeService(provider)
            shouldThrow<IllegalArgumentException> {
                service.exchange(Money(5, "USD"), "EUR")
            }
        }

        it("debe hacer conversión cruzada y verificar el orden exacto de llamadas") {
            val rateProvider = mockk<ExchangeRateProvider>()

            every { rateProvider.rate("USDEUR") } returns 0.0
            every { rateProvider.rate("USDGBP") } returns 0.8
            every { rateProvider.rate("GBPEUR") } returns 1.1

            val service = ExchangeService(rateProvider, setOf("USD", "EUR", "GBP", "JPY"))

            val result = service.exchange(Money(100, "USD"), "EUR")

            assert(result == 88L)

            verifySequence {
                rateProvider.rate("USDEUR")
                rateProvider.rate("USDGBP")
                rateProvider.rate("GBPEUR")
            }
        }
}})
