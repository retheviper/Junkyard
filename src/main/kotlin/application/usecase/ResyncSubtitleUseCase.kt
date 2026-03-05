package application.usecase

import application.processing.ProcessingContext
import domain.subtitle.ResyncSubtitleServiceStrategyFactory
import domain.subtitle.ResyncSubtitleType
import java.nio.file.Path
import kotlin.io.path.extension

class ResyncSubtitleUseCase(
    private val strategyFactory: ResyncSubtitleServiceStrategyFactory
) {
    suspend fun execute(basePath: Path, shiftMillis: Int, context: ProcessingContext) {
        val type = ResyncSubtitleType.fromString(basePath.extension)
        val strategy = strategyFactory.getStrategy(type)
        context.processWithCount {
            strategy.shiftSubtitle(basePath, shiftMillis)
        }
    }
}
