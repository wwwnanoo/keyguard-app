package com.artemchep.keyguard.desktop.instance

import com.artemchep.keyguard.util.instance.InstanceConfig
import com.artemchep.keyguard.util.instance.InstanceCoordinator
import com.artemchep.keyguard.util.instance.InstanceResult
import java.nio.file.Files

/** Exercises the packaged library and IPC without opening the user's vault or instance lock. */
internal fun verifyPackagedInstanceService() {
    val directory = Files.createTempDirectory("keyguard-instance-smoke-").toRealPath()
    try {
        val configuration = InstanceConfig(
            coordinationDirectory = directory.resolve("coordination").toString(),
            runtimeDirectory = instanceRuntimeDirectory().toString(),
            identity = "keyguard-package-smoke",
        )
        val first = InstanceCoordinator.acquireOrActivate(configuration)
        check(first is InstanceResult.Primary)
        first.instance.use { primary ->
            check(InstanceCoordinator.acquireOrActivate(configuration) == InstanceResult.Activated)
            check(primary.awaitActivation())
            primary.stop()
            check(!primary.awaitActivation())
        }
    } finally {
        directory.toFile().deleteRecursively()
    }
}
