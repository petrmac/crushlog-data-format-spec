import { Injectable, Logger } from '@nestjs/common';
import { exec } from 'child_process';
import { promisify } from 'util';
import { writeFile, unlink } from 'fs/promises';
import { tmpdir } from 'os';
import { join } from 'path';

const execAsync = promisify(exec);

@Injectable()
export class CldfService {
  private readonly CLDF_CLI = process.env.CLDF_CLI || 'cldf';
  private readonly logger = new Logger(CldfService.name);

  constructor() {
    this.logger.log(`CLDF CLI path: ${this.CLDF_CLI}`);
  }

  async executeCommand(command: string): Promise<{ stdout: string; stderr: string }> {
    this.logger.debug(`Executing command: ${command}`);
    const startTime = Date.now();
    
    try {
      const result = await execAsync(command);
      const duration = Date.now() - startTime;
      this.logger.debug(`Command completed in ${duration}ms`);
      return result;
    } catch (error) {
      const duration = Date.now() - startTime;
      this.logger.error(`Command failed after ${duration}ms: ${error}`);
      throw error;
    }
  }

  async createTempFile(content: string, prefix: string = 'cldf'): Promise<string> {
    const tempFile = join(tmpdir(), `${prefix}-${Date.now()}.json`);
    this.logger.debug(`Creating temp file: ${tempFile}`);
    await writeFile(tempFile, content);
    return tempFile;
  }

  async deleteTempFile(filePath: string): Promise<void> {
    try {
      await unlink(filePath);
      this.logger.debug(`Deleted temp file: ${filePath}`);
    } catch (error) {
      this.logger.debug(`Failed to delete temp file ${filePath}: ${error}`);
      // Ignore errors when deleting temp files
    }
  }

  getCliPath(): string {
    return this.CLDF_CLI;
  }

  buildCommand(baseCommand: string, args: Record<string, any>): string {
    let command = `${this.CLDF_CLI} ${baseCommand}`;
    
    for (const [key, value] of Object.entries(args)) {
      if (value !== undefined && value !== null) {
        if (typeof value === 'boolean') {
          if (value) {
            command += ` --${key}`;
          }
        } else {
          command += ` --${key} "${value}"`;
        }
      }
    }
    
    return command;
  }
}