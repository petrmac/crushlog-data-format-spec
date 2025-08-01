import { Test, TestingModule } from '@nestjs/testing';
import { AppModule } from '../src/app.module';
import { McpServerService } from '../src/mcp/mcp-server.service';

describe('MCP Server (e2e)', () => {
  let app: TestingModule;
  let mcpService: McpServerService;

  beforeEach(async () => {
    app = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    mcpService = app.get<McpServerService>(McpServerService);
  });

  afterEach(async () => {
    await app.close();
  });

  it('should initialize MCP server service', () => {
    expect(mcpService).toBeDefined();
  });

  it('should have connect method', () => {
    expect(mcpService.connect).toBeDefined();
    expect(typeof mcpService.connect).toBe('function');
  });
});
