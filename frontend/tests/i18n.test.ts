import assert from "node:assert/strict";
import test from "node:test";
import { isLocale, translate } from "../src/lib/i18n.ts";

test("only the three supported locale codes are accepted from a cookie or picker", () => {
  for (const code of ["ru", "en", "kk"]) assert.equal(isLocale(code), true);
  for (const value of ["kz", "constructor", "__proto__", "", null, undefined]) assert.equal(isLocale(value), false);
});

test("UI copy follows the language while unknown source strings stay intact", () => {
  const source = "Search events";
  assert.equal(translate("en", source), source);
  assert.equal(translate("ru", source), "Найти события");
  assert.equal(translate("kk", source), "Іс-шараларды іздеу");
  for (const locale of ["ru", "en", "kk"] as const) {
    assert.equal(translate(locale, "An evening with the City Quartet"), "An evening with the City Quartet");
    assert.equal(translate(locale, "constructor"), "constructor");
  }
});
