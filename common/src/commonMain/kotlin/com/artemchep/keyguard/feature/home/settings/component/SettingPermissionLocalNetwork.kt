package com.artemchep.keyguard.feature.home.settings.component

import org.kodein.di.DirectDI

expect fun settingPermissionLocalNetworkProvider(
    directDI: DirectDI,
): SettingComponent
