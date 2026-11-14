export function citySlug(city: string) {
  return city.toLowerCase().replaceAll(".", "").replaceAll(" ", "-");
}

export function cityPath(state: string, city: string) {
  return `/cities/${state.toLowerCase()}/${encodeURIComponent(citySlug(city))}`;
}
