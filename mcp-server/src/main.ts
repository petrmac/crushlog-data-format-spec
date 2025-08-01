#!/usr/bin/env node
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { McpServerService } from './mcp/mcp-server.service';
import { Logger } from '@nestjs/common';
import { getLoggerConfig } from './config/logger.config';

async function bootstrap() {
  const loggerConfig = getLoggerConfig();

  const app = await NestFactory.createApplicationContext(AppModule, {
    logger: loggerConfig.logLevels,
  });

  const logger = new Logger('MCPServer');

  logger.log('═══════════════════════════════════════════════════════════════');
  logger.log('CLDF MCP Server - Starting up');
  logger.log('═══════════════════════════════════════════════════════════════');
  logger.log(`Environment: ${process.env.NODE_ENV || 'production'}`);
  logger.log(`CLDF CLI: ${process.env.CLDF_CLI || 'cldf (from PATH)'}`);
  logger.log(`Log levels: ${loggerConfig.logLevels.join(', ')}`);
  logger.log(`Process ID: ${process.pid}`);
  logger.log(`Node version: ${process.version}`);
  logger.log('───────────────────────────────────────────────────────────────');

  const mcpService = app.get(McpServerService);
  const transport = new StdioServerTransport();

  await mcpService.connect(transport);

  logger.log('═══════════════════════════════════════════════════════════════');
  logger.log('CLDF MCP server is ready!');
  logger.log('═══════════════════════════════════════════════════════════════');
  logger.log('Communication: stdio (Model Context Protocol)');
  logger.log('Status: Waiting for MCP requests...');

  // Note for MCP: MCP servers don't use HTTP ports, they communicate via stdio
  // All logs go to stderr as stdout is reserved for MCP protocol communication

  if (process.env.NODE_ENV === 'development') {
    logger.verbose('Development mode active - verbose logging enabled');
    logger.debug('Debug logging is available');
  }
}

bootstrap().catch((error) => {
  const logger = new Logger('MCPServer');
  logger.error(
    '═══════════════════════════════════════════════════════════════',
  );
  logger.error('Failed to start CLDF MCP server');
  logger.error(
    '═══════════════════════════════════════════════════════════════',
  );
  logger.error(error);
  process.exit(1);
});
