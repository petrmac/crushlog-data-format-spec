import { execSync } from 'child_process';
import * as fs from 'fs';

export function isCldfCliAvailable(): boolean {
  const cliPath = process.env.CLDF_CLI;

  if (!cliPath) {
    return false;
  }

  try {
    // Check if the file exists
    if (!fs.existsSync(cliPath)) {
      return false;
    }

    // Check if it's executable
    fs.accessSync(cliPath, fs.constants.X_OK);

    // Try to run version command
    execSync(`${cliPath} --version`, { stdio: 'ignore' });

    return true;
  } catch (error) {
    return false;
  }
}

export function skipIfNoCldfCli() {
  if (!isCldfCliAvailable()) {
    console.warn('CLDF CLI not available, skipping tests that require it');
    return true;
  }
  return false;
}
