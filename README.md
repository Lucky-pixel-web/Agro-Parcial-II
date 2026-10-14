# PharmaMobil · Práctica 08-09: CRUD REST con Ktor

Aplicación Kotlin Multiplatform (Android e iOS) con Compose Multiplatform, arquitectura Clean + MVVM e inyección con Koin. En esta práctica el listado de productos deja de venir del repositorio en memoria y se consume desde el backend **PharmaSoft** (API REST) con **Ktor Client**.

- **Asignatura:** Desarrollo de Aplicaciones Móviles · UPeU · Semestre 2026-2
- **Autor:** Diego Contreras
- **Rama:** `feature/expect-actual-contreras` (la práctica 08 está en `feature/crud-productos-contreras`)
- **Compañero de pareja:** _(Diego Contreras)_

---

## 1. Requisitos para ejecutar

| Recurso | Detalle |
|---|---|
| Backend | [PharmaSoft](https://github.com/dreyna/pharmaSoft) (Spring Boot, Java 21) en `http://localhost:8080` |
| Base de datos | Oracle Database Free 23ai en Docker, puerto `1522`, servicio `FREEPDB1`, esquema `PHARMADB` |
| IDE | Android Studio (app) e IntelliJ IDEA (backend) |
| Android SDK | Plataforma **Android 17 (API 37)** instalada (ver sección 7) |
| iOS | Solo macOS con Xcode |

### Levantar el entorno

```bash
# 1. Base de datos
docker run -d --name pharma-oracle -p 1522:1521 \
  -e ORACLE_PASSWORD=<clave> -e APP_USER=PHARMADB -e APP_USER_PASSWORD=<clave> \
  -v pharma-oracle-data:/opt/oracle/oradata gvenzl/oracle-free:23-slim

# 2. Backend (desde IntelliJ ejecutar PharmaBackendApplication, o:)
./mvnw spring-boot:run
```

Verificación: `http://localhost:8080/api/health` y `http://localhost:8080/swagger-ui.html`.
La base arranca vacía: crear una categoría (`POST /api/v1/categorias`) y al menos tres productos (`POST /api/v1/productos`) desde Swagger UI.

---

## 2. URL base por plataforma

| Plataforma | Motor Ktor | URL base | Motivo |
|---|---|---|---|
| Android (emulador) | OkHttp | `http://10.0.2.2:8080/api/v1/` | En el emulador, `localhost` es el propio emulador; `10.0.2.2` apunta a la PC. |
| iOS (simulador) | Darwin | `http://localhost:8080/api/v1/` | En el simulador, `localhost` sí apunta a la máquina. |

Se declaran en `PlatformModule` de cada plataforma (`androidMain/di` e `iosMain/di`) junto con el motor, y el `HttpClient` los recibe por Koin.

---

## 3. Endpoint consumido

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `GET` | `/api/v1/productos?pagina=0&tamanio=20` | Listado paginado de productos | `200 OK` con envoltorio de paginación |

Parámetros de consulta: `pagina` (0 por defecto), `tamanio` (20), `ordenarPor` (`id`, `nombre`, `precio`, `stock`) y `direccion` (`asc`/`desc`). La app usa `pagina` y `tamanio`.

> El listado **no devuelve un arreglo**, sino un objeto paginado. Declararlo como `List<...>` provoca un error de deserialización.

Ejemplo de respuesta:

```json
{
  "contenido": [
    {
      "id": 1,
      "nombre": "Paracetamol 500 mg",
      "precio": 4.50,
      "stock": 120,
      "estado": true,
      "categoriaId": 1,
      "categoriaNombre": "Analgésicos",
      "fechaCreacion": "2026-09-20T09:12:44",
      "fechaModificacion": "2026-09-20T09:12:44"
    }
  ],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 3,
  "totalPaginas": 1,
  "ultima": true
}
```

---

## 4. Campos del DTO

### `ProductoResponseDto` (`data/remote/dto`)

| Campo | Tipo | Nota |
|---|---|---|
| `id` | `Long` | |
| `nombre` | `String` | |
| `precio` | `Double` | |
| `stock` | `Int` | |
| `estado` | `Boolean` | Valor por defecto `true` |
| `categoriaId` | `Long?` | Anulable, valor por defecto `null` |
| `categoriaNombre` | `String?` | Anulable, valor por defecto `null` |

Las fechas (`fechaCreacion`, `fechaModificacion`) no se modelan: se ignoran con `ignoreUnknownKeys = true`.

### `PaginaResponseDto<T>` (`data/remote/dto`)

| Campo | Tipo |
|---|---|
| `contenido` | `List<T>` |
| `pagina` | `Int` |
| `tamanio` | `Int` |
| `totalElementos` | `Long` |
| `totalPaginas` | `Int` |
| `ultima` | `Boolean` |

### Mapeo al dominio (`data/mapper/ProductoMapper.kt`)

`ProductoResponseDto.toDomain()` produce el modelo `Producto` (`id`, `nombre`, `precio`, `stock`, `activo = estado`). El paquete `domain` no contiene anotaciones `@Serializable` ni referencias a Ktor.

---

## 5. Arquitectura del consumo

```
ProductoScreen → ProductoViewModel → ListarProductosUseCase
      → ProductoRepository (interfaz, dominio)
            → ProductoRepositorioRest (data)
                  → ProductoApi → HttpClient (Ktor) → PharmaSoft
```

- **`HttpClient`:** una sola instancia (`single` de Koin), creada en `crearHttpClient(engine, urlBase)` (`data/remote/HttpClientFactory.kt`).
- **Plugins:** `ContentNegotiation` (JSON con `ignoreUnknownKeys` e `isLenient`), `Logging` (`LogLevel.HEADERS`, salida con el prefijo `KtorClient`), `HttpTimeout` (petición 15 s, conexión 10 s) y `defaultRequest` (URL base y `Content-Type: application/json`). Además `expectSuccess = true` convierte respuestas 4xx y 5xx en excepciones.
- **Errores de red:** el caso de uso envuelve la llamada en `Result`; el `ViewModel` los traduce al estado `Fase.Error`, y la pantalla muestra el mensaje con el botón **Reintentar** en lugar de cerrarse.
- **Alcance de la sesión:** el botón de registrar sigue guardando en memoria (`ProductoRepositorioEnMemoria`), por lo que un producto registrado desde la app no aparece en el listado. Es una inconsistencia deliberada que se resuelve al implementar el `POST` en la sesión 8.

### Configuración de tráfico HTTP local (solo desarrollo)

- Android: permiso `INTERNET` y `res/xml/network_security_config.xml` que permite tráfico en claro únicamente hacia `10.0.2.2` y `localhost`.
- iOS: `NSAppTransportSecurity` con `NSAllowsLocalNetworking` en `Info.plist`.

En producción esta excepción desaparece y todo viaja por HTTPS.


## 6. Pruebas de conexión

| Prueba | Resultado esperado | Evidencia |
|---|---|---|
| `GET /api/health` en el navegador | «Backend funcionando correctamente» | — |
| `GET /api/v1/productos` en Swagger UI | 200 con los productos creados | `docs/capturas/01-swagger-productos.png` |
| Log de Ktor en Logcat (filtro `KtorClient`) | Petición GET y respuesta `200 OK` | `docs/capturas/02-log-ktor-200.png` |
| Lista renderizada en Android | Productos del backend en pantalla | `docs/capturas/03-lista-android.png` |
| Lista renderizada en iOS | Productos del backend en pantalla | `docs/capturas/04-lista-ios.png` |
| Modo avión + Reintentar | Estado de error sin cierre de la app | `docs/capturas/05-error-modo-avion.png` |
| Crear producto nuevo en Swagger y recargar | Aparece sin recompilar la app | — |
|        |                    |           |

## 7. Notas técnicas y problemas resueltos

| Síntoma | Causa | Solución aplicada |
|---|---|---|
| `checkDebugAarMetadata` falla: `okhttp-android:5.5.0` requiere compilar contra la API 37 | `ktor-client-okhttp 3.6.0` depende de OkHttp 5.5.0 | `android-compileSdk = "37"` en `libs.versions.toml` y `android.suppressUnsupportedCompileSdk=37` en `gradle.properties` (AGP 9.0.1 reconoce oficialmente hasta la API 36). Es necesario tener instalada la plataforma Android 17 (API 37). |
| Ktor no imprime la petición en Logcat | El logger por defecto en Android depende de SLF4J | Logger propio en `crearHttpClient` que usa `println` con el prefijo `KtorClient` |
| La petición no llega al backend desde Android | `localhost` dentro del emulador | Usar `10.0.2.2` |

### Versiones

Kotlin 2.4.10 · Compose Multiplatform 1.11.1 · Ktor 3.6.0 · Koin 4.2.2 · kotlinx.serialization 1.7.3 · AGP 9.0.1


## 8. Estructura relevante

```
shared/src/
├── commonMain/kotlin/pe/edu/upeu/pharmamobil/
│   ├── data/
│   │   ├── remote/            ProductoApi, HttpClientFactory, dto/
│   │   ├── mapper/            ProductoMapper
│   │   └── repository/        ProductoRepositorioRest, ProductoRepositorioEnMemoria
│   ├── di/                    AppModule (Koin)
│   ├── domain/                model, repository, usecase
│   └── presentation/          producto, cliente
├── androidMain/.../di/        PlatformModule (OkHttp + 10.0.2.2)
└── iosMain/.../di/            PlatformModule (Darwin + localhost)
```

## Manejo de errores

Todo fallo de red o del servidor se traduce a un tipo del dominio, `ErrorApi`, en **un único punto**: la función `ejecutarLlamada` (`data/remote/EjecutarLlamada.kt`). La capa de presentación nunca importa `io.ktor`; solo recibe un `ErrorApiException` con el `ErrorApi` correspondiente.

| Situación | Excepción de origen | `ErrorApi` | Qué ve el usuario |
|---|---|---|---|
| 400 con detalle por campo | `ClientRequestException` | `Validacion(porCampo, mensaje)` | El mensaje del servidor debajo del campo nombre, precio o stock |
| 400 sin campo conocido | `ClientRequestException` | `Validacion` | Cuadro rojo con el mensaje del servidor |
| 404 | `ClientRequestException` | `NoEncontrado` | «No se encontró la información solicitada.» |
| 409 | `ClientRequestException` | `Conflicto(mensaje)` | El mensaje del servidor (nombre duplicado, producto ya inactivo) |
| 5xx | `ServerResponseException` | `Servidor` | «El servidor tuvo un problema. Inténtalo más tarde.» |
| Tiempo de espera agotado | `HttpRequestTimeoutException` | `TiempoAgotado` | «El servidor tardó demasiado en responder. Inténtalo de nuevo.» |
| Sin conexión u otro fallo de red | cualquier otra excepción | `SinConexion` | «No se pudo conectar con el servidor. Revisa tu conexión a internet.» |
| Respuesta con formato inesperado | `SerializationException` | `Servidor` | Igual que un error del servidor |
| Cancelación de la corrutina | `CancellationException` | ninguno | No se muestra error: la excepción se relanza |

### Cómo se refleja en la interfaz

`ProductoUiState` separa dos cosas para no confundir «cargando la lista» con «guardando un producto»:

- **`Fase`** describe la lista: `Cargando`, `SinProductos`, `ConProductos` y `Error` (con botón Reintentar).
- **`Operacion`** describe la acción en curso: `Inactiva`, `EnCurso(tipo)` y `Fallida(mensaje)`. Durante una operación el listado permanece visible.

Los errores de validación del servidor se asignan a `nombreError`, `precioError` y `stockError` del formulario. La validación local (nombre con caracteres permitidos, precio mayor que cero, stock entero no negativo) se ejecuta antes de llamar al servidor y no genera ninguna petición HTTP.

### Reglas del backend que conviene conocer

- `DELETE /api/v1/productos/{id}` es una **baja lógica**: el producto queda con `estado = false` y pasa a la pestaña Inactivos. Una segunda eliminación del mismo producto responde **409** («ya se encuentra inactivo»), no 404.
- Crear o actualizar con un nombre ya usado por otro producto responde **409**.
- Crear o actualizar con una categoría inexistente responde **404**.
- El precio mínimo que acepta el servidor es 0.01.

### Pruebas

Las pruebas están en `shared/src/commonTest` y se ejecutan en ambas plataformas:

- `ProductoViewModelTest`: transiciones de estado (carga, lista vacía, validación del servidor, eliminación con `EnCurso`, cancelación).
- `EjecutarLlamadaTest`: traducción de excepciones a `ErrorApi` y relanzamiento de la cancelación.

Se ejecutan desde Android Studio con clic derecho sobre `commonTest` → **Run Tests**.

## Capacidades nativas

El mecanismo `expect/actual` y la inyección de implementaciones por plataforma permiten que `commonMain` declare qué debe existir sin saber cómo se resuelve.

| Capacidad | Estrategia | Declaración común | Android | iOS |
|---|---|---|---|---|
| Formato de moneda | `expect/actual` (función sin estado) | `commonMain/platform/Formato.kt` | `androidMain/platform/Formato.android.kt` (`NumberFormat`, `Locale("es","PE")`) | `iosMain/platform/Formato.ios.kt` (`NSNumberFormatter`, `es_PE`) |
| Compartir un producto | Interfaz + Koin (necesita `Context` / controlador de vista) | `commonMain/domain/platform/Compartidor.kt` | `androidMain/platform/CompartidorAndroid.kt` (`Intent.ACTION_SEND`) | `iosMain/platform/CompartidorIos.kt` (`UIActivityViewController`) |

- Las implementaciones se registran en el `platformModule` de cada plataforma (`androidMain/di` e `iosMain/di`).
- El texto a compartir se arma en código común (`domain/usecase/TextoParaCompartir.kt`) y el formato de precio se aplica en la capa de presentación (`ProductoUi`).
- La capa `presentation` no importa paquetes de Android ni de UIKit.
- La pantalla de detalle (`presentation/detalle`) llama a `DetalleProductoViewModel.compartir()` y no sabe qué implementación hay detrás.
- Las pruebas de `commonTest` sustituyen `Compartidor` por un doble.


## Código específico de plataforma

Inventario de lo que baja al source set de cada plataforma. Todo lo demás vive en `commonMain`. Las rutas son relativas a `shared/src/<source set>/kotlin/pe/edu/upeu/pharmamobil/`.

| Capacidad | Declaración común | Android (`androidMain`) | iOS (`iosMain`) | Estrategia |
|---|---|---|---|---|
| Formato de moneda | `expect fun formatearSoles(valor: Double): String` en `platform/Formato.kt` | `platform/Formato.android.kt`: `NumberFormat.getCurrencyInstance(Locale("es", "PE"))` | `platform/Formato.ios.kt`: `NSNumberFormatter` con `NSLocale("es_PE")` | `expect/actual` |
| Compartir producto | `interface Compartidor` en `domain/platform/Compartidor.kt` | `platform/CompartidorAndroid.kt`: `Intent.ACTION_SEND` con `createChooser` | `platform/CompartidorIos.kt`: `UIActivityViewController` | Interfaz + Koin |
| Módulo de inyección | `expect val platformModule: Module` en `di/AppModule.kt` | `di/PlatformModule.android.kt`: `module` con `androidContext()` y motor `OkHttp` | `di/PlatformModule.kt`: `module` sin contexto y motor `Darwin` | `expect/actual` |
| Información del dispositivo | `expect class InfoDispositivo()` en `platform/InfoDispositivo.kt` | `platform/InfoDispositivo.android.kt`: `Build.VERSION.RELEASE` | `platform/InfoDispositivo.ios.kt`: `UIDevice.currentDevice.systemVersion` | `expect/actual` |

Otras piezas específicas de plataforma que no son `expect`: `androidApp/.../MainApplication.kt` (arranca Koin con `androidContext`), `androidApp/.../MainActivity.kt`, `iosMain/.../di/KoinIos.kt` (`initKoinIos`), `iosMain/.../MainViewController.kt` y los archivos Swift de `iosApp`.

Regla de aislamiento: `commonMain` no contiene ninguna importación `import android.` ni `import platform.`. Resultado observado en Android: `NumberFormat` devuelve `S/ 6.20` con un espacio duro (código 160) entre el símbolo y el número.





## Autenticación (JWT)

PharmaMobil inicia sesión contra `POST /api/v1/auth/login` (PharmaSoft). El token se adjunta con el
plugin `Auth` de Ktor y se guarda cifrado: `EncryptedSharedPreferences` en Android y Keychain en iOS
(`AlmacenSeguro`, registrado en el `platformModule` de cada plataforma).

- Logging de Ktor solo en depuración y con la cabecera `Authorization` saneada.
- 401: se borra el token y se vuelve al login con un aviso. 403: se mantiene la sesión y se avisa.
- La interfaz oculta acciones según los permisos (`sesion.puede("...")`).
- El token dura 3600 s y no se renueva: al expirar hay que iniciar sesión de nuevo.

### Usuarios de prueba

| Usuario  | Contraseña  | Rol        | Permisos                                 |
|----------|-------------|------------|------------------------------------------|
| admin    | Admin123    | ADMIN      | Los ocho permisos                        |
| vendedor | Vendedor123 | VENDEDOR   | Catálogo solo lectura; clientes y ventas |
| almacen  | Almacen123  | ALMACENERO | Solo catálogo (leer y escribir)          |