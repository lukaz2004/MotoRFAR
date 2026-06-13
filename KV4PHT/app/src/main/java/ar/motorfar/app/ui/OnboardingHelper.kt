package ar.motorfar.app.ui

import ar.motorfar.app.data.AppSetting

object OnboardingHelper {
    fun shouldShowOnboarding(settings: Map<String, String>): Boolean =
        settings[AppSetting.SETTING_ONBOARDING_COMPLETE] != "true"
}
