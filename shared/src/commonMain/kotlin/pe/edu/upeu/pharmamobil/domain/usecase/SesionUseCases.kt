package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class IniciarSesionUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(c: Credenciales) = repo.iniciarSesion(c)
}
class RecuperarSesionUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.sesionGuardada()
}
class CerrarSesionUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.cerrarSesion()
}