package com.example.notifications

import android.app.job.JobParameters
import android.app.job.JobService
import com.example.data.model.MarkupConfig
import com.example.data.repository.FlightRepository
import com.example.domain.location.NearestAirportLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * JobService nativo do Android que executa em segundo plano o Radar Global Automático
 * do AguiaFly consultando a API para detetar super promoções para diferentes países
 * a partir do aeroporto mais próximo do utilizador (ex.: IBZ, BCN, LIS, GRU) e enviar notificações Push.
 */
class GlobalPromoRadarJobService : JobService() {

    private val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        if (!PushNotificationHelper.isGlobalAutoPromoRadarEnabled(applicationContext)) {
            return false
        }
        jobScope.launch {
            try {
                val detected = NearestAirportLocator.detectNearestAirport(applicationContext)
                val originIata = detected.airport.iataCode
                val repo = FlightRepository()
                val liveDeals = try {
                    repo.fetchLivePromotionalDeals(originIata, MarkupConfig())
                } catch (_: Exception) {
                    emptyList()
                }
                PushNotificationHelper.triggerAutoGlobalPromoRadarIfEligible(
                    context = applicationContext,
                    currentOriginIata = originIata,
                    deals = liveDeals,
                    forceImmediate = true
                )
            } catch (_: Exception) {
                // Ignora falhas de rede em segundo plano
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        jobScope.cancel()
        return true
    }

    override fun onDestroy() {
        jobScope.cancel()
        super.onDestroy()
    }
}
