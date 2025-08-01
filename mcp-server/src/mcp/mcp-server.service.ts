import { Injectable, Logger } from '@nestjs/common';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { ToolHandlersService } from './tool-handlers.service';
import { TOOL_DEFINITIONS } from './tools.constants';

@Injectable()
export class McpServerService {
  private server: Server;
  private readonly logger = new Logger(McpServerService.name);

  constructor(private readonly toolHandlers: ToolHandlersService) {
    this.logger.log('Initializing MCP server...');

    this.server = new Server(
      {
        name: 'cldf-tools',
        version: '1.0.0',
      },
      {
        capabilities: {
          tools: {},
        },
      },
    );

    this.setupHandlers();
    this.logger.log(`Registered ${TOOL_DEFINITIONS.length} tools`);
  }

  private setupHandlers() {
    // Define available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      this.logger.debug('Received ListTools request');
      return {
        tools: TOOL_DEFINITIONS,
      };
    });

    // Handle tool calls
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const { name, arguments: args } = request.params;
      this.logger.log(`Received tool call: ${name}`);
      this.logger.debug(`Arguments: ${JSON.stringify(args)}`);

      try {
        const result = await this.toolHandlers.handleToolCall(name, args);
        this.logger.log(`Tool ${name} completed successfully`);
        return result;
      } catch (error) {
        this.logger.error(`Tool ${name} failed:`, error);
        return {
          content: [
            {
              type: 'text',
              text: `Error: ${error instanceof Error ? error.message : String(error)}`,
            },
          ],
        };
      }
    });
  }

  async connect(transport: any) {
    this.logger.log('Connecting to MCP transport...');
    await this.server.connect(transport);
    this.logger.log('MCP transport connected');
  }
}
