# Outly USA Frontend

- Next.js App Router, TypeScript, React, Tailwind CSS 4. UI на русском (по умолчанию), английском и казахском, документация на русском.
- Команды: `npm install`, `npm run dev`, `npm run build`, `npm run typecheck`, `npm test` (Node.js 24+ для встроенного запуска TS-тестов).
- Страницы в `src/app`, UI в `src/components`, состояние планировщика в `src/hooks/usePlanner.ts`.
- Общий контракт API: `src/types`. HTTP-клиент: `src/lib/api.ts`. Правила дат, цены и валидации: `src/lib/planner.ts`.
- Проверенный каталог: `src/data/catalog.json`, единые фильтры/география — `src/lib/catalog.ts`, фото — `public/catalog`. Добавляйте только проверенные реальные записи с официальной ссылкой, источником фото и `lastVerifiedAt`; demo-записи не используются.
- Frontend не генерирует события. Встроенный серверный SerpApi-адаптер: `src/lib/server/serpapi.ts`, маршруты — `src/app/api`; ключ — `SERPAPI_KEY` или `API_KEY`. Внешний Backend (с Google Gemini для AI) подключается через `API_PROXY_ORIGIN` с приоритетом над встроенным `/api`.
- Planner использует America/New_York и POST /api/plan с `startDate`/`endDate` включительно. Даты каталога и детали относятся к timezone события; учитывайте occurrenceDates, availableDateRanges, excludedDates и ограничения регулярных программ. Не придумывайте время для date-only записей.
- Неизвестная цена не равна бесплатной. Не храните API-секреты в NEXT_PUBLIC_*.
- P2 Auth отложен; гостевые страницы не требуют входа. Не имитируйте авторизацию.
- Не публикуйте, не пушьте и не разворачивайте проект без разрешения пользователя.

<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->
