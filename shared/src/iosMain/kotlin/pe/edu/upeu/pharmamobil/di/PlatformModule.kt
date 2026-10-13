@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroIos

actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single(named("urlBase")) { "http://localhost:8080/api/v1/" }
    single<Compartidor> { CompartidorIos() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single(named("esDepuracion")) { kotlin.native.Platform.isDebugBinary }
}