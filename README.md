# ThesisCode - Centralized Ledger System

## About

This project was developed as a proof-of-concept centralized ledger system designed for enterprise integration. It demonstrates a secure, tamper-evident record-keeping solution that can be adapted to various company needs and existing infrastructure.

## Purpose

The system provides a centralized ledger with robust security features and tamper detection capabilities. It serves as a reference implementation for companies looking to implement secure document and transaction management systems that can integrate with their existing infrastructure.

## Architecture

### Server (Kotlin + Maven)
- **Design Pattern**: Controller → Service → Repository
- **Security Features**:
  - Basic Authentication
  - Two-Factor Authentication (2FA)
  - Public Key Infrastructure (PKI)
  - Cryptographic providers with modular implementations
- **Storage**: 
  - JDBC for SQL database interactions
  - File system storage (proof-of-concept; production systems should implement secure storage interfaces)
- **Special Services**:
  - **Warden Service**: Creates detailed tamper detection reports for all ledgers
  - **EDMS Emulation**: Basic Electronic Document Management System for demonstration purposes
- **Architecture Principles**:
  - Interface-based design for low coupling and high modularity
  - Provider pattern for easy addition/removal of cryptographic functions
  - Minimal restrictions to allow company-specific customization

### Client (TypeScript + Next.js)
- **Design Pattern**: MVVM (Model-View-ViewModel)
- **Purpose**: Reference implementation for making server requests and presenting data
- Simple, clean interface designed as a template for custom client development

## Features

- Centralized ledger with full attribute management
- Tamper detection and detailed reporting via Warden service
- Modular cryptographic provider system
- PKI-based security infrastructure
- Two-factor authentication
- Basic EDMS functionality
- Interface-driven architecture for easy integration
- Reference client implementation

## Prerequisites

Before running this application, ensure you have:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) or Docker Engine
- Docker Compose

## Getting Started

### Quick Start

The application includes a convenient batch script with multiple startup modes:

1. **Clone the repository:**
```bash
   git clone https://github.com/NunoBartolomeu/ThesisCode.git
   cd ThesisCode
```

2. **Ensure Docker is running**

3. **Start the application:**

   **Regular start** (preserves data):
```bash
   execute.bat
```

   **Clean start** (wipes data and recompiles):
```bash
   execute.bat clean
```

   **Stop containers**:
```bash
   execute.bat stop
```

### What the Script Does

- **Regular Start**: Starts all services in detached mode, preserving existing data
- **Clean Start**: Removes all volumes and orphaned containers, rebuilds images, and starts fresh
- **Stop**: Gracefully stops all running containers

The script automatically displays running containers after startup (unless stopping).

## Usage

Once running, the system consists of:
- **Server API**: Kotlin-based backend handling ledger operations, authentication, and security
- **Client Interface**: Next.js web application for interacting with the ledger

Access the client at: `http://localhost:[PORT]` (check `docker compose ps` output for specific ports)

## Technologies Used

**Server:**
- Kotlin
- Maven
- JDBC (SQL database)
- PKI/Cryptographic providers

**Client:**
- TypeScript
- Next.js
- MVVM pattern

**Infrastructure:**
- Docker & Docker Compose

## Integration Notes

This is a proof-of-concept system designed for flexibility:

- **Modular Design**: Cryptographic functions can be easily added or removed via the provider system
- **Interface-Based**: Low dependency architecture allows companies to implement custom storage, authentication, and integration solutions
- **Customizable**: Minimal restrictions enable adaptation to specific company requirements
- **Reference Implementation**: The client serves as a template for building custom interfaces

⚠️ **Production Considerations**: Companies should implement secure storage solutions (replacing file system storage) and customize the system according to their security and compliance requirements.

## Author

Nuno Bartolomeu

## License

Academic/Research Purpose
