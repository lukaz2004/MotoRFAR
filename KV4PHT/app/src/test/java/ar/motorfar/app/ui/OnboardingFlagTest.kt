package ar.motorfar.app.ui

import ar.motorfar.app.data.AppSetting
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlagTest {

    @Test
    fun flag_missing_means_show_onboarding() {
        assertTrue(OnboardingHelper.shouldShowOnboarding(emptyMap()))
    }

    @Test
    fun flag_false_means_show_onboarding() {
        assertTrue(
            OnboardingHelper.shouldShowOnboarding(
                mapOf(AppSetting.SETTING_ONBOARDING_COMPLETE to "false")
            )
        )
    }

    @Test
    fun flag_true_means_skip_onboarding() {
        assertFalse(
            OnboardingHelper.shouldShowOnboarding(
                mapOf(AppSetting.SETTING_ONBOARDING_COMPLETE to "true")
            )
        )
    }
}
