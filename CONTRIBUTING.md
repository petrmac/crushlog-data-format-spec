# Contributing to CLDF Specification

We welcome contributions to the CrushLog Data Format specification! This document provides guidelines for contributing.

## How to Contribute

### Reporting Issues
- Check existing issues before creating a new one
- Provide clear description and examples
- Include version information

### Proposing Changes
1. Open an issue describing the proposed change
2. Discuss with maintainers
3. Submit a pull request after approval

### Pull Request Process
1. Fork the repository
2. Create a feature branch (`feature/your-feature`)
3. Make your changes
4. Update documentation
5. Add/update examples if needed
6. Submit pull request

## Specification Changes

### Backward Compatibility
- Maintain backward compatibility when possible
- Mark deprecated fields clearly
- Provide migration guides for breaking changes

### Version Updates
- PATCH: Documentation fixes, clarifications
- MINOR: New optional fields, enumerations
- MAJOR: Breaking changes, field removals

### Schema Updates
1. Update relevant `.schema.json` files
2. Update documentation
3. Add validation tests
4. Update examples

## Documentation Standards

### Style Guide
- Use clear, concise language
- Provide examples for complex concepts
- Keep formatting consistent
- Use proper markdown syntax

### Code Examples
- Validate all JSON examples
- Use realistic data
- Include comments where helpful

## Testing

### Validation
- All examples must validate against schemas
- Test both valid and invalid cases
- Include edge cases

### Tools
- Use JSON Schema validators
- Automated testing preferred
- Document testing procedures

## Community

### Communication
- Be respectful and inclusive
- Stay on topic
- Help others learn

### Code of Conduct
We follow the [Contributor Covenant](https://www.contributor-covenant.org/) code of conduct.

## License
By contributing, you agree that your contributions will be licensed under the same license as the project (CC BY 4.0).