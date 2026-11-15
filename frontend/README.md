# Outly — frontend

Next.js App Router · React · TypeScript · Tailwind CSS 4. Интерфейс — English.

**Основной README для жюри:** [продукт, презентация, скриншоты, сценарий демонстрации, архитектура и запуск](../README.md).

## Запуск

Node.js 24+ и npm. Сначала запустите Java/Spring backend и PostgreSQL по [общей инструкции](../README.md#локальный-запуск), затем из этой папки:

```sh
npm ci
API_PROXY_ORIGIN=http://localhost:8080 npm run dev
```

Откройте [http://localhost:3000](http://localhost:3000). `API_PROXY_ORIGIN` можно задать в `.env.local`; после изменения env перезапустите Next.js. При proxy оставьте `NEXT_PUBLIC_API_BASE_URL` пустым.

Без backend доступны локальный каталог и детали. Текущие Assistant, Pair, Buddy, Premium и Alerts используют Spring API; для AI-чата серверу нужен `GEMINI_API_KEY`. Старые встроенные demo routes сохраняются для совместимости и тестов, но не заменяют backend в текущем UI. Встроенный live-поиск SerpApi — отдельный режим с `SERPAPI_KEY`/`API_KEY`.

## Проверки

```sh
npm test
npm run typecheck
npm run build
```

Пути: `src/app` — страницы, `src/components` — UI, `src/types` — контракты, `src/lib/backend-api.ts` — гостевой клиент, `src/data/catalog.json` — проверенные события, `public/catalog` — локальные фото. Подробности источников: [расширение каталога](docs/catalog-expansion.md).

[Актуальная архитектура и ограничения MVP](../README.md#архитектура-и-код). Документ `docs/mvp2.md` содержит историю более раннего standalone-демо и не является текущим full-stack контрактом.
