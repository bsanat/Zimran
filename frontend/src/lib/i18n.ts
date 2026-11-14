import { eventDetailsMessages } from "./i18n-event-details.ts";
import { cityEventsMessages } from "./i18n-city-events.ts";
import { cityMessages } from "./i18n-city.ts";
import { homeMessages } from "./i18n-home.ts";
import { discoveryMessages } from "./i18n-discovery.ts";
import { plannerMessages } from "./i18n-planner.ts";
import { membershipMessages } from "./i18n-membership.ts";
import { socialMessages } from "./i18n-social.ts";
import { assistantMessages } from "./i18n-assistant.ts";

export const languages = { ru: "Русский", en: "English", kk: "Қазақша" } as const;
export type Locale = keyof typeof languages;
export const languageCookie = "outly-lang";
export function isLocale(value: unknown): value is Locale {
  return typeof value === "string" && Object.hasOwn(languages, value);
}

const messages: Readonly<Record<string, readonly [string, string]>> = {
  ...cityMessages, ...cityEventsMessages, ...eventDetailsMessages,
  "Close details": ["Закрыть подробности", "Мәліметтерді жабу"],
  "Previous event": ["Предыдущее событие", "Алдыңғы іс-шара"],
  "Next event": ["Следующее событие", "Келесі іс-шара"],
  "Show details": ["Подробнее", "Толығырақ"],
  ...homeMessages, ...discoveryMessages, ...plannerMessages, ...membershipMessages, ...socialMessages, ...assistantMessages,
  "Outly USA home": ["Outly USA — главная", "Outly USA — басты бет"],
  "Main navigation": ["Основная навигация", "Негізгі навигация"],
  "Skip to content": ["Перейти к содержанию", "Негізгі мазмұнға өту"],
  "Categories": ["Категории", "Санаттар"],
  "Search events and experiences": ["Поиск событий и развлечений", "Іс-шаралар мен ойын-сауықты іздеу"],
  "Search events": ["Найти события", "Іс-шараларды іздеу"],
  "What do you want to do?": ["Чем хотите заняться?", "Немен айналысқыңыз келеді?"],
  "Plan My Day": ["Спланировать день", "Күнді жоспарлау"],
  "Interface language": ["Язык интерфейса", "Интерфейс тілі"],
  "Finding your next good thing…": ["Ищем события для вас…", "Сізге лайық іс-шараларды іздеп жатырмыз…"],
  "A little pause in the plans": ["Не удалось загрузить данные", "Деректер жүктелмеді"],
  "Try again": ["Попробовать ещё раз", "Қайталап көру"],
  "Back to your preferences": ["Вернуться к настройкам плана", "Жоспар баптауларына оралу"],
  "Let’s try a different kind of day": ["Подберём другой вариант", "Басқа нұсқаны қарастырайық"],
  "No matching events this time. Try another date, a wider budget, or a few more interests.": ["Подходящих событий пока нет. Попробуйте изменить дату, увеличить бюджет или выбрать другие интересы.", "Сәйкес іс-шара табылмады. Күнді немесе бюджетті өзгертіп, басқа қызығушылықтарды таңдап көріңіз."],
  "Edit preferences": ["Изменить настройки", "Баптауларды өзгерту"],
  "This one wandered off": ["Страница не найдена", "Бет табылмады"],
  "We couldn’t find that page. There’s still plenty to explore.": ["Такой страницы нет. Другие события можно найти в каталоге.", "Бұл бет табылмады. Басқа іс-шараларды каталогтан қарай аласыз."],
  "We couldn’t open this page. Please try again.": ["Не удалось открыть страницу. Попробуйте ещё раз.", "Бет ашылмады. Қайталап көріңіз."],
  "Explore events": ["Посмотреть события", "Іс-шараларды көру"],
  "The server returned an incomplete response. Please try again.": ["Сервис вернул не все данные. Попробуйте ещё раз.", "Сервис толық жауап бермеді. Қайталап көріңіз."],
  "Please check your preferences and try again.": ["Проверьте настройки и попробуйте ещё раз.", "Баптауларды тексеріп, қайталап көріңіз."],
  "This event could not be found.": ["Событие не найдено.", "Іс-шара табылмады."],
  "Too many requests. Please wait a moment before trying again.": ["Слишком много запросов. Немного подождите и попробуйте снова.", "Сұрау тым көп. Сәл күтіп, қайталап көріңіз."],
  "The event service is temporarily unavailable. Please try again.": ["Каталог событий временно недоступен. Попробуйте позже.", "Іс-шаралар каталогы уақытша қолжетімсіз. Кейінірек көріңіз."],
  "We could not complete your request. Please try again.": ["Не удалось выполнить запрос. Попробуйте ещё раз.", "Сұрауды орындау мүмкін болмады. Қайталап көріңіз."],
  "The event service is not available right now. Please try again.": ["Каталог событий сейчас недоступен. Попробуйте позже.", "Іс-шаралар каталогы қазір қолжетімсіз. Кейінірек көріңіз."],
  "The request took too long. Your preferences are saved; please try again.": ["Время ожидания истекло. Ваши настройки сохранены — попробуйте ещё раз.", "Күту уақыты аяқталды. Баптауларыңыз сақталды. Қайталап көріңіз."],
  "We could not reach the event service. Check your connection and try again.": ["Не удалось подключиться к каталогу событий. Проверьте подключение и попробуйте снова.", "Іс-шаралар каталогына қосылу мүмкін болмады. Интернет байланысын тексеріп, қайталап көріңіз."],
  "Outly USA — Your city. Your kind of day.": ["Outly USA — события вашего города", "Outly USA — қалаңыздағы іс-шаралар"],
  "Discover your kind of New York. Choose your interests, date, and budget for personal recommendations from real events. No account needed.": ["Найдите интересные события в Нью-Йорке. Укажите интересы, дату и бюджет, чтобы получить подборку без регистрации.", "Нью-Йорктегі іс-шараларды табыңыз. Қызығушылықтарыңызды, күнді және бюджетті таңдап, тіркелусіз жеке ұсыныстар алыңыз."],
  "Explore events by city": ["События по городам", "Қалалар бойынша іс-шаралар"],
  "Explore event categories, states, and cities. Discover real events in our New York pilot.": ["Выберите штат, город и категорию. Сейчас доступны события Нью-Йорка.", "Штатты, қаланы және санатты таңдаңыз. Қазір Нью-Йорктегі іс-шаралар қолжетімді."],
  "Plan your day": ["Спланировать день", "Күнді жоспарлау"],
  "Choose your New York interests, dates, and budget. Get personal event recommendations without an account.": ["Выберите интересы, даты и бюджет, чтобы составить план на день в Нью-Йорке без регистрации.", "Нью-Йоркте күніңізді жоспарлау үшін қызығушылықтарыңызды, күнді және бюджетті таңдаңыз. Тіркелудің қажеті жоқ."],
  "Your personal picks": ["Ваша подборка", "Сізге арналған таңдау"],
  "Your personal collection of real New York events, with reasons they fit your interests.": ["События Нью-Йорка, подобранные по вашим интересам, с пояснениями к каждой рекомендации.", "Қызығушылықтарыңызға сай Нью-Йорктегі іс-шаралар және әр ұсыныстың қысқаша себебі."],
  "Event details": ["О событии", "Іс-шара туралы"],
  "Event time, venue, prices, and details from the original event source.": ["Время, место, стоимость и подробности с сайта организатора.", "Уақыты, өтетін орны, бағасы және бастапқы дереккөздегі мәліметтер."],
};

export function translate(locale: Locale, source: string): string {
  return locale === "en" || !Object.hasOwn(messages, source) ? source : messages[source][locale === "ru" ? 0 : 1];
}
