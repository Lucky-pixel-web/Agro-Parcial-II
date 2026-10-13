package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroAndroid
import android.content.pm.ApplicationInfo

actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single(named("urlBase")) { "http://10.0.2.2:8080/api/v1/" }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single(named("esDepuracion")) {
        (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}