import { Test } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { ToolHandlersService } from '../src/mcp/tool-handlers.service';
import * as fs from 'fs/promises';
import * as path from 'path';
import { tmpdir } from 'os';
import { skipIfNoCldfCli } from './helpers/cldf-cli-check';

describe('Memory Load Test', () => {
  let app: INestApplication;
  let toolHandlers: ToolHandlersService;
  let testOutputPath: string;

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
    testOutputPath = path.join(tmpdir(), `cldf-memory-test-${Date.now()}.cldf`);
  });

  afterAll(async () => {
    if (app) {
      await app.close();
    }
    if (testOutputPath) {
      try {
        await fs.unlink(testOutputPath);
      } catch (error) {
        // Ignore cleanup errors
      }
    }
  });

  it('should handle creating CLDF archive with 1000 routes without memory issues', async () => {
    if (!app) {
      console.log('Skipping test - CLDF CLI not available');
      return;
    }
    // Generate test data with 1000 routes
    const testData = generateLargeTestData(1000);
    
    // Measure initial memory
    const initialMemory = process.memoryUsage();
    console.log('Initial memory usage:', {
      heapUsed: `${Math.round(initialMemory.heapUsed / 1024 / 1024)}MB`,
      external: `${Math.round(initialMemory.external / 1024 / 1024)}MB`,
      rss: `${Math.round(initialMemory.rss / 1024 / 1024)}MB`,
    });

    // Create CLDF archive with large dataset
    const startTime = Date.now();
    
    try {
      await toolHandlers.handleToolCall('cldf_create', {
        template: 'basic',
        outputPath: testOutputPath,
        data: testData,
      });

      const duration = Date.now() - startTime;
      const finalMemory = process.memoryUsage();
      
      console.log('Final memory usage:', {
        heapUsed: `${Math.round(finalMemory.heapUsed / 1024 / 1024)}MB`,
        external: `${Math.round(finalMemory.external / 1024 / 1024)}MB`,
        rss: `${Math.round(finalMemory.rss / 1024 / 1024)}MB`,
      });

      console.log('Memory increase:', {
        heapUsed: `${Math.round((finalMemory.heapUsed - initialMemory.heapUsed) / 1024 / 1024)}MB`,
        external: `${Math.round((finalMemory.external - initialMemory.external) / 1024 / 1024)}MB`,
        rss: `${Math.round((finalMemory.rss - initialMemory.rss) / 1024 / 1024)}MB`,
      });

      console.log(`Operation completed in ${duration}ms`);

      // Verify the archive was created
      const stats = await fs.stat(testOutputPath);
      expect(stats.size).toBeGreaterThan(0);
      console.log(`Archive size: ${Math.round(stats.size / 1024 / 1024)}MB`);

      // Test querying the large dataset
      const queryStartTime = Date.now();
      const queryResult = await toolHandlers.handleToolCall('cldf_query', {
        filePath: testOutputPath,
        dataType: 'routes',
      });
      const queryDuration = Date.now() - queryStartTime;
      console.log(`Query completed in ${queryDuration}ms`);

      const queryMemory = process.memoryUsage();
      console.log('Memory after query:', {
        heapUsed: `${Math.round(queryMemory.heapUsed / 1024 / 1024)}MB`,
        increase: `${Math.round((queryMemory.heapUsed - finalMemory.heapUsed) / 1024 / 1024)}MB`,
      });

      // Parse result to verify route count
      const resultText = queryResult.content[0].text;
      const parsedResult = JSON.parse(resultText);
      expect(parsedResult.data?.count).toBe(1000);
      expect(parsedResult.data?.results?.length).toBe(1000);

    } catch (error) {
      console.error('Test failed:', error);
      throw error;
    }
  }, 60000); // 60 second timeout

  // Test repeated operations to check for memory leaks
  it('should not leak memory when processing multiple archives', async () => {
    if (!app) {
      console.log('Skipping test - CLDF CLI not available');
      return;
    }
    const iterations = 5;
    const memoryUsage: any[] = [];

    for (let i = 0; i < iterations; i++) {
      const testData = generateLargeTestData(200); // Smaller dataset for multiple iterations
      const outputPath = path.join(tmpdir(), `cldf-leak-test-${Date.now()}-${i}.cldf`);

      const beforeMemory = process.memoryUsage();
      
      try {
        await toolHandlers.handleToolCall('cldf_create', {
          template: 'basic',
          outputPath,
          data: testData,
        });

        // Force garbage collection if available
        if (global.gc) {
          global.gc();
        }

        const afterMemory = process.memoryUsage();
        memoryUsage.push({
          iteration: i,
          heapUsed: Math.round(afterMemory.heapUsed / 1024 / 1024),
          increase: Math.round((afterMemory.heapUsed - beforeMemory.heapUsed) / 1024 / 1024),
        });

        // Clean up
        await fs.unlink(outputPath);
      } catch (error) {
        console.error(`Iteration ${i} failed:`, error);
      }
    }

    console.log('Memory usage across iterations:', memoryUsage);

    // Check that memory doesn't continuously increase
    const firstIterationMemory = memoryUsage[0].heapUsed;
    const lastIterationMemory = memoryUsage[iterations - 1].heapUsed;
    const memoryGrowth = lastIterationMemory - firstIterationMemory;

    console.log(`Memory growth over ${iterations} iterations: ${memoryGrowth}MB`);
    
    // Allow some memory growth but flag if it's excessive (>50MB growth)
    expect(memoryGrowth).toBeLessThan(50);
  }, 120000); // 2 minute timeout
});

function generateLargeTestData(routeCount: number) {
  const locations = [];
  const routes = [];
  const sectors = [];

  // Create 10 locations
  for (let i = 1; i <= 10; i++) {
    locations.push({
      id: i,
      name: `Test Location ${i}`,
      country: 'TestCountry',
      isIndoor: i % 2 === 0,
      state: `Test State ${i}`,
      city: `Test City ${i}`,
      address: `${i * 100} Test Street`,
      coordinates: {
        latitude: 40.7128 + (i * 0.01),
        longitude: -74.0060 + (i * 0.01),
      },
      rockType: ['limestone', 'granite', 'sandstone'][i % 3],
      accessInfo: `A test climbing location with many routes for memory testing`,
    });
  }

  // Create sectors (100 sectors distributed across locations)
  let sectorId = 1;
  for (let locId = 1; locId <= 10; locId++) {
    for (let s = 1; s <= 10; s++) {
      sectors.push({
        id: sectorId,
        locationId: locId,
        name: `Sector ${sectorId}`,
        description: `Test sector ${sectorId} with many routes`,
        coordinates: {
          latitude: 40.7128 + (locId * 0.01) + (s * 0.001),
          longitude: -74.0060 + (locId * 0.01) + (s * 0.001),
        },
      });
      sectorId++;
    }
  }

  // Create routes distributed across sectors
  for (let i = 1; i <= routeCount; i++) {
    const sectorId = ((i - 1) % 100) + 1;
    const grade = ['5a', '5b', '5c', '6a', '6a+', '6b', '6b+', '6c', '6c+', '7a'][i % 10];
    
    const routeType = ['boulder', 'route'][i % 2];
    routes.push({
      id: i,
      locationId: Math.floor((sectorId - 1) / 10) + 1, // Map sector to location
      sectorId: sectorId,
      name: `Test Route ${i}`,
      routeType: routeType,
      grades: routeType === 'boulder' ? { vScale: `V${i % 10}` } : { french: grade },
      height: Math.floor(Math.random() * 30) + 10,
      color: `#${((i * 123456) % 16777215).toString(16).padStart(6, '0').toUpperCase()}`,
      beta: `This is test route number ${i} with a fairly long description to simulate real-world data. ` +
            `The route has various characteristics and features that climbers might find interesting. ` +
            `Additional details about the route include its exposure, rock quality, and recommended gear.`,
      qualityRating: (i % 5) + 1,
      tags: [`rockType:${['limestone', 'granite', 'sandstone'][i % 3]}`, `popularity:${i % 5}`],
    });
  }

  return {
    manifest: {
      version: '1.0.0',
      format: 'CLDF',
      platform: 'Desktop',
      appVersion: '1.0.0',
      creationDate: new Date().toISOString(),
    },
    locations,
    routes,
    sectors,
    climbs: [], // Empty climbs array to satisfy archive structure
    sessions: [], // Empty sessions array to satisfy archive structure
    checksums: {
      algorithm: 'SHA-256',
    },
  };
}