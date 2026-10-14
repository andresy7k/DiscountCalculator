# DiscountCalculator — CI/CD con GitHub Actions, JaCoCo y SonarCloud

Calculadora de descuentos en Java 25 + Maven, con pipeline de integración continua
que compila, ejecuta pruebas, valida cobertura y publica el análisis en SonarCloud.

- Repositorio: https://github.com/andresy7k/DiscountCalculator
- Pipeline: pestaña **Actions** del repositorio (workflow *Java CI*)
- Reporte de calidad: https://sonarcloud.io/project/overview?id=andresy7k_DiscountCalculator

## Cómo funciona el pipeline (`.github/workflows/ci.yml`)

Se dispara con `push` a `main` y con `pull_request` hacia `main`, y ejecuta
estos pasos en orden sobre `ubuntu-latest`:

1. **Checkout** con `fetch-depth: 0` (historial completo: SonarCloud lo necesita
   para calcular *New Code* y culpar líneas correctamente).
2. **Set up Java**: Temurin 25 + Maven (ya vienen en el runner).
3. **Cache** de `~/.m2` para no descargar dependencias en cada run.
4. **Build project**: `mvn clean verify`
   - compila (`DiscountCalculator`, `InvalidPurchaseException`),
   - corre los 6 tests con Surefire,
   - JaCoCo genera el reporte y aplica el **gate de cobertura**: cada clase
     debe tener ≥ 95% de líneas cubiertas o el build falla.
5. **SonarCloud Scan**: `mvn sonar:sonar` con `SONAR_TOKEN` (secret).
   Reutiliza `target/classes` y `target/site/jacoco/jacoco.xml` del paso
   anterior y sube el análisis a SonarCloud, que decora el PR con su propio
   check (*SonarCloud Code Analysis*).

Si un paso falla, los siguientes **no se ejecutan**: el pipeline es secuencial.

## Por qué `sonar-maven-plugin` y no `sonarqube-scan-action`

El action genérico descarga el scanner CLI, que no entiende Maven: no conoce
el classpath ni lee el reporte de JaCoCo solo (la cobertura saldría 0%).
El plugin `org.sonarsource.scanner.maven:sonar-maven-plugin` corre **dentro**
del ciclo de vida de Maven y adjunta el reporte automáticamente. Es lo que la
documentación de SonarSource recomienda para proyectos Maven.

Las coordenadas del proyecto (`sonar.organization`, `sonar.projectKey`,
`sonar.host.url`) viven en el `<properties>` del `pom.xml` porque el scanner
de Maven **no lee** `sonar-project.properties` (ese archivo es exclusivo del
scanner CLI). La credencial (`SONAR_TOKEN`) vive como secret de GitHub,
nunca en el código.

## Cómo reproducirlo desde cero

1. Crear el proyecto en https://sonarcloud.io (org + project key) y apagar
   **Automatic Analysis** en *Administration → Analysis Method* (si queda
   encendido, el servidor rechaza los análisis subidos por CI).
2. Generar un *Project Analysis Token* en SonarCloud (*My Account → Security*)
   y guardarlo como secret del repo:
   `gh secret set SONAR_TOKEN --repo <owner>/<repo>` (pegar el valor en el
   prompt interactivo, nunca en la línea de comandos).
3. Abrir un PR hacia `main`: el check `build` debe ponerse verde y SonarCloud
   debe mostrar el análisis con cobertura.
4. Proteger `main` exigiendo el check `build` antes de mergear.

## Evidencia del flujo DevOps

- **PR #1** (*Configure SonarCloud analysis*): checks verdes en el PR,
  quality gate OK y cobertura 100% en SonarCloud → mergeado a `main`.
- **PR #2** (*Demo: pipeline failure*): cambio de regla de negocio que rompió
  un test → run rojo (`expected: <800.0> but was: <850.0>`) → fix → run verde.
  Cerrado sin mergear: su propósito era demostrar el ciclo rojo-verde.
- **Runs en `main`**: cada merge a `main` re-ejecuta el pipeline y actualiza
  el dashboard general de SonarCloud.
