// Schema examples and common patterns for CLDF

export const SCHEMA_EXAMPLES = {
  minimalValid: {
    description: "Minimal valid CLDF structure with just locations",
    data: {
      manifest: {
        version: "1.0.0",
        format: "CLDF",
        platform: "Desktop",
        appVersion: "1.0.0",
        creationDate: "2024-01-29T12:00:00Z"
      },
      locations: [
        {
          id: 1,  // MUST be integer, not string
          name: "Example Crag",
          country: "USA",
          isIndoor: false,
          coordinates: {
            latitude: 40.0,
            longitude: -105.0
          }
        }
      ],
      checksums: {
        algorithm: "SHA-256"
      }
    }
  },
  
  withRoutes: {
    description: "CLDF with locations and routes",
    data: {
      manifest: {
        version: "1.0.0",
        format: "CLDF",
        platform: "Desktop",
        appVersion: "1.0.0",
        creationDate: "2024-01-29T12:00:00Z"
      },
      locations: [
        {
          id: 1,
          name: "Example Gym",
          country: "USA",
          isIndoor: true,
          city: "Boulder",
          address: "123 Main St"
        }
      ],
      routes: [
        {
          id: 1,  // MUST be integer
          locationId: 1,  // MUST be integer matching location.id
          name: "Crimpy Delight",
          routeType: "boulder",  // enum: "boulder" or "route"
          grades: {
            vScale: "V4",  // Pattern: V(B|[0-9]{1,2})
            // french: "6a+"  // Pattern: [3-9][abc][+]?
          },
          color: "#FF0000",  // Hex color for indoor routes
          qualityRating: 4  // 0-5 integer
          // belayType is NOT valid for routes - only for climbs!
        }
      ],
      checksums: {
        algorithm: "SHA-256"
      }
    }
  },
  
  withClimbs: {
    description: "CLDF with climbing session data",
    data: {
      // ... manifest and locations ...
      climbs: [
        {
          id: 1,
          routeId: "1",  // Can be string for climbs
          routeName: "Crimpy Delight",
          date: "2024-01-29",  // ISO date
          type: "boulder",
          finishType: "top",  // enum: top, fall, etc.
          attempts: 1,
          grades: {
            grade: "V4",
            system: "vScale"
          },
          belayType: "topRope"  // This IS valid for climbs
        }
      ]
    }
  }
};

export const COMMON_MISTAKES = {
  wrongIdTypes: {
    issue: "Using string IDs for locations/routes",
    wrong: { id: "1", locationId: "1" },
    correct: { id: 1, locationId: 1 },
    note: "Location and Route IDs must be integers. Climb IDs can be strings."
  },
  
  belayTypeOnRoute: {
    issue: "Adding belayType to routes instead of climbs",
    wrong: { 
      routes: [{ belayType: "lead", /* ... */ }] 
    },
    correct: { 
      climbs: [{ belayType: "lead", /* ... */ }] 
    },
    note: "belayType is a property of climbs (the ascent), not routes (the line)"
  },
  
  invalidGradeFormat: {
    issue: "Incorrect grade patterns",
    wrong: { 
      grades: { french: "6a++" },  // Double plus
      grades: { vScale: "v4" }     // Lowercase v
    },
    correct: { 
      grades: { french: "6a+" },
      grades: { vScale: "V4" }
    }
  },
  
  missingRequiredFields: {
    issue: "Missing required fields",
    requirements: {
      manifest: ["version", "format", "platform", "appVersion", "creationDate"],
      location: ["id", "name", "country", "isIndoor"],
      route: ["id", "locationId", "name", "routeType"],
      climb: ["id", "date", "type", "finishType"]
    }
  },
  
  unsupportedFields: {
    issue: "Using fields not in schema",
    examples: ["customFields", "userDefined", "extra"],
    note: "Only fields defined in the schema are allowed"
  }
};

export const FIELD_REFERENCE = {
  location: {
    required: ["id", "name", "country", "isIndoor"],
    optional: ["state", "city", "address", "coordinates", "terrainType", "rockType", "accessInfo"],
    types: {
      id: "integer",
      coordinates: "{ latitude: number, longitude: number }",
      isIndoor: "boolean"
    }
  },
  route: {
    required: ["id", "locationId", "name", "routeType"],
    optional: ["sectorId", "grades", "height", "color", "qualityRating", "firstAscent", "beta", "protectionRating", "gearNotes", "tags"],
    types: {
      id: "integer",
      locationId: "integer", 
      sectorId: "integer",
      qualityRating: "integer (0-5)",
      height: "number (meters)",
      color: "string (#RRGGBB hex)"
    }
  },
  climb: {
    required: ["id", "date", "type", "finishType"],
    optional: ["routeId", "routeName", "sessionId", "attempts", "grades", "belayType", "partner", "notes", "rating", "tags"],
    types: {
      id: "integer or string",
      date: "string (ISO date)",
      attempts: "integer",
      belayType: "enum: topRope, lead, autoBelay"
    }
  }
};