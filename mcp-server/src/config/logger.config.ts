import { LogLevel } from '@nestjs/common';

export interface LoggerConfig {
  logLevels: LogLevel[];
  timestamp: boolean;
  context: boolean;
}

export const getLoggerConfig = (): LoggerConfig => {
  const isDevelopment = process.env.NODE_ENV === 'development';
  const isDebug = process.env.DEBUG === 'true';
  
  let logLevels: LogLevel[] = ['error', 'warn'];
  
  if (isDevelopment || isDebug) {
    logLevels = ['log', 'debug', 'error', 'warn', 'verbose'];
  }
  
  return {
    logLevels,
    timestamp: true,
    context: true,
  };
};

export const formatLogMessage = (message: string, context?: string): string => {
  const timestamp = new Date().toISOString();
  const prefix = context ? `[${context}]` : '';
  return `${timestamp} ${prefix} ${message}`;
};