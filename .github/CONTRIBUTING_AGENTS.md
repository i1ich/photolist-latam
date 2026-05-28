# Как вносить изменения в photolist-latam (для агентов)

Репо защищено branch protection rules. Прямой пуш в `main` запрещён.
Любое изменение должно пройти через следующий флоу.

## Обязательный флоу

### 1. Создать ветку от main

```bash
git checkout main
git pull origin main
git checkout -b <имя-ветки>
```

Имена веток — по соглашению:
- `feat/<что-делаем>` — новая функциональность
- - `fix/<что-фиксим>` — баг-фикс
  - - `chore/<что-делаем>` — рутинные задачи (deps, config, etc.)
   
    - ### 2. Сделать изменения и запушить ветку
   
    - ```bash
      git add .
      git commit -m "feat: описание изменений"
      git push origin <имя-ветки>
      ```

      ### 3. Открыть Pull Request в main

      ```bash
      gh pr create --base main --title "feat: описание" --body "Что сделано и зачем"
      ```

      Или через GitHub UI: после пуша ветки появится кнопка "Compare & pull request".

      ### 4. Дождаться прохождения CI

      PR заблокирован до тех пор, пока не пройдут оба GitHub Actions джоба:

      | Job | Что проверяет |
      |---|---|
      | `build-functions` | Сборка Java Lambda-функций (Maven) |
      | `build-frontend` | Сборка React фронтенда (Node 20) |

      Если CI падает — смотри логи в Actions и фиксь до аппрува.

      ### 5. Запросить ревью от @i1ich

      @i1ich назначен code owner в `.github/CODEOWNERS`.
      Его аппрув обязателен — без него мерж невозможен даже при зелёном CI.

      ```bash
      gh pr edit <номер> --add-reviewer i1ich
      ```

      ### 6. Мерж после аппрува

      После аппрува от @i1ich и зелёного CI — мерж открыт.
      Предпочтительный способ мержа: **Squash and merge** (чистая история в main).

      ---

      ## Структура репо

      ```
      photolist-latam/
      ├── .github/
      │   ├── CODEOWNERS          # @i1ich — владелец всего кода
      │   ├── workflows/
      │   │   ├── ci.yml          # CI: build-functions + build-frontend
      │   │   └── deploy-dev.yml  # CD: деплой в dev при пуше в main
      │   └── ISSUE_TEMPLATE/
      ├── frontend/               # React приложение
      ├── functions/
      │   ├── analyze-photo/      # Java Lambda (Maven, Java 21)
      │   └── generate-upload-url/ # Java Lambda (Maven, Java 21)
      ├── infrastructure/         # AWS CDK (Java)
      └── docs/
      ```

      ## Важные ограничения

      - **Нельзя пушить в main напрямую** — GitHub заблокирует
      - - **Нельзя мержить без CI** — оба джоба должны быть зелёными
        - - **Нельзя мержить без аппрува @i1ich** — он code owner
          - - **Force push запрещён** на main
           
            - ## Быстрый старт для агента
           
            - ```bash
              # 1. Убедиться что на main и он актуален
              git checkout main && git pull origin main

              # 2. Создать ветку для своей задачи
              git checkout -b feat/my-task

              # 3. ... сделать изменения ...

              # 4. Закоммитить и запушить
              git add . && git commit -m "feat: my task" && git push origin feat/my-task

              # 5. Создать PR
              gh pr create --base main --title "feat: my task" --body "Description"
              ```
