import { Test, TestingModule } from '@nestjs/testing';
import { ToolHandlersService } from './tool-handlers.service';
import { CldfService } from './cldf.service';

describe('ToolHandlersService', () => {
  let service: ToolHandlersService;
  let cldfService: jest.Mocked<CldfService>;

  beforeEach(async () => {
    const mockCldfService = {
      getCliPath: jest.fn().mockReturnValue('cldf'),
      executeCommand: jest.fn(),
      createTempFile: jest.fn(),
      deleteTempFile: jest.fn(),
      buildCommand: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ToolHandlersService,
        {
          provide: CldfService,
          useValue: mockCldfService,
        },
      ],
    }).compile();

    service = module.get<ToolHandlersService>(ToolHandlersService);
    cldfService = module.get(CldfService) as jest.Mocked<CldfService>;
  });

  describe('handleToolCall', () => {
    it('should handle unknown tool error', async () => {
      await expect(service.handleToolCall('unknown_tool', {})).rejects.toThrow(
        'Unknown tool: unknown_tool',
      );
    });

    it('should handle cldf_schema_info tool', async () => {
      const mockResponse = {
        stdout: JSON.stringify({ success: true, data: { test: 'schema' } }),
        stderr: '',
      };
      cldfService.executeCommand.mockResolvedValue(mockResponse);

      const result = await service.handleToolCall('cldf_schema_info', {
        component: 'all',
      });

      expect(cldfService.executeCommand).toHaveBeenCalledWith(
        'cldf schema --component all --json json',
      );
      expect(result.content[0].text).toContain('"test": "schema"');
    });

    it('should handle cldf_validate_data tool', async () => {
      const testData = { manifest: {}, locations: [] };
      cldfService.createTempFile.mockResolvedValue('/tmp/test.json');
      cldfService.executeCommand.mockResolvedValue({ stdout: '', stderr: '' });

      const result = await service.handleToolCall('cldf_validate_data', {
        data: testData,
      });

      expect(cldfService.createTempFile).toHaveBeenCalledWith(
        JSON.stringify(testData),
        'cldf-validate',
      );
      expect(result.content[0].text).toContain('"valid": true');
    });

    it('should handle cldf_create tool', async () => {
      cldfService.executeCommand.mockResolvedValue({
        stdout: 'Archive created',
        stderr: '',
      });

      const result = await service.handleToolCall('cldf_create', {
        template: 'basic',
        outputPath: '/tmp/test.cldf',
      });

      expect(cldfService.executeCommand).toHaveBeenCalledWith(
        'cldf create --template basic --output "/tmp/test.cldf" --json json',
      );
      expect(result.content[0].text).toBe('Archive created');
    });

    it('should handle cldf_validate tool', async () => {
      const mockResponse = {
        stdout: JSON.stringify({ valid: true }),
        stderr: '',
      };
      cldfService.executeCommand.mockResolvedValue(mockResponse);

      const result = await service.handleToolCall('cldf_validate', {
        filePath: '/tmp/test.cldf',
        strict: true,
      });

      expect(cldfService.executeCommand).toHaveBeenCalledWith(
        'cldf validate "/tmp/test.cldf" --json json --strict',
      );
      expect(result.content[0].text).toContain('"valid": true');
    });

    it('should handle cldf_query tool', async () => {
      const mockResponse = { stdout: JSON.stringify({ data: [] }), stderr: '' };
      cldfService.executeCommand.mockResolvedValue(mockResponse);

      const result = await service.handleToolCall('cldf_query', {
        filePath: '/tmp/test.cldf',
        dataType: 'locations',
      });

      expect(cldfService.executeCommand).toHaveBeenCalledWith(
        'cldf query "/tmp/test.cldf" --select locations --json json',
      );
      expect(result.content[0].text).toContain('"data": []');
    });

    it('should handle cldf_merge tool', async () => {
      cldfService.executeCommand.mockResolvedValue({ stdout: '', stderr: '' });

      const result = await service.handleToolCall('cldf_merge', {
        files: ['/tmp/file1.cldf', '/tmp/file2.cldf'],
        outputPath: '/tmp/merged.cldf',
        strategy: 'merge',
      });

      expect(cldfService.executeCommand).toHaveBeenCalledWith(
        'cldf merge "/tmp/file1.cldf" "/tmp/file2.cldf" --output "/tmp/merged.cldf" --strategy merge --json json',
      );
      expect(result.content[0].text).toBe(
        'Archives merged successfully to /tmp/merged.cldf',
      );
    });

    it('should handle cldf_convert tool', async () => {
      cldfService.executeCommand.mockResolvedValue({ stdout: '', stderr: '' });

      const result = await service.handleToolCall('cldf_convert', {
        filePath: '/tmp/test.cldf',
        format: 'json',
        outputPath: '/tmp/output.json',
      });

      expect(cldfService.executeCommand).toHaveBeenCalledWith(
        'cldf convert "/tmp/test.cldf" --format json --output "/tmp/output.json" --json json',
      );
      expect(result.content[0].text).toBe(
        'Archive converted successfully to /tmp/output.json',
      );
    });

    it('should handle cldf_extract tool', async () => {
      const mockResponse = {
        stdout: JSON.stringify({ extracted: 'data' }),
        stderr: '',
      };
      cldfService.executeCommand.mockResolvedValue(mockResponse);

      const result = await service.handleToolCall('cldf_extract', {
        filePath: '/tmp/test.cldf',
        dataType: 'locations',
        outputDir: '/tmp/extract',
      });

      expect(cldfService.executeCommand).toHaveBeenCalledWith(
        'cldf extract "/tmp/test.cldf" --type locations --json json --output "/tmp/extract"',
      );
      expect(result.content[0].text).toContain('"extracted": "data"');
    });
  });

  describe('error handling', () => {
    it('should handle validation errors with enhanced message', async () => {
      cldfService.createTempFile.mockResolvedValue('/tmp/test.json');
      cldfService.executeCommand.mockResolvedValue({
        stdout: '',
        stderr: 'validation failed: missing required field',
      });

      await expect(
        service.handleToolCall('cldf_create', {
          template: 'basic',
          outputPath: '/tmp/test.cldf',
          data: { invalid: 'data' },
        }),
      ).rejects.toThrow('CLDF Archive Creation Failed');
    });

    it('should handle command execution errors', async () => {
      cldfService.executeCommand.mockRejectedValue(
        new Error('Command not found'),
      );

      await expect(
        service.handleToolCall('cldf_schema_info', {}),
      ).rejects.toThrow(
        'Failed to retrieve schema information: Command not found',
      );
    });
  });
});
