import { Test, TestingModule } from '@nestjs/testing';
import { McpServerService } from './mcp-server.service';
import { ToolHandlersService } from './tool-handlers.service';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { TOOL_DEFINITIONS } from './tools.constants';

jest.mock('@modelcontextprotocol/sdk/server/index.js');

describe('McpServerService', () => {
  let service: McpServerService;
  let toolHandlers: jest.Mocked<ToolHandlersService>;
  let mockServer: jest.Mocked<Server>;

  beforeEach(async () => {
    mockServer = {
      setRequestHandler: jest.fn(),
      connect: jest.fn(),
    } as any;

    (Server as jest.MockedClass<typeof Server>).mockImplementation(() => mockServer);

    const mockToolHandlers = {
      handleToolCall: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        McpServerService,
        {
          provide: ToolHandlersService,
          useValue: mockToolHandlers,
        },
      ],
    }).compile();

    service = module.get<McpServerService>(McpServerService);
    toolHandlers = module.get(ToolHandlersService) as jest.Mocked<ToolHandlersService>;
  });

  describe('initialization', () => {
    it('should create server with correct configuration', () => {
      expect(Server).toHaveBeenCalledWith(
        {
          name: 'cldf-tools',
          version: '1.0.0',
        },
        {
          capabilities: {
            tools: {},
          },
        }
      );
    });

    it('should setup request handlers', () => {
      expect(mockServer.setRequestHandler).toHaveBeenCalledTimes(2);
    });
  });

  describe('connect', () => {
    it('should connect to transport', async () => {
      const mockTransport = {};
      await service.connect(mockTransport);
      
      expect(mockServer.connect).toHaveBeenCalledWith(mockTransport);
    });
  });

  describe('request handlers', () => {
    it('should handle ListToolsRequest', async () => {
      const listToolsHandler = mockServer.setRequestHandler.mock.calls[0][1];
      const mockExtra = { signal: new AbortController().signal };
      const result = await listToolsHandler({}, mockExtra);
      
      expect(result).toEqual({ tools: TOOL_DEFINITIONS });
    });

    it('should handle CallToolRequest successfully', async () => {
      const callToolHandler = mockServer.setRequestHandler.mock.calls[1][1];
      const mockResult = { content: [{ type: 'text', text: 'Success' }] };
      const mockExtra = { signal: new AbortController().signal };
      
      toolHandlers.handleToolCall.mockResolvedValue(mockResult);
      
      const result = await callToolHandler({
        method: 'tools/call',
        params: { name: 'cldf_validate', arguments: { filePath: '/tmp/test.cldf' } }
      } as any, mockExtra);
      
      expect(toolHandlers.handleToolCall).toHaveBeenCalledWith('cldf_validate', { filePath: '/tmp/test.cldf' });
      expect(result).toEqual(mockResult);
    });

    it('should handle CallToolRequest errors', async () => {
      const callToolHandler = mockServer.setRequestHandler.mock.calls[1][1];
      const mockExtra = { signal: new AbortController().signal };
      
      toolHandlers.handleToolCall.mockRejectedValue(new Error('Tool error'));
      
      const result = await callToolHandler({
        method: 'tools/call',
        params: { name: 'cldf_validate', arguments: {} }
      } as any, mockExtra);
      
      expect(result).toEqual({
        content: [
          {
            type: 'text',
            text: 'Error: Tool error',
          },
        ],
      });
    });
  });
});