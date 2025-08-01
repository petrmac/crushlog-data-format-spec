export const TOOL_DEFINITIONS = [
  {
    name: 'cldf_schema_info',
    description:
      'Get CLDF data structure schema and validation rules to help understand the expected format',
    inputSchema: {
      type: 'object',
      properties: {
        component: {
          type: 'string',
          enum: [
            'all',
            'manifest',
            'location',
            'route',
            'sector',
            'climb',
            'session',
            'tag',
            'dateFormats',
            'enums',
            'commonMistakes',
            'exampleData',
            'fieldReference',
          ],
          description:
            'Specific component schema to retrieve, or "all" for complete schema information',
          default: 'all',
        },
      },
    },
  },
  {
    name: 'cldf_validate_data',
    description:
      'Validate CLDF data structure before creating an archive. Helps identify issues early.',
    inputSchema: {
      type: 'object',
      properties: {
        data: {
          type: 'object',
          description: 'JSON data to validate against CLDF schema',
        },
      },
      required: ['data'],
    },
  },
  {
    name: 'cldf_create',
    description:
      'Create a new CLDF archive with climbing data. Archives can contain just locations and routes, or full session/climb data. Use cldf_schema_info first to understand data structure requirements.',
    inputSchema: {
      type: 'object',
      properties: {
        template: {
          type: 'string',
          enum: ['basic', 'demo', 'empty'],
          description: 'Template to use for creating the archive',
        },
        outputPath: {
          type: 'string',
          description: 'Path where the CLDF archive will be saved',
        },
        data: {
          type: 'object',
          description:
            'Optional JSON data to include in the archive. Must match CLDF schema - use cldf_schema_info to understand structure. Minimum required: manifest, locations. Sessions and climbs are optional.',
        },
      },
      required: ['template', 'outputPath'],
    },
  },
  {
    name: 'cldf_validate',
    description: 'Validate a CLDF archive file',
    inputSchema: {
      type: 'object',
      properties: {
        filePath: {
          type: 'string',
          description: 'Path to the CLDF archive to validate',
        },
        strict: {
          type: 'boolean',
          description: 'Use strict validation',
          default: false,
        },
      },
      required: ['filePath'],
    },
  },
  {
    name: 'cldf_query',
    description: 'Query data from a CLDF archive',
    inputSchema: {
      type: 'object',
      properties: {
        filePath: {
          type: 'string',
          description: 'Path to the CLDF archive',
        },
        dataType: {
          type: 'string',
          enum: ['locations', 'climbs', 'sessions', 'media', 'all'],
          description: 'Type of data to query',
        },
        filter: {
          type: 'string',
          description: 'Optional filter expression',
        },
      },
      required: ['filePath', 'dataType'],
    },
  },
  {
    name: 'cldf_merge',
    description: 'Merge multiple CLDF archives',
    inputSchema: {
      type: 'object',
      properties: {
        files: {
          type: 'array',
          items: { type: 'string' },
          description: 'List of CLDF archive paths to merge',
        },
        outputPath: {
          type: 'string',
          description: 'Path for the merged archive',
        },
        strategy: {
          type: 'string',
          enum: ['replace', 'merge', 'append'],
          default: 'merge',
        },
      },
      required: ['files', 'outputPath'],
    },
  },
  {
    name: 'cldf_convert',
    description: 'Convert CLDF archive to different formats',
    inputSchema: {
      type: 'object',
      properties: {
        filePath: {
          type: 'string',
          description: 'Path to the CLDF archive',
        },
        format: {
          type: 'string',
          enum: ['json', 'csv', 'yaml'],
          description: 'Output format',
        },
        outputPath: {
          type: 'string',
          description: 'Path for the converted file',
        },
      },
      required: ['filePath', 'format', 'outputPath'],
    },
  },
  {
    name: 'cldf_extract',
    description: 'Extract specific data from a CLDF archive',
    inputSchema: {
      type: 'object',
      properties: {
        filePath: {
          type: 'string',
          description: 'Path to the CLDF archive',
        },
        dataType: {
          type: 'string',
          enum: [
            'manifest',
            'locations',
            'routes',
            'sectors',
            'climbs',
            'sessions',
            'tags',
            'media',
          ],
          description: 'Type of data to extract',
        },
        outputDir: {
          type: 'string',
          description: 'Directory to extract data to',
        },
      },
      required: ['filePath', 'dataType'],
    },
  },
  {
    name: 'cldf_query_media',
    description: 'Query media information from a CLDF archive, including metadata and embedded file details',
    inputSchema: {
      type: 'object',
      properties: {
        filePath: {
          type: 'string',
          description: 'Path to the CLDF archive',
        },
        includeEmbedded: {
          type: 'boolean',
          description: 'Include information about embedded media files',
          default: true,
        },
        mediaType: {
          type: 'string',
          enum: ['photo', 'video', 'all'],
          description: 'Filter by media type',
          default: 'all',
        },
      },
      required: ['filePath'],
    },
  },
  {
    name: 'cldf_extract_media',
    description: 'Extract media files from a CLDF archive to a directory',
    inputSchema: {
      type: 'object',
      properties: {
        filePath: {
          type: 'string',
          description: 'Path to the CLDF archive',
        },
        outputDir: {
          type: 'string',
          description: 'Directory to extract media files to',
        },
        preserveStructure: {
          type: 'boolean',
          description: 'Preserve the original directory structure',
          default: true,
        },
      },
      required: ['filePath', 'outputDir'],
    },
  },
];
