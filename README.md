# PhotoList LATAM

Photograph an item and get its resale price on MercadoLibre.

## Building the project

### Prerequisites

- **Java 21** — install via [IntelliJ IDEA](https://www.jetbrains.com/idea/) (downloads JDK automatically) or [Eclipse Adoptium](https://adoptium.net/).
- **Maven** — no global installation needed. The repo includes `mvnw` / `mvnw.cmd` (Maven Wrapper), which downloads the correct Maven version automatically.

### Lambda functions

**Windows (PowerShell):**

```powershell
# Set JAVA_HOME if Java 21 is not in the system PATH
$env:JAVA_HOME = "C:\Users\<you>\.jdks\openjdk-21.0.2"

.\mvnw.cmd -f functions/analyze-photo/pom.xml package -q
.\mvnw.cmd -f functions/generate-upload-url/pom.xml package -q
```

Output JARs: `functions/<name>/target/<name>.jar`

**Linux / macOS:**

```bash
export JAVA_HOME=~/.jdks/openjdk-21.0.2   # if needed
chmod +x mvnw
./mvnw -f functions/analyze-photo/pom.xml package -q
./mvnw -f functions/generate-upload-url/pom.xml package -q
```

The first run downloads Maven 3.9.6 into `~/.m2/wrapper/dists/`; subsequent runs use the cache.

### Infrastructure (CDK)

```powershell
$env:JAVA_HOME = "C:\Users\<you>\.jdks\openjdk-21.0.2"
.\mvnw.cmd -f infrastructure/pom.xml package -q
npx cdk deploy
```

### Frontend

```bash
cd frontend
npm install
npm run build
```

## CI/CD

GitHub Actions runs `./mvnw -f <module>/pom.xml package -q` — Maven Wrapper is bootstrapped automatically on each CI run.
