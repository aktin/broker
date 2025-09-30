# AKTIN Broker Plugin for Multi-User and OTP Authentication

This module provides user and authentication management for the AKTIN Broker with optional OTP (One-Time Password) support.

## Features
- REST endpoints for login/logout, status, and user management
- File-based user repository with caching
- Default admin user initialized from system properties
- Passwords hashed with PBKDF2 (HMAC-SHA256, salted, 120k iterations)
- Optional Yubico OTP support with client ID and secret key

## Configuration
System properties:
- `aktin.broker.users.file` – Path to user database file (default: `users.txt`)
- `aktin.broker.username` – Default admin username (default: `admin`)
- `aktin.broker.password` – Default admin password (required)
- `aktin.broker.auth.enforce.otp` – Enforce OTP for all users (default: `false`)
- `aktin.broker.auth.token.lifespan` – Session token lifespan in seconds (default: `300`)
- `aktin.broker.auth.yubico.clientId` – Yubico client ID (required for OTP)
- `aktin.broker.auth.yubico.secretKey` – Yubico secret key (required for OTP)

## Usage
1. Build this package
2. Move the created `.jar` to the other `.jar` files of the AKTIN Broker (by default in the `lib/` folder)
3. Add the system property `-Daktin.broker.auth.provider="org.aktin.broker.auth.otp.CredentialTokenAuthProvider"` to enable this authentication module
4. Dont forget to set the other required system properties required by this module!
5. Start the AKTIN Broker
