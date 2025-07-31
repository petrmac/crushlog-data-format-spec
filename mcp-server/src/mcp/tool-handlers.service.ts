import { Injectable, Logger } from '@nestjs/common';
import { CldfService } from './cldf.service';
import { join } from 'path';
import { tmpdir } from 'os';
import { SCHEMA_EXAMPLES, COMMON_MISTAKES, FIELD_REFERENCE } from './schema-examples';

@Injectable()
export class ToolHandlersService {
  private readonly logger = new Logger(ToolHandlersService.name);

  constructor(private readonly cldfService: CldfService) {}

  async handleToolCall(name: string, args: any): Promise<any> {
    this.logger.log(`Handling tool: ${name}`);
    const startTime = Date.now();
    
    try {
      let result;
      switch (name) {
        case 'cldf_schema_info':
          result = await this.handleSchemaInfo(args);
          break;
        case 'cldf_validate_data':
          result = await this.handleValidateData(args);
          break;
        case 'cldf_create':
          result = await this.handleCreate(args);
          break;
        case 'cldf_validate':
          result = await this.handleValidate(args);
          break;
        case 'cldf_query':
          result = await this.handleQuery(args);
          break;
        case 'cldf_merge':
          result = await this.handleMerge(args);
          break;
        case 'cldf_convert':
          result = await this.handleConvert(args);
          break;
        case 'cldf_extract':
          result = await this.handleExtract(args);
          break;
        default:
          throw new Error(`Unknown tool: ${name}`);
      }
      
      const duration = Date.now() - startTime;
      this.logger.log(`Tool ${name} completed in ${duration}ms`);
      return result;
    } catch (error) {
      const duration = Date.now() - startTime;
      this.logger.error(`Tool ${name} failed after ${duration}ms`, error);
      throw error;
    }
  }

  private async handleSchemaInfo(args: any) {
    const { component = 'all' } = args;
    
    // Handle special components for better AI guidance
    if (component === 'exampleData') {
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(SCHEMA_EXAMPLES, null, 2),
          },
        ],
      };
    }
    
    if (component === 'commonMistakes') {
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(COMMON_MISTAKES, null, 2),
          },
        ],
      };
    }
    
    if (component === 'fieldReference') {
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(FIELD_REFERENCE, null, 2),
          },
        ],
      };
    }
    
    const command = `${this.cldfService.getCliPath()} schema --component ${component} --json json`;
    
    try {
      const { stdout, stderr } = await this.cldfService.executeCommand(command);
      
      if (stderr && !stdout) {
        throw new Error(stderr);
      }
      
      let result;
      try {
        result = JSON.parse(stdout);
        if (result.success && result.data) {
          result = result.data;
        }
      } catch {
        throw new Error('Failed to parse schema information from CLDF tool');
      }
      
      // Add helpful context for AI agents
      if (component === 'all') {
        result._aiHints = {
          quickStart: "Use component='exampleData' for working examples",
          validation: "Use component='commonMistakes' to avoid errors",
          reference: "Use component='fieldReference' for quick field lookup"
        };
      }
      
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result, null, 2),
          },
        ],
      };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      throw new Error(`Failed to retrieve schema information: ${errorMessage}`);
    }
  }

  private async handleValidateData(args: any) {
    const { data } = args;
    
    if (!data || typeof data !== 'object') {
      throw new Error('Data must be a valid JSON object');
    }
    
    const tempDataFile = await this.cldfService.createTempFile(JSON.stringify(data), 'cldf-validate');
    const tempArchiveFile = join(tmpdir(), `cldf-validate-${Date.now()}.cldf`);
    
    try {
      const createCommand = `${this.cldfService.getCliPath()} create --template basic --output "${tempArchiveFile}" --from-json "${tempDataFile}" --json json`;
      
      try {
        const { stdout: createStdout, stderr: createStderr } = await this.cldfService.executeCommand(createCommand);
        
        if (createStderr && createStderr.includes('validation failed')) {
          return {
            content: [
              {
                type: 'text',
                text: JSON.stringify({
                  valid: false,
                  source: 'create_validation',
                  errors: [createStderr],
                  message: 'Data validation failed during CLDF archive creation',
                  suggestion: 'Use cldf_schema_info to understand the expected structure'
                }, null, 2),
              },
            ],
          };
        }
        
        const validateCommand = `${this.cldfService.getCliPath()} validate "${tempArchiveFile}" --json json`;
        const { stdout: validateStdout } = await this.cldfService.executeCommand(validateCommand);
        
        let result;
        if (validateStdout) {
          try {
            result = JSON.parse(validateStdout);
            result.source = 'binary_validation';
          } catch {
            result = {
              valid: true,
              source: 'binary_validation',
              message: validateStdout
            };
          }
        } else {
          result = {
            valid: true,
            source: 'binary_validation',
            message: 'Archive created and validated successfully'
          };
        }
        
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
        
      } catch (createError) {
        const errorMessage = createError instanceof Error ? createError.message : String(createError);
        
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                valid: false,
                source: 'binary_validation',
                errors: [errorMessage],
                message: 'Data validation failed',
                suggestion: 'Check data structure against CLDF schema using cldf_schema_info'
              }, null, 2),
            },
          ],
        };
      }
      
    } finally {
      await this.cldfService.deleteTempFile(tempDataFile);
      await this.cldfService.deleteTempFile(tempArchiveFile);
    }
  }

  private async handleCreate(args: any) {
    const { template, outputPath, data } = args;
    
    this.logger.debug(`Creating CLDF archive: template=${template}, output=${outputPath}`);
    
    let command = `${this.cldfService.getCliPath()} create --template ${template} --output "${outputPath}" --json json`;
    
    if (data) {
      this.logger.debug('Creating archive with custom data');
      const tempFile = await this.cldfService.createTempFile(JSON.stringify(data), 'cldf-data');
      command += ` --from-json "${tempFile}"`;
      
      try {
        const { stdout, stderr } = await this.cldfService.executeCommand(command);
        await this.cldfService.deleteTempFile(tempFile);
        
        if (stderr && stderr.includes('validation failed')) {
          this.logger.warn('CLDF validation failed during creation');
          const enhancedError = `
CLDF Archive Creation Failed - Validation Errors:

${stderr}

💡 Common Issues & Solutions:
- Use cldf_schema_info to understand the expected data structure
- Minimum required: manifest and at least one location
- Sessions and climbs are optional - archives can contain just locations/routes
- Check that all required fields are present
- Verify enum values match allowed options
- Ensure ID types are correct (location.id = integer, route.id = integer, sector.id = integer)
- Location now supports city and address fields (both optional)
- Date formats are flexible but must include timezone for OffsetDateTime fields
- Route grades must match pattern (e.g., French: 5c, 6a+, 7b)
- Colors must be hex format (#RRGGBB)

Use cldf_schema_info with component="commonMistakes" for more details.
`;
          throw new Error(enhancedError);
        } else if (stderr) {
          throw new Error(stderr);
        }
        
        return {
          content: [
            {
              type: 'text',
              text: stdout || `CLDF archive created successfully at ${outputPath}`,
            },
          ],
        };
      } catch (error) {
        await this.cldfService.deleteTempFile(tempFile);
        throw error;
      }
    } else {
      const { stdout, stderr } = await this.cldfService.executeCommand(command);
      
      if (stderr) {
        throw new Error(stderr);
      }
      
      return {
        content: [
          {
            type: 'text',
            text: stdout || `CLDF archive created successfully at ${outputPath}`,
          },
        ],
      };
    }
  }

  private async handleValidate(args: any) {
    const { filePath, strict } = args;
    
    const command = `${this.cldfService.getCliPath()} validate "${filePath}" --json json ${strict ? '--strict' : ''}`;
    const { stdout, stderr } = await this.cldfService.executeCommand(command);
    
    if (stderr && !stdout) {
      throw new Error(stderr);
    }
    
    try {
      const result = JSON.parse(stdout);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result, null, 2),
          },
        ],
      };
    } catch {
      return {
        content: [
          {
            type: 'text',
            text: stdout,
          },
        ],
      };
    }
  }

  private async handleQuery(args: any) {
    const { filePath, dataType, filter } = args;
    
    let command = `${this.cldfService.getCliPath()} query "${filePath}" --select ${dataType} --json json`;
    if (filter) {
      command += ` --filter "${filter}"`;
    }
    
    const { stdout, stderr } = await this.cldfService.executeCommand(command);
    
    if (stderr && !stdout) {
      throw new Error(stderr);
    }
    
    try {
      const result = JSON.parse(stdout);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result, null, 2),
          },
        ],
      };
    } catch {
      return {
        content: [
          {
            type: 'text',
            text: stdout,
          },
        ],
      };
    }
  }

  private async handleMerge(args: any) {
    const { files, outputPath, strategy } = args;
    
    const fileArgs = files.map((f: string) => `"${f}"`).join(' ');
    const command = `${this.cldfService.getCliPath()} merge ${fileArgs} --output "${outputPath}" --strategy ${strategy} --json json`;
    
    const { stdout, stderr } = await this.cldfService.executeCommand(command);
    
    if (stderr && !stdout) {
      throw new Error(stderr);
    }
    
    return {
      content: [
        {
          type: 'text',
          text: stdout || `Archives merged successfully to ${outputPath}`,
        },
      ],
    };
  }

  private async handleConvert(args: any) {
    const { filePath, format, outputPath } = args;
    
    const command = `${this.cldfService.getCliPath()} convert "${filePath}" --format ${format} --output "${outputPath}" --json json`;
    const { stdout, stderr } = await this.cldfService.executeCommand(command);
    
    if (stderr && !stdout) {
      throw new Error(stderr);
    }
    
    return {
      content: [
        {
          type: 'text',
          text: stdout || `Archive converted successfully to ${outputPath}`,
        },
      ],
    };
  }

  private async handleExtract(args: any) {
    const { filePath, dataType, outputDir } = args;
    
    let command = `${this.cldfService.getCliPath()} extract "${filePath}" --type ${dataType} --json json`;
    if (outputDir) {
      command += ` --output "${outputDir}"`;
    }
    
    const { stdout, stderr } = await this.cldfService.executeCommand(command);
    
    if (stderr && !stdout) {
      throw new Error(stderr);
    }
    
    try {
      const result = JSON.parse(stdout);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result, null, 2),
          },
        ],
      };
    } catch {
      return {
        content: [
          {
            type: 'text',
            text: stdout,
          },
        ],
      };
    }
  }
}