# Задание для агента: Инициализация GitHub репозитория `photolist-latam`

> **Контекст проекта:** PhotoList LATAM — приложение для идентификации вещей и поиска рыночных цен на MercadoLibre. Пользователь фотографирует предмет → получает название + диапазон цен на вторичном рынке LATAM.

---

## Что нужно сделать

Инициализировать публичный GitHub репозиторий с полной структурой проекта, готовой к разработке. Владелец: `ilidoin@gmail.com`.

---

## Параметры репозитория

| Параметр | Значение |
|----------|---------|
| Repo name | `photolist-latam` |
| Visibility | Public |
| Description | `📷 Photo → item ID + MercadoLibre resale price. LATAM-first. AWS Lambda + Vision LLM.` |
| Default branch | `main` |
| License | MIT |
| .gitignore | Java (+ дополнить вручную) |

---

## Структура файлов для создания

```
photolist-latam/
│
├── README.md                          # (см. ниже)
├── LICENSE                            # MIT
├── .gitignore                         # Java + Node + CDK + secrets
├── .github/
│   ├── workflows/
│   │   ├── ci.yml                     # Build + test on push
│   │   └── deploy-dev.yml             # Deploy to AWS dev on merge to main
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       └── feature_request.md
│
├── infrastructure/                    # AWS CDK (Java)
│   ├── pom.xml
│   └── src/main/java/com/photolist/
│       ├── PhotolistApp.java
│       ├── PhotolistStorageStack.java
│       └── PhotolistApiStack.java
│
├── functions/
│   ├── generate-upload-url/           # Lambda 1
│   │   ├── pom.xml
│   │   └── src/main/java/com/photolist/
│   │       └── GenerateUploadUrlHandler.java
│   └── analyze-photo/                 # Lambda 2
│       ├── pom.xml
│       └── src/main/java/com/photolist/
│           ├── AnalyzePhotoHandler.java
│           ├── service/
│           │   ├── VisionService.java
│           │   └── MercadoLibreService.java
│           └── model/
│               ├── AnalyzeRequest.java
│               └── AnalyzeResponse.java
│
├── frontend/                          # React PWA (Vite)
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── App.tsx
│       ├── components/
│       │   ├── PhotoUploader.tsx
│       │   └── ResultCard.tsx
│       └── api/
│           └── photolist.ts
│
└── docs/
    ├── aws-architecture.md            # Копия архитектурного скетча
    └── api-contract.md               # REST API контракт
```

---

## Содержимое ключевых файлов

### `README.md`

```markdown
# 📷 PhotoList LATAM

> Сфотографируй вещь — узнай цену перепродажи на MercadoLibre.

**Photo → Item Identification + LATAM Resale Price**

Built for the Latin American secondhand market. Integrates with MercadoLibre (🇦🇷🇧🇷🇲🇽🇺🇾) for real local pricing.

## Stack

- **Backend:** Java 21 + Spring Cloud Function → AWS Lambda
- **AI:** OpenAI GPT-4o Vision / Anthropic Claude (image input)
- **Marketplace:** MercadoLibre Search API (public)
- **Infrastructure:** AWS CDK (S3, Lambda, API Gateway, DynamoDB, CloudFront)
- **Frontend:** React + Vite PWA

## Architecture

```
Photo Upload → S3 → Lambda → Vision LLM → MercadoLibre API → Price Response
```

See [docs/aws-architecture.md](docs/aws-architecture.md) for full diagram.

## Quick Start

```bash
# Infrastructure
cd infrastructure && mvn package
cdk deploy --all

# Analyze function
cd functions/analyze-photo && mvn package
```

## API

`POST /analyze` — returns item name, category, and top MercadoLibre listings with prices.

See [docs/api-contract.md](docs/api-contract.md).

## Status

🚧 MVP in progress — Week 1

## License

MIT
```

---

### `.gitignore` (дополнить стандартный Java)

```
# Java / Maven
target/
*.class
*.jar
!*-sources.jar

# CDK
cdk.out/
cdk.context.json

# Node / Frontend
node_modules/
dist/
.env
.env.local

# AWS / Secrets
.aws/
*.pem
credentials

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db
```

---

### `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-functions:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'corretto'
      - name: Build analyze-photo
        run: cd functions/analyze-photo && mvn package -q
      - name: Build generate-upload-url
        run: cd functions/generate-upload-url && mvn package -q

  build-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: cd frontend && npm ci && npm run build
```

---

### `functions/analyze-photo/src/main/java/.../AnalyzePhotoHandler.java` (скелет)

```java
package com.photolist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.photolist.model.AnalyzeRequest;
import com.photolist.model.AnalyzeResponse;
import com.photolist.service.VisionService;
import com.photolist.service.MercadoLibreService;

public class AnalyzePhotoHandler implements RequestHandler<AnalyzeRequest, AnalyzeResponse> {

    private final VisionService visionService = new VisionService();
    private final MercadoLibreService mlService = new MercadoLibreService();

    @Override
    public AnalyzeResponse handleRequest(AnalyzeRequest request, Context context) {
        // 1. Get image from S3
        // 2. Call Vision LLM → item identification
        // 3. Call MercadoLibre Search API → prices
        // 4. Cache result in DynamoDB
        // 5. Return response
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

---

## Первые коммиты (порядок)

1. **Initial commit** — только `README.md`, `.gitignore`, `LICENSE`
2. **Add project structure** — пустые папки с `.gitkeep`, `pom.xml` скелеты
3. **Add Lambda skeletons** — Java скелеты обоих Lambda-функций
4. **Add CDK stacks** — инфраструктурный код
5. **Add frontend scaffold** — `npm create vite@latest frontend -- --template react-ts`
6. **Add CI workflow** — `.github/workflows/ci.yml`

---

## Labels для GitHub Issues (создать)

| Label | Color | Описание |
|-------|-------|---------|
| `mvp` | `#0075ca` | Core MVP scope |
| `spike` | `#e4e669` | Research/exploration task |
| `aws` | `#FF9900` | Infrastructure |
| `ai` | `#d93f0b` | Vision LLM integration |
| `mercadolibre` | `#FFE600` | MercadoLibre API |
| `frontend` | `#0e8a16` | React PWA |

---

## Issues для создания (Day 1 backlog)

1. **[spike] MercadoLibre Search API — confirm rate limits and response shape**
   - Test `GET /sites/MLA/search?q=iphone+13` 
   - Document: auth requirements, rate limits, pagination, listing fields
   
2. **[spike] Vision LLM evaluation — test 5 photos**
   - Test GPT-4o mini vs GPT-4o vs Claude Haiku on 5 different objects
   - Measure: accuracy of item name, category detection, confidence
   - Decision: which model for MVP

3. **[aws] CDK stack — S3 buckets + DynamoDB**
   - `PhotolistStorageStack`: photo-uploads bucket (24h lifecycle) + results-cache table

4. **[aws] Lambda: generate-upload-url**
   - Pre-signed S3 URL generation, 5 min expiry

5. **[aws] Lambda: analyze-photo — core orchestration**
   - Vision LLM call → MercadoLibre search → DynamoDB cache → response

6. **[frontend] PWA scaffold — photo upload + result display**
   - Camera/file input → upload to S3 via pre-signed URL → call /analyze → show ResultCard

---

## Технические решения (уже принятые)

- **Backend runtime:** Java 21 (AWS Corretto) — соответствует основному стеку
- **Lambda framework:** Spring Cloud Function (или AWS Lambda Java runtime напрямую для скорости)
- **IaC:** AWS CDK v2 (Java)
- **Region:** `sa-east-1` (São Paulo) — минимальная задержка для LATAM
- **Vision LLM:** OpenAI GPT-4o mini (MVP) — дешевле, достаточно точен
- **MercadoLibre:** начинаем с MLA (Argentina), расширяем позже

---

*Создано: 2026-05-28*  
*На основе: `aws-architecture-sketch.md`, `Analytics -- proto.md`, `mvp-4-photolist.md`*
