import { Test, TestingModule } from '@nestjs/testing';
import { CldfService } from './cldf.service';
import { exec } from 'child_process';
import { writeFile, unlink } from 'fs/promises';

jest.mock('child_process');
jest.mock('fs/promises');

describe('CldfService', () => {
  let service: CldfService;
  const mockExec = exec as unknown as jest.Mock;
  const mockWriteFile = writeFile as jest.Mock;
  const mockUnlink = unlink as jest.Mock;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [CldfService],
    }).compile();

    service = module.get<CldfService>(CldfService);
    jest.clearAllMocks();
  });

  describe('executeCommand', () => {
    it('should execute command successfully', async () => {
      mockExec.mockImplementation((command, callback) => {
        callback(null, { stdout: 'output', stderr: '' });
      });

      const result = await service.executeCommand('test command');
      expect(result.stdout).toBe('output');
      expect(result.stderr).toBe('');
    });

    it('should handle command errors', async () => {
      const mockCallback = jest.fn((command, callback) => {
        callback(new Error('Command failed'), '', 'error output');
      });
      mockExec.mockImplementation(mockCallback);

      await expect(service.executeCommand('test command')).rejects.toThrow('Command failed');
    });
  });

  describe('createTempFile', () => {
    it('should create temp file with content', async () => {
      mockWriteFile.mockResolvedValue(undefined);

      const filePath = await service.createTempFile('test content', 'test-prefix');
      
      expect(filePath).toMatch(/test-prefix-\d+\.json$/);
      expect(mockWriteFile).toHaveBeenCalledWith(filePath, 'test content');
    });
  });

  describe('deleteTempFile', () => {
    it('should delete temp file', async () => {
      mockUnlink.mockResolvedValue(undefined);

      await service.deleteTempFile('/tmp/test.json');
      
      expect(mockUnlink).toHaveBeenCalledWith('/tmp/test.json');
    });

    it('should ignore errors when deleting', async () => {
      mockUnlink.mockRejectedValue(new Error('File not found'));

      await expect(service.deleteTempFile('/tmp/test.json')).resolves.toBeUndefined();
    });
  });

  describe('buildCommand', () => {
    it('should build command with various argument types', () => {
      const command = service.buildCommand('test', {
        string: 'value',
        number: 123,
        boolean: true,
        falseBoolean: false,
        nullValue: null,
        undefinedValue: undefined,
      });

      expect(command).toBe('cldf test --string "value" --number "123" --boolean');
      expect(command).not.toContain('falseBoolean');
      expect(command).not.toContain('nullValue');
      expect(command).not.toContain('undefinedValue');
    });
  });

  describe('getCliPath', () => {
    it('should return default CLI path', () => {
      expect(service.getCliPath()).toBe('cldf');
    });

    it('should return custom CLI path from env', () => {
      const originalEnv = process.env.CLDF_CLI;
      process.env.CLDF_CLI = '/custom/path/cldf';
      
      const newService = new CldfService();
      expect(newService.getCliPath()).toBe('/custom/path/cldf');
      
      process.env.CLDF_CLI = originalEnv;
    });
  });
});