export interface USState {
  code: string;
  name: string;
  cities: string[];
}

// shortcut: Cities are an initial major-city selection; replace with the location API when nationwide discovery is available.
export const US_STATES: USState[] = [
  { code: "AL", name: "Alabama", cities: ["Birmingham", "Huntsville", "Mobile", "Montgomery"] },
  { code: "AK", name: "Alaska", cities: ["Anchorage", "Fairbanks", "Juneau"] },
  { code: "AZ", name: "Arizona", cities: ["Phoenix", "Scottsdale", "Tempe", "Tucson"] },
  { code: "AR", name: "Arkansas", cities: ["Fayetteville", "Fort Smith", "Little Rock"] },
  { code: "CA", name: "California", cities: ["Los Angeles", "Sacramento", "San Diego", "San Francisco", "San Jose"] },
  { code: "CO", name: "Colorado", cities: ["Boulder", "Colorado Springs", "Denver", "Fort Collins"] },
  { code: "CT", name: "Connecticut", cities: ["Bridgeport", "Hartford", "New Haven", "Stamford"] },
  { code: "DE", name: "Delaware", cities: ["Dover", "Newark", "Wilmington"] },
  { code: "FL", name: "Florida", cities: ["Jacksonville", "Miami", "Orlando", "Tampa"] },
  { code: "GA", name: "Georgia", cities: ["Athens", "Atlanta", "Augusta", "Savannah"] },
  { code: "HI", name: "Hawaii", cities: ["Hilo", "Honolulu", "Kailua"] },
  { code: "ID", name: "Idaho", cities: ["Boise", "Coeur d’Alene", "Idaho Falls"] },
  { code: "IL", name: "Illinois", cities: ["Chicago", "Naperville", "Peoria", "Springfield"] },
  { code: "IN", name: "Indiana", cities: ["Bloomington", "Fort Wayne", "Indianapolis", "South Bend"] },
  { code: "IA", name: "Iowa", cities: ["Cedar Rapids", "Davenport", "Des Moines", "Iowa City"] },
  { code: "KS", name: "Kansas", cities: ["Kansas City", "Lawrence", "Topeka", "Wichita"] },
  { code: "KY", name: "Kentucky", cities: ["Bowling Green", "Lexington", "Louisville"] },
  { code: "LA", name: "Louisiana", cities: ["Baton Rouge", "Lafayette", "New Orleans", "Shreveport"] },
  { code: "ME", name: "Maine", cities: ["Augusta", "Bangor", "Portland"] },
  { code: "MD", name: "Maryland", cities: ["Annapolis", "Baltimore", "Frederick"] },
  { code: "MA", name: "Massachusetts", cities: ["Boston", "Cambridge", "Springfield", "Worcester"] },
  { code: "MI", name: "Michigan", cities: ["Ann Arbor", "Detroit", "Grand Rapids", "Lansing"] },
  { code: "MN", name: "Minnesota", cities: ["Duluth", "Minneapolis", "Rochester", "Saint Paul"] },
  { code: "MS", name: "Mississippi", cities: ["Biloxi", "Gulfport", "Jackson", "Oxford"] },
  { code: "MO", name: "Missouri", cities: ["Columbia", "Kansas City", "Springfield", "St. Louis"] },
  { code: "MT", name: "Montana", cities: ["Billings", "Bozeman", "Helena", "Missoula"] },
  { code: "NE", name: "Nebraska", cities: ["Bellevue", "Grand Island", "Lincoln", "Omaha"] },
  { code: "NV", name: "Nevada", cities: ["Carson City", "Henderson", "Las Vegas", "Reno"] },
  { code: "NH", name: "New Hampshire", cities: ["Concord", "Manchester", "Nashua", "Portsmouth"] },
  { code: "NJ", name: "New Jersey", cities: ["Atlantic City", "Jersey City", "Newark", "Trenton"] },
  { code: "NM", name: "New Mexico", cities: ["Albuquerque", "Las Cruces", "Santa Fe"] },
  { code: "NY", name: "New York", cities: ["New York", "Albany", "Buffalo", "Rochester", "Syracuse"] },
  { code: "NC", name: "North Carolina", cities: ["Asheville", "Charlotte", "Durham", "Raleigh"] },
  { code: "ND", name: "North Dakota", cities: ["Bismarck", "Fargo", "Grand Forks"] },
  { code: "OH", name: "Ohio", cities: ["Akron", "Cincinnati", "Cleveland", "Columbus"] },
  { code: "OK", name: "Oklahoma", cities: ["Norman", "Oklahoma City", "Tulsa"] },
  { code: "OR", name: "Oregon", cities: ["Bend", "Eugene", "Portland", "Salem"] },
  { code: "PA", name: "Pennsylvania", cities: ["Allentown", "Harrisburg", "Philadelphia", "Pittsburgh"] },
  { code: "RI", name: "Rhode Island", cities: ["Cranston", "Newport", "Providence", "Warwick"] },
  { code: "SC", name: "South Carolina", cities: ["Charleston", "Columbia", "Greenville", "Myrtle Beach"] },
  { code: "SD", name: "South Dakota", cities: ["Aberdeen", "Pierre", "Rapid City", "Sioux Falls"] },
  { code: "TN", name: "Tennessee", cities: ["Chattanooga", "Knoxville", "Memphis", "Nashville"] },
  { code: "TX", name: "Texas", cities: ["Austin", "Dallas", "Fort Worth", "Houston", "San Antonio"] },
  { code: "UT", name: "Utah", cities: ["Ogden", "Park City", "Provo", "Salt Lake City"] },
  { code: "VT", name: "Vermont", cities: ["Burlington", "Montpelier", "Rutland"] },
  { code: "VA", name: "Virginia", cities: ["Alexandria", "Norfolk", "Richmond", "Virginia Beach"] },
  { code: "WA", name: "Washington", cities: ["Bellevue", "Olympia", "Seattle", "Spokane", "Tacoma"] },
  { code: "WV", name: "West Virginia", cities: ["Charleston", "Huntington", "Morgantown"] },
  { code: "WI", name: "Wisconsin", cities: ["Green Bay", "Madison", "Milwaukee"] },
  { code: "WY", name: "Wyoming", cities: ["Casper", "Cheyenne", "Laramie"] },
];

export function getStateByCode(code: string): USState | undefined {
  return US_STATES.find(state => state.code === code);
}

export function isSupportedCity(city: string, stateCode: string): boolean {
  return stateCode === "NY" && city === "New York";
}
