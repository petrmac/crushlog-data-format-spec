import { Test } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { ToolHandlersService } from '../src/mcp/tool-handlers.service';
import * as fs from 'fs/promises';
import * as path from 'path';
import { tmpdir } from 'os';
import { skipIfNoCldfCli } from './helpers/cldf-cli-check';

describe('CLDF Simple Scenarios', () => {
  let app: INestApplication;
  let toolHandlers: ToolHandlersService;

  beforeAll(async () => {
    // Skip these tests if CLDF CLI is not available (e.g., in CI)
    if (skipIfNoCldfCli()) {
      return;
    }

    // Set the CLDF CLI path for testing if not already set
    if (!process.env.CLDF_CLI) {
      process.env.CLDF_CLI =
        '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf';
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

  describe('Scenario 1: Just Locations and Routes', () => {
    it('should create and query archive with only locations and routes', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const outputPath = path.join(
        tmpdir(),
        `cldf-scenario1-${Date.now()}.cldf`,
      );

      try {
        // Minimal valid data - just locations and routes
        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations: [
            {
              id: 1,
              name: 'Test Crag',
              country: 'USA',
              isIndoor: false,
              coordinates: { latitude: 40.0, longitude: -105.0 },
            },
            {
              id: 2,
              name: 'Test Gym',
              country: 'USA',
              isIndoor: true,
              city: 'Boulder',
            },
          ],
          routes: [
            {
              id: 1,
              locationId: 1,
              name: 'Classic Route',
              routeType: 'route',
              grades: { french: '6a' },
              height: 20,
            },
            {
              id: 2,
              locationId: 2,
              name: 'Gym Problem',
              routeType: 'boulder',
              grades: { vScale: 'V4' },
              color: '#FF0000',
            },
          ],
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        // Create archive
        try {
          await toolHandlers.handleToolCall('cldf_create', {
            template: 'basic',
            outputPath,
            data: testData,
          });
        } catch (error) {
          console.error('Scenario 2 error:', error.stdout || error.message);
          throw error;
        }

        // Query routes
        const result = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'routes',
        });

        const data = JSON.parse(result.content[0].text);
        expect(data.data?.count).toBe(2);
        expect(data.data?.results?.[0].name).toBe('Classic Route');
        expect(data.data?.results?.[1].name).toBe('Gym Problem');

        console.log(
          '✓ Scenario 1: Successfully created and queried locations+routes only',
        );
      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });

  describe('Scenario 2: Just Climbs Without Route Links', () => {
    it('should create and query archive with climbs that have no route references', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const outputPath = path.join(
        tmpdir(),
        `cldf-scenario2-${Date.now()}.cldf`,
      );

      try {
        // Climbs without routes - just locations, sessions and climbs
        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations: [
            {
              id: 1,
              name: 'Outdoor Crag',
              country: 'USA',
              isIndoor: false,
            },
          ],
          climbs: [
            {
              id: 1,
              date: '2025-01-15',
              routeName: 'Unknown Boulder 1',
              type: 'boulder',
              finishType: 'top',
              attempts: 3,
            },
            {
              id: 2,
              date: '2025-01-15',
              routeName: 'Unknown Boulder 2',
              type: 'boulder',
              finishType: 'flash',
              attempts: 1,
            },
          ],
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        // Create archive
        try {
          await toolHandlers.handleToolCall('cldf_create', {
            template: 'basic',
            outputPath,
            data: testData,
          });
        } catch (error) {
          console.error('Scenario 2 error:', error.stdout || error.message);
          throw error;
        }

        // Query climbs
        const result = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'climbs',
        });

        const data = JSON.parse(result.content[0].text);
        expect(data.data?.count).toBe(2);
        expect(data.data?.results?.[0].routeName).toBe('Unknown Boulder 1');
        expect(data.data?.results?.[0].routeId).toBeUndefined();

        console.log(
          '✓ Scenario 2: Successfully created and queried climbs without route links',
        );
      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });

  describe('Scenario 3: Full Data With Tags', () => {
    it('should create and query archive with locations, routes, climbs enriched with tags', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const outputPath = path.join(
        tmpdir(),
        `cldf-scenario3-${Date.now()}.cldf`,
      );

      try {
        // Full data with tags
        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations: [
            {
              id: 1,
              name: 'Tagged Crag',
              country: 'USA',
              isIndoor: false,
            },
          ],
          routes: [
            {
              id: 1,
              locationId: 1,
              name: 'Tagged Route',
              routeType: 'route',
              grades: { french: '7a' },
              tags: ['style:crimpy', 'condition:polished'],
            },
            {
              id: 2,
              locationId: 1,
              name: 'Tagged Boulder',
              routeType: 'boulder',
              grades: { vScale: 'V6' },
              tags: ['style:dynamic'],
            },
          ],
          sessions: [
            {
              id: 1,
              locationId: 1,
              date: '2025-01-20',
              sessionType: 'sportClimbing',
            },
          ],
          climbs: [
            {
              id: 1,
              sessionId: 1,
              routeId: 1,
              date: '2025-01-20',
              type: 'route',
              finishType: 'redpoint',
              attempts: 5,
              tags: ['condition:wet', 'personal:project'],
            },
            {
              id: 2,
              sessionId: 1,
              routeId: 2,
              date: '2025-01-20',
              type: 'boulder',
              finishType: 'flash',
              attempts: 1,
              tags: ['personal:warmup'],
            },
          ],
          tags: [
            { id: 1, category: 'style', name: 'crimpy' },
            { id: 2, category: 'style', name: 'dynamic' },
            { id: 3, category: 'condition', name: 'polished' },
            { id: 4, category: 'condition', name: 'wet' },
            { id: 5, category: 'personal', name: 'project' },
            { id: 6, category: 'personal', name: 'warmup' },
          ],
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        // Create archive
        try {
          await toolHandlers.handleToolCall('cldf_create', {
            template: 'basic',
            outputPath,
            data: testData,
          });
        } catch (error) {
          console.error('Scenario 2 error:', error.stdout || error.message);
          throw error;
        }

        // Query routes
        const routesResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'routes',
        });
        const routesData = JSON.parse(routesResult.content[0].text);
        expect(routesData.data?.count).toBe(2);
        expect(routesData.data?.results?.length).toBe(2);

        // Query climbs
        const climbsResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'climbs',
        });
        const climbsData = JSON.parse(climbsResult.content[0].text);
        expect(climbsData.data?.count).toBe(2);
        expect(climbsData.data?.results?.length).toBe(2);

        // Query tags
        const tagsResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'tags',
        });
        const tagsData = JSON.parse(tagsResult.content[0].text);
        expect(tagsData.data?.count).toBe(6);
        expect(tagsData.data?.results?.length).toBe(6);

        // Verify tags are present
        const routeWithTags = routesData.data.results[0];
        expect(routeWithTags.tags).toContain('style:crimpy');
        expect(routeWithTags.tags).toContain('condition:polished');

        const climbWithTags = climbsData.data.results[0];
        expect(climbWithTags.tags).toContain('condition:wet');
        expect(climbWithTags.tags).toContain('personal:project');

        console.log(
          '✓ Scenario 3: Successfully created and queried full data with tags',
        );
      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });

  describe('Scenario 4: Climbs Referencing Routes by ID', () => {
    it('should create and query archive with climbs that reference routes by integer ID', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const outputPath = path.join(
        tmpdir(),
        `cldf-scenario4-${Date.now()}.cldf`,
      );

      try {
        // Test data with routes and climbs that reference them by ID
        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations: [
            {
              id: 1,
              name: 'Test Gym',
              country: 'USA',
              isIndoor: true,
            },
          ],
          routes: [
            {
              id: 1,
              locationId: 1,
              name: 'Blue Route',
              routeType: 'route',
              grades: { french: '5c' },
              color: '#0000FF',
            },
            {
              id: 2,
              locationId: 1,
              name: 'Red Boulder',
              routeType: 'boulder',
              grades: { vScale: 'V3' },
              color: '#FF0000',
            },
            {
              id: 3,
              locationId: 1,
              name: 'Green Route',
              routeType: 'route',
              grades: { french: '6a+' },
              color: '#00FF00',
            },
          ],
          climbs: [
            {
              id: 1,
              routeId: 1, // Integer reference to route
              date: '2025-01-20',
              type: 'route',
              finishType: 'onsight',
              attempts: 1,
            },
            {
              id: 2,
              routeId: 2, // Integer reference to boulder
              date: '2025-01-20',
              type: 'boulder',
              finishType: 'flash',
              attempts: 1,
            },
            {
              id: 3,
              routeId: 3, // Integer reference to route
              date: '2025-01-21',
              type: 'route',
              finishType: 'redpoint',
              attempts: 3,
              rating: 4,
              notes: 'Great route, tricky crux',
            },
          ],
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        // Create archive
        try {
          await toolHandlers.handleToolCall('cldf_create', {
            template: 'basic',
            outputPath,
            data: testData,
          });
        } catch (error) {
          console.error('Scenario 4 error:', error.stdout || error.message);
          throw error;
        }

        // Query routes to verify relationships
        const routesResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'routes',
        });
        const routesData = JSON.parse(routesResult.content[0].text);

        // Verify routes exist
        expect(routesData.data?.count).toBe(3);
        expect(routesData.data?.results?.length).toBe(3);
        expect(routesData.data?.results?.[0].id).toBe(1);
        expect(routesData.data?.results?.[1].id).toBe(2);
        expect(routesData.data?.results?.[2].id).toBe(3);

        // Query climbs
        const climbsResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'climbs',
        });
        const climbsData = JSON.parse(climbsResult.content[0].text);

        // Verify climbs exist and reference routes by ID
        expect(climbsData.data?.count).toBe(3);
        expect(climbsData.data?.results?.length).toBe(3);
        expect(climbsData.data?.results?.[0].routeId).toBe(1);
        expect(climbsData.data?.results?.[1].routeId).toBe(2);
        expect(climbsData.data?.results?.[2].routeId).toBe(3);

        // Verify climb should NOT have routeName when routeId is present
        expect(climbsData.data?.results?.[0].routeName).toBeUndefined();
        expect(climbsData.data?.results?.[1].routeName).toBeUndefined();
        expect(climbsData.data?.results?.[2].routeName).toBeUndefined();

        console.log(
          '✓ Scenario 4: Successfully created and queried climbs with route ID references',
        );
      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });

  describe('Memory Performance Summary', () => {
    it('should show memory usage for each scenario', async () => {
      if (!app) {
        console.log('Skipping test - CLDF CLI not available');
        return;
      }
      const scenarios = [
        {
          name: 'Locations+Routes (1000 routes)',
          locationCount: 100,
          routeCount: 1000,
        },
        {
          name: 'Climbs Only (1000 climbs)',
          locationCount: 20,
          sessionCount: 100,
          climbCount: 1000,
        },
        {
          name: 'Full Data (500 of each)',
          locationCount: 50,
          routeCount: 500,
          sessionCount: 100,
          climbCount: 500,
          tagCount: 20,
        },
      ];

      console.log('\nMemory Performance Summary:');
      console.log('---------------------------');

      for (const scenario of scenarios) {
        const outputPath = path.join(tmpdir(), `cldf-perf-${Date.now()}.cldf`);

        try {
          const initialMemory = process.memoryUsage();

          // Generate test data based on scenario
          const testData: any = {
            manifest: {
              version: '1.0.0',
              format: 'CLDF',
              platform: 'Desktop',
              appVersion: '1.0.0',
              creationDate: new Date().toISOString(),
            },
            locations: generateSimpleLocations(scenario.locationCount),
            checksums: { algorithm: 'SHA-256' },
          };

          if (scenario.routeCount) {
            testData.routes = generateSimpleRoutes(
              scenario.routeCount,
              scenario.locationCount,
            );
          }

          if (scenario.sessionCount) {
            testData.sessions = generateSimpleSessions(
              scenario.sessionCount,
              scenario.locationCount,
            );
          }

          if (scenario.climbCount) {
            testData.climbs = generateSimpleClimbs(
              scenario.climbCount,
              scenario.sessionCount || 0,
            );
          }

          if (scenario.tagCount) {
            testData.tags = generateSimpleTags(scenario.tagCount);
          }

          const startTime = Date.now();
          await toolHandlers.handleToolCall('cldf_create', {
            template: 'basic',
            outputPath,
            data: testData,
          });

          const duration = Date.now() - startTime;
          const finalMemory = process.memoryUsage();
          const memoryIncrease = Math.round(
            (finalMemory.heapUsed - initialMemory.heapUsed) / 1024 / 1024,
          );

          console.log(`${scenario.name}:`);
          console.log(`  Creation time: ${duration}ms`);
          console.log(`  Memory increase: ${memoryIncrease}MB`);
        } finally {
          await fs.unlink(outputPath).catch(() => {});
        }
      }
    });
  });
});

// Simplified helper functions

function generateSimpleLocations(count: number) {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    name: `Location ${i + 1}`,
    country: 'USA',
    isIndoor: i % 2 === 0,
  }));
}

function generateSimpleRoutes(count: number, locationCount: number) {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    locationId: (i % locationCount) + 1,
    name: `Route ${i + 1}`,
    routeType: i % 2 === 0 ? 'boulder' : 'route',
  }));
}

function generateSimpleSessions(count: number, locationCount: number) {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    locationId: (i % locationCount) + 1,
    date: '2025-01-01',
    sessionType: 'bouldering',
  }));
}

function generateSimpleClimbs(count: number, sessionCount: number) {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    sessionId: sessionCount > 0 ? (i % sessionCount) + 1 : undefined,
    date: '2025-01-01',
    routeName: `Climb ${i + 1}`,
    type: 'boulder',
    finishType: 'top',
    attempts: 1,
  }));
}

function generateSimpleTags(count: number) {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    category: 'tag',
    name: `value${i + 1}`,
  }));
}
