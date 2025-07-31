import { Module } from '@nestjs/common';
import { McpServerService } from './mcp-server.service';
import { CldfService } from './cldf.service';
import { ToolHandlersService } from './tool-handlers.service';

@Module({
  providers: [McpServerService, CldfService, ToolHandlersService],
  exports: [McpServerService],
})
export class McpModule {}
