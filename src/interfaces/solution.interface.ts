export interface RequestSolution {
  customerPath: string;
  latitude: number;
  longitude: number;
}

export interface TravelTime {
  hours: number;
  minutes: number;
}

export interface Leg {
  originStopName: string;
  originLat: number;
  originLon: number;
  departureTime: string;
  destinationStopName: string;
  destinationLat: number;
  destinationLon: number;
  arrivalTime: string;
  routeShortName: string;
  routeLongName: string;
  routeColor: string;
  headSign: string;
  travelTime: TravelTime;
}

export interface Proposal {
  departureTime: string;
  arrivalTime: string;
  travelTime: TravelTime;
  legs: Leg[];
  transferCount: number;
  multiDay: boolean;
}

export interface CustomerQuery {
  origin: string;
  destination: string;
  vias: string[];
  departureTime: string;
}

export interface SolutionResponse {
  customerQuery: CustomerQuery;
  proposals: Proposal[];
}
