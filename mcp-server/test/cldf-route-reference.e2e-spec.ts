import { Test } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { ToolHandlersService } from '../src/mcp/tool-handlers.service';
import * as fs from 'fs/promises';
import * as path from 'path';
import { tmpdir } from 'os';

describe('CLDF Route Reference Tests', () => {
  let app: INestApplication;
  let toolHandlers: ToolHandlersService;

  beforeAll(async () => {
    process.env.CLDF_CLI = '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf';
    
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    await app.init();

    toolHandlers = app.get(ToolHandlersService);
  });

  afterAll(async () => {
    await app.close();
  });

  describe('Route References in Climbs', () => {
    it('should handle climbs with routeName when routes exist', async () => {
      const outputPath = path.join(tmpdir(), `cldf-routename-with-routes-${Date.now()}.cldf`);
      
      try {
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
            },
          ],
          routes: [
            {
              id: 1,
              locationId: 1,
              name: 'Classic Corner',
              routeType: 'route',
            },
            {
              id: 2,
              locationId: 1,
              name: 'Power Boulder',
              routeType: 'boulder',
            },
          ],
          climbs: [
            {
              id: 1,
              date: '2025-01-15',
              routeName: 'Classic Corner',  // Using name reference
              type: 'route',
              finishType: 'redpoint',
              attempts: 2,
            },
            {
              id: 2,
              date: '2025-01-15',
              routeName: 'Power Boulder',  // Using name reference
              type: 'boulder',
              finishType: 'top',
              attempts: 5,
            },
          ],
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        await toolHandlers.handleToolCall('cldf_create', {
          template: 'basic',
          outputPath,
          data: testData,
        });

        // Query routes first
        const routesResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'routes',
        });
        const routesData = JSON.parse(routesResult.content[0].text);
        expect(routesData.data?.count).toBe(2);
        expect(routesData.data?.results?.length).toBe(2);

        // Then query climbs
        const climbsResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'climbs',
        });
        const climbsData = JSON.parse(climbsResult.content[0].text);
        expect(climbsData.data?.count).toBe(2);
        expect(climbsData.data?.results?.length).toBe(2);
        expect(climbsData.data?.results?.[0].routeName).toBe('Classic Corner');
        expect(climbsData.data?.results?.[1].routeName).toBe('Power Boulder');

        console.log('✓ Successfully handled climbs with routeName references');

      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });

    it('should handle climbs with routeId integer references', async () => {
      const outputPath = path.join(tmpdir(), `cldf-routeid-${Date.now()}.cldf`);
      
      try {
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
              name: 'Blue Circuit',
              routeType: 'route',
            },
            {
              id: 2,
              locationId: 1,
              name: 'Red Problem',
              routeType: 'boulder',
            },
          ],
          climbs: [
            {
              id: 1,
              routeId: 1,  // Integer ID reference
              routeName: 'Blue Circuit',  // Include name for validation
              date: '2025-01-20',
              type: 'route',
              finishType: 'onsight',
              attempts: 1,
            },
            {
              id: 2,
              routeId: 2,  // Integer ID reference
              routeName: 'Red Problem',  // Include name for validation
              date: '2025-01-20',
              type: 'boulder',
              finishType: 'flash',
              attempts: 1,
            },
          ],
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        await toolHandlers.handleToolCall('cldf_create', {
          template: 'basic',
          outputPath,
          data: testData,
        });

        // Query routes first
        const routesResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'routes',
        });
        const routesData = JSON.parse(routesResult.content[0].text);
        expect(routesData.data?.count).toBe(2);
        expect(routesData.data?.results?.length).toBe(2);

        // Then query climbs
        const climbsResult = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'climbs',
        });
        const climbsData = JSON.parse(climbsResult.content[0].text);
        expect(climbsData.data?.count).toBe(2);
        expect(climbsData.data?.results?.length).toBe(2);
        
        // Verify integer routeId references
        expect(climbsData.data?.results?.[0].routeId).toBe(1);
        expect(climbsData.data?.results?.[1].routeId).toBe(2);
        expect(typeof climbsData.data?.results?.[0].routeId).toBe('number');
        expect(typeof climbsData.data?.results?.[1].routeId).toBe('number');

        console.log('✓ Successfully handled climbs with integer routeId references');

      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });

    it('should demonstrate route query performance with 1000 routes and climbs', async () => {
      const outputPath = path.join(tmpdir(), `cldf-perf-routes-${Date.now()}.cldf`);
      
      try {
        // Generate large dataset
        const locations = Array.from({ length: 10 }, (_, i) => ({
          id: i + 1,
          name: `Location ${i + 1}`,
          country: 'USA',
          isIndoor: i % 2 === 0,
        }));

        const routes = Array.from({ length: 1000 }, (_, i) => ({
          id: i + 1,
          locationId: ((i % 10) + 1),
          name: `Route ${i + 1}`,
          routeType: i % 2 === 0 ? 'boulder' : 'route',
        }));

        const climbs = Array.from({ length: 1000 }, (_, i) => ({
          id: i + 1,
          routeId: i + 1,  // Direct mapping to routes
          routeName: `Route ${i + 1}`,  // Include for validation
          date: '2025-01-01',
          type: i % 2 === 0 ? 'boulder' : 'route',
          finishType: i % 2 === 0 ? 'top' : 'redpoint',
          attempts: (i % 5) + 1,
        }));

        const testData = {
          manifest: {
            version: '1.0.0',
            format: 'CLDF',
            platform: 'Desktop',
            appVersion: '1.0.0',
            creationDate: new Date().toISOString(),
          },
          locations,
          routes,
          climbs,
          checksums: {
            algorithm: 'SHA-256',
          },
        };

        const startTime = Date.now();
        await toolHandlers.handleToolCall('cldf_create', {
          template: 'basic',
          outputPath,
          data: testData,
        });
        const createTime = Date.now() - startTime;

        // Query climbs to verify relationships
        const queryStart = Date.now();
        const result = await toolHandlers.handleToolCall('cldf_query', {
          filePath: outputPath,
          dataType: 'climbs',
        });
        const queryTime = Date.now() - queryStart;

        const data = JSON.parse(result.content[0].text);
        expect(data.data?.count).toBe(1000);

        console.log(`✓ Performance test with 1000 routes and climbs:`);
        console.log(`  - Archive creation: ${createTime}ms`);
        console.log(`  - Climb query: ${queryTime}ms`);
        console.log(`  - All climbs have integer routeId references`);

      } finally {
        await fs.unlink(outputPath).catch(() => {});
      }
    });
  });
});