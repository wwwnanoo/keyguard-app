package com.artemchep.keyguard.common.usecase.impl

import com.artemchep.keyguard.common.service.backup.BackupConfig
import com.artemchep.keyguard.common.service.backup.BackupConfigRepository
import com.artemchep.keyguard.common.usecase.GetLocalNetworkAccessHint
import com.artemchep.keyguard.common.usecase.mayAccessLocalNetwork
import com.artemchep.keyguard.core.store.bitwarden.ServiceToken
import com.artemchep.keyguard.provider.bitwarden.repository.ServiceTokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kodein.di.DirectDI
import org.kodein.di.instance

class GetLocalNetworkAccessHintImpl(
    private val tokensFlow: Flow<List<ServiceToken>>,
    private val backupConfigFlow: Flow<BackupConfig>,
) : GetLocalNetworkAccessHint {
    constructor(
        tokenRepository: ServiceTokenRepository,
        backupConfigRepository: BackupConfigRepository,
    ) : this(
        tokensFlow = tokenRepository.get(),
        backupConfigFlow = backupConfigRepository.getConfig(),
    )

    constructor(directDI: DirectDI) : this(
        tokenRepository = directDI.instance(),
        backupConfigRepository = directDI.instance(),
    )

    override fun invoke(): Flow<Boolean> = combine(
        tokensFlow,
        backupConfigFlow,
    ) { tokens, backupConfig ->
        tokens.any(ServiceToken::mayAccessLocalNetwork) ||
                backupConfig.mayAccessLocalNetwork()
    }.distinctUntilChanged()
}
