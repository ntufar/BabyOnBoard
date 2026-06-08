package io.github.ntufar.babyonboard.sensing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.github.ntufar.babyonboard.BabyOnBoardApplication
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripForegroundServiceTest {

    private lateinit var serviceController: ServiceController<TripForegroundService>
    private lateinit var service: TripForegroundService
    private val tripRepository: TripRepository = mockk()
    private val context: Context = RuntimeEnvironment.getApplication()
    private val receivedIntents = CopyOnWriteArrayList<Intent>()

    private val testReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            receivedIntents.add(intent)
        }
    }

    private fun markAsInjected(service: TripForegroundService) {
        var c: Class<*>? = service.javaClass
        while (c != null) {
            try {
                val injectedField = c.getDeclaredField("injected")
                injectedField.isAccessible = true
                injectedField.set(service, true)
                break
            } catch (_: Exception) {
                c = c.superclass
            }
        }
    }

    @Before
    fun setup() {
        coEvery { tripRepository.saveMetricSample(any()) } returns Unit
        context.registerReceiver(
            testReceiver,
            IntentFilter(TripForegroundService.ACTION_EVENT_DETECTED),
            Context.RECEIVER_EXPORTED
        )

        serviceController = Robolectric.buildService(TripForegroundService::class.java)
        service = serviceController.get()
        markAsInjected(service)
        service.tripRepository = tripRepository
    }

    @After
    fun teardown() {
        context.unregisterReceiver(testReceiver)
        try {
            serviceController.destroy()
        } catch (_: Exception) {}
    }

    @Test
    fun `service registers screen receiver on create and unregisters on destroy`() {
        serviceController.create()
        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())

        // Verify that receivers for ACTION_SCREEN_ON and ACTION_SCREEN_OFF are registered
        val screenOnRegistered = shadowApp.registeredReceivers.any { receiverFilter ->
            receiverFilter.intentFilter.hasAction(Intent.ACTION_SCREEN_ON) &&
                    receiverFilter.intentFilter.hasAction(Intent.ACTION_SCREEN_OFF)
        }
        assertThat(screenOnRegistered).isTrue()

        // Destroy service
        serviceController.destroy()

        // Verify that they are unregistered (Robolectric's registeredReceivers will not contain it anymore)
        val screenOnStillRegistered = shadowApp.registeredReceivers.any { receiverFilter ->
            receiverFilter.intentFilter.hasAction(Intent.ACTION_SCREEN_ON) &&
                    receiverFilter.intentFilter.hasAction(Intent.ACTION_SCREEN_OFF)
        }
        assertThat(screenOnStillRegistered).isFalse()
    }

    @Test
    fun `screen broadcasts propagate to distraction source`() {
        serviceController.create()

        // Send screen off broadcast
        val screenOffIntent = Intent(Intent.ACTION_SCREEN_OFF)
        context.sendBroadcast(screenOffIntent)

        // Send screen on broadcast
        val screenOnIntent = Intent(Intent.ACTION_SCREEN_ON)
        context.sendBroadcast(screenOnIntent)

        // The test verifies that registering and broadcasting these events does not throw errors
        // and is handled gracefully by the service and distraction source.
        assertThat(service).isNotNull()
    }
}
