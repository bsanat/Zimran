import assert from "node:assert/strict";
import test from "node:test";
import { getStateByCode, isSupportedCity, US_STATES } from "../src/lib/locations.ts";
import { US_STATE_MAP } from "../src/lib/us-map.ts";

test("map covers every directory state with a valid outline and visible label", () => {
  assert.deepEqual(Object.keys(US_STATE_MAP).sort(), US_STATES.map(state => state.code).sort());
  for (const { path, label, anchor } of Object.values(US_STATE_MAP)) {
    assert.match(path, /^M[\d.,LZM-]+Z$/);
    assert.ok(!path.includes("NaN"));
    for (const [x, y] of [label, ...(anchor ? [anchor] : [])]) {
      assert.ok(x >= -70 && x < 1130 && y > 0 && y < 625);
    }
  }
});

test("directory contains all 50 states with distinct codes in name order", () => {
  assert.equal(US_STATES.length, 50);
  assert.equal(new Set(US_STATES.map(state => state.code)).size, 50);
  assert.equal(new Set(US_STATES.map(state => state.name)).size, 50);
  assert.deepEqual(US_STATES.map(state => state.name), US_STATES.map(state => state.name).sort((a, b) => a.localeCompare(b, "en-US")));
  for (const state of US_STATES) {
    assert.match(state.code, /^[A-Z]{2}$/);
    assert.ok(state.cities.length > 0);
    assert.equal(new Set(state.cities).size, state.cities.length);
    assert.ok(state.cities.every(city => city.length > 0));
  }
});

test("city availability requires the supported city and its state", () => {
  assert.ok(getStateByCode("NY")?.cities.includes("New York"));
  assert.equal(getStateByCode("invalid"), undefined);
  assert.equal(isSupportedCity("New York", "NY"), true);
  assert.equal(isSupportedCity("New York", "NJ"), false);
  assert.equal(isSupportedCity("Buffalo", "NY"), false);
  for (const state of US_STATES) {
    for (const city of state.cities) assert.equal(isSupportedCity(city, state.code), city === "New York" && state.code === "NY");
  }
});
