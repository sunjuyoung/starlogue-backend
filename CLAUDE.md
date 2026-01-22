# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Starlogue is a Spring Boot 3.5.9 application using Java 21. It follows a layered architecture pattern with Spring MVC for web/REST services and Spring Data JPA for database access.

## Build Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.starlogue.StarlogueApplicationTests"

# Clean build artifacts
./gradlew clean

# On Windows, use gradlew.bat instead of ./gradlew
```

## Architecture

The project uses Spring's layered architecture:

- **controller/** - REST/Web controllers handling HTTP requests
- **service/** - Business logic layer
- **repository/** - Spring Data JPA interfaces for data access
- **domain/** - Entity/Domain models with JPA annotations
- **config/** - Spring configuration classes

Entry point: `StarlogueApplication.java`

## Key Dependencies

- **Spring Boot Starter Web** - REST API support
- **Spring Boot Starter Data JPA** - Database access via Hibernate
- **Lombok** - Annotation-based boilerplate reduction (@Data, @Builder, etc.)

## Current State
 domain.md, entity.md , api.md, service.md   참조