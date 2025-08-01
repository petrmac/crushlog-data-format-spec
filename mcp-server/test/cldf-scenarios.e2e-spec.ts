import { Test } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { ToolHandlersService } from '../src/mcp/tool-handlers.service';
import * as fs from 'fs/promises';
import * as path from 'path';
import { tmpdir } from 'os';
import { skipIfNoCldfCli } from './helpers/cldf-cli-check';

describe('CLDF Data Scenarios', () => {
  let app: INestApplication;
  let toolHandlers: ToolHandlersService;

  beforeAll(async () => {
    // Skip these tests if CLDF CLI is not available (e.g., in CI)
    if (skipIfNoCldfCli()) {
      return;
    }

    // Set the CLDF CLI path for testing if not already set
    if (!process.env.CLDF_CLI) {
      process.env.CLDF_CLI = '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf';
    }
    
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    await app.init();

    toolHandlers = app.get(ToolHandlersService);
  });

  afterAll(async () => {
    if (app) {
      await app.close();
    }
  });

  describe('Scenario 1: Locations and Routes Only', () => {
    it('should create and query archive with only locations and routes', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const outputPath = path.join(tmpdir(), `cldf-locations-routes-${Date.now()}.cldf`);
      
      try {
        // Generate test data with 100 locations and 500 routes
        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations: generateLocations(100),
          routes: generateRoutes(500, 100),
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        // Create archive
        const createResult = await toolHandlers.handleToolCall('cldf_create', {
          template: 'basic',
          outputPath,
          data: testData,
        });

        expect(createResult).toBeDefined();

        // Query locations
        const locationsResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'locations',
        });

        const locationsData = JSON.parse(locationsResult.content[0].text);
        expect(locationsData.data?.count).toBe(100);
        expect(locationsData.data?.results?.length).toBe(100);

        // Query routes
        const routesResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'routes',
        });

        const routesData = JSON.parse(routesResult.content[0].text);
        expect(routesData.data?.count).toBe(500);
        expect(routesData.data?.results?.length).toBe(500);

        // Verify route has location reference
        const firstRoute = routesData.data.results[0];
        expect(firstRoute.locationId).toBeDefined();
        expect(firstRoute.locationId).toBeGreaterThanOrEqual(1);
        expect(firstRoute.locationId).toBeLessThanOrEqual(100);

      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });

  describe('Scenario 2: Climbs Without Route Links', () => {
    it('should create and query archive with climbs that have no route references', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const outputPath = path.join(tmpdir(), `cldf-climbs-only-${Date.now()}.cldf`);
      
      try {
        // Generate test data with locations, sessions, and climbs (no routes)
        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations: generateLocations(10),
          sessions: generateSessions(20, 10),
          climbs: generateClimbsWithoutRoutes(200, 20),
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        // Create archive
        try {
          const createResult = await toolHandlers.handleToolCall('cldf_create', {
            template: 'basic',
            outputPath,
            data: testData,
          });

          expect(createResult).toBeDefined();
        } catch (error) {
          console.error('Create failed with error:', error);
          console.error('Test data climbs sample:', JSON.stringify(testData.climbs.slice(0, 2), null, 2));
          throw error;
        }

        // Query climbs
        const climbsResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'climbs',
        });

        const climbsData = JSON.parse(climbsResult.content[0].text);
        expect(climbsData.data?.count).toBe(200);
        expect(climbsData.data?.results?.length).toBe(200);

        // Verify climbs have no route references but have route names
        const firstClimb = climbsData.data.results[0];
        expect(firstClimb.routeId).toBeUndefined();
        expect(firstClimb.routeName).toBeDefined();
        expect(firstClimb.sessionId).toBeDefined();

      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });

  describe('Scenario 3: Full Data with Tags', () => {
    it('should create and query archive with locations, routes, climbs, and tags', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const outputPath = path.join(tmpdir(), `cldf-full-data-${Date.now()}.cldf`);
      
      try {
        // Generate comprehensive test data
        const tags = generateTags();
        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations: generateLocations(20),
          routes: generateRoutesWithTags(100, 20, tags),
          sessions: generateSessions(30, 20),
          climbs: generateClimbsWithTags(300, 30, 100, tags),
          tags: tags,
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        // Create archive
        const createResult = await toolHandlers.handleToolCall('cldf_create', {
          template: 'basic',
          outputPath,
          data: testData,
        });

        expect(createResult).toBeDefined();

        // Query all data types
        const allResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'all',
        });

        const allData = JSON.parse(allResult.content[0].text);
        expect(allData.data?.locations?.length).toBe(20);
        expect(allData.data?.routes?.length).toBe(100);
        expect(allData.data?.sessions?.length).toBe(30);
        expect(allData.data?.climbs?.length).toBe(300);
        expect(allData.data?.tags?.length).toBe(tags.length);

        // Verify tags are properly linked
        const routeWithTags = allData.data.routes.find(r => r.tags && r.tags.length > 0);
        expect(routeWithTags).toBeDefined();
        expect(routeWithTags.tags).toContain(expect.stringMatching(/^tag:\d+$/));

        const climbWithTags = allData.data.climbs.find(c => c.tags && c.tags.length > 0);
        expect(climbWithTags).toBeDefined();
        expect(climbWithTags.tags).toContain(expect.stringMatching(/^tag:\d+$/));

      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });

  describe('Memory Performance', () => {
    it('should handle large datasets efficiently across all scenarios', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const scenarios = [
        { name: 'Large Routes Only', locations: 500, routes: 5000 },
        { name: 'Large Climbs Only', locations: 100, sessions: 500, climbs: 5000 },
        { name: 'Large Mixed Data', locations: 200, routes: 2000, sessions: 300, climbs: 3000 },
      ];

      for (const scenario of scenarios) {
        const outputPath = path.join(tmpdir(), `cldf-perf-${Date.now()}.cldf`);
        
        try {
          const initialMemory = process.memoryUsage();
          console.log(`\n${scenario.name} - Initial memory:`, {
            heapUsed: `${Math.round(initialMemory.heapUsed / 1024 / 1024)}MB`,
          });

          const testData: any = {
            manifest: {
              version: '1.0.0',
              format: 'CLDF',
              platform: 'Desktop',
              appVersion: '1.0.0',
              creationDate: new Date().toISOString(),
            },
            locations: generateLocations(scenario.locations),
            checksums: {
              algorithm: 'SHA-256',
            },
          };

          if (scenario.routes) {
            testData.routes = generateRoutes(scenario.routes, scenario.locations);
          }

          if (scenario.sessions) {
            testData.sessions = generateSessions(scenario.sessions, scenario.locations);
          }

          if (scenario.climbs) {
            testData.climbs = generateClimbsWithoutRoutes(scenario.climbs, scenario.sessions || 0);
          }

          const startTime = Date.now();
          await toolHandlers.handleToolCall('cldf_create', {
            template: 'basic',
            outputPath,
            data: testData,
          });

          const duration = Date.now() - startTime;
          const finalMemory = process.memoryUsage();
          
          console.log(`${scenario.name} - Completed in ${duration}ms`);
          console.log(`${scenario.name} - Memory increase:`, {
            heapUsed: `${Math.round((finalMemory.heapUsed - initialMemory.heapUsed) / 1024 / 1024)}MB`,
          });

          // Memory increase should be reasonable
          const memoryIncreaseMB = (finalMemory.heapUsed - initialMemory.heapUsed) / 1024 / 1024;
          expect(memoryIncreaseMB).toBeLessThan(100); // Less than 100MB increase

        } finally {
          await fs.unlink(outputPath).catch(() => {});
        }
      }
    });
  });
});

// Helper functions to generate test data

function generateLocations(count: number) {
  const locations = [];
  for (let i = 1; i <= count; i++) {
    locations.push({
      id: i,
      name: `Location ${i}`,
      country: ['USA', 'Canada', 'France', 'Spain', 'Germany'][i % 5],
      isIndoor: i % 3 === 0,
      state: `State ${i % 50}`,
      city: `City ${i % 20}`,
      coordinates: {
        latitude: 40 + (i * 0.01),
        longitude: -105 + (i * 0.01),
      },
      terrainType: ['natural', 'artificial'][i % 2],
      rockType: ['limestone', 'granite', 'sandstone'][i % 3],
      accessInfo: `Access info for location ${i}`,
    });
  }
  return locations;
}

function generateRoutes(count: number, locationCount: number) {
  const routes = [];
  for (let i = 1; i <= count; i++) {
    const routeType = ['boulder', 'route'][i % 2];
    routes.push({
      id: i,
      locationId: ((i - 1) % locationCount) + 1,
      name: `Route ${i}`,
      routeType: routeType,
      grades: routeType === 'boulder' 
        ? { vScale: `V${i % 15}` } 
        : { french: ['5a', '5b', '5c', '6a', '6a+', '6b', '6b+', '6c', '6c+', '7a'][i % 10] },
      height: Math.floor(Math.random() * 30) + 5,
      color: `#${((i * 123456) % 16777215).toString(16).padStart(6, '0').toUpperCase()}`,
      qualityRating: (i % 5) + 1,
      beta: `Beta information for route ${i}`,
    });
  }
  return routes;
}

function generateRoutesWithTags(count: number, locationCount: number, tags: any[]) {
  const routes = generateRoutes(count, locationCount);
  routes.forEach((route, index) => {
    // Add tags to 70% of routes
    if (index % 10 < 7) {
      const tagCount = (index % 3) + 1;
      route.tags = [];
      for (let i = 0; i < tagCount; i++) {
        const tagId = ((index + i) % tags.length) + 1;
        route.tags.push(`tag:${tagId}`);
      }
    }
  });
  return routes;
}

function generateSessions(count: number, locationCount: number) {
  const sessions = [];
  const sessionTypes = ['bouldering', 'sportClimbing', 'indoorClimbing', 'indoorBouldering'];
  
  for (let i = 1; i <= count; i++) {
    const date = new Date();
    date.setDate(date.getDate() - (count - i)); // Sessions from past days
    
    sessions.push({
      id: i,
      locationId: ((i - 1) % locationCount) + 1,
      date: date.toISOString().split('T')[0],
      sessionType: sessionTypes[i % sessionTypes.length],
      notes: `Session ${i} notes`,
      partners: i % 3 === 0 ? [`Partner ${i % 5}`] : undefined,
      startTime: '09:00:00',
      endTime: '11:00:00',
    });
  }
  return sessions;
}

function generateClimbsWithoutRoutes(count: number, sessionCount: number) {
  const climbs = [];
  const climbTypes = ['boulder', 'route'];
  const finishTypes = {
    boulder: ['flash', 'top', 'repeat', 'project', 'attempt'],
    route: ['flash', 'top', 'repeat', 'project', 'attempt', 'onsight', 'redpoint'],
  };

  for (let i = 1; i <= count; i++) {
    const climbType = climbTypes[i % 2];
    const date = new Date();
    date.setDate(date.getDate() - Math.floor(i / 10));
    
    climbs.push({
      id: i, // Climbs have integer IDs
      sessionId: sessionCount > 0 ? ((i - 1) % sessionCount) + 1 : undefined,
      date: date.toISOString().split('T')[0],
      routeName: `Climb ${i}`,
      type: climbType,
      finishType: finishTypes[climbType][i % finishTypes[climbType].length],
      attempts: (i % 5) + 1,
      grades: climbType === 'boulder'
        ? { grade: `V${i % 15}`, system: 'vScale' }
        : { grade: ['5a', '5b', '5c', '6a', '6a+', '6b', '6b+', '6c', '6c+', '7a'][i % 10], system: 'french' },
      rating: (i % 5) + 1,
      notes: `Notes for climb ${i}`,
    });
  }
  return climbs;
}

function generateClimbsWithTags(count: number, sessionCount: number, routeCount: number, tags: any[]) {
  const climbs = generateClimbsWithoutRoutes(count, sessionCount);
  
  climbs.forEach((climb, index) => {
    // 50% of climbs reference actual routes
    if (index % 2 === 0 && routeCount > 0) {
      climb.routeId = ((index / 2) % routeCount) + 1;
      delete climb.routeName; // Remove routeName when we have routeId
    }
    
    // Add tags to 60% of climbs
    if (index % 10 < 6) {
      const tagCount = (index % 2) + 1;
      climb.tags = [];
      for (let i = 0; i < tagCount; i++) {
        const tagId = ((index + i + 5) % tags.length) + 1; // Offset to get different tags than routes
        climb.tags.push(`tag:${tagId}`);
      }
    }
  });
  
  return climbs;
}

function generateTags() {
  const tags = [];
  const categories = ['style', 'condition', 'personal', 'technique', 'weather'];
  const tagNames = {
    style: ['onsight', 'flash', 'redpoint', 'headpoint', 'pinkpoint'],
    condition: ['wet', 'dry', 'dusty', 'polished', 'sharp'],
    personal: ['project', 'warmup', 'limit', 'training', 'fun'],
    technique: ['crimpy', 'sloper', 'dynamic', 'technical', 'powerful'],
    weather: ['sunny', 'cloudy', 'cold', 'hot', 'windy'],
  };

  let id = 1;
  for (const category of categories) {
    for (const name of tagNames[category]) {
      tags.push({
        id: id++,
        key: category,
        value: name,
      });
    }
  }
  
  return tags;
}