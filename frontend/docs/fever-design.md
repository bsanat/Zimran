# Дизайн по референсу Fever London

Референс: [Fever London](https://feverup.com/en/london). Проверка выполнена 14 ноября 2026 года по странице, её [основным стилям](https://feverup.com/_ng/styles-NZ25YI7P.css) и значениям computed styles в браузере.

## Типографика и цвета

| Элемент | Подтверждённое значение |
| --- | --- |
| Семейство | Montserrat, sans-serif |
| Основной текст | 16 px, вес 400, line-height 1.4 |
| Заголовок раздела | 24 px, вес 600 |
| Основной текстовый цвет | `#031419` |
| Вторичный текст | `#536b75` |
| Фон и вторичная поверхность | `#ffffff`, `#f2f3f3` |
| Основной синий акцент | `#0079ca`, hover `#005795` |
| Тёмный цвет | `#06232c` |
| Граница | `#ccd2d8` |
| Кнопки | Вес 600, скругление 100 px |
| Поля и изображения slideshow | Скругление 8 px |

## Композиция

Белая шапка объединяет город, категории и поиск. Большой фотоблок представляет город, затем идут чипы интересов и подборки событий. Карточки отдают большую часть площади изображению; ниже расположены название, место, даты и цена. На странице есть группы Top 10, Originals, Live Shows, Music и Family.

В Outly эти приёмы применены к главной, шапке с поиском, общим кнопкам, фильтрам, категориям, карточкам, Planner и деталям. Сохранено разделение «штат → город». Рейтинги, цены и события приходят из нашего API; фотографии обзорных карточек представляют категории и не имитируют мероприятия.

## Шрифт в проекте

`public/fonts/montserrat-latin-variable.woff2` — Montserrat Latin variable 100–900 из [Google Fonts CSS](https://fonts.googleapis.com/css2?family=Montserrat:wght@100..900&display=swap). [Лицензия SIL OFL 1.1](https://github.com/google/fonts/blob/main/ofl/montserrat/OFL.txt) сохранена в `public/fonts/OFL-Montserrat.txt`. Для русского и казахского добавлены локальные наборы Cyrillic и Cyrillic Extended той же версии Montserrat. В браузере шрифт загружается с сайта Outly.

## Фотографии категорий

Для ссылок Live music, Arts & Culture и Food & Drinks используются иллюстрации категорий, а не фотографии конкретных событий:

- `public/photos/music.jpg` — [диджейский пульт, Unsplash](https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80).
- `public/photos/art.jpg` — [абстрактная живопись, Unsplash](https://images.unsplash.com/photo-1561214115-f2f134cc4912?auto=format&fit=crop&w=800&q=80).
- `public/photos/food.jpg` — [интерьер ресторана, Unsplash](https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80).

Эти изображения не подтверждают наличие мероприятия, его место, цену или рейтинг.

Панорамный баннер `public/photos/manhattan-panorama.webp`: [New York and Jersey City Skyline Panorama](https://commons.wikimedia.org/wiki/File:New_York_and_Jersey_City_Skyline_Panorama.jpg), Pierre Blaché, съёмка с Liberty Island 26 октября 2019 года, [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). Использована версия Wikimedia Commons 3840×960, преобразованная из JPEG в WebP без кадрирования; это вид города, а не конкретное мероприятие.
