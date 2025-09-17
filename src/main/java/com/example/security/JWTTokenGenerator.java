package com.example.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * JWT Token Generator - Java Version
 * Generates JWT tokens using the exact same secret key as your application
 */
public class JWTTokenGenerator {

    // Your exact Base64 secret key

    // Default expiration: 24 hours in seconds
    private static final long DEFAULT_EXPIRATION_SECONDS = 8640000;

    private final SecretKey secretKey;

    public JWTTokenGenerator(String base64_secret) {
        // Decode the Base64 secret and create SecretKey
        byte[] keyBytes = Base64.getDecoder().decode(base64_secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT token with username and default expiration (24 hours)
     */
    public String generateToken(String username) {
        return generateToken(username, DEFAULT_EXPIRATION_SECONDS);
    }

    /**
     * Generate JWT token with username and custom expiration
     */
    public String generateToken(String username, long expirationSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (expirationSeconds * 1000));

        return Jwts.builder()
                .setSubject(username)
                .claim("role", "ADMIN")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate JWT token with custom claims
     */
    public String generateTokenWithClaims(String username, Map<String, Object> claims, long expirationSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (expirationSeconds * 1000));

        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("role", "ADMIN")
                .setIssuedAt(now)
                .setExpiration(expiry);



        return builder.signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    /**
     * Validate and parse JWT token
     */
    public Claims validateAndParseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            System.err.println("❌ Token has expired: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Token validation failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Display token information
     */
    public void displayTokenInfo(String token) {
        try {
            // Parse without validation to show structure
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.err.println("❌ Invalid JWT structure");
                return;
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("🎫 JWT TOKEN INFORMATION");
            System.out.println("=".repeat(60));

            // Decode header
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            System.out.println("📋 Header: " + headerJson);

            // Decode payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            System.out.println("📦 Payload: " + payloadJson);

            // Signature info
            System.out.println("🔐 Signature: " + parts[2] + " (" + parts[2].length() + " chars)");

            // Token stats
            System.out.println("\n📊 Token Statistics:");
            System.out.println("   • Total Length: " + token.length() + " characters");
            System.out.println("   • Header Length: " + parts[0].length() + " chars");
            System.out.println("   • Payload Length: " + parts[1].length() + " chars");
            System.out.println("   • Signature Length: " + parts[2].length() + " chars");

            // Validate and show claims
            Claims claims = validateAndParseToken(token);
            System.out.println("\n✅ Token Validation: SUCCESS");
            System.out.println("   • Subject: " + claims.getSubject());
            System.out.println("   • Issued At: " + claims.getIssuedAt());
            System.out.println("   • Expires At: " + claims.getExpiration());
            System.out.println("   • Is Expired: " + (claims.getExpiration().before(new Date()) ? "Yes" : "No"));

            // Custom claims
            claims.forEach((key, value) -> {
                if (!"sub".equals(key) && !"iat".equals(key) && !"exp".equals(key)) {
                    System.out.println("   • " + key + ": " + value);
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Error analyzing token: " + e.getMessage());
        }
    }

    /**
     * Display configuration information
     */
    public void displayConfig(String base64_secret) {
        byte[] keyBytes = Base64.getDecoder().decode(base64_secret);
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔧 JWT CONFIGURATION");
        System.out.println("=".repeat(60));
        System.out.println("📋 Algorithm: HS256");
        System.out.println("🔑 Secret Key (Base64): " + base64_secret);
        System.out.println("📏 Key Length: " + keyBytes.length + " bytes (" + (keyBytes.length * 8) + " bits)");
        System.out.println("⏰ Default Expiration: " + DEFAULT_EXPIRATION_SECONDS + " seconds (" + (DEFAULT_EXPIRATION_SECONDS / 3600) + " hours)");
        System.out.println("\n📝 Application Properties:");
        System.out.println("   jwt.secret=" + base64_secret);
        System.out.println("   jwt.expiration=" + DEFAULT_EXPIRATION_SECONDS);
    }

    /**
     * Interactive menu
     */
    public void runInteractiveMode(String base64_secret) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔐 JWT TOKEN GENERATOR - Interactive Mode");
            System.out.println("=".repeat(60));
            System.out.println("1. Generate simple token (username only)");
            System.out.println("2. Generate token with custom expiration");
            System.out.println("3. Generate token with custom claims");
            System.out.println("4. Validate existing token");
            System.out.println("5. Show configuration");
            System.out.println("6. Exit");
            System.out.print("\nChoose an option (1-6): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        generateSimpleToken(scanner);
                        break;
                    case 2:
                        generateTokenWithExpiration(scanner);
                        break;
                    case 3:
                        generateTokenWithCustomClaims(scanner);
                        break;
                    case 4:
                        validateExistingToken(scanner);
                        break;
                    case 5:
                        displayConfig(base64_secret);
                        break;
                    case 6:
                        System.out.println("\n👋 Goodbye!");
                        return;
                    default:
                        System.out.println("❌ Invalid option. Please choose 1-6.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Please enter a valid number.");
            }
        }
    }

    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private void generateSimpleToken(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("❌ Username cannot be empty.");
            return;
        }

        String token = generateToken(username);
        System.out.println("\n✅ Token Generated Successfully!");
        System.out.println("🎫 Token: " + token);
        System.out.println("\n🧪 Test Command:");
        System.out.println("curl -H \"Authorization: Bearer " + token + "\" http://localhost:8080/api/protected");

        displayTokenInfo(token);
    }

    private void generateTokenWithExpiration(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("❌ Username cannot be empty.");
            return;
        }

        System.out.print("Enter expiration time in hours (default 24): ");
        String expInput = scanner.nextLine().trim();

        long expirationSeconds = DEFAULT_EXPIRATION_SECONDS;
        if (!expInput.isEmpty()) {
            try {
                int hours = Integer.parseInt(expInput);
                expirationSeconds = hours * 3600L;
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Invalid expiration, using default (24 hours)");
            }
        }

        String token = generateToken(username, expirationSeconds);
        System.out.println("\n✅ Token Generated Successfully!");
        System.out.println("🎫 Token: " + token);

        displayTokenInfo(token);
    }

    private void generateTokenWithCustomClaims(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("❌ Username cannot be empty.");
            return;
        }

        Map<String, Object> claims = new HashMap<>();

        System.out.println("Enter custom claims (press Enter to finish):");
        while (true) {
            System.out.print("Claim key (or Enter to finish): ");
            String key = scanner.nextLine().trim();
            if (key.isEmpty()) break;

            System.out.print("Claim value: ");
            String value = scanner.nextLine().trim();
            claims.put(key, value);
        }

        String token = generateTokenWithClaims(username, claims, DEFAULT_EXPIRATION_SECONDS);
        System.out.println("\n✅ Token Generated Successfully!");
        System.out.println("🎫 Token: " + token);

        displayTokenInfo(token);
    }

    private void validateExistingToken(Scanner scanner) {
        System.out.print("Enter JWT token to validate: ");
        String token = scanner.nextLine().trim();

        if (token.isEmpty()) {
            System.out.println("❌ Token cannot be empty.");
            return;
        }

        displayTokenInfo(token);
    }

    /**
     * Main method - Entry point
     */
    public static void main(String[] args) {


        String base64_secret = "hHSScWk9zwVfwesvQjE5ItOqoEZTBqV7UVuFbrqnXPE=";//generateSecret();
        JWTTokenGenerator generator = new JWTTokenGenerator(base64_secret);

        System.out.println("🔐 JWT Token Generator - Java Edition");
        System.out.println("Using secret: " + base64_secret);
        /*
        if (args.length == 1) {
            // Interactive mode
            generator.runInteractiveMode(base64_secret);
        } else {

         */
            // Command line mode
            String username = "admin";
            long expiration = DEFAULT_EXPIRATION_SECONDS;

            /*
            if (args.length >= 3) {
                try {
                    int hours = Integer.parseInt(args[2]);
                    expiration = hours * 3600L;
                } catch (NumberFormatException e) {
                    System.out.println("⚠️  Invalid expiration, using default");
                }
            }

             */

            String token = generator.generateToken(username, expiration);
            System.out.println("\n✅ Generated Token for '" + username + "':");
            System.out.println(token);

            generator.displayTokenInfo(token);

            System.out.println("\n🧪 Test Commands:");
            System.out.println("curl -H \"Authorization: Bearer " + token + "\" http://localhost:8080/api/protected");
       // }
    }
}