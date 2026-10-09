package application.processing

/** Records what a use case reports so that tests can assert on counts and errors. */
class RecordingProcessingContext {
    var total = 0
        private set
    var current = 0
        private set
    var processed = 0
        private set
    var failed = 0
        private set
    val errors = mutableListOf<Throwable>()

    fun toProcessingContext() = ProcessingContext(
        setTotalFn = { total = it },
        updateCurrentFileFn = {},
        processWithCountFn = { block ->
            runCatching(block)
                .onSuccess { processed++ }
                .onFailure {
                    failed++
                    errors += it
                }
            current++
        },
        incrementCurrentFn = { current++ },
        incrementProcessedFn = { processed++ },
        incrementFailedFn = { failed++ },
        printErrorFn = { errors += it },
        yieldFn = {}
    )
}
