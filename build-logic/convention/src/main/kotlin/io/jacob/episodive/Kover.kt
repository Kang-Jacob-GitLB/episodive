package io.jacob.episodive

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Kover(kotlinx-kover) 리포트 설정을 convention plugin에서 구성한다.
 *
 * 네이티브 Kover(IntelliJ) 엔진을 사용해 coroutine/inline 등 Kotlin 구문의
 * 정확한 커버리지를 측정한다. 아래 excludes 는 기존 Jacoco 의 제외 집합을
 * FQN 클래스명 패턴으로 옮긴 것이다. (Kover 필터는 파일 경로가 아닌
 * fully-qualified class name 기준 glob 만 지원 — `*` 는 0개 이상 임의 문자.)
 */
internal fun Project.configureKover() {
    extensions.configure<KoverProjectExtension> {
        reports {
            filters {
                includes {
                    classes("io.jacob.episodive.*")
                }
                excludes {
                    // Android
                    classes("*.R", "*.R\$*", "*.BuildConfig", "*.Manifest", "*.Manifest\$*")
                    // Hilt generated
                    classes(
                        "*_Hilt*", "*.Hilt_*", "*_HiltModules*",
                        "*_Provide*", "*Module_Provide*",
                        "*_Factory", "*_MembersInjector",
                    )
                    classes("*.hilt_aggregated_deps.*")
                    // Dagger generated
                    classes(
                        "*_Generated", "*.Dagger*",
                        "*Component\$Builder", "*Component\$*", "*Subcomponent*",
                    )
                    // Model
                    classes("*.model.*")
                    // DI
                    classes("*.di.*")
                    // Repository interfaces
                    classes("*.repository.*Repository")
                    // DataSource interfaces
                    classes("*.datasource.*DataSource")
                    // Network API
                    classes("*.network.api.*Api")
                    // Database generated
                    classes(
                        "*AutoMigration*Impl",
                        "*Dao_Impl", "*Dao_Impl\$*",
                        "*Database_Impl", "*Database_Impl\$*",
                    )
                    classes("*.database.migration.*")
                    // Download (Android system dependencies)
                    classes("*.download.*")
                    // Caption 네이티브/외부 SDK 어댑터 (JVM 테스트 불가)
                    classes("*.caption.sherpa.*", "*.caption.mlkit.*")
                    // Compose Screen/Bar
                    classes("*ScreenKt*", "*BarKt*")
                    // Navigation
                    classes("*.navigation.*")
                    // Route
                    classes("*Route", "*Route\$*", "*BaseRoute", "*BaseRoute\$*")
                    // Android framework
                    classes(
                        "*Activity", "*Activity\$*",
                        "*Service", "*Service\$*",
                        "*Application", "*Application\$*",
                    )
                    // Media service (CustomCommand)
                    classes("*.CustomCommand", "*.CustomCommand\$*")
                    // App shell
                    classes("*AppKt*", "*AppState*")
                    // designsystem (재사용 Compose UI 컴포넌트)
                    classes("*.designsystem.*")
                    // core:ui — 도메인 특화 Compose UI 컴포넌트. designsystem 과 동일 성격
                    // (분리 가능한 로직 없이 전부 @Composable). Compose UI는 커버리지 미집계 관례.
                    classes("*.core.ui.*")
                    // feature:widget — Glance @Composable UI + AppWidget/DI glue (designsystem·*Service 류와 동일)
                    classes("*.feature.widget.component.*", "*.feature.widget.theme.*")
                    classes("*.feature.widget.dispatcher.*")
                    classes("*.feature.widget.EpisodiveWidget", "*.feature.widget.EpisodiveWidget\$*")
                    classes("*.feature.widget.EpisodiveWidgetReceiver")
                }
            }
        }
    }
}
